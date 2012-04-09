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

import static com.taobao.diamond.common.Constants.WORD_SEPARATOR;
import static com.taobao.pushit.commons.Constants.DEFAULT_GROUP;
import static com.taobao.pushit.commons.Constants.REAPER_ID;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.client.ContentIdentityPattern;
import com.taobao.diamond.client.DiamondConfigure;
import com.taobao.diamond.client.DiamondPublisher;
import com.taobao.diamond.client.DiamondSubscriber;
import com.taobao.diamond.client.jmx.DiamondClientUtil;
import com.taobao.diamond.client.processor.ServerAddressProcessor;
import com.taobao.diamond.client.task.PublishTask;
import com.taobao.diamond.client.task.UnpublishTask;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.configinfo.PublishCacheData;
import com.taobao.diamond.mockserver.MockServer;
import com.taobao.diamond.notify.utils.task.TaskManager;
import com.taobao.diamond.utils.AppNameUtils;
import com.taobao.diamond.utils.ContentUtils;
import com.taobao.diamond.utils.LoggerInit;
import com.taobao.diamond.utils.SimpleFlowData;
import com.taobao.pushit.client.NotifyListener;
import com.taobao.pushit.client.PushitClient;


/**
 * Diamond������Ĭ��ʵ�� �÷����߲�ϣ���û�ֱ��ʹ�ã�������Ϊ�������ķ����ߵĻ����� ���basestone
 * manager����һ��������DefaultDiamondPublisher��ÿ����Ⱥ��һ��������DefaultDiamondPublisher
 * 
 * @author leiwen
 * 
 */
class DefaultDiamondPublisher implements DiamondPublisher {

    private static final Log log = LogFactory.getLog(DefaultDiamondPublisher.class);

    private String userName = Constants.DEFAULT_USERNAME;
    private String password = Constants.DEFAULT_PASSWORD;

    private int requestTimeout;

    private HttpClient httpClient;

    private PushitClient pushitClient;

    private ServerAddressProcessor serverAddressProcessor;

    private ScheduledExecutorService scheduledExecutor;

    private ScheduledExecutorService scheduler;

    private TaskManager publishTaskManager;

    private DefaultDiamondSubscriber diamondSubscriber;

    private volatile DiamondConfigure diamondConfigure;

    private final SimpleFlowData flowData = new SimpleFlowData(10, 2000);

    private final AtomicInteger domainNamePos = new AtomicInteger(0);

    private final ConcurrentHashMap<String/* dataId+group */, PublishCacheData> cache =
            new ConcurrentHashMap<String, PublishCacheData>();

    private final CopyOnWriteArrayList<String> pushitClientCache = new CopyOnWriteArrayList<String>();

    private final long REPORT_INTERVAL = 20L;

    private volatile boolean isRun = false;

    private String appName;
    // ��Ⱥ��ʶ
    private String clusterType;

    // ȱʡΨһ�Ա�ʶ��ȡpattern
    private ContentIdentityPattern defaultPattern = new URLContentIdentityPattern();

    private PublishTaskProcessor publishProcessor;

    private UnpublishTaskProcessor unpublishProcessor;

    private PublishAllTaskProcessor pubAllProcessor;

    static {
        LoggerInit.initLogFromBizLog();
    }

    public static class Builder {
        private DiamondConfigure diamondConfigure;
        private String appName;
        private String type;


        public Builder(String type) {
            this.type = type;
            this.diamondConfigure = new DiamondConfigure(this.type);
        }


        public Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }


        public Builder setDiamondConfigure(DiamondConfigure diamondConfigure) {
            this.diamondConfigure = diamondConfigure;
            return this;
        }


