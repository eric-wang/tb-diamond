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

import java.util.List;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.manager.BaseStoneSubManager;
import com.taobao.diamond.manager.ManagerListener;


/**
 * �������Ĺ����ߣ�һ��JVM�У�һ��dataIdֻ�ܶ�Ӧһ��������
 * 
 * @author leiwen
 * 
 */
public class DefaultBaseStoneSubManager extends DefaultDiamondManager implements BaseStoneSubManager {

    /**
     * �������ݵĹ��췽����ָ����Ⱥ���ͣ�ָ����������ָ���Ƿ�ʹ��ʵʱ֪ͨ
     * 
     * @param subDataId
     * @param subGroup
     * @param clusterType
     * @param managerListener
     * @param useRealTimeNotification
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, String clusterType,
            ManagerListener managerListener, boolean useRealTimeNotification) {
        super(subGroup, subDataId, managerListener, useRealTimeNotification, clusterType);
    }


    /**
     * �������ݵĹ��췽����ָ����Ⱥ���ͣ�ָ����������ʹ��ʵʱ֪ͨ
     * 
     * @param subDataId
     * @param subGroup
     * @param clusterType
     * @param managerListener
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, String clusterType,
            ManagerListener managerListener) {
        super(subGroup, subDataId, managerListener, clusterType);
    }


    /**
     * �������ݵĹ��췽����ʹ��Ĭ�ϵļ�Ⱥ����basestone��ָ����������ʹ��ʵʱ֪ͨ
     * 
     * @param subDataId
     * @param subGroup
     * @param managerListener
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, ManagerListener managerListener) {
        super(subGroup, subDataId, managerListener, Constants.DEFAULT_BASESTONE_CLUSTER);
    }


    /**
     * �������ݵĹ��췽����ʹ��Ĭ�ϵļ�Ⱥ����basestone��ָ����������ָ���Ƿ�ʹ��ʵʱ֪ͨ
     * 
     * @param subDataId
     * @param subGroup
     * @param managerListener
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, ManagerListener managerListener,
            boolean useRealTimeNotification) {
        super(subGroup, subDataId, managerListener, useRealTimeNotification, Constants.DEFAULT_BASESTONE_CLUSTER);
    }


    /**
     * �������ݵĹ��췽����ָ����Ⱥ���ͣ�ָ���������б�ָ���Ƿ�ʹ��ʵʱ֪ͨ
     * 
     * @param subDataId
     * @param subGroup
     * @param clusterType
     * @param managerListenerList
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, String clusterType,
            List<ManagerListener> managerListenerList, boolean useRealTimeNotification) {
        super(subGroup, subDataId, managerListenerList, useRealTimeNotification, clusterType);
    }


    /**
     * �������ݵĹ��췽����ָ����Ⱥ���ͣ�ָ���������б�ʹ��ʵʱ֪ͨ
     * 
     * @param subDataId
     * @param subGroup
     * @param clusterType
     * @param managerListenerList
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, String clusterType,
            List<ManagerListener> managerListenerList) {
        super(subGroup, subDataId, managerListenerList, clusterType);
    }


    /**
     * �������ݵĹ��췽����ʹ��Ĭ�ϵļ�Ⱥ����basestone��ָ���������б�ʹ��ʵʱ֪ͨ
     * 
     * @param subDataId
     * @param subGroup
     * @param managerListenerList
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, List<ManagerListener> managerListenerList) {
        super(subGroup, subDataId, managerListenerList, Constants.DEFAULT_BASESTONE_CLUSTER);
    }


    /**
     * �������ݵĹ��췽����ʹ��Ĭ�ϵļ�Ⱥ����basestone��ָ���������б�ָ���Ƿ�ʹ��ʵʱ֪ͨ
     * 
     * @param subDataId
     * @param subGroup
     * @param managerListenerList
     */
    public DefaultBaseStoneSubManager(String subDataId, String subGroup, List<ManagerListener> managerListenerList,
            boolean useRealTimeNotification) {
        super(subGroup, subDataId, managerListenerList, useRealTimeNotification, Constants.DEFAULT_BASESTONE_CLUSTER);
    }

}
