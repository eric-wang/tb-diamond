/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.manager;

import com.taobao.diamond.client.ContentIdentityPattern;
import com.taobao.diamond.client.DiamondConfigure;


/**
 * ���ط����߽ӿڣ�һ��dataIdֻ�ܶ�Ӧһ��������
 * 
 * @author leiwen
 * 
 */
public interface BaseStonePubManager {

    /**
     * �첽��������
     * 
     * @param configInfo
     *            ������������
     */
    void publish(String configInfo);


    /**
     * �첽�������ݣ�ָ��dataId��ʹ��Ĭ�Ϸ��飬���dataIdΪnull����ʹ�ù���ʱ�����dataId
     * 
     * @param dataId
     * @param configInfo
     */
    void publish(String dataId, String configInfo);


    /**
     * �첽�������ݣ�ָ��dataId��group�����dataId��groupΪnull����ʹ�ù���ʱ�����dataId��group
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void publish(String dataId, String group, String configInfo);


    /**
     * �첽��������, ָ��dataId��group, ָ������Ψһ��ʶ��parrern
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param pattern
     */
    void publish(String dataId, String group, String configInfo, ContentIdentityPattern pattern);


    /**
     * ͬ����������
     * 
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncPublish(String configInfo, long timeout);


    /**
     * ͬ����������, ָ��dataId, ʹ��Ĭ�Ϸ��飬���dataIdΪnull����ʹ�ù���ʱ�����dataId
     * 
     * @param dataId
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncPublish(String dataId, String configInfo, long timeout);


    /**
     * ͬ���������ݣ�ָ��dataId��group�����dataId��groupΪnull����ʹ�ù���ʱ�����dataId��group
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param timeout
     * @return
     */
    boolean syncPublish(String dataId, String group, String configInfo, long timeout);


    /**
     * ͬ����������, ָ��dataId��group, ָ������Ψһ��ʶ��parrern
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @param timeout
     * @param pattern
     * @return
     */
    boolean syncPublish(String dataId, String group, String configInfo, long timeout, ContentIdentityPattern pattern);


    /**
     * ɾ���������ã�����ɾ���������ݣ���ʹ�ù���ʱ�����dataId��group
     * 
     * @param configInfo
     */
    void unpublish(String configInfo);


    /**
     * ɾ���������ã�����ɾ���������ݣ�
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void unpublish(String dataId, String group, String configInfo);


    /**
     * ɾ���������ã�����ɾ���������ݣ���ʹ��Ĭ�Ϸ���
     * 
     * @param dataId
     * @param configInfo
     */
    void unpublish(String dataId, String configInfo);


    /**
     * ͬ��ɾ����������, ʹ��Ĭ�Ϸ���
     * 
     * @param dataId
     * @param configInfo
     * @return
     */
    boolean syncUnpublish(String dataId, String configInfo, long timeout);


    /**
     * ͬ��ɾ����������
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @return
     */
    boolean syncUnpublish(String dataId, String group, String configInfo, long timeout);


    /**
     * ����ȫ������, �÷����Ὣ�Ѿ����ڵ�����ȫ������, ����
     * 
     * @param configInfo
     */
    void publishAll(String configInfo);


    /**
     * ����ȫ������, ָ��dataId��group, �÷����Ὣ�Ѿ����ڵ�����ȫ������, ����
     * 
     * @param dataId
     * @param group
     * @param configInfo
     */
    void publishAll(String dataId, String group, String configInfo);


    /**
     * ����ȫ������, ʹ��Ĭ�Ϸ���, �÷����Ὣ�Ѿ����ڵ�����ȫ������, ����
     * 
     * @param dataId
     * @param configInfo
     */
    void publishAll(String dataId, String configInfo);


    /**
     * ͬ������ȫ������, ʹ�ù���ʱ�����dataId��group, �÷����Ὣ�Ѿ����ڵ�����ȫ������, ����
     * 
     * @param configInfo
     * @return
     */
    boolean syncPublishAll(String configInfo, long timeout);


    /**
     * ͬ������ȫ������, ʹ��Ĭ�Ϸ���, �÷����Ὣ�Ѿ����ڵ�����ȫ������, ����
     * 
     * @param dataId
     * @param configInfo
     * @return
     */
    boolean syncPublishAll(String dataId, String configInfo, long timeout);


    /**
     * ͬ������ȫ������, �÷����Ὣ�Ѿ����ڵ�����ȫ������, ����
     * 
     * @param dataId
     * @param group
     * @param configInfo
     * @return
     */
    boolean syncPublishAll(String dataId, String group, String configInfo, long timeout);


    /**
     * ��ȡ������ص�����
     * 
     * @return
     */
    DiamondConfigure getDiamondConfigure();


    /**
     * ���÷�����ص�����
     * 
     * @param diamondConfigure
     */
    void setDiamondConfigure(DiamondConfigure diamondConfigure);


    /**
     * �رշ�����
     */
    // void close();

    /**
     * �ȴ��������
     */
    void awaitPublishFinish() throws InterruptedException;


    /**
     * �����Եı����Լ���ip�ͷ�����dataId, ��������ַʱ����ͨ���˷�����ʵ�ֻ�������ʱ��ַ���Զ�ɾ��
     */
    void scheduledReport();
}
