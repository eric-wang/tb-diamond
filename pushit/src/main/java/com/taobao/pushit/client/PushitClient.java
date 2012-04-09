/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.client;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.gecko.core.util.StringUtils;
import com.taobao.gecko.service.Connection;
import com.taobao.gecko.service.ConnectionLifeCycleListener;
import com.taobao.gecko.service.RemotingClient;
import com.taobao.gecko.service.RemotingFactory;
import com.taobao.gecko.service.RequestProcessor;
import com.taobao.gecko.service.config.ClientConfig;
import com.taobao.gecko.service.exception.NotifyRemotingException;
import com.taobao.pushit.commons.AddrUtils;
import com.taobao.pushit.commons.ClientInterest;
import com.taobao.pushit.commons.Constants;
import com.taobao.pushit.commons.ClientLoggerInit;
import com.taobao.pushit.network.InterestCommand;
import com.taobao.pushit.network.NotifyCommand;
import com.taobao.pushit.network.PushitWireFormatType;


/**
 * Pushit�ͻ���
 * <p>
 * ���͵������£� <blockquote>
 * 
 * <pre>
 * PushitClient ptClient=new PushitClient("host:port",new NotifyListener(){
 *             public void onNotify(String dataId,String group,String message){
 *                     doSomething...
 *             }
 *             
 * });
 * ptClient.interest(dataId,group);
 * ptClient.push(notifyDataId,notifyGroup);
 * </pre>
 * 
 * </blockquote>
 * 
 * <p>
 * 
 * @author boyan
 * @Date 2011-5-25
 * 
 */
public class PushitClient {
    
    static {
        try {
            ClientLoggerInit.initLog();
        }
        catch(Throwable t) {
            // ignore
        }
    }

    private RemotingClient remotingClient;
    private final NotifyListener notifyListener;
    private String serverUrl;
    static final Log log = LogFactory.getLog(PushitClient.class);

    private final List<ClientInterest> clientInterests;

    /**
     * ��ʼ��Pushit�ͻ���,Ĭ�����ӳ�ʱΪ30�룬������push֪ͨ���޷�����֪ͨ
     * 
     * @param servers
     *            �������б�����"host:port host:port..."���ַ���
     * @throws IOException
     *             �����쳣
     * @throws InterruptedException
     *             �������ӣ���Ӧ�ж��׳����쳣
     */
    public PushitClient(String servers) throws IOException, InterruptedException {
        this(servers, (NotifyListener) null);
    }


    /**
     * ��ʼ��Pushit�ͻ���,Ĭ�����ӳ�ʱΪ30��
     * 
     * @param servers
     *            �������б�����"host:port host:port..."���ַ���
     * @param notifyListener
     *            ֪ͨ������
     * @throws IOException
     *             �����쳣
     * @throws InterruptedException
     *             �������ӣ���Ӧ�ж��׳����쳣
     */
    public PushitClient(String servers, NotifyListener notifyListener) throws IOException, InterruptedException {
        this(servers, notifyListener, 30000L);
    }


    /**
     * ��ʼ��Pushit�ͻ���
     * 
     * @param servers
     *            �������б�����"host:port host:port..."���ַ���
     * @param notifyListener
     *            ֪ͨ������
     * @param connectTimeoutInMills
     *            ���ӳ�ʱ����λ����
     * @throws IOException
     *             �����쳣
     * @throws InterruptedException
     *             �������ӣ���Ӧ�ж��׳����쳣
     */
    public PushitClient(String servers, NotifyListener notifyListener, long connectTimeoutInMills) throws IOException,
            InterruptedException {
        super();
        this.notifyListener = notifyListener;
        if (connectTimeoutInMills <= 0) {
            throw new IllegalArgumentException("connectTimeoutInMills must be great than zero");
        }
        clientInterests = new CopyOnWriteArrayList<ClientInterest>();
        initRemotingClient(connectTimeoutInMills);
        connect(servers);
    }


    private void initRemotingClient(long connectTimeoutInMills) throws IOException {
        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setWireFormatType(new PushitWireFormatType());
        // clientConfig.setIdleTime(-1);// ��ֹ�������
        clientConfig.setConnectTimeout(connectTimeoutInMills);
        remotingClient = RemotingFactory.newRemotingClient(clientConfig);
        remotingClient.registerProcessor(NotifyCommand.class, new RequestProcessor<NotifyCommand>() {

            public ThreadPoolExecutor getExecutor() {
                return null;
            }


            public void handleRequest(NotifyCommand request, Connection conn) {
                if (notifyListener != null) {
                    notifyListener.onNotify(request.getDataId(), request.getGroup(), request.getMessage());
                }
            }

        });
        remotingClient.addConnectionLifeCycleListener(new ReconnectionListener());

        try {
            remotingClient.start();
        }
        catch (NotifyRemotingException e) {
            throw new IOException(e);
        }
    }


