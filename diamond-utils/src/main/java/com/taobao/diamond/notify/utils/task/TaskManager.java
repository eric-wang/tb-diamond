/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.notify.utils.task;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.ObjectName;

import org.apache.log4j.Logger;

import com.taobao.diamond.common.Constants;


/**
 * ���ڴ���һ��Ҫִ�гɹ������� ���̵߳ķ�ʽ�������񣬱�֤����һ�����ɹ�����
 * 
 * @author huali
 * 
 */
public final class TaskManager implements TaskManagerMBean {
    
    private static final Logger log = Logger.getLogger("TaskManager");

    private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<String, Task>();

    private final ConcurrentHashMap<String, TaskProcessor> taskProcessors =
            new ConcurrentHashMap<String, TaskProcessor>();

    private TaskProcessor defaultTaskProcessor;

    Thread processingThread;

    private volatile boolean empty;

    private final AtomicBoolean closed = new AtomicBoolean(true);
    
    private String name;

    class ProcessRunnable implements Runnable {

        public void run() {
            while (!TaskManager.this.closed.get()) {
                try {
                    Thread.sleep(100);
                    TaskManager.this.process();
                }
                catch (Throwable e) {
                }
            }

        }

    }

    ReentrantLock lock = new ReentrantLock();

    Condition notEmpty = this.lock.newCondition();


    public TaskManager() {
        this(null);
    }


    public Task getTask(String type) {
        return this.tasks.get(type);
    }


    public TaskProcessor getTaskProcessor(String type) {
        return this.taskProcessors.get(type);
    }


    public TaskManager(String name) {
        this.name = name;
        if (null != name && name.length() > 0) {
            this.processingThread = new Thread(new ProcessRunnable(), name);
        }
        else {
            this.processingThread = new Thread(new ProcessRunnable());
        }
        this.processingThread.setDaemon(true);
        this.closed.set(false);
        this.processingThread.start();
        this.empty = true;

    }


    public void close() {
        this.closed.set(true);
        this.processingThread.interrupt();
    }


    public void await() throws InterruptedException {
        this.lock.lock();
        try {
            while (!this.isEmpty()) {
                this.notEmpty.await();
            }
        }
        finally {
            this.lock.unlock();
        }
    }


    public void await(long timeout, TimeUnit unit) throws InterruptedException {
        this.lock.lock();
        try {
            while (!this.isEmpty()) {
                this.notEmpty.await(timeout, unit);
            }
        }
        finally {
            this.lock.unlock();
        }
    }


    public void addProcessor(String type, TaskProcessor taskProcessor) {
        this.taskProcessors.put(type, taskProcessor);
    }


    public void removeProcessor(String type) {
        this.taskProcessors.remove(type);
    }


    public void removeTask(String type) {
        this.lock.lock();
        try {
            this.tasks.remove(type);
        }
        finally {
            this.lock.unlock();
        }
    }


    public void addTask(String type, Task task) {
        this.addTask(type, task, false);
    }


    /**
     * ��������뵽����Map��
     * 
     * @param type
     * @param task
     * @param previousTask
     * */
    public void addTask(String type, Task task, boolean previousTask) {
        this.lock.lock();
        try {
            // ������Map�л�ȡ����
            Task curTask = this.tasks.get(type);
            if (null == curTask) {
                // Map��û������ֱ�����
                this.tasks.put(type, task);
            }
            else if (previousTask) {
                // ���Ҫ��ӵ���֮ǰ������(��Map��ȡ������ִ�е�)
                // ���������Map��������Merge
                this.tasks.put(type, task);
                task.merge(curTask);
            }
            else {
                // ֱ��Merge
                curTask.merge(task);
            }
            this.empty = false;
        }
        finally {
            this.lock.unlock();
        }
    }


    /**
     * 
     */
    protected void process() {
        for (Map.Entry<String, Task> entry : this.tasks.entrySet()) {
            Task task = null;
            this.lock.lock();
            try {
                // ��ȡ����
                task = entry.getValue();
                if (null != task) {
                    if (!task.shouldProcess()) {
                        // ����ǰ����Ҫ��ִ�У�ֱ������
                        continue;
                    }
                    // �Ƚ����������Map��ɾ��
                    this.tasks.remove(entry.getKey());
                }
            }
            finally {
                this.lock.unlock();
            }

            if (null != task) {
                // ��ȡ��������
                TaskProcessor processor = this.taskProcessors.get(entry.getKey());
                if (null == processor) {
                    // ���û�и��������������õĴ�������ʹ��Ĭ�ϴ�����
                    processor = this.getDefaultTaskProcessor();
                }
                if (null != processor) {
                    boolean result = false;
                    try {
                        // ��������
                        result = processor.process(entry.getKey(), task);
                    }
                    catch (Throwable t) {
                        log.error("����taskʧ��", t);
                    }
                    if (!result) {
                        // ������ʧ�ܣ����������ʱ��
                        task.setLastProcessTime(System.currentTimeMillis());

                        // ���������¼��뵽����Map��
                        this.addTask(entry.getKey(), task, true);
                    }
                }
            }
        }
        this.empty = this.tasks.isEmpty();
        if (this.empty) {
            this.lock.lock();
            try {
                this.notEmpty.signalAll();
            }
            finally {
                this.lock.unlock();
            }
        }
    }


    public boolean isEmpty() {
        return this.empty;
    }


    public TaskProcessor getDefaultTaskProcessor() {
        this.lock.lock();
        try {
            return this.defaultTaskProcessor;
        }
        finally {
            this.lock.unlock();
        }
    }


    public void setDefaultTaskProcessor(TaskProcessor defaultTaskProcessor) {
        this.lock.lock();
        try {
            this.defaultTaskProcessor = defaultTaskProcessor;
        }
        finally {
            this.lock.unlock();
        }
    }


    public String getTaskInfos() {
        StringBuilder sb = new StringBuilder();
        for(String taskType: this.taskProcessors.keySet()) {
            sb.append(taskType).append(":");
            Task task = this.tasks.get(taskType);
            if(task != null) {
                sb.append(new Date(task.getLastProcessTime()).toString());
            } else {
                sb.append("finished");
            }
            sb.append(Constants.DIAMOND_LINE_SEPARATOR);
        }
        
        return sb.toString();
    }
    
    
    public void init() {
        try {
            ObjectName oName = new ObjectName(this.name + ":type=" + TaskManager.class.getSimpleName());
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, oName);
        }
        catch (Exception e) {
            log.error("ע��mbean����", e);
        }
    }
}
