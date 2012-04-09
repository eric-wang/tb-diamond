/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.sdkapi.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.type.TypeReference;

import com.google.common.base.Joiner;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.BatchContextResult;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.ContextResult;
import com.taobao.diamond.domain.DiamondConf;
import com.taobao.diamond.domain.DiamondSDKConf;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.domain.Page;
import com.taobao.diamond.domain.PageContextResult;
import com.taobao.diamond.sdkapi.DiamondSDKManager;
import com.taobao.diamond.util.DiamondUtils;
import com.taobao.diamond.util.PatternUtils;
import com.taobao.diamond.util.RandomDiamondUtils;
import com.taobao.diamond.utils.JSONUtils;
import com.taobao.diamond.utils.SimpleFlowData;


/**
 * SDK���⿪�ŵ����ݽӿڵĹ���ʵ��
 * 
 * @filename DiamondSDKManagerImpl.java
 * @author libinbin.pt
 * @datetime 2010-7-16 ����04:00:19
 */
public class DiamondSDKManagerImpl implements DiamondSDKManager {

    private static final Log log = LogFactory.getLog("diamondSdkLog");

    private static final String CONFIG_CLIENT_PUBLISH_SUFFIX = "diamondRealTime";

    public static final String DIAMOND_REALTIME_GROUP = CONFIG_CLIENT_PUBLISH_SUFFIX;

    // DiamondSDKConf���ü�map
    private Map<String, DiamondSDKConf> diamondSDKConfMaps;

    // ���ӳ�ʱʱ��
    private final int connection_timeout;
    // ����ʱʱ��
    private final int require_timeout;

    private final SimpleFlowData flowData = new SimpleFlowData(10, 2000);


    // ����ʱ��Ҫ�������ӳ�ʱʱ�䣬����ʱʱ��
    public DiamondSDKManagerImpl(int connection_timeout, int require_timeout) throws IllegalArgumentException {
        if (connection_timeout < 0)
            throw new IllegalArgumentException("���ӳ�ʱʱ�����ñ������0[��λ(����)]!");
        if (require_timeout < 0)
            throw new IllegalArgumentException("����ʱʱ�����ñ������0[��λ(����)]!");
        this.connection_timeout = connection_timeout;
        this.require_timeout = require_timeout;
        int maxHostConnections = 50;
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

        connectionManager.getParams().setDefaultMaxConnectionsPerHost(maxHostConnections);
        connectionManager.getParams().setStaleCheckingEnabled(true);
        this.client = new HttpClient(connectionManager);
        // �������ӳ�ʱʱ��
        client.getHttpConnectionManager().getParams().setConnectionTimeout(this.connection_timeout);
        // ���ö���ʱΪ1����
        client.getHttpConnectionManager().getParams().setSoTimeout(60 * 1000);
        client.getParams().setContentCharset("GBK");
        log.info("�������ӳ�ʱʱ��Ϊ: " + this.connection_timeout + "����");
    }


    /**
     * ��ʼ��
     * 
     * @throws Exception
     */
    public void init() throws Exception {
        // do nothing
    }


    /**
     * �õ�diamondSDKConfMaps�׸����õļ�ֵ
     * 
     * @return �����׸����õļ�ֵ
     */
    private String getSingleKey() {
        String singleKey = null;
        if (null != diamondSDKConfMaps) {
            singleKey = diamondSDKConfMaps.keySet().iterator().next();
        }
        return singleKey;
    }


    /**
     * ����diamondSDKConfMaps���ü���map
     * 
     * @param diamondSDKConfMaps
     */
    public synchronized void setDiamondSDKConfMaps(final Map<String, DiamondSDKConf> diamondSDKConfMaps) {
        this.diamondSDKConfMaps = diamondSDKConfMaps;
    }


    // ///////////////////////////�ӿڷ���ʵ��////////////////////////////////////////

    /**
     * �õ�diamondSDKConfMaps���ü���map
     * 
     * @return
     */
    public synchronized Map<String, DiamondSDKConf> getDiamondSDKConfMaps() {
        return diamondSDKConfMaps;
    }


    /**
     * ʹ��ָ����diamond�����ͷ�����Ϣ
     * 
     * @param address
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextRestult ��������
     */
    public synchronized ContextResult publishGroup(String address, String dataId, String groupName, String serverId) {
        ContextResult response = null;
        if (validate(address, dataId, groupName, serverId)) {
            response = this.processPublishGroupInfoByDefinedServerId(address, dataId, groupName, serverId);
            return response;
        }
        response = new ContextResult();
        response.setSuccess(false);
        response.setStatusMsg("��ȷ��address,dataId,groupName,serverId��Ϊ��");
        return response;
    }


    /**
     * ʹ��ָ����diamond����������
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @return ContextResult ��������
     */
    public synchronized ContextResult create(String dataId, String groupName, String context, String serverId) {
        ContextResult response = null;
        // ����dataId,groupName,context,serverIdΪ����֤
        if (validate(dataId, groupName, context)) {
            // ��diamondserver��������
            // ���з���, srcIp��srcUser����null, ����needProofΪfalse
            response = this.processPulishByDefinedServerId(dataId, groupName, context, serverId, null, null, false);
            /*
             * if (response.isSuccess()) { // ��ͣ600ms��ʵʱ֪ͨ���ݵ��� try {
             * Thread.sleep(600); } catch (InterruptedException e) {
             * e.printStackTrace(); } }
             */
            return response;
        }

        // δͨ��Ϊ����֤
        response = new ContextResult();
        response.setSuccess(false);
        response.setStatusMsg("��ȷ��dataId,group,content��Ϊ��");
        return response;
    }


    public ContextResult publish(String dataId, String group, String content, String serverId, String srcIp,
            String srcUser) {
        ContextResult response = null;

        if (validate(dataId, group, content)) {
            // ��������, ��������е�srcIp��srcUser, needProofΪtrue
            response = this.processPulishByDefinedServerId(dataId, group, content, serverId, srcIp, srcUser, true);
            return response;
        }

        // δͨ���ǿ���֤
        response = new ContextResult();
        response.setSuccess(false);
        response.setStatusMsg("��ȷ��dataId,group,content��Ϊ��");
        return response;
    }


    /**
     * ʹ���׸����õ�diamond����������
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult ��������
     */
    public synchronized ContextResult pulishFromDefaultServer(String dataId, String groupName, String context) {
        ContextResult response = null;
        // ����dataId,groupName,contextΪ����֤
        if (validate(dataId, groupName, context)) {
            // ��diamondserver��������
            response = this.processPulishByFirstServerId(dataId, groupName, context);
            /*
             * if (response.isSuccess()) { try { Thread.sleep(600); } catch
             * (InterruptedException e) { e.printStackTrace(); } }
             */
            return response;
        }
        response = new ContextResult();
        // δͨ��Ϊ����֤
        response.setSuccess(false);
        response.setStatusMsg("��ȷ��dataId,groupName,context��Ϊ��");
        return response;
    }


    /**
     * ʹ��ָ����diamond���ƶ�������Ϣ
     * 
     * @param address
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult ��������
     */

    public synchronized ContextResult moveGroup(long id, String groupName, String serverId) {
        ContextResult response = null;
        response = this.processPublishGroupInfoAfterModifiedByDefinedServerId(id, groupName, serverId);
        return response;
    }


    /**
     * ʹ��ָ����diamond�������޸ĺ������
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @return ContextResult ��������
     */
    public synchronized ContextResult pulishAfterModified(String dataId, String groupName, String context,
            String serverId) {

        ContextResult response = null;
        // ����dataId,groupName,context,serverIdΪ����֤
        if (validate(dataId, groupName, context)) {
            // ��diamondserver�����޸�����
            // ���з���, src_ip��src_user����null, needProofΪfalse
            response =
                    this.processPulishAfterModifiedByDefinedServerId(dataId, groupName, context, serverId, null, null,
                        false);
            /*
             * if (response.isSuccess()) { try { Thread.sleep(600); } catch
             * (InterruptedException e) { e.printStackTrace(); } }
             */
            return response;
        }
        else {
            response = new ContextResult();
            // δͨ��Ϊ����֤
            response.setSuccess(false);
            response.setStatusMsg("��ȷ��dataId,group,content��Ϊ��");
            return response;
        }

    }


    public ContextResult publishAfterModified(String dataId, String group, String content, String serverId,
            String srcIp, String srcUser) {
        ContextResult response = null;

        if (validate(dataId, group, content)) {
            // ��������, ��������е�srcIp��srcUser, needProofΪtrue
            response =
                    this.processPulishAfterModifiedByDefinedServerId(dataId, group, content, serverId, srcIp, srcUser,
                        true);
            return response;
        }

        // δͨ���ǿ���֤
        response = new ContextResult();
        response.setSuccess(false);
        response.setStatusMsg("��ȷ��dataId,group,content��Ϊ��");
        return response;

    }


    /**
     * ʹ���׸����õ� diamond�������޸ĺ������
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult ��������
     */
    public synchronized ContextResult pulishFromDefaultServerAfterModified(String dataId, String groupName,
            String context) {
        ContextResult response = null;
        // ����dataId,groupName,contextΪ����֤
        if (validate(dataId, groupName, context)) {
            // ��diamondserver�����޸�����
            response = this.processPulishAfterModifiedByFirstServerId(dataId, groupName, context);
            /*
             * if (response.isSuccess()) { try { Thread.sleep(600); } catch
             * (InterruptedException e) { e.printStackTrace(); } }
             */
            return response;
        }
        else {
            response = new ContextResult();
            // δͨ��Ϊ����֤
            response.setSuccess(false);
            response.setStatusMsg("�޸���������ʱ��ȷ��dataId,groupName,context��Ϊ��");
            return response;
        }

    }


