package com.taobao.diamond.sdkapi.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.BatchContextResult;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.ContextResult;
import com.taobao.diamond.domain.DiamondConf;
import com.taobao.diamond.domain.DiamondSDKConf;
import com.taobao.diamond.domain.PageContextResult;
import com.taobao.diamond.sdkapi.DiamondSDKManager;


public class DiamondSDKManagerTest {

    private static final String GROUP = "sdk-test";

    private DiamondSDKManager diamondSDKManager;


    @Before
    public void setUp() throws Exception {
        List<DiamondConf> testDiamondConfs = new ArrayList<DiamondConf>();
        // diamond server address, port, username, password
        DiamondConf diamondConf = new DiamondConf("", "", "", "");
        testDiamondConfs.add(diamondConf);

        // ��Ч�ĵ�ַ
        List<DiamondConf> invalidDiamondConfs = new ArrayList<DiamondConf>();
        //diamond server address, port, username, password
        DiamondConf invalidConf = new DiamondConf("", "", "", "");
        invalidDiamondConfs.add(invalidConf);

        DiamondSDKConf testDiamondConf = new DiamondSDKConf(testDiamondConfs);
        testDiamondConf.setServerId("test");

        DiamondSDKConf invalidDiamondConf = new DiamondSDKConf(invalidDiamondConfs);
        invalidDiamondConf.setServerId("xtest");

        // TreeMap����key����Ȼ˳������, test��ǰxtest֮ǰ, ʹ��Ĭ�ϵ�server idΪtest
        Map<String, DiamondSDKConf> diamondSDKConfMaps = new TreeMap<String, DiamondSDKConf>();
        diamondSDKConfMaps.put("test", testDiamondConf);
        diamondSDKConfMaps.put("xtest", invalidDiamondConf);

        DiamondSDKManagerImpl diamondSDKManagerImpl = new DiamondSDKManagerImpl(2000, 2000);
        diamondSDKManagerImpl.setDiamondSDKConfMaps(diamondSDKConfMaps);

        diamondSDKManager = diamondSDKManagerImpl;
    }


