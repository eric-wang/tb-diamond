/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.client.impl;

import java.security.SecureRandom;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.client.task.PublishTask;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.notify.utils.task.Task;
import com.taobao.diamond.notify.utils.task.TaskProcessor;
import com.taobao.diamond.utils.ContentUtils;


/**
 * ��������������������, ���Դ���ͬ�����첽������
 * 
 * @author leiwen.zh
 * 
 */
public class PublishTaskProcessor implements TaskProcessor {
    private static final Log log = LogFactory.getLog(PublishTaskProcessor.class);

    private DefaultDiamondPublisher diamondPublisher;
    private DefaultDiamondSubscriber diamondSubscriber;

    private final SecureRandom random = new SecureRandom();


    public void setDiamondPublisher(DefaultDiamondPublisher diamondPublisher) {
        this.diamondPublisher = diamondPublisher;
    }


    public void setDiamondSubscriber(DefaultDiamondSubscriber diamondSubscriber) {
        this.diamondSubscriber = diamondSubscriber;
    }


    /**
     * �첽����
     */
    public boolean process(String taskType, Task task) {
        return processPublish((PublishTask) task);
    }


    /**
     * ͬ������
     * 
     * @param dataId
     * @param group
     * @param content
     * @param timeout
     * @return
     */
    public boolean syncProcess(String dataId, String group, String content, long timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("���Ϸ��ĳ�ʱʱ��, ��ʱʱ��������0");
        }

        while (timeout > 0) {
            try {
                this.diamondPublisher.publishUpdate(dataId, group, content);
                return true;
            }
            catch (Exception e) {
                log.error("ͬ���������ݳ���, dataId=" + dataId + ",group=" + group + ",content=" + content + ",timeout="
                        + timeout + "ms", e);
                // ���¼���timeout
                timeout = timeout - Constants.ONCE_TIMEOUT;
                // �л�����һ̨server����
                this.diamondPublisher.rotateToNextDomain();
            }
        }

        log.error("ͬ���������ݳ�ʱ, dataId=" + dataId + ",group=" + group + ",content=" + content + ",timeout=" + timeout
                + "ms");
        return false;
    }


    private boolean processPublish(PublishTask publishTask) {
        int count = publishTask.getCount();
        int waitTime = getWaitTime(++count);
        log.info("�������ݣ���" + count + "�γ���");
        publishTask.setCount(count);
        String dataId = publishTask.getDataId();
        String group = publishTask.getGroup();

        Iterator<String> it = publishTask.getContents().iterator();
        while (it.hasNext()) {
            String fullConfig = it.next();
            String config = ContentUtils.getContent(fullConfig);
            if (innerPublish(publishTask, waitTime, dataId, group, config, fullConfig)) {
                it.remove();
            }
            else {
                return false;
            }
        }
        return true;
    }


    private boolean innerPublish(PublishTask publishTask, int waitTime, String dataId, String group, String config,
            String fullConfig) {
        try {
            // ����ǰ����dataId��ȡ���ݣ������Ƿ���Ҫ����
            String receivedConfigInfo = diamondSubscriber.getConfigureInfomation(dataId, group, 60000);

            if (receivedConfigInfo == null) {
                log.info("����ȫ������: dataId=" + dataId + ", group=" + group + ", configInfo=" + config);
                diamondPublisher.publishNew(dataId, group, config);
                // �����ɹ�
                return true;
            }
            else if (!receivedConfigInfo.contains(config)) {
                log.info("������������: dataId=" + dataId + ", group=" + group + ", configInfo=" + fullConfig);
                diamondPublisher.publishUpdate(dataId, group, fullConfig);
                // ���³ɹ�
                return true;
            }
            else {
                log.info("����Ҫ����: dataId=" + dataId + ", group=" + group);
                return true;
            }
        }
        catch (Exception e) {
            log.error("�������ݳ����쳣: dataId=" + dataId + ", group=" + group, e);
            publishTask.setTaskInterval(waitTime * 1000);
            this.diamondPublisher.rotateToNextDomain();
        }
        return false;
    }


    private int getWaitTime(int count) {
        if (count > 0 && count <= 5) {
            return random.nextInt(30) + 1;
        }
        else {
            return random.nextInt(30) + 31;
        }
    }

}
