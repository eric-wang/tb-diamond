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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.Page;
import com.taobao.diamond.md5.MD5;
import com.taobao.diamond.server.exception.ConfigServiceException;
import com.taobao.diamond.server.service.task.NotifyTask;
import com.taobao.diamond.server.service.task.processor.NotifyTaskProcessor;
import com.taobao.diamond.server.utils.DiamondUtils;
import com.taobao.diamond.server.utils.SystemConfig;


//@Service
public class ConfigService {
    static final Log log = LogFactory.getLog(ConfigService.class);
    // @Autowired
    private GroupService groupService;

    // @Autowired
    private DiskService diskService;

    // @Autowired
    // @Qualifier("persistService")
    private PersistService persistService;

    // @Autowired
    private NotifyService notifyService;

    // @Autowired
    private ValidationService validationService;

    private RedisService redisService;

    private TaskManagerService taskManagerService;
    
    private NotifyTaskProcessor notifyTaskProcessor;

    /**
     * content��MD5�Ļ���,keyΪgroup/dataId��valueΪmd5ֵ
     */
    // TODO  ��ֵ, ����������
    private final ConcurrentHashMap<String, String> contentMD5Cache = new ConcurrentHashMap<String, String>();


    /**
     * ����group��IP����������Ϣ��url
     * 
     * @param dataId
     * @param address
     * @param clientGroup
     *            TODO
     * @return
     */
    public String getConfigInfoPath(String dataId, String address, String clientGroup) {
        final String group = groupService.getGroupByAddress(address, dataId, clientGroup);
        return generatePath(dataId, group);
    }


    public void updateMD5Cache(ConfigInfo configInfo) {
        this.contentMD5Cache.put(generateMD5CacheKey(configInfo.getDataId(), configInfo.getGroup()), MD5.getInstance()
            .getMD5String(configInfo.getContent()));
    }


    public String getContentMD5(String dataId, String group) {
        String key = generateMD5CacheKey(dataId, group);
        String md5 = this.contentMD5Cache.get(key);
        if (md5 == null) {
            synchronized (this) {
                // ���ؼ��
                md5 = this.contentMD5Cache.get(key);
                if (md5 == null) {
                    return null;
                }
                else
                    return md5;
            }
        }
        else
            return md5;
    }


    String generateMD5CacheKey(String dataId, String group) {
        String key = group + "/" + dataId;
        return key;
    }


    String generatePath(String dataId, final String group) {
        if (!StringUtils.hasLength(dataId) || StringUtils.containsWhitespace(dataId))
            throw new IllegalArgumentException("��Ч��dataId");

        if (!StringUtils.hasLength(group) || StringUtils.containsWhitespace(group))
            throw new IllegalArgumentException("��Ч��group");
        String fnDataId = SystemConfig.encodeDataIdForFNIfUnderWin(dataId);
        StringBuilder sb = new StringBuilder("/");
        sb.append(Constants.BASE_DIR).append("/");
        sb.append(group).append("/");
        sb.append(fnDataId);
        return sb.toString();
    }


    /**
     * ����dataId��group����������Ϣ
     * 
     * @param dataId
     * @param group
     * @return
     */
    public ConfigInfo findConfigInfo(String dataId, String group) {
        if (!StringUtils.hasLength(dataId) || StringUtils.containsWhitespace(dataId))
            throw new IllegalArgumentException("��Ч��dataId");

        if (!StringUtils.hasLength(group) || StringUtils.containsWhitespace(group))
            throw new IllegalArgumentException("��Ч��group");
        return persistService.findConfigInfo(dataId, group);
    }


    /**
     * ����IDɾ��GroupInfo
     * 
     * @param id
     */
    public void removeConfigInfo(long id) {
        checkOperation("removeConfigInfo");
        // FIXME ���ǲ�Ҫ����ɾ��
        try {
            ConfigInfo configInfo = this.persistService.findConfigInfoByID(id);
            this.diskService.removeConfigInfo(configInfo);
            this.contentMD5Cache.remove(generateMD5CacheKey(configInfo.getDataId(), configInfo.getGroup()));

            this.persistService.removeConfigInfoByID(id);
            // ֪ͨ�����ڵ�
            this.notifyOtherNodes(configInfo.getDataId(), configInfo.getGroup());

        }
        catch (Exception e) {
            log.error("ɾ��������Ϣ����", e);
            throw new ConfigServiceException(e);
        }
    }