    @Test
    public void test_��ȷ��ѯ_ָ��server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk query, use specific server";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());
        Assert.assertEquals(content, result.getReceiveResult());
        Assert.assertEquals("ָ��diamond�Ĳ�ѯ���", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_��ȷ��ѯ_Ĭ��server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk query, use default server";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryFromDefaultServerByDataIdAndGroupName(dataId, GROUP);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());
        Assert.assertEquals(content, result.getReceiveResult());
        Assert.assertEquals("ָ��diamond�Ĳ�ѯ���", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_��ȷ��ѯ_���ݲ�����() throws Exception {
        String dataId = UUID.randomUUID().toString();

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(null, result.getConfigInfo());
        Assert.assertEquals(null, result.getReceiveResult());
        Assert.assertEquals("ָ��diamond�Ĳ�ѯ���", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());
    }


    @Test
    public void test_��ȷ��ѯ_ʧ��() throws Exception {
        String dataId = UUID.randomUUID().toString();

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "xtest");
        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(null, result.getConfigInfo());
        Assert.assertEquals(null, result.getReceiveResult());
        Assert.assertEquals("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���", result.getStatusMsg());
        Assert.assertEquals(0, result.getStatusCode());
    }


    @Test
    public void test_ģ����ѯ_ָ��server() throws Exception {
        String dataId = "test" + UUID.randomUUID().toString();
        String content = "test sdk vague query, use specific server";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        // ��dataIdģ����ѯ
        PageContextResult<ConfigInfo> result = diamondSDKManager.queryBy("test*", GROUP, "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(1, result.getDiamondData().size());
        Assert.assertEquals(dataId, result.getDiamondData().get(0).getDataId());
        Assert.assertEquals(GROUP, result.getDiamondData().get(0).getGroup());
        Assert.assertEquals(content, result.getDiamondData().get(0).getContent());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(1, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("ָ��diamond�Ĳ�ѯ���", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        // ��groupģ����ѯ
        result = diamondSDKManager.queryBy(dataId, "sdk*", "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(1, result.getDiamondData().size());
        Assert.assertEquals(dataId, result.getDiamondData().get(0).getDataId());
        Assert.assertEquals(GROUP, result.getDiamondData().get(0).getGroup());
        Assert.assertEquals(content, result.getDiamondData().get(0).getContent());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(1, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("ָ��diamond�Ĳ�ѯ���", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        // ������ģ����ѯ
        result = diamondSDKManager.queryBy("*", GROUP, "vague", "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(1, result.getDiamondData().size());
        Assert.assertEquals(dataId, result.getDiamondData().get(0).getDataId());
        Assert.assertEquals(GROUP, result.getDiamondData().get(0).getGroup());
        Assert.assertEquals(content, result.getDiamondData().get(0).getContent());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(1, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("ָ��diamond�Ĳ�ѯ���", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getDiamondData().get(0).getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_ģ����ѯ_Ĭ��server() throws Exception {
        String dataId = "asdf" + UUID.randomUUID().toString();
        String content = "test sdk mohu query, use default server";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        PageContextResult<ConfigInfo> result =
                diamondSDKManager.queryFromDefaultServerBy("asdf*", GROUP, "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(1, result.getDiamondData().size());
        Assert.assertEquals(dataId, result.getDiamondData().get(0).getDataId());
        Assert.assertEquals(GROUP, result.getDiamondData().get(0).getGroup());
        Assert.assertEquals(content, result.getDiamondData().get(0).getContent());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(1, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("ָ��diamond�Ĳ�ѯ���", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getDiamondData().get(0).getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_ģ����ѯ_���ݲ�����() throws Exception {
        // ��dataIdģ����ѯ
        PageContextResult<ConfigInfo> result = diamondSDKManager.queryBy("abcd*", GROUP, "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(0, result.getDiamondData().size());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(0, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(0, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("ָ��diamond�Ĳ�ѯ���", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());
    }


    @Test
    public void test_ģ����ѯ_ʧ��() throws Exception {
        PageContextResult<ConfigInfo> result = diamondSDKManager.queryBy("abcd*", GROUP, "xtest", 1, 10);
        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(null, result.getDiamondData());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(0, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("��¼ʧ��,��ɴ����ԭ�������ָ����serverIdΪ�ջ򲻴���", result.getStatusMsg());
        Assert.assertEquals(0, result.getStatusCode());
    }


    @Test
    public void test_ģ����ѯת��Ϊ��ȷ��ѯ() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk query, mohu to jingque";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        // ģ����ѯ, ��û��*
        PageContextResult<ConfigInfo> result = diamondSDKManager.queryBy(dataId, GROUP, "test", 1, 10);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(1, result.getDiamondData().size());
        Assert.assertEquals(dataId, result.getDiamondData().get(0).getDataId());
        Assert.assertEquals(GROUP, result.getDiamondData().get(0).getGroup());
        Assert.assertEquals(content, result.getDiamondData().get(0).getContent());
        Assert.assertEquals(1, result.getCurrentPage());
        Assert.assertEquals(1, result.getLength());
        Assert.assertEquals(1, result.getTotalPages());
        Assert.assertEquals(1, result.getTotalCounts());
        Assert.assertEquals(10, result.getSizeOfPerPage());
        Assert.assertEquals("ָ��diamond�Ĳ�ѯ���", result.getStatusMsg());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getDiamondData().get(0).getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_��������_ָ��server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk publish, use specific server";

        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_��������_Ĭ��server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk publish, use default server";

        diamondSDKManager.pulishFromDefaultServer(dataId, GROUP, content);

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublishFromDefaultServer(id);
    }


    @Test
    public void test_��������_ָ��server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk update, use specific server";
        String newContent = "test sdk update, use specific server: new";

        // ����
        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        // ����
        diamondSDKManager.pulishAfterModified(dataId, GROUP, newContent, "test");

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(newContent, result.getConfigInfo().getContent());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_��������_Ĭ��server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk update, use default server";
        String newContent = "test sdk update, use default server: new";

        // ����
        diamondSDKManager.pulishFromDefaultServer(dataId, GROUP, content);

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        // ����
        diamondSDKManager.pulishFromDefaultServerAfterModified(dataId, GROUP, newContent);

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(newContent, result.getConfigInfo().getContent());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublishFromDefaultServer(id);
    }


    @Test
    public void test_������������_�ڶ���Ϊ����() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk twice publish";
        String newContent = "test sdk twice publish: new";

        // ��һ�η���
        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        // �ڶ��η���
        diamondSDKManager.pulish(dataId, GROUP, newContent, "test");

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(newContent, result.getConfigInfo().getContent());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_ɾ������_ָ��server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk unpublish, use specific server";

        // ����
        diamondSDKManager.pulish(dataId, GROUP, content, "test");

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        // ɾ��
        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(null, result.getConfigInfo());
    }


    @Test
    public void test_ɾ������_Ĭ��server() throws Exception {
        String dataId = UUID.randomUUID().toString();
        String content = "test sdk unpublish, use default server";

        // ����
        diamondSDKManager.pulishFromDefaultServer(dataId, GROUP, content);

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());

        // ɾ��
        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublishFromDefaultServer(id);

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, "test");
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(null, result.getConfigInfo());
    }


    @Test
    public void test_�����Ƿ����() throws Exception {
        // ����
        String dataId = UUID.randomUUID().toString();
        diamondSDKManager.pulish(dataId, GROUP, "aaa", "test");
        Assert.assertTrue(diamondSDKManager.exists(dataId, GROUP, "test"));

        // ������
        dataId = UUID.randomUUID().toString();
        Assert.assertFalse(diamondSDKManager.exists(dataId, GROUP, "test"));
    }


    // ==================== �����ӿڲ��� =================== //

    @Test
    public void test_����д_ȫ��������() {
        // ����dataId��content��map
        String baseDataId = UUID.randomUUID().toString() + "-sdkBatchWrite-";
        String baseContent = UUID.randomUUID().toString() + "-allAdd-";
        Map<String, String> dataId2ContentMap = new HashMap<String, String>();
        for (int i = 0; i < 5; i++) {
            String dataId = baseDataId + i;
            String content = baseContent + i;
            dataId2ContentMap.put(dataId, content);
        }

        // ����д
        BatchContextResult<ConfigInfoEx> response =
                this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", dataId2ContentMap);

        // ��֤���
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(200, response.getStatusCode());
        List<ConfigInfoEx> resultList = response.getResult();
        Assert.assertEquals(5, resultList.size());

        for (ConfigInfoEx configInfoEx : resultList) {
            String recvDataId = configInfoEx.getDataId();
            // �ж��յ���dataId�Ƿ���dataId2ContentMap��
            Assert.assertTrue(dataId2ContentMap.containsKey(recvDataId));
            // �ж��յ��������Ƿ��Ǹ�dataId��Ӧ������
            Assert.assertEquals(dataId2ContentMap.get(recvDataId), configInfoEx.getContent());
            // �ж�group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // �ж�״̬���Ƿ�Ϊ�����ɹ�
            Assert.assertEquals(Constants.BATCH_ADD_SUCCESS, configInfoEx.getStatus());
        }
    }


    @Test
    public void test_����д_ȫ���Ǹ���() {
        // ����dataId��content��map
        String baseDataId = UUID.randomUUID().toString() + "-sdkBatchWrite-";
        String baseContent = UUID.randomUUID().toString() + "-allUpdate-";
        Map<String, String> dataId2ContentMap = new HashMap<String, String>();
        for (int i = 0; i < 5; i++) {
            String dataId = baseDataId + i;
            String content = baseContent + i;
            dataId2ContentMap.put(dataId, content);
        }

        // ������дһ��, ����дΪ����
        BatchContextResult<ConfigInfoEx> response =
                this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", dataId2ContentMap);

        // ��֤���
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(200, response.getStatusCode());
        List<ConfigInfoEx> resultList = response.getResult();
        Assert.assertEquals(5, resultList.size());

        for (ConfigInfoEx configInfoEx : resultList) {
            String recvDataId = configInfoEx.getDataId();
            // �ж��յ���dataId�Ƿ���dataId2ContentMap��
            Assert.assertTrue(dataId2ContentMap.containsKey(recvDataId));
            // �ж��յ��������Ƿ��Ǹ�dataId��Ӧ������
            Assert.assertEquals(dataId2ContentMap.get(recvDataId), configInfoEx.getContent());
            // �ж�group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // �ж������Ƿ�ɹ�
            Assert.assertEquals(Constants.BATCH_ADD_SUCCESS, configInfoEx.getStatus());
        }

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // ignore
        }

        // ������дһ��, ����дΪ����
        response = this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", dataId2ContentMap);

        // ��֤���
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(200, response.getStatusCode());
        resultList = response.getResult();
        Assert.assertEquals(5, resultList.size());

        for (ConfigInfoEx configInfoEx : resultList) {
            String recvDataId = configInfoEx.getDataId();
            // �ж��յ���dataId�Ƿ���dataId2ContentMap��
            Assert.assertTrue(dataId2ContentMap.containsKey(recvDataId));
            // �ж��յ��������Ƿ��Ǹ�dataId��Ӧ������
            Assert.assertEquals(dataId2ContentMap.get(recvDataId), configInfoEx.getContent());
            // �ж�group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // �ж������Ƿ�ɹ�
            Assert.assertEquals(Constants.BATCH_UPDATE_SUCCESS, configInfoEx.getStatus());
        }
    }


    @Test
    public void test_����д_�����������ָ���() {
        // ����dataId��content��map, map1�ǵ�һ������������, map2�ǵڶ�������������
        String baseDataId1 = UUID.randomUUID().toString() + "-batchWriteFirst-";
        String baseContent1 = UUID.randomUUID().toString() + "-batchWriteFirstContent-";
        String baseDataId2 = UUID.randomUUID().toString() + "-batchWriteSecond-";
        String baseContent2 = UUID.randomUUID().toString() + "-batchWriteSecondContent-";

        Map<String, String> firstMap = new HashMap<String, String>();
        for (int i = 0; i < 3; i++) {
            firstMap.put(baseDataId1 + i, baseContent1 + i);
        }

        Map<String, String> secondMap = new HashMap<String, String>();
        for (int i = 0; i < 2; i++) {
            secondMap.put(baseDataId2 + i, baseContent2 + i);
        }
        secondMap.putAll(firstMap);

        // ��һ������д, ����3������
        BatchContextResult<ConfigInfoEx> firstResponse =
                this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", firstMap);
        // ��֤��һ������д�Ľ��
        Assert.assertTrue(firstResponse.isSuccess());
        Assert.assertEquals(200, firstResponse.getStatusCode());
        List<ConfigInfoEx> firstResultList = firstResponse.getResult();
        Assert.assertEquals(3, firstResultList.size());

        for (ConfigInfoEx configInfoEx : firstResultList) {
            String recvDataId = configInfoEx.getDataId();
            // �ж��յ���dataId�Ƿ���dataId2ContentMap��
            Assert.assertTrue(firstMap.containsKey(recvDataId));
            // �ж��յ��������Ƿ��Ǹ�dataId��Ӧ������
            Assert.assertEquals(firstMap.get(recvDataId), configInfoEx.getContent());
            // �ж�group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // �ж������Ƿ�ɹ�
            Assert.assertEquals(Constants.BATCH_ADD_SUCCESS, configInfoEx.getStatus());
        }

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // ignore
        }

        // �ڶ�������д, ����3������, ����2������
        BatchContextResult<ConfigInfoEx> secondResponse =
                this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", secondMap);
        // ��֤�ڶ�������д�Ľ��
        Assert.assertTrue(secondResponse.isSuccess());
        Assert.assertEquals(200, secondResponse.getStatusCode());
        List<ConfigInfoEx> secondResultList = secondResponse.getResult();
        Assert.assertEquals(5, secondResultList.size());

        for (ConfigInfoEx configInfoEx : secondResultList) {
            String recvDataId = configInfoEx.getDataId();
            // �ж��յ���dataId�Ƿ���dataId2ContentMap��
            Assert.assertTrue(secondMap.containsKey(recvDataId));
            // �ж��յ��������Ƿ��Ǹ�dataId��Ӧ������
            Assert.assertEquals(secondMap.get(recvDataId), configInfoEx.getContent());
            // �ж�group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // �ж��������Ǹ���
            if (recvDataId.startsWith(baseDataId1)) {
                Assert.assertEquals(Constants.BATCH_UPDATE_SUCCESS, configInfoEx.getStatus());
            }
            else if (recvDataId.startsWith(baseDataId2)) {
                Assert.assertEquals(Constants.BATCH_ADD_SUCCESS, configInfoEx.getStatus());
            }
            else {
                Assert.fail("������δ֪��dataId");
            }
        }
    }


    /**
     * ������ѯ, ���ٷ�ȫ���ɹ�, ȫ��ʧ�ܳ���
     */
    @Test
    public void test_������ѯ() {
        String baseDataId1 = UUID.randomUUID().toString() + "-batchUpdateFirst-";
        String baseContent1 = UUID.randomUUID().toString() + "-batchUpdateFirstContent-";
        String baseDataId2 = UUID.randomUUID().toString() + "-batchUpdateSecond-";
        String baseContent2 = UUID.randomUUID().toString() + "-batchUpdateSecondContent-";

        Map<String, String> firstMap = new HashMap<String, String>();
        List<String> firstList = new LinkedList<String>();
        for (int i = 0; i < 3; i++) {
            firstMap.put(baseDataId1 + i, baseContent1 + i);
            firstList.add(baseDataId1 + i);
        }

        Map<String, String> secondMap = new HashMap<String, String>();
        List<String> secondList = new LinkedList<String>();
        for (int i = 0; i < 2; i++) {
            secondMap.put(baseDataId2 + i, baseContent2 + i);
            secondList.add(baseDataId2 + i);
        }

        // ��������
        BatchContextResult<ConfigInfoEx> addResult =
                this.diamondSDKManager.batchAddOrUpdate("test", GROUP, "xxx", "xxx", firstMap);
        // ��֤���
        Assert.assertTrue(addResult.isSuccess());
        Assert.assertEquals(200, addResult.getStatusCode());
        List<ConfigInfoEx> resultList = addResult.getResult();
        Assert.assertEquals(3, resultList.size());

        for (ConfigInfoEx configInfoEx : resultList) {
            String recvDataId = configInfoEx.getDataId();
            // �ж��յ���dataId�Ƿ���dataId2ContentMap��
            Assert.assertTrue(firstMap.containsKey(recvDataId));
            // �ж��յ��������Ƿ��Ǹ�dataId��Ӧ������
            Assert.assertEquals(firstMap.get(recvDataId), configInfoEx.getContent());
            // �ж�group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // �ж������Ƿ�ɹ�
            Assert.assertEquals(Constants.BATCH_ADD_SUCCESS, configInfoEx.getStatus());
        }

        // ������ѯ, �ɹ�
        BatchContextResult<ConfigInfoEx> queryResult = this.diamondSDKManager.batchQuery("test", GROUP, firstList);
        // ��֤���
        Assert.assertTrue(queryResult.isSuccess());
        Assert.assertEquals(200, queryResult.getStatusCode());
        List<ConfigInfoEx> queryResultList = queryResult.getResult();
        Assert.assertEquals(3, queryResultList.size());

        for (ConfigInfoEx configInfoEx : queryResultList) {
            String recvDataId = configInfoEx.getDataId();
            // �ж��յ���dataId�Ƿ���dataId2ContentMap��
            Assert.assertTrue(firstMap.containsKey(recvDataId));
            // �ж��յ��������Ƿ��Ǹ�dataId��Ӧ������
            Assert.assertEquals(firstMap.get(recvDataId), configInfoEx.getContent());
            // �ж�group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // �ж������Ƿ�ɹ�
            Assert.assertEquals(Constants.BATCH_QUERY_EXISTS, configInfoEx.getStatus());
        }

        // ������ѯ, ʧ��
        queryResult = this.diamondSDKManager.batchQuery("test", GROUP, secondList);
        // ��֤���
        Assert.assertTrue(queryResult.isSuccess());
        Assert.assertEquals(200, queryResult.getStatusCode());
        queryResultList = queryResult.getResult();
        Assert.assertEquals(2, queryResultList.size());

        for (ConfigInfoEx configInfoEx : queryResultList) {
            String recvDataId = configInfoEx.getDataId();
            // �ж��յ���dataId�Ƿ���dataId2ContentMap��
            Assert.assertTrue(secondMap.containsKey(recvDataId));
            // ��ѯʧ������Ϊ��
            Assert.assertNull(configInfoEx.getContent());
            // �ж�group
            Assert.assertEquals(GROUP, configInfoEx.getGroup());
            // �ж������Ƿ�ɹ�
            Assert.assertEquals(Constants.BATCH_QUERY_NONEXISTS, configInfoEx.getStatus());
        }

    }


    @Test
    public void test_����ԴIP��Դ�û���Ϣ() {
        String dataId = UUID.randomUUID().toString();
        String content = "test add src ip and src user";
        String serverId = "test";
        String srcIp = "xxx";
        String srcUser = "xxx";

        diamondSDKManager.publish(dataId, GROUP, content, serverId, srcIp, srcUser);

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, serverId);

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());
        Assert.assertEquals(content, result.getReceiveResult());
        Assert.assertEquals(200, result.getStatusCode());

        long id = result.getConfigInfo().getId();
        diamondSDKManager.unpublish("test", id);
    }


    @Test
    public void test_����ԴIP��Դ�û���Ϣ() {
        String dataId = UUID.randomUUID().toString();
        String content = "test update src ip and src user";
        String serverId = "test";
        String srcIp = "xxx";
        String srcUser = "xxx";

        // ����
        diamondSDKManager.publish(dataId, GROUP, content, serverId, srcIp, srcUser);

        ContextResult result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, serverId);

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(content, result.getConfigInfo().getContent());
        Assert.assertEquals(content, result.getReceiveResult());
        Assert.assertEquals(200, result.getStatusCode());

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            // ignore
        }

        // ����
        String newContent = "test update src ip and src user =====";
        String newSrcUser = "xxx";
        diamondSDKManager.publishAfterModified(dataId, GROUP, newContent, serverId, srcIp, newSrcUser);

        result = diamondSDKManager.queryByDataIdAndGroupName(dataId, GROUP, serverId);

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(dataId, result.getConfigInfo().getDataId());
        Assert.assertEquals(GROUP, result.getConfigInfo().getGroup());
        Assert.assertEquals(newContent, result.getConfigInfo().getContent());
        Assert.assertEquals(newContent, result.getReceiveResult());
        Assert.assertEquals(200, result.getStatusCode());
    }

}
