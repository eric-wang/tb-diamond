package com.taobao.diamond.manager.impl;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.taobao.diamond.client.ContentIdentityPattern;
import com.taobao.diamond.client.DiamondPublisher;
import com.taobao.diamond.client.DiamondSubscriber;
import com.taobao.diamond.client.impl.DiamondClientFactory;
import com.taobao.diamond.common.Constants;
import com.taobao.diamond.manager.BaseStonePubManager;
import com.taobao.diamond.manager.BaseStoneSubManager;
import com.taobao.diamond.manager.ManagerListener;


public class DefaultBaseStonePubManagerUnitTest {
    // diamond server address (ip)
    private static final String DIAMOND_SERVER_ADDR = "";
    // pushit server address (ip:port)
    private static final String PUSHIT_SERVER_ADDR = "";

    private static final String GROUP = "BS_PUB_TEST";

    private BaseStonePubManager pubManager;
    private BaseStoneSubManager subManager;
    private DiamondPublisher publisher;
    private DiamondSubscriber subscriber;

    private final AtomicBoolean notified = new AtomicBoolean();
    private final AtomicReference<String> info = new AtomicReference<String>();

    private String clusterType = "basestone";


    @Before
    public void setUp() throws Exception {
        initPubAndSub();
    }


    @After
    public void tearDown() throws Exception {
        publisher.close();
        subscriber.close();
    }


    /**
     * ���Է�����������, ��һ��, dataId������, ʹ��Ĭ�ϵ�ContentIdentityPattern
     * 
     * @throws Exception
     */
    @Test
    public void testPublish1() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            // protocal://ip:port?k=v
            String content = "";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publish(content);

            while (!notified.get()) {
                Thread.sleep(1000);
            }