    private void checkOperation(String operation) {
        if (SystemConfig.isOfflineMode()) {
            String msg = "OFFLINEģʽ����֧�ֵĲ���:" + operation + "";
            throw new UnsupportedOperationException(msg);
        }
    }


    /**
     * ����ConfigInfo
     * 
     * @param dataId
     * @param group
     * @param content
     */
    @Deprecated
    public void addConfigInfo(String dataId, String group, String content) {
        checkOperation("addConfigInfo");
        checkParameter(dataId, group, content);
        ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
        if (!this.validationService.validate(configInfo))
            throw new IllegalStateException("������Ϣ������У��δͨ��");
        // ����˳�������ݿ⣬�ٴ���
        try {
            // TODO ��֤����һ��, �����ð汾�ŵķ�ʽ����
            persistService.addConfigInfo(configInfo);
            // �мǸ��»���
            this.contentMD5Cache.put(generateMD5CacheKey(dataId, group), configInfo.getMd5());
            diskService.saveToDisk(configInfo);
            // ֪ͨ�����ڵ�
            this.notifyOtherNodes(dataId, group);
        }
        catch (Exception e) {
            log.error("����ConfigInfoʧ��", e);
            throw new ConfigServiceException(e);
        }
    }

    
    /**
     * ����������Ϣ, ����ʱ�����ԴIP��Դ�û����ӵ����ݿ����
     * 
     * @param dataId
     * @param group
     * @param content
     * @param srcIp
     * @param srcUser
     */
    public void addConfigInfo(String dataId, String group, String content, String srcIp, String srcUser) {
        checkOperation("addConfigInfo");
        checkParameter(dataId, group, content);
        ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
        if (!this.validationService.validate(configInfo))
            throw new IllegalStateException("������Ϣ������У��δͨ��");
        
        // ��ȡ��ǰʱ��
        Timestamp currentTime = DiamondUtils.getCurrentTime();
        // д��˳��: ���ݿ⡢�ڴ桢����
        try {
            persistService.addConfigInfo(srcIp, srcUser, currentTime, configInfo);
            // �мǸ��»���
            this.contentMD5Cache.put(generateMD5CacheKey(dataId, group), configInfo.getMd5());
            diskService.saveToDisk(configInfo);
            // ֪ͨ�����ڵ�
            this.notifyOtherNodes(dataId, group);
        }
        catch (Exception e) {
            log.error("����ConfigInfoʧ��", e);
            throw new ConfigServiceException(e);
        }
    }

    /**
     * ����������Ϣ
     * 
     * @param dataId
     * @param group
     * @param content
     */
    @Deprecated
    public void updateConfigInfo(String dataId, String group, String content) {
        checkOperation("updateConfigInfo");
        checkParameter(dataId, group, content);
        ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
        if (!this.validationService.validate(configInfo))
            throw new IllegalStateException("������Ϣ������У��δͨ��");
        // �ȸ������ݿ⣬�ٸ��´���
        try {
            persistService.updateConfigInfo(configInfo);
            // �мǸ��»���
            this.contentMD5Cache.put(generateMD5CacheKey(dataId, group), configInfo.getMd5());
            diskService.saveToDisk(configInfo);
            // ֪ͨ�����ڵ�
            this.notifyOtherNodes(dataId, group);
        }
        catch (Exception e) {
            log.error("����ConfigInfoʧ��", e);
            throw new ConfigServiceException(e);
        }
    }
    
    
    /**
     * ����������Ϣ, ����ʱ�����ԴIP��Դ�û����µ����ݿ����
     * @param dataId
     * @param group
     * @param content
     * @param srcIp
     * @param srcUser
     */
    public void updateConfigInfo(String dataId, String group, String content, String srcIp, String srcUser) {
        checkOperation("updateConfigInfo");
        checkParameter(dataId, group, content);
        ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
        if (!this.validationService.validate(configInfo))
            throw new IllegalStateException("������Ϣ������У��δͨ��");
        
        Timestamp currentTime = DiamondUtils.getCurrentTime();
        try {
            persistService.updateConfigInfo(srcIp, srcUser, currentTime, configInfo);
            this.contentMD5Cache.put(generateMD5CacheKey(dataId, group), configInfo.getMd5());
            diskService.saveToDisk(configInfo);
            this.notifyOtherNodes(dataId, group);
        }
        catch (Exception e) {
            log.error("����ConfigInfoʧ��", e);
            throw new ConfigServiceException(e);
        }
    }


