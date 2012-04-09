/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.manager.impl;

import com.taobao.diamond.client.ContentIdentityPattern;
import com.taobao.diamond.client.DiamondConfigure;
import com.taobao.diamond.client.DiamondPublisher;
import com.taobao.diamond.client.DiamondSubscriber;
import com.taobao.diamond.client.impl.DiamondClientFactory;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.manager.BaseStonePubManager;


public class DefaultBaseStonePubManager implements BaseStonePubManager {
    // private static final Log log =
    // LogFactory.getLog(DefaultBaseStonePubManager.class);

    private DiamondPublisher diamondPublisher = null;
    private DiamondSubscriber diamondSubscriber = null;

    // ������dataId��group
    private final String pubDataId;
    private final String pubGroup;


    /**
     * ָ����Ⱥ����
     * 
     * @param pubDataId
     * @param pubGroup
     * @param clusterType
     *            ��Ⱥ����
     */
    public DefaultBaseStonePubManager(String pubDataId, String pubGroup, String clusterType) {
        this.pubDataId = pubDataId;
        this.pubGroup = pubGroup;

        diamondPublisher = DiamondClientFactory.getSingletonDiamondPublisher(clusterType);
        diamondSubscriber = DiamondClientFactory.getSingletonDiamondSubscriber(clusterType);
        diamondPublisher.setDiamondSubscriber(diamondSubscriber);
        diamondPublisher.start();
        diamondSubscriber.start();
    }


    /**
     * ��ָ��dataId��group�Ĺ��췽�������ʹ�øù��췽������ʹ��ָ��dataId��group�ķ�������
     */
    public DefaultBaseStonePubManager() {
        this.pubDataId = null;
        this.pubGroup = null;

        diamondPublisher = DiamondClientFactory.getSingletonDiamondPublisher(Constants.DEFAULT_BASESTONE_CLUSTER);
        diamondSubscriber = DiamondClientFactory.getSingletonDiamondSubscriber(Constants.DEFAULT_BASESTONE_CLUSTER);
        diamondPublisher.setDiamondSubscriber(diamondSubscriber);
        diamondPublisher.start();
        diamondSubscriber.start();
    }


    /**
     * ʹ��Ĭ�ϵļ�Ⱥ����basestone
     * 
     * @param pubDataId
     * @param pubGroup
     */
    public DefaultBaseStonePubManager(String pubDataId, String pubGroup) {
        this(pubDataId, pubGroup, Constants.DEFAULT_BASESTONE_CLUSTER);
    }


    /**
     * ʹ��Ĭ�ϵļ�Ⱥ����basestone��ʹ��Ĭ�ϵķ���DEFAULT_GROUP
     * 
     * @param pubDataId
     */
    public DefaultBaseStonePubManager(String pubDataId) {
        this(pubDataId, Constants.DEFAULT_GROUP);
    }


    public void publish(String configInfo) {
        this.publish(pubDataId, pubGroup, configInfo);
    }


    public void publish(String dataId, String configInfo) {
        this.publish(dataId, Constants.DEFAULT_GROUP, configInfo);
    }


    public void publish(String dataId, String group, String configInfo) {
        if (dataId == null || group == null) {
            this.diamondPublisher.publish(pubDataId, pubGroup, configInfo);
        }
        else {
            this.diamondPublisher.publish(dataId, group, configInfo);
        }

    }


    public void publish(String dataId, String group, String configInfo, ContentIdentityPattern pattern) {
        if (dataId == null || group == null) {
            this.diamondPublisher.publish(pubDataId, pubGroup, configInfo, pattern);
        }
        else {
            this.diamondPublisher.publish(dataId, group, configInfo, pattern);
        }
    }


    public boolean syncPublish(String configInfo, long timeout) {
        return this.syncPublish(pubDataId, pubGroup, configInfo, timeout);
    }