            Assert.assertEquals(content, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * ���Է�����������, ǰ�����η������ݵ�Ψһ��ʶ��ͬ, ʹ��Ĭ�ϵ�ContentIdentityPattern
     * 
     * @throws Exception
     */
    @Test
    public void testPublish2() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            // protocal://ip:port?k=v
            String content = "";
            // protocal://ip:port?k=v
            String newContent = "";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });

            pubManager.publish(content);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publish(newContent);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            String expected = content + Constants.DIAMOND_LINE_SEPARATOR + newContent;
            Assert.assertEquals(expected, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * ���Է�����������, ǰ�����η������ݵ�Ψһ��ʶ��ͬ, ʹ��Ĭ�ϵ�ContentIdentityPattern
     * 
     * @throws Exception
     */
    @Test
    public void testPublish3() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            // protocal://ip:port?k=v
            String content = "";
            // protocal://ip:port?k=v
            String newContent = "";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });

            pubManager.publish(content);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publish(newContent);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(newContent, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * �����Զ����ContentIdentifyPattern
     * 
     * @throws Exception
     */
    @Test
    public void testPublishByPattern1() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is a pattern test content";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publish(dataId, group, content, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }

            Assert.assertEquals(content, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * �����Զ����ContentIdentifyPattern, ǰ�����η������ݵı�ʶ��ͬ
     * 
     * @throws Exception
     */
    @Test
    public void testPublishByPattern2() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is a pattern test content";
            String newContent = "that is a pattern test content";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publish(dataId, group, content, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publish(dataId, group, newContent, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            String expected = content + Constants.DIAMOND_LINE_SEPARATOR + newContent;
            Assert.assertEquals(expected, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * �����Զ����ContentIdentifyPattern, ǰ�����η������ݵı�ʶ��ͬ
     * 
     * @throws Exception
     */
    @Test
    public void testPublishByPattern3() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is a pattern test content";
            String newContent = "this is a new pattern test content";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publish(dataId, group, content, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publish(dataId, group, newContent, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(newContent, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * ���Է���ȫ������, dataId������
     * 
     * @throws Exception
     */
    @Test
    public void testPublishAll1() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is publish all test";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publishAll(content);

            while (!notified.get()) {
                Thread.sleep(1000);
            }

            Assert.assertEquals(content, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }

    }


    /**
     * ���Է���ȫ������, dataId����
     * 
     * @throws Exception
     */
    @Test
    public void testPublishAll2() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is publish all test";
            String newContent = "this is another publish all test";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publishAll(content);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publishAll(newContent);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(newContent, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }

    }


    /**
     * ����ɾ������, ����һ��, ɾ��һ��, ɾ����dataIdҲ��ɾ��
     * 
     * @throws Exception
     */
    @Test
    public void testUnpublish1() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is unpublish test";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publishAll(content);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.unpublish(content);

            // ��Ϊ��ȡ����Ϊnullʱ������ü�����, ��������ֻ�ܵȴ�15��, ʹTTL CacheʧЧ
            Thread.sleep(15000);

            String receivedConfigInfo = subManager.getAvailableConfigureInfomation(60000);
            Assert.assertEquals(null, receivedConfigInfo);
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * ����ɾ������, ��������, ɾ��һ��
     * 
     * @throws Exception
     */
    @Test
    public void testUnpublish2() throws Exception {
        try {
            publisher.close();
            subscriber.close();
            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is unpublish test";
            String newContent = "that is unpublish test";
            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            pubManager.publish(dataId, group, content, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content, info.get());
            notified.set(false);
            info.set(null);

            pubManager.publish(dataId, group, newContent, new CustomContentIdentityPattern());
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(content + Constants.DIAMOND_LINE_SEPARATOR + newContent, info.get());
            notified.set(false);
            info.set(null);

            pubManager.unpublish(content);
            while (!notified.get()) {
                Thread.sleep(1000);
            }
            Assert.assertEquals(newContent, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    /**
     * ���ĵ����ݲ����� ����> ���� ����> ���ĳɹ�
     * 
     * @throws Exception
     */
    @Test
    public void testSubAndPub() throws Exception {
        try {
            publisher.close();
            subscriber.close();

            String dataId = UUID.randomUUID().toString();
            String group = GROUP;
            String content = "this is func test";

            pubManager = new DefaultBaseStonePubManager(dataId, group);
            subManager = new DefaultBaseStoneSubManager(dataId, group, new ManagerListener() {

                public Executor getExecutor() {
                    return null;
                }


                public void receiveConfigInfo(String configInfo) {
                    System.out.println("���յ�������Ϣ:" + configInfo);
                    info.set(configInfo);
                    notified.set(true);
                }

            });
            // ȷ�϶��ĵ����ݲ�����
            Assert.assertFalse(subManager.exists(dataId, group));

            // �ȴ�����
            int count = 0;
            while (!notified.get() && count++ < 10) {
                Thread.sleep(500);
            }
            Assert.assertFalse(content.equals(info.get()));

            // ����
            pubManager.publish(dataId, group, content, new CustomContentIdentityPattern());

            // �ȴ�����
            count = 0;
            while (!notified.get() && count++ < 10) {
                Thread.sleep(1000);
            }

            // �ɹ����ĵ�����
            Assert.assertEquals(content, info.get());
        }
        finally {
            notified.set(false);
            info.set(null);
        }
    }


    private void initPubAndSub() throws Exception {
        publisher = DiamondClientFactory.getSingletonDiamondPublisher(clusterType);
        subscriber = DiamondClientFactory.getSingletonDiamondSubscriber(clusterType);
        publisher.getDiamondConfigure().addDomainName(DIAMOND_SERVER_ADDR);
        publisher.getDiamondConfigure().addPushitDomainName(PUSHIT_SERVER_ADDR);
        subscriber.getDiamondConfigure().addDomainName(DIAMOND_SERVER_ADDR);
        subscriber.getDiamondConfigure().addPushitDomainName(PUSHIT_SERVER_ADDR);
        publisher.getDiamondConfigure().setLocalFirst(true);
        subscriber.getDiamondConfigure().setLocalFirst(true);
        publisher.setDiamondSubscriber(subscriber);
    }

    private class CustomContentIdentityPattern implements ContentIdentityPattern {

        public String getContentIdentity(String content) {
            // �����ݵ�ǰ�ĸ��ַ���ΪΨһ��ʶ
            return content.substring(0, 4);
        }

    }

}