    /**
     * ͨ���Ƚ�md5ֵ������������Ϣ
     * 
     * @param dataId
     * @param group
     * @param content
     * @param oldMd5
     * @return �����Ƿ�ɹ�
     */
    @Deprecated
    public boolean updateConfigInfoByMd5(String dataId, String group, String content, String oldMd5) {
        checkOperation("updateConfigInfoByMd5");
        checkParameter(dataId, group, content);
        ConfigInfoEx configInfoEx = new ConfigInfoEx(dataId, group, content);
        configInfoEx.setOldMd5(oldMd5);
        if (!this.validationService.validate(configInfoEx))
            throw new IllegalStateException("������Ϣ������У��δͨ��");
        // �ȸ������ݿ⣬�ٸ��´���
        try {
            int updatedRow = persistService.updateConfigInfoByMd5(configInfoEx);
            if (updatedRow == 0) {
                // ���²��ɹ�
                return false;
            }
            // �мǸ��»���
            this.contentMD5Cache.put(generateMD5CacheKey(dataId, group), configInfoEx.getMd5());
            diskService.saveToDisk(configInfoEx);
            // ֪ͨ�����ڵ�
            this.notifyOtherNodes(dataId, group);
        }
        catch (Exception e) {
            log.error("����ConfigInfoʧ��", e);
            throw new ConfigServiceException(e);
        }
        return true;
    }
    
    
    /**
     * ͨ���Ƚ�md5ֵ������������Ϣ, ����ʱ�����ԴIP��Դ�û����µ����ݿ����
     * @param dataId
     * @param group
     * @param content
     * @param oldMd5
     * @param srcIp
     * @param srcUser
     * @return
     */
    public boolean updateConfigInfoByMd5(String dataId, String group, String content, String oldMd5, String srcIp, String srcUser) {
        checkOperation("updateConfigInfoByMd5");
        checkParameter(dataId, group, content);
        ConfigInfoEx configInfoEx = new ConfigInfoEx(dataId, group, content);
        configInfoEx.setOldMd5(oldMd5);
        if (!this.validationService.validate(configInfoEx))
            throw new IllegalStateException("������Ϣ������У��δͨ��");
        
        Timestamp currentTime = DiamondUtils.getCurrentTime();
        try {
            int updatedRow = persistService.updateConfigInfoByMd5(srcIp, srcUser, currentTime, configInfoEx);
            if (updatedRow == 0) {
                // ���²��ɹ�
                return false;
            }
            // �мǸ��»���
            this.contentMD5Cache.put(generateMD5CacheKey(dataId, group), configInfoEx.getMd5());
            diskService.saveToDisk(configInfoEx);
            // ֪ͨ�����ڵ�
            this.notifyOtherNodes(dataId, group);
        }
        catch (Exception e) {
            log.error("����ConfigInfoʧ��", e);
            throw new ConfigServiceException(e);
        }
        return true;
    }


    /**
     * ɾ��ConfigInfo
     * 
     * @param dataId
     * @param group
     */
    public void removeConfigInfo(String dataId, String group) {
        checkParameter(dataId, group);
        try {
            this.contentMD5Cache.remove(generateMD5CacheKey(dataId, group));
            diskService.removeConfigInfo(dataId, group);
            persistService.removeConfigInfo(dataId, group);
            notifyOtherNodes(dataId, group);// ֪ͨ�����ڵ�

        }
        catch (Exception e) {
            log.error("����ConfigInfoʧ��", e);
            throw new ConfigServiceException(e);
        }
    }


    /**
     * ��������Ϣ�����ݿ���ص�����
     * 
     * @param id
     */
    public void loadConfigInfoToDisk(String dataId, String group) {
        try {
            ConfigInfo configInfo = this.persistService.findConfigInfo(dataId, group);
            if (configInfo != null) {
                this.contentMD5Cache.put(generateMD5CacheKey(dataId, group), configInfo.getMd5());
                this.diskService.saveToDisk(configInfo);
            }
            else {
                // ɾ���ļ�
                this.contentMD5Cache.remove(generateMD5CacheKey(dataId, group));
                this.diskService.removeConfigInfo(dataId, group);
            }
        }
        catch (Exception e) {
            log.error("����ConfigInfo������ʧ��", e);
            throw new ConfigServiceException(e);
        }
    }


    private void checkParameter(String dataId, String group, String content) {
        if (!StringUtils.hasLength(dataId) || StringUtils.containsWhitespace(dataId))
            throw new ConfigServiceException("��Ч��dataId");

        if (!StringUtils.hasLength(group) || StringUtils.containsWhitespace(group))
            throw new ConfigServiceException("��Ч��group");

        if (!StringUtils.hasLength(content))
            throw new ConfigServiceException("��Ч��content");
    }