    // -------------------------ģ����ѯ-------------------------------//
    /**
     * ʹ��ָ����diamond��ģ����ѯ����
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param serverId
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> ��������
     * @throws SQLException
     */
    public synchronized PageContextResult<ConfigInfo> queryBy(String dataIdPattern, String groupNamePattern,
            String serverId, long currentPage, long sizeOfPerPage) {
        return processQuery(dataIdPattern, groupNamePattern, null, serverId, currentPage, sizeOfPerPage);
    }


    /**
     * ʹ���׸����õ�diamond��ģ����ѯ����
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param currentPage
     * @param sizeOfPerPage
     * @return PageContextResult<ConfigInfo> ��������
     * @throws SQLException
     */
    public synchronized PageContextResult<ConfigInfo> queryFromDefaultServerBy(String dataIdPattern,
            String groupNamePattern, long currentPage, long sizeOfPerPage) {
        String serverKey = this.getSingleKey();
        return processQuery(dataIdPattern, groupNamePattern, null, serverKey, currentPage, sizeOfPerPage);
    }


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

    public synchronized PageContextResult<ConfigInfo> queryBy(String dataIdPattern, String groupNamePattern,
            String contentPattern, String serverId, long currentPage, long sizeOfPerPage) {
        return processQuery(dataIdPattern, groupNamePattern, contentPattern, serverId, currentPage, sizeOfPerPage);
    }


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
    public synchronized PageContextResult<ConfigInfo> queryFromDefaultServerBy(String dataIdPattern,
            String groupNamePattern, String contentPattern, long currentPage, long sizeOfPerPage) {
        String serverKey = this.getSingleKey();
        return processQuery(dataIdPattern, groupNamePattern, contentPattern, serverKey, currentPage, sizeOfPerPage);
    }


    // =====================��ȷ��ѯ ==================================
    /**
     * ʹ��ָ����diamond��ָ����address,dataId����ѯ������Ϣ
     * 
     * @param address
     * @param dataId
     * @param serverId
     * @return ContextResult
     * @throws SQLException
     */
    public synchronized ContextResult queryByAddressAndDataId(String address, String dataId, String serverId) {
        ContextResult result = new ContextResult();
        ContextResult ContextResult = processQuery(address, dataId, serverId);
        result.setStatusMsg(ContextResult.getStatusMsg());
        result.setSuccess(ContextResult.isSuccess());
        result.setStatusCode(ContextResult.getStatusCode());
        if (ContextResult.isSuccess()) {
            List<GroupInfo> list = ContextResult.getReceive();
            if (list != null && !list.isEmpty()) {
                GroupInfo info = list.iterator().next();
                result.setGroupInfo(info);
                result.setReceiveResult(info.getGroup());
                result.setStatusCode(ContextResult.getStatusCode());

            }
        }
        return result;

    }


    /**
     * ʹ��ָ����diamond��ָ����dataId,groupName����ȷ��ѯ����
     * 
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult ��������
     * @throws SQLException
     */
    public synchronized ContextResult queryByDataIdAndGroupName(String dataId, String groupName, String serverId) {
        ContextResult result = new ContextResult();
        PageContextResult<ConfigInfo> pageContextResult = processQuery(dataId, groupName, null, serverId, 1, 1);
        result.setStatusMsg(pageContextResult.getStatusMsg());
        result.setSuccess(pageContextResult.isSuccess());
        result.setStatusCode(pageContextResult.getStatusCode());
        if (pageContextResult.isSuccess()) {
            List<ConfigInfo> list = pageContextResult.getDiamondData();
            if (list != null && !list.isEmpty()) {
                ConfigInfo info = list.iterator().next();
                result.setConfigInfo(info);
                result.setReceiveResult(info.getContent());
                result.setStatusCode(pageContextResult.getStatusCode());

            }
        }
        return result;
    }


    /**
     * ʹ���׸����õ�diamond��ָ����dataId,groupName����ȷ��ѯ����
     * 
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult ��������
     * @throws SQLException
     */
    public synchronized ContextResult queryFromDefaultServerByDataIdAndGroupName(String dataId, String groupName) {
        return queryByDataIdAndGroupName(dataId, groupName, getSingleKey());
    }

    // ========================��ȷ��ѯ����==================================

    // /////////////////////////˽�й��߶�����͹��߷���ʵ��////////////////////////////////////////

    private final HttpClient client;


    public String getRealTimeDataId(String dataId) {
        return dataId + "." + CONFIG_CLIENT_PUBLISH_SUFFIX;
    }


    // =========================== ���� ===============================
    /**
     * ʹ��ָ����serverId����������Ϣ
     * 
     * @param address
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult ���ط�����Ϣ�Ľ��
     */

    private ContextResult processPublishGroupInfoByDefinedServerId(String address, String dataId, String groupName,
            String serverId) {
        ContextResult response = new ContextResult();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("ʹ��processPublishGroupInfoByDefinedServerId(" + address + "," + dataId + "," + groupName
                    + ")���ͷ�����Ϣ��");
        PostMethod post = new PostMethod("/diamond-server/admin.do?method=addGroup");
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            NameValuePair dataId_value = new NameValuePair("dataId", dataId);
            NameValuePair group_value = new NameValuePair("group", groupName);
            NameValuePair address_value = new NameValuePair("address", address);
            // ���ò���
            post.setRequestBody(new NameValuePair[] { address_value, dataId_value, group_value });
            // ���ö���
            GroupInfo groupInfo = new GroupInfo();
            groupInfo.setDataId(dataId);
            groupInfo.setGroup(groupName);
            groupInfo.setAddress(address);
            if (log.isDebugEnabled())
                log.debug("�����͵�GroupInfo: " + groupInfo);
            // ���һ�����ö�����Ӧ�����
            response.setGroupInfo(groupInfo);
            // ִ�з���������http״̬��
            int status = client.executeMethod(post);
            response.setReceiveResult(post.getResponseBodyAsString());
            log.info("״̬�룺" + status + ",��Ӧ�����" + post.getResponseBodyAsString());
            if (status == HttpStatus.SC_OK) {
                response.setSuccess(true);
                response.setStatusMsg("���ͷ�����Ϣ����ɹ�");
                log.info("���ͷ�����Ϣ����ɹ�");
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("���ͷ�����Ϣ����ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                log.info("���ͷ�����Ϣ����ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("���ͷ�����Ϣ����ʧ�ܣ�ʧ��ԭ����ͨ��ContextResult��getReceiveResult()�����鿴");
                log.info("���ͷ�����Ϣ����ʧ��:" + response.getReceiveResult());
            }
            response.setStatusCode(status);
        }
        catch (HttpException e) {
            response.setStatusMsg("���ͷ���ִ�й��̷���HttpException ,��ϸ���£�" + e.getMessage());
            log.error("�����ͷ���processPulishByDefinedServerId(String dataId, String groupName, String context,String serverId)ִ�й����з���HttpException: "
                    + e.getMessage());
        }
        catch (IOException e) {
            response.setStatusMsg("���ͷ���ִ�й��̷���IOException��" + e.getMessage());
            log.error("�����ͷ���processPulishByDefinedServerId(String dataId, String groupName, String context,String serverId)ִ�й����з���IOException: "
                    + e.getMessage());
        }
        finally {
            // �ͷ�������Դ
            post.releaseConnection();
        }
        return response;

    }


