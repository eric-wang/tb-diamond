/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service;

import java.sql.Timestamp;
import java.util.List;

import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.domain.Page;


/**
 * ���ݿ�����ṩConfigInfo,GroupInfo�����ݿ�Ĵ�ȡ
 * 
 * @author boyan
 * @version 1.0 2010-5-5
 * @version 1.0.1 2011/04/02 ����Ϊ�ӿڣ��ֳ�DB��Offline����ʵ��
 * @version 2.0 2012/03/21 д �����ṩ�����޸�ʱ�䡢�û�����ԴIP�Ľӿڣ�ɾ��ʱ��Щ����û��, ɾ����Ȼ������ɾ����
 * @since 1.0
 */

public interface PersistService {

    /**
     * ���ConfigInfo�����ݿ�
     * 
     * @param configInfo
     */
    public void addConfigInfo(final ConfigInfo configInfo);


    /**
     * ���ConfigInfo�����ݿ�, �����Ӵ���ʱ���ԴͷIP��Դͷ�û�
     * 
     * @param srcIp
     * @param srcUser
     * @param date
     * @param configInfo
     */
    public void addConfigInfo(final String srcIp, final String srcUser, final Timestamp time,
            final ConfigInfo configInfo);


    /**
     * ���GroupInfo�����ݿ�
     * 
     * @param groupInfo
     */
    public void addGroupInfo(final GroupInfo groupInfo);


    /**
     * ���GroupInfo�����ݿ�, �����Ӵ���ʱ���ԴͷIP��Դͷ�û�
     * 
     * @param srcIp
     * @param srcUser
     * @param date
     * @param groupInfo
     */
    public void addGroupInfo(final String srcIp, final String srcUser, final Timestamp time, final GroupInfo groupInfo);


    /**
     * ����dataId��groupɾ��������Ϣ
     * 
     * @param dataId
     * @param group
     */
    public void removeConfigInfo(final String dataId, final String group);


    /**
     * �����ݿ�ɾ��Group��Ϣ
     * 
     * @param dataId
     * @param group
     */
    public void removeGroupInfoByID(final long id);


    /**
     * 
     * @param configInfo
     */
    public void removeConfigInfo(ConfigInfo configInfo);


    /**
     * 
     * @param id
     */
    public void removeConfigInfoByID(final long id);


    /**
     * ���·�����Ϣ
     * 
     * @param address
     *            ip��ַ
     * @param oldGroup
     *            �ϵķ�����
     * @param newGroup
     *            �µķ�����
     */
    public void updateGroup(final long id, final String newGroup);


    /**
     * ���·�����Ϣ, �����Ӹ���ʱ���ԴͷIP��Դͷ�û�
     * 
     * @param id
     * @param srcIp
     * @param srcUser
     * @param time
     * @param newGroup
     */
    public void updateGroup(final long id, final String srcIp, final String srcUser, final Timestamp time,
            final String newGroup);


    /**
     * ������������
     * 
     * @param configInfo
     */
    public void updateConfigInfo(final ConfigInfo configInfo);


    /**
     * ������������, �����Ӹ���ʱ���ԴͷIP��Դͷ�û�
     * 
     * @param srcIp
     * @param srcUser
     * @param time
     * @param configInfo
     */
    public void updateConfigInfo(final String srcIp, final String srcUser, final Timestamp time,
            final ConfigInfo configInfo);


    /**
     * ͨ���Ƚ�ConfigInfo�����ݿ��¼��md5ֵ����������
     * 
     * @param configInfo
     * @return ���µļ�¼����
     * @author leiwen
     */
    public int updateConfigInfoByMd5(final ConfigInfoEx configInfoEx);


    /**
     * ͨ���Ƚ�ConfigInfo�����ݿ��¼��md5ֵ����������, �����Ӹ���ʱ���ԴͷIP��Դͷ�û�
     * 
     * @param srcIp
     * @param srcUser
     * @param time
     * @param configInfoEx
     * @return
     */
    public int updateConfigInfoByMd5(final String srcIp, final String srcUser, final Timestamp time,
            final ConfigInfoEx configInfoEx);


    /**
     * ����dataId��group��ѯConfigInfo
     * 
     * @param dataId
     * @param group
     * @return
     */
    public ConfigInfo findConfigInfo(final String dataId, final String group);


    public ConfigInfo findConfigInfoByID(long id);


    public GroupInfo findGroupInfoByID(long id);


    /**
     * ����IP���ҷ�����Ϣ
     * 
     * @param address
     * @return
     */
    public GroupInfo findGroupInfoByAddressDataId(String address, String dataId);


    /**
     * ����group���Ҹ÷�������ip
     * 
     * @param group
     * @return
     */
    public List<GroupInfo> findGroupInfoByGroup(String group);


    /**
     * �������еķ�����Ϣ
     * 
     * @return
     */
    public List<GroupInfo> findAllGroupInfo();


    /**
     * ����group��ѯ������Ϣ
     * 
     * @param pageNo
     *            ҳ��
     * @param pageSize
     *            ÿҳ��С
     * @param group
     *            ����
     * @return
     */
    public Page<ConfigInfo> findConfigInfoByGroup(final int pageNo, final int pageSize, final String group);


    /**
     * ��ҳ��ѯ���е�������Ϣ
     * 
     * @param pageNo
     * @param pageSize
     * @return
     */
    public Page<ConfigInfo> findAllConfigInfo(final int pageNo, final int pageSize);


    /**
     * ����dataId��ѯ������Ϣ
     * 
     * @param pageNo
     *            ҳ��
     * @param pageSize
     *            ÿҳ��С
     * @param dataId
     *            dataId
     * @return
     */
    public Page<ConfigInfo> findConfigInfoByDataId(final int pageNo, final int pageSize, final String dataId);


    /**
     * ����dataId��groupģ����ѯ������Ϣ
     * 
     * @param pageNo
     *            ҳ��
     * @param pageSize
     *            ÿҳ��С
     * @param dataId
     *            dataId
     * @return
     */
    public Page<ConfigInfo> findConfigInfoLike(final int pageNo, final int pageSize, final String dataId,
            final String group);


    /**
     * ��ѯdataId������
     * 
     * @return
     */
    public int countAllDataIds();
}