    private void connect(String servers) throws InterruptedException, IOException {
        if (StringUtils.isBlank(servers)) {
            throw new IllegalArgumentException("blank servers");
        }
        String[] hosts = servers.split(",");
        if (hosts.length <= 0) {
            throw new IllegalArgumentException("Empty hosts");
        }
        String host = getTargetHost(hosts);
        if (host.equals(serverUrl)) {
            return;
        }
        // �رվɵ�
        if (!StringUtils.isBlank(serverUrl)) {

            try {
                log.info("Closing " + serverUrl);
                remotingClient.close(serverUrl, false);
            }
            catch (NotifyRemotingException e) {
                throw new IOException(e);
            }
        }
        // �����µ�
        serverUrl = AddrUtils.getUrlFromHost(host);
        try {
            log.info("Connecting to " + serverUrl);
            remotingClient.connect(serverUrl);
            remotingClient.awaitReadyInterrupt(serverUrl);
        }
        catch (NotifyRemotingException e) {
            throw new IOException(e);
        }
    }


    /**
     * ���ص�ǰ���ӵķ�������URL
     * 
     * @return
     */
    public String getServerUrl() {
        return serverUrl;
    }


    private String getTargetHost(String[] hosts) {
        Random rand = new SecureRandom();
        int targetIndex = rand.nextInt(hosts.length);
        return hosts[targetIndex];
    }


    /**
     * ע�����Ȥ��Ĭ�Ϸ����dataId������ָ����֪ͨ�ʹ��ʱ��ص�NotifyListener
     * 
     * @param dataId
     * @throws IOException
     */
    public void interest(String dataId) throws IOException {
        this.interest(dataId, Constants.DEFAULT_GROUP);
    }


    /**
     * ע�����Ȥ��dataId��group������ָ����֪ͨ�ʹ��ʱ��ص�NotifyListener
     * 
     * @param dataId
     * @param group
     * @throws IOException
     */
    public void interest(String dataId, String group) throws IOException {
        List<ClientInterest> clientInterests = new ArrayList<ClientInterest>();
        clientInterests.add(new ClientInterest(dataId, group));
        this.interest(clientInterests);
    }


    /**
     * ע�����Ȥ��dataId��group�б�����ָ����֪ͨ�ʹ��ʱ��ص�NotifyListener
     * 
     * @param clientInterests
     * @throws IOException
     */
    public void interest(List<ClientInterest> clientInterests) throws IOException {
        checkParams(clientInterests);
        this.clientInterests.addAll(clientInterests);
        try {
            remotingClient.sendToGroup(serverUrl, new InterestCommand(clientInterests));
        }
        catch (NotifyRemotingException e) {
            throw new IOException("Interests failed", e);
        }

    }


    private void checkParams(List<ClientInterest> clientInterests) {
        if (notifyListener == null) {
            throw new IllegalStateException("Null notifyListener for interests");
        }
        for (ClientInterest clientInterest : clientInterests) {
            if (StringUtils.isBlank(clientInterest.dataId)) {
                throw new IllegalArgumentException("Blank dataId");
            }
            checkCharacter(clientInterest.dataId);
            if (clientInterest.group != null) {
                checkCharacter(clientInterest.group);
            }

        }
    }


    private void checkCharacter(String dataId) {
        if (dataId.contains(" ")) {
            throw new IllegalArgumentException("DataId contains blank");
        }
        if (dataId.contains(PushitWireFormatType.BLANK_REPLACE)) {
            throw new IllegalArgumentException("DataId contains invalid character");
        }
    }


    /**
     * ����֪ͨ
     * 
     * @param dataId
     *            ֪ͨ����dataId
     * @param group
     *            ֪ͨ���ķ���
     * @param message
     *            ������Ϣ���ݣ�����
     * @throws IOException
     */
    public void push(String dataId, String group, String message) throws IOException {
        if (StringUtils.isBlank(dataId)) {
            throw new IllegalArgumentException("Blank dataId");
        }
        checkCharacter(dataId);
        if (StringUtils.isBlank(group)) {
            throw new IllegalArgumentException("Blank group");
        }
        checkCharacter(group);
        try {
            remotingClient.sendToGroup(serverUrl, new NotifyCommand(dataId, group, message));
        }
        catch (NotifyRemotingException e) {
            throw new IOException("Push failed", e);
        }
    }


    /**
     * ����֪ͨ��Ĭ�Ϸ���
     * 
     * @param dataId
     *            ֪ͨ����dataId
     * @param message
     *            ������Ϣ���ݣ�����
     * @throws IOException
     */
    public void push(String dataId, String message) throws IOException {
        this.push(dataId, Constants.DEFAULT_GROUP, message);
    }


    public void stop() throws IOException {
        try {
            remotingClient.stop();
        }
        catch (NotifyRemotingException e) {
            throw new IOException(e);
        }
    }

    private class ReconnectionListener implements ConnectionLifeCycleListener {

        public void onConnectionCreated(Connection conn) {
        }


        public void onConnectionClosed(Connection conn) {
        }


        public void onConnectionReady(Connection conn) {
            if (conn.isConnected()) {
                try {
                    if (!clientInterests.isEmpty()) {
                        conn.send(new InterestCommand(clientInterests));
                        log.warn("�������Ӳ�����clientInterests��" + conn);
                    }
                }
                catch (Exception e) {
                    log.error("����clientInterestsʧ��:" + conn, e);
                }
            }
            else {
                log.error("��Ч������" + conn);
            }

        }

    }

}