    /**
     * ʹ��ָ����serverId�������Ͷ���
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @param needProof
     *            �Ƿ���Ҫserver�˴���srcIp��srcUser, Ϊtrue,
     *            server�˻�����ﴫ���srcIp��srcUser�������ݿ�, Ϊfalse,
     *            server��������IP��ΪsrcIp��srcUser�������ݿ�
     * @return ContextResult ����������Ӧ���
     * 
     *         ���Ӳ���srcIp��srcUser 2012-03-23 leiwen.zh
     */
    private ContextResult processPulishByDefinedServerId(String dataId, String groupName, String context,
            String serverId, String srcIp, String srcUser, boolean needProof) {
        flowControl();
        ContextResult response = new ContextResult();
        // ��¼
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("ʹ��processPulishByDefinedServerId(" + dataId + "," + groupName + "," + context + "," + serverId
                    + ")��������");

        String postUrl = "/diamond-server/admin.do?method=postConfig";
        if (needProof) {
            postUrl = "/diamond-server/admin.do?method=postConfigNew";
        }
        PostMethod post = new PostMethod(postUrl);
        // ��������ʱʱ��
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            NameValuePair dataId_value = new NameValuePair("dataId", dataId);
            NameValuePair group_value = new NameValuePair("group", groupName);
            NameValuePair content_value = new NameValuePair("content", context);
            // ��srcIp��srcUserҲ�ŵ�request body��
            NameValuePair src_ip_value = new NameValuePair("src_ip", srcIp);
            NameValuePair src_user_value = new NameValuePair("src_user", srcUser);

            // ���ò���
            post.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value, src_ip_value,
                                                     src_user_value });
            // ���ö���
            ConfigInfo configInfo = new ConfigInfo();
            configInfo.setDataId(dataId);
            configInfo.setGroup(groupName);
            configInfo.setContent(context);
            if (log.isDebugEnabled())
                log.debug("�����͵�ConfigInfo: " + configInfo);
            // ���һ�����ö�����Ӧ�����
            response.setConfigInfo(configInfo);
            // ִ�з���������http״̬��
            int status = client.executeMethod(post);
            response.setReceiveResult(post.getResponseBodyAsString());
            log.info("״̬�룺" + status + ",��Ӧ�����" + post.getResponseBodyAsString());
            if (status == HttpStatus.SC_OK) {
                response.setSuccess(true);
                response.setStatusMsg("���ʹ���ɹ�");
                log.info("���ʹ���ɹ�, dataId=" + dataId + ",group=" + groupName + ",content=" + context + ",serverId="
                        + serverId + ",srcIp=" + srcIp + ",srcUser=" + srcUser);
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("���ʹ���ʱ, Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                log.error("���ʹ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����, dataId=" + dataId + ",group=" + groupName
                        + ",content=" + context + ",serverId=" + serverId + ",srcIp=" + srcIp + ",srcUser=" + srcUser);
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("���ʹ���ʧ��, ״̬��Ϊ:" + status);
                log.error("���ʹ���ʧ��:" + response.getReceiveResult() + ",dataId=" + dataId + ",group=" + groupName
                        + ",content=" + context + ",serverId=" + serverId);
            }
            response.setStatusCode(status);
        }
        catch (HttpException e) {
            response.setStatusMsg("���ʹ�����HttpException��" + e.getMessage());
            log.error("���ʹ�����HttpException: dataId=" + dataId + ",group=" + groupName + ",content=" + context
                    + ",serverId=" + serverId + ",srcIp=" + srcIp + ",srcUser=" + srcUser, e);
        }
        catch (IOException e) {
            response.setStatusMsg("���ʹ�����IOException��" + e.getMessage());
            log.error("���ʹ�����IOException: dataId=" + dataId + ",group=" + groupName + ",content=" + context
                    + ",serverId=" + serverId + ",srcIp=" + srcIp + ",srcUser=" + srcUser, e);
        }
        finally {
            // �ͷ�������Դ
            post.releaseConnection();
        }

        return response;
    }


    /**
     * ʹ�õ�һ��serverId�������Ͷ���
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult ����������Ӧ���
     */
    private ContextResult processPulishByFirstServerId(String dataId, String groupName, String context) {
        flowControl();
        ContextResult response = new ContextResult();
        // ��¼
        String serverId = this.getSingleKey();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ��������׸����õ�serverIdΪ�ջ򲻴���");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("ʹ��processPulishByFirstServerId(" + dataId + "," + groupName + "," + context + ")��������");
        PostMethod post = new PostMethod("/diamond-server/admin.do?method=postConfig");
        // ��������ʱʱ��
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            NameValuePair dataId_value = new NameValuePair("dataId", dataId);
            NameValuePair group_value = new NameValuePair("group", groupName);
            NameValuePair content_value = new NameValuePair("content", context);
            // ���ò���
            post.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });
            // ���ö���
            ConfigInfo configInfo = new ConfigInfo();
            configInfo.setDataId(dataId);
            configInfo.setGroup(groupName);
            configInfo.setContent(context);
            if (log.isDebugEnabled())
                log.debug("�����͵�ConfigInfo: " + configInfo);
            // ���һ�����ö�����Ӧ�����
            response.setConfigInfo(configInfo);
            // ִ�з���������http״̬��
            int status = client.executeMethod(post);
            response.setReceiveResult(post.getResponseBodyAsString());
            log.info("״̬�룺" + status + ",��Ӧ�����" + post.getResponseBodyAsString());
            if (status == HttpStatus.SC_OK) {
                response.setSuccess(true);
                response.setStatusMsg("ʹ�õ�һ��diamond server���ʹ���ɹ�");
                log.info("ʹ�õ�һ��diamond server���ʹ���ɹ�");
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("ʹ�õ�һ��diamond server���ʹ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                log.error("ʹ�õ�һ��diamond server���ʹ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����, dataId=" + dataId + ",group="
                        + groupName + ",content=" + context + ",serverId=" + serverId);
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("ʹ�õ�һ��diamond server���ʹ���ʧ�ܣ�ʧ��ԭ����ͨ��ContextResult��getReceiveResult()�����鿴");
                log.error("ʹ�õ�һ��diamond server���ʹ���ʧ��:" + response.getReceiveResult() + ",dataId=" + dataId + ",group="
                        + groupName + ",content=" + context + ",serverId=" + serverId);
            }
            response.setStatusCode(status);
        }
        catch (HttpException e) {
            response.setStatusMsg("���ͷ���ִ�й��̷���HttpException��" + e.getMessage());
            log.error(
                "�����ͷ���processPulishByFirstServerId(String dataId, String groupName, String context)ִ�й����з���HttpException��dataId="
                        + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
        }
        catch (IOException e) {
            response.setStatusMsg("���ͷ���ִ�й��̷���IOException��" + e.getMessage());
            log.error(
                "�����ͷ���processPulishByFirstServerId(String dataId, String groupName, String context)ִ�й����з���IOException��dataId="
                        + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
        }
        finally {
            // �ͷ�������Դ
            post.releaseConnection();
        }

        return response;
    }

    // =========================== ���ͽ��� ===============================

    // =========================== �޸� ===============================
    /**
     * ʹ��ָ����serverId�ƶ�������Ϣ
     * 
     * @param address
     * @param dataId
     * @param groupName
     * @param serverId
     * @return ContextResult �����޸ĺ�ķ�����Ϣ
     */
    static final String LIST_FORMAT_URL_GROUP = "/diamond-server/admin.do?method=moveGroup&id=%d&newGroup=%s";


    private ContextResult processPublishGroupInfoAfterModifiedByDefinedServerId(long id, String groupName,
            String serverId) {
        ContextResult response = new ContextResult();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ��");
            return response;
        }
        log.info("ʹ��processPublishGroupInfoAfterModifiedByDefinedServerId(" + id + "," + groupName + "," + serverId
                + ")���������޸�");

        String url = String.format(LIST_FORMAT_URL_GROUP, id, groupName);
        GetMethod method = new GetMethod(url);
        method.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            int status = client.executeMethod(method);
            if (status == HttpStatus.SC_OK) {
                response.setSuccess(true);
                response.setStatusMsg("���ͷ�����Ϣ�޸Ĵ���ɹ�");
                log.info("���ͷ�����Ϣ�޸Ĵ���ɹ�");
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("���ͷ�����Ϣ�޸Ĵ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                log.info("���ͷ�����Ϣ�޸Ĵ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("���ͷ�����Ϣ�޸Ĵ���ʧ��,ʧ��ԭ����ͨ��ContextResult��getReceiveResult()�����鿴");
                log.info("���ͷ�����Ϣ�޸Ĵ���ʧ��:" + response.getReceiveResult());
            }

            response.setStatusCode(status);
        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("�����޸ķ���ִ�й��̷���HttpException��" + e.getMessage());
            log.error("�������޸ķ���processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)ִ�й����з���HttpException ��"
                    + e.getMessage());
            return response;
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("�����޸ķ���ִ�й��̷���IOException��" + e.getMessage());
            log.error("�������޸ķ���processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)ִ�й����з���IOException ��"
                    + e.getMessage());
            return response;
        }
        finally {
            // �ͷ�������Դ
            method.releaseConnection();
        }

        return response;
    }


    /**
     * ʹ��ָ����serverId���������޸Ķ���
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @param needProof
     *            �Ƿ���Ҫserver�˴���srcIp��srcUser, Ϊtrue,
     *            server�˻�����ﴫ���srcIp��srcUser�������ݿ�, Ϊfalse,
     *            server��������IP��ΪsrcIp��srcUser�������ݿ�
     * @return ContextResult ���������޸ĵ���Ӧ���
     */
    private ContextResult processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,
            String serverId, String srcIp, String srcUser, boolean needProof) {
        flowControl();
        ContextResult response = new ContextResult();
        // ��¼
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ��");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("ʹ��processPulishAfterModifiedByDefinedServerId(" + dataId + "," + groupName + "," + context + ","
                    + serverId + ")���������޸�");
        // �Ƿ���ڴ�dataId,groupName�����ݼ�¼
        ContextResult result = null;
        result = queryByDataIdAndGroupName(dataId, groupName, serverId);
        if (null == result || !result.isSuccess()) {
            response.setSuccess(false);
            response.setStatusMsg("�Ҳ�����Ҫ�޸ĵ����ݼ�¼����¼������!");
            log.warn("�Ҳ�����Ҫ�޸ĵ����ݼ�¼����¼������! dataId=" + dataId + ",group=" + groupName + ",serverId=" + serverId);
            return response;
        }
        // �����ݣ����޸�
        else {
            String postUrl = "/diamond-server/admin.do?method=updateConfig";
            if (needProof) {
                postUrl = "/diamond-server/admin.do?method=updateConfigNew";
            }
            PostMethod post = new PostMethod(postUrl);
            // ��������ʱʱ��
            post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
            try {
                NameValuePair dataId_value = new NameValuePair("dataId", dataId);
                NameValuePair group_value = new NameValuePair("group", groupName);
                NameValuePair content_value = new NameValuePair("content", context);
                NameValuePair src_ip_value = new NameValuePair("src_ip", srcIp);
                NameValuePair src_user_value = new NameValuePair("src_user", srcUser);
                // ���ò���
                post.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value, src_ip_value,
                                                         src_user_value });
                // ���ö���
                ConfigInfo configInfo = new ConfigInfo();
                configInfo.setDataId(dataId);
                configInfo.setGroup(groupName);
                configInfo.setContent(context);
                if (log.isDebugEnabled())
                    log.debug("�����͵��޸�ConfigInfo: " + configInfo);
                // ���һ�����ö�����Ӧ�����
                response.setConfigInfo(configInfo);
                // ִ�з���������http״̬��
                int status = client.executeMethod(post);
                response.setReceiveResult(post.getResponseBodyAsString());
                log.info("״̬�룺" + status + ",��Ӧ�����" + post.getResponseBodyAsString());
                if (status == HttpStatus.SC_OK) {
                    response.setSuccess(true);
                    response.setStatusMsg("�����޸Ĵ���ɹ�");
                    log.info("�����޸Ĵ���ɹ�");
                }
                else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                    response.setSuccess(false);
                    response.setStatusMsg("�����޸Ĵ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                    log.error("�����޸Ĵ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����, dataId=" + dataId + ",group=" + groupName
                            + ",content=" + context + ",serverId=" + serverId);
                }
                else {
                    response.setSuccess(false);
                    response.setStatusMsg("�����޸Ĵ���ʧ��,ʧ��ԭ����ͨ��ContextResult��getReceiveResult()�����鿴");
                    log.error("�����޸Ĵ���ʧ��:" + response.getReceiveResult() + ",dataId=" + dataId + ",group=" + groupName
                            + ",content=" + context + ",serverId=" + serverId);
                }

                response.setStatusCode(status);
            }
            catch (HttpException e) {
                response.setSuccess(false);
                response.setStatusMsg("�����޸ķ���ִ�й��̷���HttpException��" + e.getMessage());
                log.error(
                    "�������޸ķ���processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)ִ�й����з���HttpException��dataId="
                            + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
                return response;
            }
            catch (IOException e) {
                response.setSuccess(false);
                response.setStatusMsg("�����޸ķ���ִ�й��̷���IOException��" + e.getMessage());
                log.error(
                    "�������޸ķ���processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)ִ�й����з���IOException��dataId="
                            + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
                return response;
            }
            finally {
                // �ͷ�������Դ
                post.releaseConnection();
            }

            return response;
        }
    }


    /**
     * ʹ�õ�һ��serverId���������޸Ķ���
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return ContextResult ���������޸ĵ���Ӧ���
     */
    private ContextResult processPulishAfterModifiedByFirstServerId(String dataId, String groupName, String context) {
        flowControl();
        ContextResult response = new ContextResult();
        // ��¼
        String serverId = this.getSingleKey();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ��������׸����õ�serverIdΪ��");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("ʹ��processPulishAfterModifiedByFirstServerId(" + dataId + "," + groupName + "," + context
                    + ")���������޸�");
        // �Ƿ���ڴ�dataId,groupName�����ݼ�¼
        ContextResult result = null;
        result = queryFromDefaultServerByDataIdAndGroupName(dataId, groupName);
        if (null == result || !result.isSuccess()) {
            response.setSuccess(false);
            response.setStatusMsg("�Ҳ�����Ҫ�޸ĵ����ݼ�¼����¼������!");
            log.warn("�Ҳ�����Ҫ�޸ĵ����ݼ�¼����¼������! dataId=" + dataId + ",group=" + groupName);
            return response;
        }
        // �����ݣ����޸�
        else {
            PostMethod post = new PostMethod("/diamond-server/admin.do?method=updateConfig");
            // ��������ʱʱ��
            post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
            try {
                NameValuePair dataId_value = new NameValuePair("dataId", dataId);
                NameValuePair group_value = new NameValuePair("group", groupName);
                NameValuePair content_value = new NameValuePair("content", context);
                // ���ò���
                post.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });
                // ���ö���
                ConfigInfo configInfo = new ConfigInfo();
                configInfo.setDataId(dataId);
                configInfo.setGroup(groupName);
                configInfo.setContent(context);
                if (log.isDebugEnabled())
                    log.debug("�����͵��޸�ConfigInfo: " + configInfo);
                // ���һ�����ö�����Ӧ�����
                response.setConfigInfo(configInfo);
                // ִ�з���������http״̬��
                int status = client.executeMethod(post);
                response.setReceiveResult(post.getResponseBodyAsString());
                log.info("״̬�룺" + status + ",��Ӧ�����" + post.getResponseBodyAsString());
                if (status == HttpStatus.SC_OK) {
                    response.setSuccess(true);
                    response.setStatusMsg("ʹ�õ�һ��diamond server�����޸Ĵ���ɹ�");
                    log.info("ʹ�õ�һ��diamond server�����޸Ĵ���ɹ�");
                }
                else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                    response.setSuccess(false);
                    response.setStatusMsg("ʹ�õ�һ��diamond server�����޸Ĵ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                    log.error("ʹ�õ�һ��diamond server�����޸Ĵ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����, dataId=" + dataId
                            + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId);
                }
                else {
                    response.setSuccess(false);
                    response.setStatusMsg("ʹ�õ�һ��diamond server�����޸Ĵ���ʧ��,ʧ��ԭ����ͨ��ContextResult��getReceiveResult()�����鿴");
                    log.error("ʹ�õ�һ��diamond server�����޸Ĵ���ʧ��:" + response.getReceiveResult() + ",dataId=" + dataId
                            + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId);
                }

                response.setStatusCode(status);
            }
            catch (HttpException e) {
                response.setSuccess(false);
                response.setStatusMsg("�����޸ķ���ִ�й��̷���HttpException��" + e.getMessage());
                log.error(
                    "�������޸ķ���processPulishAfterModifiedByFirstServerId(String dataId, String groupName, String context)ִ�й����з���HttpException��dataId="
                            + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
                return response;
            }
            catch (IOException e) {
                response.setSuccess(false);
                response.setStatusMsg("�����޸ķ���ִ�й��̷���IOException��" + e.getMessage());
                log.error(
                    "�������޸ķ���processPulishAfterModifiedByFirstServerId(String dataId, String groupName, String context)ִ�й����з���IOException��dataId="
                            + dataId + ",group=" + groupName + ",content=" + context + ",serverId=" + serverId, e);
                return response;
            }
            finally {
                // �ͷ�������Դ
                post.releaseConnection();
            }

            return response;
        }

    }

    // =========================== �޸Ľ��� ===============================

    /**
     * ���� httpclientʵ��ҳ���¼
     * 
     * @return ��¼��� true:��¼�ɹ�,false:��¼ʧ��
     */

    ReentrantLock clientLock = new ReentrantLock();


    private boolean login(String serverId) {
        // serverId Ϊ���ж�
        if (StringUtils.isEmpty(serverId) || StringUtils.isBlank(serverId))
            return false;
        DiamondSDKConf defaultConf = diamondSDKConfMaps.get(serverId);
        log.info("[login] ��¼ʹ��serverId:" + serverId + ",�û����������ԣ�" + defaultConf);
        if (null == defaultConf)
            return false;
        RandomDiamondUtils util = new RandomDiamondUtils();
        // ��ʼ�����ȡֵ��
        util.init(defaultConf.getDiamondConfs());
        if (defaultConf.getDiamondConfs().size() == 0)
            return false;
        boolean flag = false;
        log.info("[randomSequence] �˴η�������Ϊ: " + util.getSequenceToString());
        // ������Դ���Ϊ��ĳ�����������������õ�diamondConf�ĳ���
        while (util.getRetry_times() < util.getMax_times()) {

            // �õ����ȡ�õ�diamondConf
            DiamondConf diamondConf = util.generatorOneDiamondConf();
            log.info("��" + util.getRetry_times() + "�γ���:" + diamondConf);
            if (diamondConf == null)
                break;
            client.getHostConfiguration().setHost(diamondConf.getDiamondIp(),
                Integer.parseInt(diamondConf.getDiamondPort()), "http");
            PostMethod post = new PostMethod("/diamond-server/login.do?method=login");
            // ��������ʱʱ��
            post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
            // ����û���������
            NameValuePair username_value = new NameValuePair("username", diamondConf.getDiamondUsername());
            NameValuePair password_value = new NameValuePair("password", diamondConf.getDiamondPassword());
            // ������������
            post.setRequestBody(new NameValuePair[] { username_value, password_value });
            log.info("ʹ��diamondIp: " + diamondConf.getDiamondIp() + ",diamondPort: " + diamondConf.getDiamondPort()
                    + ",diamondUsername: " + diamondConf.getDiamondUsername() + ",diamondPassword: "
                    + diamondConf.getDiamondPassword() + "��¼diamondServerUrl: [" + diamondConf.getDiamondConUrl() + "]");

            try {
                int state = client.executeMethod(post);
                log.info("��¼����״̬�룺" + state);
                // ״̬��Ϊ200�����¼�ɹ�,����ѭ��������true
                if (state == HttpStatus.SC_OK) {
                    log.info("��" + util.getRetry_times() + "�γ��Գɹ�");
                    flag = true;
                    break;
                }

            }
            catch (HttpException e) {
                log.error("��¼���̷���HttpException", e);
            }
            catch (IOException e) {
                log.error("��¼���̷���IOException", e);
            }
            finally {
                post.releaseConnection();
            }
        }
        if (flag == false) {
            log.error("���loginʧ�ܵ�ԭ������ǣ�����diamondServer�����û���Ŀǰ�������ã�serverId=" + serverId);
        }
        return flag;
    }

    static final String LIST_FORMAT_URL =
            "/diamond-server/admin.do?method=listConfig&group=%s&dataId=%s&pageNo=%d&pageSize=%d";
    static final String LIST_LIKE_FORMAT_URL =
            "/diamond-server/admin.do?method=listConfigLike&group=%s&dataId=%s&pageNo=%d&pageSize=%d";


    /**
     * ��������ѯ
     * 
     * @param address
     * @param dataId
     * @param serverId
     * @param currentPage
     * @param sizeOfPerPage
     * @return
     */
    private ContextResult processQuery(String address, String dataId, String serverId) {
        ContextResult response = new ContextResult();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���");
            return response;
        }
        String url = "/diamond-server/admin.do?method=listGroup";
        GetMethod method = new GetMethod(url);
        configureGetMethod(method);
        try {

            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                try {
                    String json = getContent(method).trim();
                    List<GroupInfo> list = null;

                    if (!json.equals("null")) {
                        list =
                                (List<GroupInfo>) JSONUtils.deserializeObject(json,
                                    new TypeReference<List<GroupInfo>>() {
                                    });
                    }
                    // List<String> list = new ArrayList<String>();
                    List<GroupInfo> list1 = new ArrayList<GroupInfo>();
                    if (list != null) {
                        // for (Entry<String, Map<String, GroupInfo>> entry :
                        // map.entrySet())
                        // // {
                        // // list.add(entry.getKey());
                        // for(Entry<String,GroupInfo> inner:
                        // entry.getValue().entrySet()){
                        // // list.add(inner.getKey());
                        // // list.add(inner.getValue());
                        // // }
                        // list1.add(inner.getValue());
                        // }
                        for (Iterator<GroupInfo> iter = list.iterator(); iter.hasNext();) {
                            GroupInfo info = (GroupInfo) iter.next();
                            if (info.getAddress().equals(address) && info.getDataId().equals(dataId)) {
                                list1.add(info);
                                break;
                            }
                        }
                        if (list1.size() != 0) {
                            response.setSuccess(true);
                            response.setStatusMsg("ָ��diamond�ķ�����Ϣ��ѯ���");
                            response.setReceive(list1);
                            log.info("ָ��diamond�ķ�����Ϣ��ѯ���");
                        }
                        else {
                            response.setReceive(null);
                            response.setSuccess(false);
                            response.setStatusMsg("��ѯ�������");
                            log.info("��ѯ�������");
                        }
                    }
                    else {
                        response.setReceive(null);
                        response.setSuccess(false);
                        response.setStatusMsg("��ѯ�������");
                        log.info("��ѯ�������");
                    }
                }
                catch (Exception e) {
                    response.setSuccess(false);
                    response.setStatusMsg("�����л�ʧ��,������ϢΪ��" + e.getLocalizedMessage());
                    log.error("�����л�page����ʧ��", e);
                }
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("��ѯ���ݳ�ʱ" + require_timeout + "����");
                log.error("��ѯ���ݳ�ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("��ѯ���ݳ�������������״̬��Ϊ" + status);
                log.error("��ѯ���ݳ���״̬��Ϊ��" + status);
                break;
            }

        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("��ѯ���ݳ���,������Ϣ���£�" + e.getMessage());
            log.error("��ѯ���ݳ���", e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("��ѯ���ݳ���,������Ϣ���£�" + e.getMessage());
            log.error("��ѯ���ݳ���", e);
        }
        finally {
            // �ͷ�������Դ
            method.releaseConnection();
        }

        return response;
    }


    /**
     * ��ѯ���з���
     * 
     * @param serverId
     * @return
     */
    public ContextResult queryAllGroup(String serverId) {
        ContextResult response;
        ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> map;
        GetMethod method;
        response = new ContextResult();
        map = new ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>>();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���");
            return response;
        }
        String url = "/diamond-server/admin.do?method=listGroup";
        method = new GetMethod(url);
        configureGetMethod(method);
        try {
            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                try {
                    String json = getContent(method).trim();
                    List<GroupInfo> list = null;
                    if (!json.equals("null"))
                        if (!json.equals("null")) {
                            list =
                                    (List<GroupInfo>) JSONUtils.deserializeObject(json,
                                        new TypeReference<List<GroupInfo>>() {
                                        });
                        }

                    if (list != null) {
                        for (Iterator iter = list.iterator(); iter.hasNext();) {
                            GroupInfo info = (GroupInfo) iter.next();
                            ConcurrentHashMap<String, GroupInfo> data = map.get(info.getAddress());
                            if (data == null) {
                                ConcurrentHashMap<String, GroupInfo> newMap =
                                        new ConcurrentHashMap<String, GroupInfo>();
                                map.putIfAbsent(info.getAddress(), newMap);
                                data = newMap;
                            }
                            GroupInfo groupInfo = (GroupInfo) data.get(info.getDataId());
                            if (groupInfo == null)
                                data.putIfAbsent(info.getDataId(), info);
                        }

                    }
                    log.info(map.toString());
                    response.setSuccess(true);
                    response.setStatusMsg("�����л���ɣ�");
                    response.setMap(map);
                    break;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    response.setSuccess(false);
                    response.setStatusMsg("�����л�ʧ��,������ϢΪ��" + e.getLocalizedMessage());
                    log.error("�����л�page����ʧ��", e);
                }
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("��ѯ���ݳ�ʱ" + require_timeout + "����");
                log.error("��ѯ���ݳ�ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("��ѯ���ݳ�������������״̬��Ϊ" + status);
                log.error("��ѯ���ݳ���״̬��Ϊ��" + status);
                break;
            }
        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("��ѯ���ݳ���,������Ϣ���£�" + e.getMessage());
            log.error("��ѯ���ݳ���", e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("��ѯ���ݳ���,������Ϣ���£�" + e.getMessage());
            log.error("��ѯ���ݳ���", e);
        }
        finally {
            // �ͷ�������Դ
            method.releaseConnection();
        }

        return response;
    }


    /**
     * �����ѯ
     * 
     * @param dataIdPattern
     * @param groupNamePattern
     * @param contentPattern
     * @param serverId
     * @param currentPage
     * @param sizeOfPerPage
     * @return
     */
    @SuppressWarnings("unchecked")
    private PageContextResult<ConfigInfo> processQuery(String dataIdPattern, String groupNamePattern,
            String contentPattern, String serverId, long currentPage, long sizeOfPerPage) {
        flowControl();
        PageContextResult<ConfigInfo> response = new PageContextResult<ConfigInfo>();
        // ��¼
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���");
            return response;
        }
        if (log.isDebugEnabled())
            log.debug("ʹ��processQuery(" + dataIdPattern + "," + groupNamePattern + "," + contentPattern + ","
                    + serverId + ")���в�ѯ");
        boolean hasPattern =
                PatternUtils.hasCharPattern(dataIdPattern) || PatternUtils.hasCharPattern(groupNamePattern)
                        || PatternUtils.hasCharPattern(contentPattern);
        String url = null;
        if (hasPattern) {
            if (!StringUtils.isBlank(contentPattern)) {
                log.warn("ע��, ���ڸ�������������ģ����ѯ, dataIdPattern=" + dataIdPattern + ",groupNamePattern=" + groupNamePattern
                        + ",contentPattern=" + contentPattern);
                // ģ����ѯ���ݣ�ȫ�������
                url = String.format(LIST_LIKE_FORMAT_URL, groupNamePattern, dataIdPattern, 1, Integer.MAX_VALUE);
            }
            else
                url = String.format(LIST_LIKE_FORMAT_URL, groupNamePattern, dataIdPattern, currentPage, sizeOfPerPage);
        }
        else {
            url = String.format(LIST_FORMAT_URL, groupNamePattern, dataIdPattern, currentPage, sizeOfPerPage);
        }

        GetMethod method = new GetMethod(url);
        configureGetMethod(method);
        try {

            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                String json = "";
                try {
                    json = getContent(method).trim();

                    Page<ConfigInfo> page = null;

                    if (!json.equals("null")) {
                        page =
                                (Page<ConfigInfo>) JSONUtils.deserializeObject(json,
                                    new TypeReference<Page<ConfigInfo>>() {
                                    });
                    }
                    if (page != null) {
                        List<ConfigInfo> diamondData = page.getPageItems();
                        if (!StringUtils.isBlank(contentPattern)) {
                            Pattern pattern = Pattern.compile(contentPattern.replaceAll("\\*", ".*"));
                            List<ConfigInfo> newList = new ArrayList<ConfigInfo>();
                            // ǿ������
                            Collections.sort(diamondData);
                            int totalCount = 0;
                            long begin = sizeOfPerPage * (currentPage - 1);
                            long end = sizeOfPerPage * currentPage;
                            for (ConfigInfo configInfo : diamondData) {
                                if (configInfo.getContent() != null) {
                                    Matcher m = pattern.matcher(configInfo.getContent());
                                    if (m.find()) {
                                        // ֻ���sizeOfPerPage��
                                        if (totalCount >= begin && totalCount < end) {
                                            newList.add(configInfo);
                                        }
                                        totalCount++;
                                    }
                                }
                            }
                            page.setPageItems(newList);
                            page.setTotalCount(totalCount);
                        }
                        response.setOriginalDataSize(diamondData.size());
                        response.setTotalCounts(page.getTotalCount());
                        response.setCurrentPage(currentPage);
                        response.setSizeOfPerPage(sizeOfPerPage);
                    }
                    else {
                        response.setOriginalDataSize(0);
                        response.setTotalCounts(0);
                        response.setCurrentPage(currentPage);
                        response.setSizeOfPerPage(sizeOfPerPage);
                    }
                    response.operation();
                    List<ConfigInfo> pageItems = new ArrayList<ConfigInfo>();
                    if (page != null) {
                        pageItems = page.getPageItems();
                    }
                    response.setDiamondData(pageItems);
                    response.setSuccess(true);
                    response.setStatusMsg("ָ��diamond�Ĳ�ѯ���");
                    log.info("ָ��diamond�Ĳ�ѯ���, url=" + url);
                }
                catch (Exception e) {
                    response.setSuccess(false);
                    response.setStatusMsg("�����л�ʧ��,������ϢΪ��" + e.getLocalizedMessage());
                    log.error("�����л�page����ʧ��, dataId=" + dataIdPattern + ",group=" + groupNamePattern + ",serverId="
                            + serverId + ",json=" + json, e);
                }
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("��ѯ���ݳ�ʱ" + require_timeout + "����");
                log.error("��ѯ���ݳ�ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����, dataId=" + dataIdPattern + ",group="
                        + groupNamePattern + ",serverId=" + serverId);
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("��ѯ���ݳ�������������״̬��Ϊ" + status);
                log.error("��ѯ���ݳ���״̬��Ϊ��" + status + ",dataId=" + dataIdPattern + ",group=" + groupNamePattern
                        + ",serverId=" + serverId);
                break;
            }

        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("��ѯ���ݳ���,������Ϣ���£�" + e.getMessage());
            log.error("��ѯ���ݳ���, dataId=" + dataIdPattern + ",group=" + groupNamePattern + ",serverId=" + serverId, e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("��ѯ���ݳ���,������Ϣ���£�" + e.getMessage());
            log.error("��ѯ���ݳ���, dataId=" + dataIdPattern + ",group=" + groupNamePattern + ",serverId=" + serverId, e);
        }
        finally {
            // �ͷ�������Դ
            method.releaseConnection();
        }

        return response;
    }


    /**
     * �鿴�Ƿ�Ϊѹ��������
     * 
     * @param httpMethod
     * @return
     */
    boolean isZipContent(HttpMethod httpMethod) {
        if (null != httpMethod.getResponseHeader(Constants.CONTENT_ENCODING)) {
            String acceptEncoding = httpMethod.getResponseHeader(Constants.CONTENT_ENCODING).getValue();
            if (acceptEncoding.toLowerCase().indexOf("gzip") > -1) {
                return true;
            }
        }
        return false;
    }


    /**
     * ��ȡResponse��������Ϣ
     * 
     * @param httpMethod
     * @return
     */
    String getContent(HttpMethod httpMethod) throws UnsupportedEncodingException {
        StringBuilder contentBuilder = new StringBuilder();
        if (isZipContent(httpMethod)) {
            // ����ѹ������������Ϣ���߼�
            InputStream is = null;
            GZIPInputStream gzin = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                is = httpMethod.getResponseBodyAsStream();
                gzin = new GZIPInputStream(is);
                isr = new InputStreamReader(gzin, ((HttpMethodBase) httpMethod).getResponseCharSet()); // ���ö�ȡ���ı����ʽ���Զ������
                br = new BufferedReader(isr);
                char[] buffer = new char[4096];
                int readlen = -1;
                while ((readlen = br.read(buffer, 0, 4096)) != -1) {
                    contentBuilder.append(buffer, 0, readlen);
                }
            }
            catch (Exception e) {
                log.error("��ѹ��ʧ��", e);
            }
            finally {
                try {
                    br.close();
                }
                catch (Exception e1) {
                    // ignore
                }
                try {
                    isr.close();
                }
                catch (Exception e1) {
                    // ignore
                }
                try {
                    gzin.close();
                }
                catch (Exception e1) {
                    // ignore
                }
                try {
                    is.close();
                }
                catch (Exception e1) {
                    // ignore
                }
            }
        }
        else {
            // ����û�б�ѹ������������Ϣ���߼�
            String content = null;
            try {
                content = httpMethod.getResponseBodyAsString();
            }
            catch (Exception e) {
                log.error("��ȡ������Ϣʧ��", e);
            }
            if (null == content) {
                return null;
            }
            contentBuilder.append(content);
        }
        return StringEscapeUtils.unescapeHtml(contentBuilder.toString());
    }


    private void configureGetMethod(GetMethod method) {
        method.addRequestHeader(Constants.ACCEPT_ENCODING, "gzip,deflate");
        method.addRequestHeader("Accept", "application/json");
        // ��������ʱʱ��
        method.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
    }


    /**
     * �ֶ�dataId,groupName,contextΪ����֤,��һ��Ϊ����������false
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @return
     */
    private boolean validate(String dataId, String groupName, String context) {
        if (StringUtils.isEmpty(dataId) || StringUtils.isEmpty(groupName) || StringUtils.isEmpty(context)
                || StringUtils.isBlank(dataId) || StringUtils.isBlank(groupName) || StringUtils.isBlank(context))
            return false;
        return true;
    }


    /**
     * �ֶ�dataId,groupName,context,serverIdΪ����֤,��һ��Ϊ����������false
     * 
     * @param dataId
     * @param groupName
     * @param context
     * @param serverId
     * @return
     */
    private boolean validate(String dataId, String groupName, String context, String serverId) {
        if (StringUtils.isEmpty(dataId) || StringUtils.isEmpty(groupName) || StringUtils.isEmpty(context)
                || StringUtils.isEmpty(serverId) || StringUtils.isBlank(dataId) || StringUtils.isBlank(groupName)
                || StringUtils.isBlank(context) || StringUtils.isBlank(serverId))
            return false;
        return true;
    }


    public synchronized ContextResult unpublish(String serverId, long id) {
        return processDelete(serverId, id);
    }


    public synchronized ContextResult unpublishFromDefaultServer(long id) {
        String serverKey = this.getSingleKey();
        return processDelete(serverKey, id);
    }


    public synchronized ContextResult deleteGroup(String serverId, long id) {
        return processDeleteGroup(serverId, id);
    }


    /**
     * ����ָ����idɾ��������Ϣ
     * 
     * @param serverId
     * @param id
     * @return
     */
    private ContextResult processDeleteGroup(String serverId, long id) {
        ContextResult response = new ContextResult();
        // ��¼
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���");
            return response;
        }
        String url = "/diamond-server/admin.do?method=deleteGroup&id=" + id;
        GetMethod method = new GetMethod(url);
        configureGetMethod(method);
        try {

            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                response.setSuccess(true);
                response.setStatusMsg("ɾ���ɹ�");
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("ɾ��������Ϣ��ʱ" + require_timeout + "����");
                log.error("ɾ��������Ϣ��ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("ɾ��������Ϣ��������������״̬��Ϊ" + status);
                log.error("ɾ��������Ϣ����״̬��Ϊ��" + status);
                break;
            }

        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("ɾ��������Ϣ����,������Ϣ���£�" + e.getMessage());
            log.error("ɾ��������Ϣ����", e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("ɾ��������Ϣ����,������Ϣ���£�" + e.getMessage());
            log.error("ɾ��������Ϣ����", e);
        }
        finally {
            // �ͷ�������Դ
            method.releaseConnection();
        }

        return response;
    }


    // /**
    // * ����ɾ��������Ϣ
    // * @param serverId
    // * @param id
    // * @return
    // */
    //
    // private ContextResult processDeleteGroup(String address, String dataId,
    // String serverId){
    // ContextResult response = new ContextResult();
    // // ��¼
    // if (!login(serverId)) {
    // response.setSuccess(false);
    // response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���");
    // return response;
    // }
    // ContextResult result = null;
    // result = queryByAddressAndDataId(address, dataId, serverId);
    // long id = result.getGroupInfo().getId();
    // if (null == result || !result.isSuccess()) {
    // response.setSuccess(false);
    // response.setStatusMsg("�Ҳ�����Ҫɾ���ķ�����Ϣ����¼�����ڣ�");
    // log.warn("�Ҳ�����Ҫɾ���ķ�����Ϣ����¼�����ڣ�");
    // return response;
    // }
    // log.info("ʹ��processDeleteGroup(" + serverId + "," + id);
    // String url = "/diamond-server/admin.do?method=deleteGroup&id=" + id;
    // GetMethod method = new GetMethod(url);
    // configureGetMethod(method);
    // try {
    //
    // int status = client.executeMethod(method);
    // response.setStatusCode(status);
    // switch (status) {
    // case HttpStatus.SC_OK:
    // response.setSuccess(true);
    // response.setStatusMsg("ɾ���ɹ�");
    // break;
    // case HttpStatus.SC_REQUEST_TIMEOUT:
    // response.setSuccess(false);
    // response.setStatusMsg("ɾ��������Ϣ��ʱ" + require_timeout + "����");
    // log.error("ɾ��������Ϣ��ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
    // break;
    // default:
    // response.setSuccess(false);
    // response.setStatusMsg("ɾ��������Ϣ��������������״̬��Ϊ" + status);
    // log.error("ɾ��������Ϣ����״̬��Ϊ��" + status);
    // break;
    // }
    //
    // }
    // catch (HttpException e) {
    // response.setSuccess(false);
    // response.setStatusMsg("ɾ��������Ϣ����,������Ϣ���£�" + e.getStackTrace());
    // log.error("ɾ��������Ϣ����", e);
    // }
    // catch (IOException e) {
    // response.setSuccess(false);
    // response.setStatusMsg("ɾ��������Ϣ����,������Ϣ���£�" + e.getStackTrace());
    // log.error("ɾ��������Ϣ����", e);
    // }
    // finally {
    // // �ͷ�������Դ
    // method.releaseConnection();
    // }
    //
    // return response;
    // }
    /**
     * ����ɾ��
     * 
     * @param serverId
     * @param id
     * @return
     */
    private ContextResult processDelete(String serverId, long id) {
        flowControl();
        ContextResult response = new ContextResult();
        // ��¼
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���");
            return response;
        }
        log.info("ʹ��processDelete(" + serverId + "," + id);
        String url = "/diamond-server/admin.do?method=deleteConfig&id=" + id;
        GetMethod method = new GetMethod(url);
        configureGetMethod(method);
        try {

            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                response.setSuccess(true);
                response.setReceiveResult(getContent(method));
                response.setStatusMsg("ɾ���ɹ�, url=" + url);
                log.warn("ɾ���������ݳɹ�, url=" + url);
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("ɾ�����ݳ�ʱ" + require_timeout + "����");
                log.error("ɾ�����ݳ�ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����, id=" + id + ",serverId=" + serverId);
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("ɾ�����ݳ�������������״̬��Ϊ" + status);
                log.error("ɾ�����ݳ���״̬��Ϊ��" + status + ", id=" + id + ",serverId=" + serverId);
                break;
            }

        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("ɾ�����ݳ���,������Ϣ���£�" + e.getMessage());
            log.error("ɾ�����ݳ���, id=" + id + ",serverId=" + serverId, e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("ɾ�����ݳ���,������Ϣ���£�" + e.getMessage());
            log.error("ɾ�����ݳ���, id=" + id + ",serverId=" + serverId, e);
        }
        finally {
            // �ͷ�������Դ
            method.releaseConnection();
        }

        return response;
    }


    // /////////////////////////////////////���ط�����Ϣ�ӿ�ʵ��/////////////////////////
    public synchronized ContextResult reloadGroup(String serverId) {
        ContextResult response = new ContextResult();
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���");
            return response;
        }
        String url = "/diamond-server/admin.do?method=reloadGroup";
        GetMethod method = new GetMethod(url);
        try {

            int status = client.executeMethod(method);
            response.setStatusCode(status);
            switch (status) {
            case HttpStatus.SC_OK:
                response.setSuccess(true);
                response.setStatusMsg("���·���ɹ�");
                break;
            case HttpStatus.SC_REQUEST_TIMEOUT:
                response.setSuccess(false);
                response.setStatusMsg("���·�����Ϣ��ʱ" + require_timeout + "����");
                log.error("���·�����Ϣ��ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                break;
            default:
                response.setSuccess(false);
                response.setStatusMsg("���·�����Ϣ��������������״̬��Ϊ" + status);
                log.error("���·�����Ϣ����״̬��Ϊ��" + status);
                break;
            }

        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("���·�����Ϣ����,������Ϣ���£�" + e.getMessage());
            log.error("���·�����Ϣ����", e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("���·�����Ϣ����,������Ϣ���£�" + e.getMessage());
            log.error("���з�����Ϣ����", e);
        }
        finally {
            // �ͷ�������Դ
            method.releaseConnection();
        }

        return response;
    }

    // /////////////////////////////////���ط���ʵ��///////////////////////////////////////

    static final int FLOW_CONTROL_THRESHOLD = Integer.parseInt(System.getProperty("diamond.sdk.flow_control_threshold",
        "100"));


    private void flowControl() {
        flowData.addAndGet(1);
        // �������ռ�sleep
        while (flowData.getAverageCount() > FLOW_CONTROL_THRESHOLD) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }


    /**
     * �ж�ָ��dataId��group�������Ƿ����
     */
    public synchronized boolean exists(String dataId, String group, String serverId) {
        // ����
        flowControl();
        // ��¼
        if (!login(serverId)) {
            log.error("��¼ʧ��, ԭ����鿴�ͻ�����־");
            throw new RuntimeException("����exists(), ��¼diamond-serverʧ��, dataId=" + dataId + ",group=" + group
                    + ",serverId=" + serverId);
        }

        // ��������url
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("/diamond-server/config.co?");
        urlBuilder.append("dataId=").append(dataId).append("&");
        urlBuilder.append("group=").append(group);

        // ����HTTP method
        GetMethod method = new GetMethod(urlBuilder.toString());
        configureGetMethod(method);

        try {
            // ִ��HTTP method
            int status = client.executeMethod(method);
            switch (status) {
            case HttpStatus.SC_OK:
                log.info("����exists(), ���ݴ���, dataId=" + dataId + ",group=" + group);
                return true;

            case HttpStatus.SC_NOT_FOUND:
                log.info("����exists(), ���ݲ�����, dataId=" + dataId + ",group=" + group);
                return false;

            default:
                log.error("����exists(), ��ѯ�����Ƿ���ڷ�������, HTTP״̬��=" + status + ",dataId=" + dataId + ",group=" + group);
                throw new RuntimeException("����exists(), ��ѯ�����Ƿ���ڷ�������, HTTP״̬��=" + status + ",dataId=" + dataId
                        + ",group=" + group);
            }
        }
        catch (Exception e) {
            log.error("����exists(), ��ѯ�����Ƿ���ڷ�������, dataId=" + dataId + ",group=" + group, e);
            throw new RuntimeException(e);
        }
        finally {
            // �ͷ�������Դ
            method.releaseConnection();
        }
    }


    // /////////////////////////////////zhidao.2011/07/13///////////////////////////////////////

    // ================================ �����ӿ���д, leiwen.zh, 2012.3.3
    // ============================== //

    /**
     * �������»�����, �ѹ�ʱ, ��ʹ��batchAdd()��batchUpdate()
     */
    @Deprecated
    public ContextResult createOrUpdate(String groupName, List<String> dataIds, List<String> contents, String serverId) {
        flowControl();
        ContextResult response = new ContextResult();
        // ��¼
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ��");
            return response;
        }

        PostMethod post = new PostMethod("/diamond-server/admin.do?method=updateOrCreateConfig");
        // ��������ʱʱ��
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            String dataIdParams = Joiner.on(",").join(dataIds);
            String contentParams = Joiner.on(",").join(contents);

            NameValuePair dataId_value = new NameValuePair("dataIds", dataIdParams);
            NameValuePair group_value = new NameValuePair("group", groupName);
            NameValuePair content_value = new NameValuePair("contents", contentParams);
            // ���ò���
            post.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });
            // ���ö���

            // ���һ�����ö�����Ӧ�����

            // ִ�з���������http״̬��
            int status = client.executeMethod(post);
            String json = post.getResponseBodyAsString();
            try {
                // List<ConfigInfo> configInfos =
                // (List<ConfigInfo>)JSONUtils.deserializeObject(json,List.class);
                response.setReceiveResult(json);

            }
            catch (Exception e) {
                response.setSuccess(false);
            }
            // response.setReceiveResult(HtmlParserUtils.getErrorBody(post.getResponseBodyAsString()));

            if (status == HttpStatus.SC_OK) {
                response.setSuccess(true);
                response.setStatusMsg("�����޸Ĵ���ɹ�");
                log.info("�����޸Ĵ���ɹ�");
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("�����޸Ĵ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
                log.info("�����޸Ĵ���ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����");
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("�����޸Ĵ���ʧ��,ʧ��ԭ����ͨ��ContextResult��getReceiveResult()�����鿴");
                log.info("�����޸Ĵ���ʧ��:" + response.getReceiveResult());
            }

            response.setStatusCode(status);
        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("�����޸ķ���ִ�й��̷���HttpException��" + e.getMessage());
            log.error("�������޸ķ���processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)ִ�й����з���HttpException ��"
                    + e.getMessage());
            return response;
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("�����޸ķ���ִ�й��̷���IOException��" + e.getMessage());
            log.error("�������޸ķ���processPulishAfterModifiedByDefinedServerId(String dataId, String groupName, String context,String serverId)ִ�й����з���IOException ��"
                    + e.getMessage());
            return response;
        }
        finally {
            // �ͷ�������Դ
            post.releaseConnection();
        }

        return response;

    }


    public synchronized BatchContextResult<ConfigInfoEx> batchAddOrUpdate(String serverId, String groupName,
            String srcIp, String srcUser, Map<String/* dataId */, String/* content */> dataId2ContentMap) {
        // ����
        flowControl();
        // �������ؽ��
        BatchContextResult<ConfigInfoEx> response = new BatchContextResult<ConfigInfoEx>();
        // ��dataId��content��map����Ϊ��һ�����ɼ��ַ��ָ����ַ���
        List<String> dataIdAndContentList = new LinkedList<String>();
        List<String> dataIdList = new LinkedList<String>();
        for (String dataId : dataId2ContentMap.keySet()) {
            String content = dataId2ContentMap.get(dataId);
            dataIdAndContentList.add(dataId + Constants.WORD_SEPARATOR + content);
            dataIdList.add(dataId);
        }
        String allDataIdAndContent = Joiner.on(Constants.LINE_SEPARATOR).join(dataIdAndContentList);
        String allDataId = Joiner.on(Constants.WORD_SEPARATOR).join(dataIdAndContentList);

        // ��¼
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("����д����,��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ��, serverId=" + serverId);
            return response;
        }

        // ����HTTP method
        PostMethod post = new PostMethod("/diamond-server/admin.do?method=batchAddOrUpdate");
        // ��������ʱʱ��
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            // ���ò���
            NameValuePair dataId_value = new NameValuePair("allDataIdAndContent", allDataIdAndContent);
            NameValuePair group_value = new NameValuePair("group", groupName);
            NameValuePair src_ip_value = new NameValuePair("src_ip", srcIp);
            NameValuePair src_user_value = new NameValuePair("src_user", srcUser);

            post.setRequestBody(new NameValuePair[] { dataId_value, group_value, src_ip_value, src_user_value });

            // ִ�з���������http״̬��
            int status = client.executeMethod(post);
            response.setStatusCode(status);

            if (status == HttpStatus.SC_OK) {
                String json = null;
                try {
                    json = post.getResponseBodyAsString();

                    // �����л�json�ַ���, ���������������BatchContextResult��
                    List<ConfigInfoEx> configInfoExList = new LinkedList<ConfigInfoEx>();
                    Object resultObj = JSONUtils.deserializeObject(json, new TypeReference<List<ConfigInfoEx>>() {
                    });
                    if (!(resultObj instanceof List<?>)) {
                        throw new RuntimeException("����д����,�����л���Ľ������List����, json=" + json);
                    }
                    List<ConfigInfoEx> resultList = (List<ConfigInfoEx>) resultObj;
                    for (ConfigInfoEx configInfoEx : resultList) {
                        configInfoExList.add(configInfoEx);
                    }
                    response.getResult().addAll(configInfoExList);
                    // �����л��ɹ�, �������������ɹ�
                    response.setStatusMsg("����д����,����ɹ�, serverId=" + serverId + ",allDataId=" + allDataId + ",group="
                            + groupName);
                    log.info("����д�����ɹ�,serverId=" + serverId + ",allDataIdAndContent=" + allDataIdAndContent + ",group="
                            + groupName + "\njson=" + json);
                }
                catch (Exception e) {
                    response.setSuccess(false);
                    response.setStatusMsg("����д����,�����л�ʧ��, serverId=" + serverId + ",allDataId=" + allDataId + ",group="
                            + groupName);
                    log.error("����д����,�����л�ʧ��, serverId=" + serverId + ",allDataIdAndContent=" + allDataIdAndContent
                            + ",group=" + groupName + "\njson=" + json, e);
                }
            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("����д����,����ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����, serverId=" + serverId
                        + ",allDataId=" + allDataId + ",group=" + groupName);
                log.error("����д����,����ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����, serverId=" + serverId
                        + ",allDataIdAndContent=" + allDataIdAndContent + ",group=" + groupName);
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("����д����,����ʧ��, HTTP״̬��=" + status + "serverId=" + serverId + ",allDataId="
                        + allDataId + ",group=" + groupName);
                log.error("����д����,����ʧ��,״̬��:" + status + ",serverId=" + serverId + ",allDataIdAndContent="
                        + allDataIdAndContent + ",group=" + groupName);
            }
        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("����д����,����HttpException��" + e.getMessage());
            log.error("����д��������, serverId=" + serverId + ",allDataIdAndContent=" + allDataIdAndContent + ",group="
                    + groupName, e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("����д����,����IOException��" + e.getMessage());
            log.error("����д��������, serverId=" + serverId + ",allDataIdAndContent=" + allDataIdAndContent + ",group="
                    + groupName, e);
        }
        finally {
            // �ͷ�������Դ
            post.releaseConnection();
        }

        return response;
    }


    public synchronized BatchContextResult<ConfigInfoEx> batchQuery(String serverId, String groupName,
            List<String> dataIds) {
        // ����
        flowControl();
        // �������ؽ��
        BatchContextResult<ConfigInfoEx> response = new BatchContextResult<ConfigInfoEx>();
        // ��dataId��list����Ϊ��һ�����ɼ��ַ��ָ����ַ���
        String dataIdStr = Joiner.on(Constants.WORD_SEPARATOR).join(dataIds);
        // ��¼
        if (!login(serverId)) {
            response.setSuccess(false);
            response.setStatusMsg("������ѯ��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ��, serverId=" + serverId);
            return response;
        }

        // ����HTTP method
        PostMethod post = new PostMethod("/diamond-server/admin.do?method=batchQuery");
        // ��������ʱʱ��
        post.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, require_timeout);
        try {
            // ���ò���
            NameValuePair dataId_value = new NameValuePair("dataIds", dataIdStr);
            NameValuePair group_value = new NameValuePair("group", groupName);

            post.setRequestBody(new NameValuePair[] { dataId_value, group_value });

            // ִ�з���������http״̬��
            int status = client.executeMethod(post);
            response.setStatusCode(status);

            if (status == HttpStatus.SC_OK) {
                String json = null;
                try {
                    json = post.getResponseBodyAsString();

                    // �����л�json�ַ���, ���������������BatchContextResult��
                    List<ConfigInfoEx> configInfoExList = new LinkedList<ConfigInfoEx>();
                    Object resultObj = JSONUtils.deserializeObject(json, new TypeReference<List<ConfigInfoEx>>() {
                    });
                    if (!(resultObj instanceof List<?>)) {
                        throw new RuntimeException("������ѯ,�����л���Ľ������List����, json=" + json);
                    }
                    List<ConfigInfoEx> resultList = (List<ConfigInfoEx>) resultObj;
                    for (ConfigInfoEx configInfoEx : resultList) {
                        configInfoExList.add(configInfoEx);
                    }
                    response.getResult().addAll(configInfoExList);

                    // �����л��ɹ�, ����������ѯ�ɹ�
                    response.setSuccess(true);
                    response.setStatusMsg("������ѯ����ɹ�, serverId=" + serverId + ",dataIds=" + dataIdStr + ",group="
                            + groupName);
                    log.info("������ѯ����ɹ�, serverId=" + serverId + ",dataIds=" + dataIdStr + ",group=" + groupName
                            + "\njson=" + json);
                }
                catch (Exception e) {
                    response.setSuccess(false);
                    response.setStatusMsg("������ѯ�����л�ʧ��, serverId=" + serverId + ",dataIdStr=" + dataIdStr + ",group="
                            + groupName);
                    log.error("������ѯ�����л�ʧ��, serverId=" + serverId + ",dataIdStr=" + dataIdStr + ",group=" + groupName
                            + "\njson=" + json, e);
                }

            }
            else if (status == HttpStatus.SC_REQUEST_TIMEOUT) {
                response.setSuccess(false);
                response.setStatusMsg("������ѯ����ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����, serverId=" + serverId + ",dataIds="
                        + dataIdStr + ",group=" + groupName);
                log.error("������ѯ����ʱ��Ĭ�ϳ�ʱʱ��Ϊ:" + require_timeout + "����, serverId=" + serverId + ",dataIds=" + dataIdStr
                        + ",group=" + groupName);
            }
            else {
                response.setSuccess(false);
                response.setStatusMsg("������ѯ����ʧ��, HTTP״̬��=" + status + ",serverId=" + serverId + ",dataIds=" + dataIdStr
                        + ",group=" + groupName);
                log.error("������ѯ����ʧ��, ״̬��:" + status + ",serverId=" + serverId + ",dataIds=" + dataIdStr + ",group="
                        + groupName);
            }
        }
        catch (HttpException e) {
            response.setSuccess(false);
            response.setStatusMsg("������ѯ����HttpException��" + e.getMessage());
            log.error("������ѯ����, serverId=" + serverId + ",dataIds=" + dataIdStr + ",group=" + groupName, e);
        }
        catch (IOException e) {
            response.setSuccess(false);
            response.setStatusMsg("������ѯ����IOException��" + e.getMessage());
            log.error("������ѯ����, serverId=" + serverId + ",dataIds=" + dataIdStr + ",group=" + groupName, e);
        }
        finally {
            // �ͷ�������Դ
            post.releaseConnection();
        }

        return response;
    }


    /**
     * ���������󵽵����������Ǹ��µķ���, �ѹ�ʱ
     * 
     * ��ʹ��pulish(String dataId, String groupName, String content, String
     * serverId)
     */
    @SuppressWarnings("serial")
    @Deprecated
    public ContextResult createOrUpdate(String groupName, String dataId, final String content, String serverId) {
        final String dataId2 = dataId;
        final String content2 = content;
        return createOrUpdate(groupName, new ArrayList<String>() {
            {
                add(dataId2);
            }
        }, new ArrayList<String>() {
            {
                add(content2);
            }
        }, serverId);

    }


    /**
     * �������������������Ǹ��µķ���
     */
    public synchronized ContextResult pulish(String dataId, String groupName, String content, String serverId) {
        ContextResult cr = this.queryByDataIdAndGroupName(dataId, groupName, serverId);

        ConfigInfo ci = cr.getConfigInfo();
        if (ci == null) {
            log.info("����pulish()����, dataId=" + dataId + ",group=" + groupName + ",content=" + content + ",serverId="
                    + serverId);
            return this.create(dataId, groupName, content, serverId);
        }
        else {
            log.info("����pulish()����, dataId=" + dataId + ",group=" + groupName + ",content=" + content + ",serverId="
                    + serverId);
            return this.pulishAfterModified(dataId, groupName, content, serverId);
        }
    }

}
