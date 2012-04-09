/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.sdkapi;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.taobao.diamond.domain.BatchContextResult;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.ContextResult;
import com.taobao.diamond.domain.DiamondSDKConf;
import com.taobao.diamond.domain.PageContextResult;


/**
 * ����SDK���⿪�ŵ����ݷ��ʽӿ�
 * 
 * @filename DiamondSDKManager.java
 * @author libinbin.pt
 * @datetime 2010-7-16 ����04:03:28
 * 
 *           {@link #exists(String, String, String)}
 */
public interface DiamondSDKManager {

    /**
     * �õ�diamondSDKConfMaps���ü���map
     * 
     * @return Map<String, DiamondSDKConf>
     */
    public Map<String, DiamondSDKConf> getDiamondSDKConfMaps();


    // /////////////////////////////////////////�������ݽӿڶ���////////////////////////////////////////
    /**
     * ʹ��ָ����diamond����������
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @return ContextResult ��������
     */
    public ContextResult pulish(String dataId, String groupName, String context, String serverId);


    /**
     * ʹ���׸����õ� diamond����������
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult ��������
     */
    public ContextResult pulishFromDefaultServer(String dataId, String groupName, String context);


    /**
     * ʹ��ָ����diamond���ͷ�����Ϣ
     * 
     * @param address
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult ��������
     */
    public ContextResult publishGroup(String address, String dataId, String groupName, String serverId);


    // /////////////////////////////////////////�����޸ĺ�����ݽӿڶ���////////////////////////////////////////
    /**
     * ʹ��ָ����diamond�������޸ĺ������,�޸�ǰ�ȼ�����ݴ�����
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @return ContextResult ��������
     */
    public ContextResult pulishAfterModified(String dataId, String groupName, String context, String serverId);


    /**
     * ʹ���׸����õ� diamond�������޸ĺ������,�޸�ǰ�ȼ�����ݴ�����
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult ��������
     */
    public ContextResult pulishFromDefaultServerAfterModified(String dataId, String groupName, String context);


    /**
     * ����id���޸ķ�����Ϣ
     * 
     * @param id
     * @param groupName
     * @param serverId
     * @return ContextResult ��������
     */
    public ContextResult moveGroup(long id, String groupName, String serverId);


    // /////////////////////////////////////////ģ����ѯ�ӿڶ���////////////////////////////////////////
    /**
     * ����ָ���� dataId��������ָ����diamond�ϲ�ѯ�����б� ���ģʽ�а�������'*',����Զ��滻Ϊ'%'��ʹ��[ like ]���
     * ���ģʽ�в���������'*'���Ҳ�Ϊ�մ�������" "��,��ʹ��[ = ]���
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param serverId
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> ��������
     * @throws SQLException
     */
    public PageContextResult<ConfigInfo> queryBy(String dataIdPattern, String groupNamePattern, String serverId,
            long currentPage, long sizeOfPerPage);


    /**
     * ����ָ���� dataId���������׸����õ�diamond����ѯ�����б� ���ģʽ�а�������'*',����Զ��滻Ϊ'%'��ʹ��[ like ]���
     * ���ģʽ�в���������'*'���Ҳ�Ϊ�մ�������" "��,��ʹ��[ = ]���
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> ��������
     * @throws SQLException
     */
    public PageContextResult<ConfigInfo> queryFromDefaultServerBy(String dataIdPattern, String groupNamePattern,
            long currentPage, long sizeOfPerPage);


    /**
     * ����ָ���� dataId,������content��ָ�����õ�diamond����ѯ�����б� ���ģʽ�а�������'*',����Զ��滻Ϊ'%'��ʹ��[
     * like ]��� ���ģʽ�в���������'*'���Ҳ�Ϊ�մ�������" "��,��ʹ��[ = ]���
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param contentPattern
     * @param serverId
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> ��������
     * @throws SQLException
     */
    public PageContextResult<ConfigInfo> queryBy(String dataIdPattern, String groupNamePattern, String contentPattern,
            String serverId, long currentPage, long sizeOfPerPage);


    /**
     * ����ָ���� dataId,������content���׸����õ�diamond����ѯ�����б� ���ģʽ�а�������'*',����Զ��滻Ϊ'%'��ʹ��[
     * like ]��� ���ģʽ�в���������'*'���Ҳ�Ϊ�մ�������" "��,��ʹ��[ = ]���
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param contentPattern
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> ��������
     * @throws SQLException
     */
    public PageContextResult<ConfigInfo> queryFromDefaultServerBy(String dataIdPattern, String groupNamePattern,
            String contentPattern, long currentPage, long sizeOfPerPage);