    public boolean syncPublish(String dataId, String configInfo, long timeout) {
        return this.syncPublish(dataId, Constants.DEFAULT_GROUP, configInfo, timeout);
    }


    public boolean syncPublish(String dataId, String group, String configInfo, long timeout) {
        if (dataId == null || group == null) {
            return this.diamondPublisher.syncPublish(pubDataId, pubGroup, configInfo, timeout);
        }
        else {
            return this.diamondPublisher.syncPublish(dataId, group, configInfo, timeout);
        }
    }


    public boolean syncPublish(String dataId, String group, String configInfo, long timeout,
            ContentIdentityPattern pattern) {
        if (dataId == null || group == null) {
            return this.diamondPublisher.syncPublish(pubDataId, pubGroup, configInfo, timeout, pattern);
        }
        else {
            return this.diamondPublisher.syncPublish(dataId, group, configInfo, timeout, pattern);
        }
    }


    public void unpublish(final String configInfo) {
        this.unpublish(pubDataId, pubGroup, configInfo);
    }


    public void unpublish(String dataId, String configInfo) {
        this.unpublish(dataId, Constants.DEFAULT_GROUP, configInfo);
    }


    public void unpublish(final String dataId, final String group, final String configInfo) {
        if (dataId == null || group == null) {
            this.diamondPublisher.unpublish(pubDataId, pubGroup, configInfo);
        }
        else {
            this.diamondPublisher.unpublish(dataId, group, configInfo);
        }
    }


    public boolean syncUnpublish(String configInfo, long timeout) {
        return this.syncUnpublish(pubDataId, pubGroup, configInfo, timeout);
    }


    public boolean syncUnpublish(String dataId, String configInfo, long timeout) {
        return this.syncUnpublish(dataId, Constants.DEFAULT_GROUP, configInfo, timeout);
    }


    public boolean syncUnpublish(String dataId, String group, String configInfo, long timeout) {
        if (dataId == null || group == null) {
            return this.diamondPublisher.syncUnpublish(pubDataId, pubGroup, configInfo, timeout);
        }
        else {
            return this.diamondPublisher.syncUnpublish(dataId, group, configInfo, timeout);
        }
    }


    public void publishAll(String configInfo) {
        this.publishAll(pubDataId, pubGroup, configInfo);
    }


    public void publishAll(String dataId, String group, String configInfo) {
        if (dataId == null || group == null) {
            this.diamondPublisher.publishAll(pubDataId, pubGroup, configInfo);
        }
        else {
            this.diamondPublisher.publishAll(dataId, group, configInfo);
        }
    }


    public void publishAll(String dataId, String configInfo) {
        this.publishAll(dataId, Constants.DEFAULT_GROUP, configInfo);
    }


    public boolean syncPublishAll(String configInfo, long timeout) {
        return this.syncPublishAll(pubDataId, pubGroup, configInfo, timeout);
    }


    public boolean syncPublishAll(String dataId, String configInfo, long timeout) {
        return this.syncPublishAll(dataId, Constants.DEFAULT_GROUP, configInfo, timeout);
    }


    public boolean syncPublishAll(String dataId, String group, String configInfo, long timeout) {
        if (dataId == null || group == null) {
            return this.diamondPublisher.syncPublishAll(pubDataId, pubGroup, configInfo, timeout);
        }
        else {
            return this.diamondPublisher.syncPublishAll(dataId, group, configInfo, timeout);
        }
    }


    public DiamondConfigure getDiamondConfigure() {
        return this.diamondPublisher.getDiamondConfigure();
    }


    public void setDiamondConfigure(DiamondConfigure diamondConfigure) {
        this.diamondPublisher.setDiamondConfigure(diamondConfigure);
    }


    public void awaitPublishFinish() throws InterruptedException {
        this.diamondPublisher.awaitPublishFinish();
    }


    public void scheduledReport() {
        this.diamondPublisher.scheduledReport();
    }

}
