/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.pushit.commons;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * pushit-client��־��ʼ��, pushit-client����־������ӡ��ĳ���ļ���, ���ļ���Ӧ�õ���־��ͬһ��Ŀ¼�У� ���Ӧ��û����root
 * logger������file appender, ���ļ���λ��Ϊ${user.home}/diamond/logs/pushit-client.log
 * 
 * @author leiwen.zh
 * 
 */
public class ClientLoggerInit {

    private static final Log log = LogFactory.getLog(ClientLoggerInit.class);

    private static volatile boolean initOK = false;

    private static final String PUSHIT_CLIENT_LOG4J_FILE = "pushit_client_log4j.properties";
    private static final String PUSHIT_CLIENT_LOGGER = "com.taobao.pushit";


    public static void initLog() {

        if (initOK) {
            return;
        }

        URL url = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null || (url = cl.getResource(PUSHIT_CLIENT_LOG4J_FILE)) == null) {
            cl = ClientLoggerInit.class.getClassLoader();
            if (cl == null || (url = cl.getResource(PUSHIT_CLIENT_LOG4J_FILE)) == null) {
                fallback();
                return;
            }
        }

        final ClassLoader pre = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(PropertyConfigurator.class.getClassLoader());
            PropertyConfigurator.configure(url);
        }
        finally {
            Thread.currentThread().setContextClassLoader(pre);
        }

        // ��ȡӦ����root logger�����õ�file appender, ���ݴ�appenderȷ��pushit.log���ڵ�Ŀ¼
        FileAppender rootFileAppender = getFileAppender(Logger.getRootLogger());
        if (rootFileAppender == null) {
            log.warn("Ӧ��û����root logger������file appender!!!");
            rootFileAppender = new FileAppender();
            rootFileAppender.setFile(System.getProperty("user.home") + "/diamond/logs/pushit-client.log");
        }

        // ����pushit logger����, ����ͬһ��log4j�����ļ��е�appender�ᱻ�ļ������е�logger����,
        // ����gecko����־Ҳ�����pushit���õ�appender
        setFileAppender(rootFileAppender, PUSHIT_CLIENT_LOGGER);

        initOK = true;
    }


    private static FileAppender getFileAppender(Logger logger) {
        Enumeration<?> allAppenders = logger.getAllAppenders();
        while (allAppenders.hasMoreElements()) {
            Object appender = allAppenders.nextElement();
            if (appender instanceof FileAppender) {
                return (FileAppender) appender;
            }
        }
        return null;
    }


    private static void setFileAppender(FileAppender rootFileAppender, String loggerName) {
        String rootLogDir = new File(rootFileAppender.getFile()).getParent();
        FileAppender logFileAppender = getFileAppender(Logger.getLogger(loggerName));
        File logFile = new File(rootLogDir, logFileAppender.getFile());
        String logFileAbsolutePath = logFile.getAbsolutePath();
        logFileAppender.setFile(logFileAbsolutePath);
        logFileAppender.activateOptions();
        log.warn("�ɹ�Ϊ" + loggerName + "���Appender. ���·��:" + logFileAbsolutePath);
    }


    private static void fallback() {
        log.warn("[Global] Failed to read pushit logger configuration, use root log configuration.");
    }
}
