/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.server.listener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.util.RemotingUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.exception.NotifyRemotingException;


public class ConnectionNumberListener implements ConnectionLifeCycleListener {

    private static final Log log = LogFactory.getLog(ConnectionNumberListener.class);

    // ��ͬ��IP��������������ֵ
    private int connThreshold;
    // IP������ֵ
    private int ipCountThreshold;
    // ���IP����������ִ�м��, ��λ��
    private int ipCheckTaskInterval;
    // IP������û�����
    private volatile boolean isOverflow;

    // ����������ӵ�pushit-server��Զ��IP, �Լ�����
    private ConcurrentHashMap<String, AtomicInteger> connectionIpNumMap =
            new ConcurrentHashMap<String, AtomicInteger>();

    private Lock lock = new ReentrantLock();

    private ScheduledExecutorService scheduler;


    public ConnectionNumberListener(int connThreshold, int ipCountThreshold, int ipCheckTaskInterval) {
        this.connThreshold = connThreshold;
        this.ipCountThreshold = ipCountThreshold;
        this.ipCheckTaskInterval = ipCheckTaskInterval;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("connection num control thread");
                t.setDaemon(true);
                return t;
            }
        });

        this.scheduler.scheduleAtFixedRate(new Runnable() {

            public void run() {
                int ipCount = ConnectionNumberListener.this.connectionIpNumMap.size();
                if (ipCount >= ConnectionNumberListener.this.ipCountThreshold) {
                    log.warn("IP����������ֵ, ���ٽ��ܸ���IP������, ��ǰIP��=" + ipCount + ", ��ֵ="
                            + ConnectionNumberListener.this.ipCountThreshold);
                    isOverflow = true;
                }
                else {
                    isOverflow = false;
                }
            }

        }, this.ipCheckTaskInterval, this.ipCheckTaskInterval, TimeUnit.SECONDS);
    }


    /**
     * ���Ӵ���ʱ, ����������
     */
    public void onConnectionCreated(Connection conn) {

        // ��ȡԶ��IP
        String remoteIp = this.getRemoteIp(conn);

        try {
            // ����Զ��IP��server�˵�������, �����Ƿ�ܾ�����
            AtomicInteger connNum = this.connectionIpNumMap.get(remoteIp);
            if (connNum == null) {
                AtomicInteger newConnNum = new AtomicInteger(0);
                AtomicInteger oldConnNum = this.connectionIpNumMap.putIfAbsent(remoteIp, newConnNum);
                if (oldConnNum != null) {
                    connNum = oldConnNum;
                }
                else {
                    connNum = newConnNum;
                }
            }

            connNum.incrementAndGet();
            // ������ֵ, �ܾ�����
            if (isOverflow || connNum.get() > this.connThreshold) {
                // ���ɶ�ɱһǧ�����ɷŹ�һ��������
                log.warn("��pushit-server��������������ֵ, �ܾ�����, ��ǰ������:" + connNum.get() + ",��ֵ:" + this.connThreshold);
                conn.close(false);
            }
        }
        catch (NotifyRemotingException e) {
            log.error("�ر����Ӵ���, remoteIp=" + remoteIp, e);
        }
        catch (Exception e) {
            log.error("��������, remoteIp=" + remoteIp, e);
        }

    }


    public void onConnectionReady(Connection conn) {

    }


    public void onConnectionClosed(Connection conn) {
        String remoteIp = null;
        try {
            // ��ȡԶ��IP
            remoteIp = this.getRemoteIp(conn);
            // �ر����Ӻ����������
            lock.lock();
            AtomicInteger connNum = this.connectionIpNumMap.get(remoteIp);
            if (connNum == null) {
                return;
            }
            if (connNum.decrementAndGet() <= 0) {
                this.connectionIpNumMap.remove(remoteIp);
            }
        }
        finally {
            lock.unlock();
        }
    }


    private String getRemoteIp(Connection connection) {
        // ��ȡԶ��IP
        String remoteAddr = RemotingUtils.getAddrString(connection.getRemoteSocketAddress());
        String remoteIp = null;
        if (remoteAddr.indexOf(":") == -1) {
            remoteIp = remoteAddr;
        }
        else {
            remoteIp = remoteAddr.substring(0, remoteAddr.indexOf(":"));
        }
        return remoteIp;
    }

}