    /**
     * ��ҳ����������Ϣ
     * 
     * @param pageNo
     * @param pageSize
     * @param group
     * @param dataId
     * @return
     */
    public Page<ConfigInfo> findConfigInfo(final int pageNo, final int pageSize, final String group, final String dataId) {
        if (StringUtils.hasLength(dataId) && StringUtils.hasLength(group)) {
            ConfigInfo ConfigInfo = this.persistService.findConfigInfo(dataId, group);
            Page<ConfigInfo> page = new Page<ConfigInfo>();
            if (ConfigInfo != null) {
                page.setPageNumber(1);
                page.setTotalCount(1);
                page.setPagesAvailable(1);
                page.getPageItems().add(ConfigInfo);
            }
            return page;
        }
        else if (StringUtils.hasLength(dataId) && !StringUtils.hasLength(group)) {
            return this.persistService.findConfigInfoByDataId(pageNo, pageSize, dataId);
        }
        else if (!StringUtils.hasLength(dataId) && StringUtils.hasLength(group)) {
            return this.persistService.findConfigInfoByGroup(pageNo, pageSize, group);
        }
        else
            return this.persistService.findAllConfigInfo(pageNo, pageSize);
    }


    /**
     * ��ҳģ������������Ϣ
     * 
     * @param pageNo
     * @param pageSize
     * @param group
     * @param dataId
     * @return
     */
    public Page<ConfigInfo> findConfigInfoLike(final int pageNo, final int pageSize, final String group,
            final String dataId) {
        return this.persistService.findConfigInfoLike(pageNo, pageSize, dataId, group);
    }
    
    
    /**
     * ��ѯdataId������
     * @return
     */
    public int countAllDataIds() {
        int result = 0;
        try {
            result = this.persistService.countAllDataIds();
        }
        catch(Exception e) {
            log.error("��ѯdataId��������", e);
            result = -1;
        }
        return result;
    }


    /**
     * ����ip��dataId+group��ӳ��
     * 
     * @author leiwen
     */
    public void addIpToDataIdAndGroup(String ip, String dataId, String group) {
        this.redisService.add(ip, dataId + "-" + group);
    }


    /**
     * ɾ��ip��dataId+group��ӳ��
     * 
     * @author leiwen
     */
    public void removeIpToDataIdAndGroup(String ip, String dataId, String group) {
        this.redisService.remove(ip, dataId + "-" + group);
    }


    private void checkParameter(String dataId, String group) {
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim()))
            throw new ConfigServiceException("��Ч��dataId");

        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim()))
            throw new ConfigServiceException("��Ч��group");
    }
    
    private void notifyOtherNodes(String dataId, String group) {
        String taskType = dataId + "-" + group + "-notify";
        NotifyTask notifyTask = new NotifyTask(dataId, group);
        this.taskManagerService.addNotifyProcessor(taskType, notifyTaskProcessor);
        this.taskManagerService.addNotifyTask(taskType, notifyTask, true);
    }


    public GroupService getGroupService() {
        return groupService;
    }


    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }


    public DiskService getDiskService() {
        return diskService;
    }


    public void setDiskService(DiskService diskService) {
        this.diskService = diskService;
    }


    public PersistService getPersistService() {
        return persistService;
    }


    public void setPersistService(PersistService persistService) {
        this.persistService = persistService;
    }


    public NotifyService getNotifyService() {
        return notifyService;
    }


    public void setNotifyService(NotifyService notifyService) {
        this.notifyService = notifyService;
    }


    public ValidationService getValidationService() {
        return validationService;
    }


    public void setValidationService(ValidationService validationService) {
        this.validationService = validationService;
    }


    public RedisService getRedisService() {
        return redisService;
    }


    public void setRedisService(RedisService redisService) {
        this.redisService = redisService;
    }


    public TaskManagerService getTaskManagerService() {
        return taskManagerService;
    }


    public void setTaskManagerService(TaskManagerService taskManagerService) {
        this.taskManagerService = taskManagerService;
    }


    public NotifyTaskProcessor getNotifyTaskProcessor() {
        return notifyTaskProcessor;
    }


    public void setNotifyTaskProcessor(NotifyTaskProcessor notifyTaskProcessor) {
        this.notifyTaskProcessor = notifyTaskProcessor;
    }


}