        public DiamondPublisher build() {
            return new DefaultDiamondPublisher(this);
        }
    }


    private DefaultDiamondPublisher(Builder builder) {
        this.diamondConfigure = builder.diamondConfigure;
        this.appName = builder.appName;
        if (this.appName == null) {
            this.appName = getAppName();
        }
        this.clusterType = builder.type;

        publishProcessor = new PublishTaskProcessor();
        publishProcessor.setDiamondPublisher(this);

        unpublishProcessor = new UnpublishTaskProcessor();
        unpublishProcessor.setDiamondPublisher(this);

        pubAllProcessor = new PublishAllTaskProcessor();
        pubAllProcessor.setDiamondPublisher(this);
    }


    HttpClient getHttpClient() {
        return httpClient;
    }


    void setUserName(String userName) {
        this.userName = userName;
    }


    void setPassword(String password) {
        this.password = password;
    }


    void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }


    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }


    public DiamondConfigure getDiamondConfigure() {
        return this.diamondConfigure;
    }


    public void setDiamondConfigure(DiamondConfigure diamondConfigure) {
        if (!isRun) {
            this.diamondConfigure = diamondConfigure;
        }
        else {
            copyDiamondConfigure(diamondConfigure);
        }
    }


    public TaskManager getPublishTaskManager() {
        return publishTaskManager;
    }


    public PushitClient getPushitClient() {
        return pushitClient;
    }


    public void setDiamondSubscriber(DiamondSubscriber diamondSubscriber) {
        this.diamondSubscriber = (DefaultDiamondSubscriber) diamondSubscriber;
        publishProcessor.setDiamondSubscriber(this.diamondSubscriber);
        unpublishProcessor.setDiamondSubscriber(this.diamondSubscriber);
        pubAllProcessor.setDiamondSubscriber(this.diamondSubscriber);
    }


    public void addDataId(String dataId, String group, String configInfo) {
        String key = dataId + "-" + group;
        PublishCacheData pubCacheData = new PublishCacheData(dataId, group, configInfo);
        this.cache.put(key, pubCacheData);
        DiamondClientUtil.addPubDataId(clusterType, key);
        log.info("����������cache�������dataId:" + dataId + ",group:" + group);
        this.start();
    }


    public void removeDataId(String dataId, String group) {
        String key = dataId + "-" + group;
        this.cache.remove(key);
        log.info("������cache���Ƴ���dataId:" + dataId + ",group:" + group);
    }


    public synchronized void start() {
        if (isRun) {
            return;
        }

        if (null == scheduledExecutor || scheduledExecutor.isTerminated()) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        if (null == scheduler || scheduler.isTerminated()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        serverAddressProcessor = new ServerAddressProcessor(this.diamondConfigure, this.scheduledExecutor);
        serverAddressProcessor.setClusterType(clusterType);
        serverAddressProcessor.start();

        // ����domainNamePosֵ
        randomDomainNamePos();

        initHttpClient();

        try {
            initPushitClient();
        }
        catch (Exception e) {
            log.error("������ʵʱ֪ͨ�ͻ��˳�ʼ������", e);
        }

        publishTaskManager = new TaskManager("publish task manager thread");

        // ��ʼ�����
        isRun = true;

        // ��ѯ����Ƿ񷢲��ɹ�
        rotateCheckAndPublish();
    }


    public void close() {
        if (!isRun) {
            return;
        }
        serverAddressProcessor.stop();
        httpClient = null;
        try {
            pushitClient.stop();
        }
        catch (Exception e) {
            log.error("�ر�pushit client����", e);
        }
        isRun = false;
    }


    public void publish(String dataId, String group, String configInfo) {
        this.publish(dataId, group, configInfo, defaultPattern);
    }


    public void publish(String dataId, String group, String configInfo, ContentIdentityPattern pattern) {
        // ����Ӧ��������, ˲�䴴����������, �ڴ渺�ɻ�ܸ�
        this.flowControl();

        // У�������������ݵĸ�ʽ
        ContentUtils.verifyIncrementPubContent(configInfo);

        // У��dataId��group�Ƿ�Ϊ��
        this.validate(dataId, group, configInfo);

        // ��ȡ���ݵ�Ψһ��ʶ
        String identify = pattern.getContentIdentity(configInfo);
        // ƴ�ӳ�Ҫ����server������
        String content = identify + WORD_SEPARATOR + configInfo;

        this.addDataId(dataId, group, content);

        String taskType = dataId + "-" + group + "-pub";
        PublishTask publishTask = new PublishTask(dataId, group, content);

        this.publishTaskManager.addProcessor(taskType, publishProcessor);
        this.publishTaskManager.addTask(taskType, publishTask, true);

        try {
            if (this.pushitClientCache.addIfAbsent(dataId + Constants.WORD_SEPARATOR + group)) {
                this.pushitClient.interest(dataId, group);
            }
        }
        catch (Exception e) {
            log.error("ע��ʵʱ֪ͨ����: dataId=" + dataId + ", group=" + group, e);
        }

    }


    public boolean syncPublish(String dataId, String group, String configInfo, long timeout) {
        return this.syncPublish(dataId, group, configInfo, timeout, defaultPattern);
    }


    public boolean syncPublish(String dataId, String group, String configInfo, long timeout,
            ContentIdentityPattern pattern) {
        this.flowControl();

        // У�������������ݵĸ�ʽ
        ContentUtils.verifyIncrementPubContent(configInfo);

        // У��dataId��group�Ƿ�Ϊ��
        this.validate(dataId, group, configInfo);

        // ��ȡ���ݵ�Ψһ��ʶ
        String identify = pattern.getContentIdentity(configInfo);
        // ƴ�ӳ�Ҫ����server������
        String content = identify + WORD_SEPARATOR + configInfo;

        // ȥ��cache�ж�Ӧ����, �������ٽ����Ƿ�ɹ��ļ��
        this.removeDataId(dataId, group);

        // ͬ������
        return this.publishProcessor.syncProcess(dataId, group, content, timeout);
    }


    public void unpublish(String dataId, String group, String configInfo) {
        this.flowControl();

        this.validate(dataId, group, configInfo);

        this.removeDataId(dataId, group);

        String taskType = dataId + "-" + group + "-unpub";
        UnpublishTask task = new UnpublishTask(dataId, group, configInfo);

        this.publishTaskManager.addProcessor(taskType, unpublishProcessor);
        this.publishTaskManager.addTask(taskType, task, true);
    }


    public boolean syncUnpublish(String dataId, String group, String configInfo, long timeout) {
        this.flowControl();

        this.validate(dataId, group, configInfo);

        this.removeDataId(dataId, group);

        return this.unpublishProcessor.syncProcess(dataId, group, configInfo, timeout);
    }


    public void publishAll(String dataId, String group, String configInfo) {
        this.flowControl();

        this.validate(dataId, group, configInfo);

        String taskType = dataId + "-" + group + "-all";
        PublishTask publishTask = new PublishTask(dataId, group, configInfo);
        this.publishTaskManager.addProcessor(taskType, this.pubAllProcessor);
        this.publishTaskManager.addTask(taskType, publishTask, true);

        // ȫ�����ݲ�������, ���ܳ����໥���ǵ����
        /*
         * try { if (this.pushitClientCache.addIfAbsent(dataId +
         * Constants.WORD_SEPARATOR + group)) {
         * this.pushitClient.interest(dataId, group); } } catch (Exception e) {
         * log.error("ע��ʵʱ֪ͨ����: dataId=" + dataId + ", group=" + group, e); }
         */
    }
    
    
    public boolean syncPublishAll(String dataId, String group, String configInfo, long timeout) {
        this.flowControl();
        
        this.validate(dataId, group, configInfo);
        
        this.removeDataId(dataId, group);
        
        return this.pubAllProcessor.syncProcess(dataId, group, configInfo, timeout);
    }


    public void scheduledReport() {
        final String ip = this.getHostAddress();

        this.scheduler.scheduleWithFixedDelay(new Runnable() {

            public void run() {
                try {
                    StringBuilder msg = new StringBuilder();
                    msg.append("R").append(ip).append("-");

                    for (PublishCacheData pubCache : cache.values()) {
                        String dataId = pubCache.getDataId();
                        String group = pubCache.getGroup();
                        msg.append(dataId).append(WORD_SEPARATOR).append(group);
                        msg.append(",");
                    }

                    pushitClient.push(REAPER_ID, DEFAULT_GROUP, msg.toString());
                }
                catch (Exception e) {
                    log.error("����report messageʧ��", e);
                }

            }

        }, 0L, REPORT_INTERVAL, TimeUnit.SECONDS);
    }


    void publishNew(String dataId, String group, String configInfo) throws Exception {

        // ��¼
        login();

        // ����ȫ�µ�����
        processPublish(dataId, group, configInfo);
    }


    void publishUpdate(String dataId, String group, String configInfo) throws Exception {

        // ��¼
        login();

        // �������º������
        processUpdate(dataId, group, configInfo);
    }


    void publishRemove(String dataId, String group, String configInfo) throws Exception {

        // ��¼
        login();

        // ɾ������
        processRemove(dataId, group, configInfo);
    }


    void updateAll(String dataId, String group, String configInfo) throws Exception {

        // ��¼
        login();

        // ȫ������
        processUpdateAll(dataId, group, configInfo);
    }


    public void awaitPublishFinish() throws InterruptedException {
        publishTaskManager.await();

    }


    private void rotateCheckAndPublish() {
        scheduledExecutor.scheduleAtFixedRate(new Runnable() {

            public void run() {
                for (PublishCacheData cacheData : cache.values()) {
                    checkAndPublish(cacheData);
                }
            }

        }, 10, 15, TimeUnit.MINUTES);
    }


    private void checkAndPublish(PublishCacheData cacheData) {
        String dataId = cacheData.getDataId();
        String group = cacheData.getGroup();
        String configInfo = cacheData.getConfigInfo();

        String taskType = dataId + "-" + group + "-pub";
        PublishTask publishTask = new PublishTask(dataId, group, configInfo);

        this.publishTaskManager.addProcessor(taskType, publishProcessor);
        this.publishTaskManager.addTask(taskType, publishTask, true);
    }


    private String getPublishUri() {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.BASESTONE_POST_URI);
        uriBuilder.append("?method=postConfig");
        return uriBuilder.toString();
    }


    private String getUpdateUri() {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.BASESTONE_POST_URI);
        uriBuilder.append("?method=updateConfig");
        return uriBuilder.toString();
    }


    private String getRemoveUri() {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.BASESTONE_POST_URI);
        uriBuilder.append("?method=deleteConfig");
        return uriBuilder.toString();
    }


    private String getUpdateAllUri() {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.BASESTONE_POST_URI);
        uriBuilder.append("?method=updateAll");
        return uriBuilder.toString();
    }


    private String getLoginUri() {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.HTTP_URI_LOGIN);
        uriBuilder.append("?method=login");
        return uriBuilder.toString();
    }


    private void randomDomainNamePos() {
        Random rand = new Random();
        List<String> domainList = this.diamondConfigure.getDomainNameList();
        if (!domainList.isEmpty()) {
            this.domainNamePos.set(rand.nextInt(domainList.size()));
        }
    }


    private void initHttpClient() {
        if (MockServer.isTestMode()) {
            return;
        }
        HostConfiguration hostConfiguration = new HostConfiguration();
        hostConfiguration.setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.closeIdleConnections(60 * 1000);

        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setStaleCheckingEnabled(diamondConfigure.isConnectionStaleCheckingEnabled());
        params.setMaxConnectionsPerHost(hostConfiguration, diamondConfigure.getMaxHostConnections());
        params.setMaxTotalConnections(diamondConfigure.getMaxTotalConnections());
        params.setConnectionTimeout(diamondConfigure.getConnectionTimeout());
        params.setSoTimeout(requestTimeout);

        connectionManager.setParams(params);

        httpClient = new HttpClient(connectionManager);
        httpClient.setHostConfiguration(hostConfiguration);
    }


    private void initPushitClient() throws Exception {
        String servers = getPushitServers();
        pushitClient = new PushitClient(servers, new NotifyListener() {

            public void onNotify(String dataId, String group, String message) {
                if ("publish".equals(message)) {
                    // �յ�֪ͨ��������������
                    log.info("�յ�֪ͨ����ʼ������������: dataId=" + dataId + ", group=" + group);
                    innerPublish(dataId, group);
                }
                else if ("remove".equals(message)) {
                    log.info("�յ�remove֪ͨ����ʼ������������: dataId=" + dataId + ", group=" + group);
                    innerRemove(dataId, group);
                }
            }

        });
    }


    private void innerPublish(String dataId, String group) {
        PublishCacheData cacheData = cache.get(dataId + "-" + group);
        if (cacheData != null) {
            checkAndPublish(cacheData);
        }
    }


    private void innerRemove(String dataId, String group) {
        PublishCacheData cacheData = cache.get(dataId + "-" + group);
        if (cache != null) {
            this.unpublish(dataId, group, cacheData.getConfigInfo());
        }
    }


    private String getPushitServers() {
        StringBuilder result = new StringBuilder();
        List<String> serverList = this.diamondConfigure.getPushitDomainNameList();
        for (String server : serverList) {
            result.append(server).append(",");
        }
        result.deleteCharAt(result.length() - 1);

        return result.toString();
    }


    private void login() throws Exception {
        PostMethod loginMethod = new PostMethod(getLoginUri());

        NameValuePair username_value = new NameValuePair("username", userName);
        NameValuePair password_value = new NameValuePair("password", password);

        loginMethod.addRequestHeader(Constants.APPNAME, this.appName);
        loginMethod.setRequestBody(new NameValuePair[] { username_value, password_value });

        this.configureHttpMethod(loginMethod);

        log.info("server list:" + this.diamondConfigure.getDomainNameList());
        log.info("index=" + this.domainNamePos.get());
        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());

        try {
            int statusCode = httpClient.executeMethod(loginMethod);
            String responseMsg = loginMethod.getResponseBodyAsString();
            if (statusCode == HttpStatus.SC_OK) {
                log.info("��¼�ɹ�");
            }
            else {
                log.error("��¼ʧ�ܣ�ԭ��Ϊ��" + responseMsg + ", ������Ϊ��" + statusCode);
                throw new Exception("��¼ʧ��, ԭ����鿴�ͻ�����־");
            }

        }
        finally {
            loginMethod.releaseConnection();
        }
    }


    private void processPublish(String dataId, String group, String content) throws Exception {
        PostMethod publishMethod = new PostMethod(getPublishUri());

        NameValuePair dataId_value = new NameValuePair("dataId", dataId);
        NameValuePair group_value = new NameValuePair("group", group);
        NameValuePair content_value = new NameValuePair("content", content);

        publishMethod.addRequestHeader(Constants.APPNAME, this.appName);
        publishMethod.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });

        this.configureHttpMethod(publishMethod);

        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());

        try {
            int statusCode = httpClient.executeMethod(publishMethod);
            String responseMsg = publishMethod.getResponseBodyAsString();

            if (statusCode == HttpStatus.SC_OK) {
                log.info("���ݷ����ɹ�");
            }
            else {
                log.error("���ݷ���ʧ��, ԭ��Ϊ��" + responseMsg + ", ������Ϊ:" + statusCode);
                throw new Exception("���ݷ���ʧ��, ԭ����鿴�ͻ�����־");
            }
        }
        finally {
            publishMethod.releaseConnection();
        }
    }


    private void processUpdate(String dataId, String group, String content) throws Exception {
        PostMethod updateMethod = new PostMethod(getUpdateUri());

        NameValuePair dataId_value = new NameValuePair("dataId", dataId);
        NameValuePair group_value = new NameValuePair("group", group);
        NameValuePair content_value = new NameValuePair("content", content);

        updateMethod.addRequestHeader(Constants.APPNAME, this.appName);
        updateMethod.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });

        this.configureHttpMethod(updateMethod);

        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());

        try {
            int statusCode = httpClient.executeMethod(updateMethod);
            String responseMsg = updateMethod.getResponseBodyAsString();

            if (statusCode == HttpStatus.SC_OK) {
                log.info("���ݸ��³ɹ�");
            }
            else {
                log.error("���ݸ���ʧ��, ԭ��Ϊ��" + responseMsg + ", ������Ϊ:" + statusCode);
                throw new Exception("���ݸ���ʧ�ܣ�ԭ����鿴�ͻ�����־");
            }
        }
        finally {
            updateMethod.releaseConnection();
        }
    }


    private void processRemove(String dataId, String group, String content) throws Exception {
        PostMethod removeMethod = new PostMethod(getRemoveUri());

        NameValuePair dataId_value = new NameValuePair("dataId", dataId);
        NameValuePair group_value = new NameValuePair("group", group);
        NameValuePair content_value = new NameValuePair("content", content);

        removeMethod.addRequestHeader(Constants.APPNAME, this.appName);
        removeMethod.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });

        this.configureHttpMethod(removeMethod);

        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());

        try {
            int statusCode = httpClient.executeMethod(removeMethod);
            String responseMsg = removeMethod.getResponseBodyAsString();

            if (statusCode == HttpStatus.SC_OK) {
                log.info("����ɾ���ɹ�");
            }
            else {
                log.error("����ɾ��ʧ��, ԭ��Ϊ��" + responseMsg + ", ������Ϊ:" + statusCode);
                throw new Exception("����ɾ��ʧ�ܣ�ԭ����鿴�ͻ�����־");
            }
        }
        finally {
            removeMethod.releaseConnection();
        }
    }


    private void processUpdateAll(String dataId, String group, String content) throws Exception {
        PostMethod updateMethod = new PostMethod(getUpdateAllUri());

        NameValuePair dataId_value = new NameValuePair("dataId", dataId);
        NameValuePair group_value = new NameValuePair("group", group);
        NameValuePair content_value = new NameValuePair("content", content);

        updateMethod.addRequestHeader(Constants.APPNAME, this.appName);
        updateMethod.setRequestBody(new NameValuePair[] { dataId_value, group_value, content_value });

        this.configureHttpMethod(updateMethod);

        httpClient.getHostConfiguration().setHost(diamondConfigure.getDomainNameList().get(domainNamePos.get()),
            diamondConfigure.getPort());

        try {
            int statusCode = httpClient.executeMethod(updateMethod);
            String responseMsg = updateMethod.getResponseBodyAsString();

            if (statusCode == HttpStatus.SC_OK) {
                log.info("����ȫ�����³ɹ�");
            }
            else {
                log.error("����ȫ������ʧ��, ԭ��Ϊ��" + responseMsg + ", ������Ϊ:" + statusCode);
                throw new Exception("����ȫ������ʧ�ܣ�ԭ����鿴�ͻ�����־");
            }
        }
        finally {
            updateMethod.releaseConnection();
        }
    }


    private void configureHttpMethod(HttpMethod method) {
        // ����socket��ʱ
        HttpMethodParams params = new HttpMethodParams();
        params.setSoTimeout(Constants.ONCE_TIMEOUT);
        method.setParams(params);
    }


    private void validate(String dataId, String group, String content) {
        if (StringUtils.isBlank(dataId) || StringUtils.isBlank(group) || StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("dataId, group, content������Ϊ��");
        }
    }


    synchronized void rotateToNextDomain() {
        int domainNameCount = diamondConfigure.getDomainNameList().size();
        int index = domainNamePos.incrementAndGet();
        if (index < 0) {
            index = -index;
        }
        domainNamePos.set(index % domainNameCount);
        if (diamondConfigure.getDomainNameList().size() > 0)
            log.warn("����/��������ʱ���ֻ�server��������" + diamondConfigure.getDomainNameList().get(domainNamePos.get()));
    }


    private void flowControl() {
        if (diamondConfigure.isUseFlowControl()) {
            flowData.addAndGet(1);

            while (flowData.getAverageCount() > diamondConfigure.getFlowControlThreshold()) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }


    private static String getAppName() {
        String appName = null;
        try {
            appName = AppNameUtils.getAppName();
        }
        catch (Throwable t) {
            log.warn("Can not getAppName. AppName = null");
            appName = null;
        }
        if (appName == null) {
            appName = "null";
        }
        else {
            appName = appName.trim();
        }
        return appName;
    }


    private void copyDiamondConfigure(DiamondConfigure diamondConfigure) {
        // TODO ��Щֵ��������ʱ��̬����?
    }


    private String getHostAddress() {
        String address = "127.0.0.1";
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                Enumeration<InetAddress> ads = ni.getInetAddresses();
                while (ads.hasMoreElements()) {
                    InetAddress ip = ads.nextElement();
                    if (!ip.isLoopbackAddress() && ip.isSiteLocalAddress()) {
                        return ip.getHostAddress();
                    }
                }
            }
        }
        catch (Exception e) {
            log.error("��ȡ������ַ����", e);
        }
        return address;
    }

}
