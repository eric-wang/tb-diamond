/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.client;

import com.taobao.pushit.client.PushitClient;


/**
 * �����߽ӿ�
 * 
 * @author leiwen
 * 
 */
public interface DiamondPublisher {

    /**
     * ���ü�Ⱥ��ʶ
     * 
     * @param clusterType
     */
    void setClusterType(String clusterType);


    /**
     * ������������, ʹ��Ĭ�ϵ�pattern
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void publish(String dataId, String group, String configInfo);


    /**
     * �����������ݣ�����pattern��ȡcontent��Ψһ�Ա�ʶ
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param pattern
     */
    void publish(String dataId, String group, String configInfo, ContentIdentityPattern pattern);


    /**
     * ͬ��������������, ʹ��Ĭ�ϵ�pattern
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncPublish(String dataId, String group, String configInfo, long timeout);


    /**
     * ͬ��������������, ����pattern��ȡcontent��Ψһ�Ա�ʶ
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param pattern
     * @return
     */
    boolean syncPublish(String dataId, String group, String configInfo, long timeout, ContentIdentityPattern pattern);


    /**
     * ɾ������
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void unpublish(String dataId, String group, String configInfo);

    
    /**
     * ͬ��ɾ������
     * @param dataId
     * @param group
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncUnpublish(String dataId, String group, String configInfo, long timeout);

    /**
     * ����ȫ������
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void publishAll(String dataId, String group, String configInfo);
    
    
    /**
     * ͬ������ȫ������
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncPublishAll(String dataId, String group, String configInfo, long timeout);


    /**
     * ��ȡdiamond����
     * 
     * @return
     */
    DiamondConfigure getDiamondConfigure();


    /**
     * ��ȡʵʱ֪ͨ�ͻ���
     * 
     * @return
     */
    PushitClient getPushitClient();


    /**
     * ����diamond����
     * 
     * @param diamondConfigure
     */
    void setDiamondConfigure(DiamondConfigure diamondConfigure);


    /**
     * 
     */
    void setDiamondSubscriber(DiamondSubscriber diamondSubscriber);


    /**
     * ����������
     */
    void start();


    /**
     * �رշ�����
     */
    void close();


    /**
     * ��������������dataId������
     * 
     * @param dataId
     * @param group
     */
    void addDataId(String dataId, String group, String configInfo);


    /**
     * 
     * @param dataId
     * @param group
     */
    void removeDataId(String dataId, String group);


    /**
     * �ȴ������������
     * 
     * @throws InterruptedException
     */
    void awaitPublishFinish() throws InterruptedException;


    void scheduledReport();
}
