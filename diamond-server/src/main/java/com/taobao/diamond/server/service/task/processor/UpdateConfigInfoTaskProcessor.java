/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service.task.processor;

import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.notify.utils.task.Task;
import com.taobao.diamond.notify.utils.task.TaskProcessor;
import com.taobao.diamond.server.service.AggregationService;
import com.taobao.diamond.server.service.ConfigService;
import com.taobao.diamond.server.service.TaskManagerService;
import com.taobao.diamond.server.service.task.ConfigInfoTask;
import com.taobao.diamond.server.service.task.RealTimeNotifyTask;
import com.taobao.diamond.server.utils.DiamondServerConstants;
import com.taobao.diamond.utils.ContentUtils;


public class UpdateConfigInfoTaskProcessor implements TaskProcessor, UpdateConfigInfoTaskProcessorMBean {
    private static final Log log = LogFactory.getLog(UpdateConfigInfoTaskProcessor.class);
    private static final Log failCountLog = LogFactory.getLog("failLog");

    private ConfigService configService;

    private AggregationService aggrService;

    private TaskManagerService taskManagerService;

    private RedisTaskProcessor redisTaskProcessor;

    private RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor;

    private static long startTime;
    private AtomicLong lastUpdateTime = new AtomicLong(0);


    public ConfigService getConfigService() {
        return configService;
    }


    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }


    public AggregationService getAggrService() {
        return aggrService;
    }


    public void setAggrService(AggregationService aggrService) {
        this.aggrService = aggrService;
    }


    public TaskManagerService getTaskManagerService() {
        return taskManagerService;
    }


    public void setTaskManagerService(TaskManagerService taskManagerService) {
        this.taskManagerService = taskManagerService;
    }


    public RedisTaskProcessor getRedisTaskProcessor() {
        return redisTaskProcessor;
    }


    public void setRedisTaskProcessor(RedisTaskProcessor redisTaskProcessor) {
        this.redisTaskProcessor = redisTaskProcessor;
    }


    public RealTimeNotifyTaskProcessor getRealTimeNotifyTaskProcessor() {
        return realTimeNotifyTaskProcessor;
    }


    public void setRealTimeNotifyTaskProcessor(RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor) {
        this.realTimeNotifyTaskProcessor = realTimeNotifyTaskProcessor;
    }


    public void init() {
        try {
            ObjectName oName =
                    new ObjectName(UpdateConfigInfoTaskProcessor.class.getPackage().getName() + ":type="
                            + UpdateConfigInfoTaskProcessor.class.getSimpleName());
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, oName);
        }
        catch (Exception e) {
            log.error("ע��mbean����", e);
        }
    }


    public boolean process(String taskType, Task task) {
        LazyHolder.init();

        ConfigInfoTask updateConfigInfoTask = (ConfigInfoTask) task;
        String dataId = updateConfigInfoTask.getDataId();
        String group = updateConfigInfoTask.getGroup();
        Iterator<String> it = updateConfigInfoTask.getContents().iterator();

        while (it.hasNext()) {
            String content = it.next();
            int failCount = updateConfigInfoTask.getFailCount();
            if (innerProcess(dataId, group, content)) {
                // �������ø��³ɹ�, �����list���Ƴ�, ����������һ������
                log.info("���³ɹ�, dataId=" + dataId + ", group=" + group + ", content=" + content);
                it.remove();
                updateConfigInfoTask.setFailCount(0);
            }
            else {
                // �������ø���ʧ��, ������������
                log.warn("����ʧ��, dataId=" + dataId + ", group=" + group + ", content=" + content);
                failCount++;
                updateConfigInfoTask.setFailCount(failCount);
                if (failCount >= DiamondServerConstants.MAX_UPDATE_FAIL_COUNT) {
                    // ��ʧ�ܴ����ﵽһ��ֵʱ, ��¼һ����־, ���������ݴ�list�Ƴ�
                    failCountLog.info("��������ʧ�ܴ����ﵽ����: dataId=" + dataId + ", group=" + group + ", content=" + content);
                    it.remove();
                    updateConfigInfoTask.setFailCount(0);
                }
                return false;
            }
        }

        // ȫ�����ø��³ɹ�, ��¼���θ���ʱ��
        lastUpdateTime.set(System.currentTimeMillis());
        updateConfigInfoTask.setFailCount(0);

        try {
            this.aggrService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("���ݾۺϳ���, dataId=" + dataId + "group=" + group, e);
        }

        try {
            this.realTimeNotify(dataId, group);
        }
        catch (Exception e) {
            log.error("ʵʱ֪ͨ����, dataId=" + dataId + "group=" + group, e);
        }

        return true;
    }


    private boolean innerProcess(String dataId, String group, String content) {
        boolean result = false;
        try {
            // ��ȡ���ݱ�ʶ
            String contentIdentity = ContentUtils.getContentIdentity(content);
            // ��ȡ����������
            String realContent = ContentUtils.getContent(content);

            ConfigInfo oldConfigInfo = this.configService.findConfigInfo(dataId, group);
            if (oldConfigInfo == null) {
                // ���²���ת��Ϊ��������
                this.configService.addConfigInfo(dataId, group, realContent);
                log.info("����ת��Ϊ�����ɹ�, dataId=" + dataId + ", group=" + group + ", content=" + realContent);
                result = true;
            }
            else {
                String oldContent = oldConfigInfo.getContent();
                String oldMd5 = oldConfigInfo.getMd5();
                if (oldContent.contains(contentIdentity)) {
                    log.info("Ҫ���µ������Ѿ�����, ɾ��ԭ����, ����Ϊ���µ�, dataId=" + dataId + ", group=" + group + ", content="
                            + realContent);
                    String newContent = generateNewContentByIdentity(oldContent, realContent, contentIdentity);
                    result = this.configService.updateConfigInfoByMd5(dataId, group, newContent, oldMd5);
                }
                else {
                    String newContent = generateNewContent(oldContent, realContent);
                    result = this.configService.updateConfigInfoByMd5(dataId, group, newContent, oldMd5);
                }
            }

            return result;
        }
        catch (Exception e) {
            log.error("����������Ϣ���ִ���dataId=" + dataId + ", group=" + group + ", content=" + content, e);
            return false;
        }
    }


    private String generateNewContent(String oldContent, String appendant) {
        StringBuilder sb = new StringBuilder(oldContent);
        sb.append(Constants.DIAMOND_LINE_SEPARATOR);
        sb.append(appendant);
        return sb.toString();
    }


    @SuppressWarnings("unchecked")
    private String generateNewContentByIdentity(String oldContent, String appendant, String appendantIdentity) {
        StringBuilder sb = new StringBuilder();
        StringReader reader = null;
        try {
            reader = new StringReader(oldContent);
            List<String> lines = IOUtils.readLines(reader);
            for (String line : lines) {
                if (!line.contains(appendantIdentity)) {
                    sb.append(line);
                    sb.append(Constants.DIAMOND_LINE_SEPARATOR);
                }
            }
            sb.append(appendant);

            return sb.toString();
        }
        catch (Exception e) {
            log.error("���ݱ�ʶ���ɴ����µ�����ʱ����", e);
            return oldContent;
        }
        finally {
            reader.close();
        }
    }


    private void realTimeNotify(String dataId, String group) {
        String taskType = dataId + "-" + group + "-pushit";
        RealTimeNotifyTask task = new RealTimeNotifyTask(dataId, group);
        task.setLastProcessTime(System.currentTimeMillis());
        task.setTaskInterval(2000L);
        this.taskManagerService.addPushitProcessor(taskType, realTimeNotifyTaskProcessor);
        this.taskManagerService.addPushitTask(taskType, task, false);
    }


    public long getCurrentTotalUpdateTime() {
        return lastUpdateTime.addAndGet(-startTime);
        // return lastUpdateTime.get() - startTime;
    }

    private static class LazyHolder {
        static {
            UpdateConfigInfoTaskProcessor.startTime = System.currentTimeMillis();
        }


        private LazyHolder() {
        }


        public static void init() {
        }
    }

}