    // /////////////////////////////////////////��ȷ��ѯ�ӿڶ���////////////////////////////////////////
    /**
     * ����ָ����dataId��������ָ����diamond�ϲ�ѯ�����б�
     * 
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult ��������
     * @throws SQLException
     */
    public ContextResult queryByDataIdAndGroupName(String dataId, String groupName, String serverId);


    /**
     * ����ָ���� dataId���������׸����õ�diamond�ϲ�ѯ�����б�
     * 
     * @param dataId
     * @param groupName
     * @return ContextResult ��������
     * @throws SQLException
     */
    public ContextResult queryFromDefaultServerByDataIdAndGroupName(String dataId, String groupName);


    /**
     * ����ָ����dataId��ip address��ָ����diamond�ϲ�ѯ������Ϣ
     * 
     * @param address
     * @param dataId
     * @param serverId
     * @return ContextResult ��������
     */
    public ContextResult queryByAddressAndDataId(String address, String dataId, String serverId);


    /**
     * ��ѯ���з���
     * 
     * @param serverId
     * @return ContextResult ��������
     */
    public ContextResult queryAllGroup(String serverId);


    // /////////////////////////////////////////�Ƴ���Ϣ�ӿڶ���////////////////////////////////////
    /**
     * �Ƴ��ض���������idָ����������Ϣ
     * 
     * @param serverId
     * @param id
     * @return ContextResult ��������
     */
    public ContextResult unpublish(String serverId, long id);


    /**
     * �Ƴ�Ĭ�Ϸ�������idָ����������Ϣ
     * 
     * @param serverId
     * @param id
     * @return ContextResult ��������
     */
    public ContextResult unpublishFromDefaultServer(long id);


    /**
     * �Ƴ�ָ���������ϵ�idָ���ķ�����Ϣ
     * 
     * @param serverId
     * @param id
     * @return ContextResult ��������
     */
    public ContextResult deleteGroup(String serverId, long id);


    /**
     * ���¼��ط�����Ϣ
     * 
     * @param serverId
     * @return ContextResult ��������
     * 
     */
    public ContextResult reloadGroup(String serverId);


    /**
     * �ж�ָ�����������Ƿ����
     * 
     * @param dataId
     * @param group
     * @param serverId
     * @return
     */
    public boolean exists(String dataId, String group, String serverId);


    /**
     * �ѹ�ʱ, ��ʹ��batchAdd()��batchUpdate()
     * 
     * @param groupName
     * @param dataIds
     * @param contents
     * @param serverId
     * @return
     */
    @Deprecated
    public ContextResult createOrUpdate(String groupName, List<String> dataIds, List<String> contents, String serverId);


    /**
     * ������ѯ
     * 
     * @param groupName
     * @param dataIds
     * @param serverId
     * @return
     */
    public BatchContextResult<ConfigInfoEx> batchQuery(String serverId, String groupName, List<String> dataIds);


    /**
     * �������������
     * 
     * @param serverId
     * @param groupName
     * @param dataId2ContentMap
     *            key:dataId,value:content
     * @return
     */
    public BatchContextResult<ConfigInfoEx> batchAddOrUpdate(String serverId, String groupName, String srcIp,
            String srcUser, Map<String/* dataId */, String/* content */> dataId2ContentMap);


    /**
     * �ѹ�ʱ, ��ʹ��batchAdd()��batchUpdate()
     * 
     * @param groupName
     * @param dataIds
     * @param contents
     * @param serverId
     * 
     * @return
     */
    @Deprecated
    public ContextResult createOrUpdate(String groupName, String dataId, String content, String serverId);


    /**
     * ��ȡ������dataId
     * 
     * @param dataId
     * @return
     */
    public String getRealTimeDataId(String dataId);


    /**
     * ��������, ����¼�����ߵ�IP������, �ýӿ���Ҫ�ṩ��diamond-ops��rtools����ά����ʹ��
     * 
     * @param dataId
     * @param group
     * @param content
     * @param srcIp
     * @param srcUser
     * @return
     */
    public ContextResult publish(String dataId, String group, String content, String serverId, String srcIp,
            String srcUser);


    /**
     * 
     * ��������, ����¼�����ߵ�IP������, �ýӿ���Ҫ�ṩ��diamond-ops��rtools����ά����ʹ��
     * 
     * @param dataId
     * @param group
     * @param content
     * @param serverId
     * @param srcIp
     * @param srcUser
     * @return
     */
    public ContextResult publishAfterModified(String dataId, String group, String content, String serverId,
            String srcIp, String srcUser);

}
