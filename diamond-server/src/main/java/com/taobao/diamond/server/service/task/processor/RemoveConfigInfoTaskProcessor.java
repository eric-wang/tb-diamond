/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service.task.processor;

import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.notify.utils.task.Task;
import com.taobao.diamond.notify.utils.task.TaskProcessor;
import com.taobao.diamond.server.service.AggregationService;
import com.taobao.diamond.server.service.ConfigService;
import com.taobao.diamond.server.service.TaskManagerService;
import com.taobao.diamond.server.service.task.ConfigInfoTask;
import com.taobao.diamond.server.service.task.RealTimeNotifyTask;
import com.taobao.diamond.server.utils.DiamondServerConstants;


/**
 * ɾ������������Ϣ����������
 * 
 * @author leiwen.zh
 * 
 */
public class RemoveConfigInfoTaskProcessor implements TaskProcessor {

    private static final Log log = LogFactory.getLog(RemoveConfigInfoTaskProcessor.class);
    private static final Log failCountLog = LogFactory.getLog("failLog");

    private ConfigService configService;

    private AggregationService aggrService;

    private TaskManagerService taskManagerService;

    private RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor;


    public ConfigService getConfigService() {
        return configService;
    }


    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }


    public AggregationService getAggrService() {
        return aggrService;
    }


    public void setAggrService(AggregationService aggrService) {
        this.aggrService = aggrService;
    }


    public TaskManagerService getTaskManagerService() {
        return taskManagerService;
    }


    public void setTaskManagerService(TaskManagerService taskManagerService) {
        this.taskManagerService = taskManagerService;
    }


    public RealTimeNotifyTaskProcessor getRealTimeNotifyTaskProcessor() {
        return realTimeNotifyTaskProcessor;
    }


    public void setRealTimeNotifyTaskProcessor(RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor) {
        this.realTimeNotifyTaskProcessor = realTimeNotifyTaskProcessor;
    }


    public boolean process(String taskType, Task task) {
        ConfigInfoTask rmTask = (ConfigInfoTask) task;
        String dataId = rmTask.getDataId();
        String group = rmTask.getGroup();
        Iterator<String> it = rmTask.getContents().iterator();

        while (it.hasNext()) {
            String content = it.next();
            int failCount = rmTask.getFailCount();
            if (innerProcess(dataId, group, content)) {
                // ��������ɾ���ɹ�, �����list���Ƴ�, ����ɾ����һ������
                log.info("ɾ���ɹ�, dataId=" + dataId + ", group=" + group + ", content=" + content);
                it.remove();
                rmTask.setFailCount(0);
            }
            else {
                // ��������ɾ��ʧ��, ������������
                log.warn("ɾ��ʧ��, dataId=" + dataId + ", group=" + group + ", content=" + content);
                failCount++;
                rmTask.setFailCount(failCount);
                if (failCount >= DiamondServerConstants.MAX_REMOVE_FAIL_COUNT) {
                    failCountLog.info("ɾ������ʧ�ܴ����ﵽ����: dataId=" + dataId + ", group=" + group + ", content=" + content);
                    it.remove();
                    rmTask.setFailCount(0);
                }
                return false;
            }
        }

        rmTask.setFailCount(0);

        try {
            this.aggrService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("���ݾۺϳ���, dataId=" + dataId + "group=" + group, e);
        }

        try {
            this.realTimeNotify(dataId, group);
        }
        catch (Exception e) {
            log.error("ʵʱ֪ͨ����, dataId=" + dataId + "group=" + group, e);
        }

        return true;
    }


    private boolean innerProcess(String dataId, String group, String content) {
        boolean result = false;
        try {
            ConfigInfo oldConfigInfo = this.configService.findConfigInfo(dataId, group);
            if (oldConfigInfo == null) {
                log.info("ɾ����dataId������, dataId=" + dataId + ", group=" + group + ", content=" + content);
                result = true;
            }
            else {
                String oldContent = oldConfigInfo.getContent();
                String oldMd5 = oldConfigInfo.getMd5();
                if (!oldContent.contains(content)) {
                    log.info("ɾ�������ݲ�����, dataId=" + dataId + ", group=" + group + ", content=" + content);
                    result = true;
                }
                else {
                    String newContent = generateNewContent(oldContent, content);
                    if (newContent.isEmpty()) {
                        log.info("ɾ�������һ������, ����������Ҳɾ��, dataId=" + dataId + ", group=" + group + ", content=" + content);
                        this.configService.removeConfigInfo(dataId, group);
                        result = true;
                    }
                    else {
                        log.info("ɾ����һ������, ����ɾ����Ľ��, dataId=" + dataId + ", group=" + group + ", content=" + content);
                        result = this.configService.updateConfigInfoByMd5(dataId, group, newContent, oldMd5);
                    }
                }
            }

            return result;
        }
        catch (Exception e) {
            log.error("ɾ�����ݳ���:dataId=" + dataId + ", group=" + group + ", content=" + content, e);
            return false;
        }
    }


    @SuppressWarnings("unchecked")
    private String generateNewContent(String oldContent, String content) throws Exception {
        StringBuilder sb = new StringBuilder();
        StringReader strReader = new StringReader(oldContent);
        try {
            List<String> lines = IOUtils.readLines(strReader);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.contains(content)) {
                    sb.append(line);
                    if (i != lines.size() - 1) {
                        sb.append(Constants.DIAMOND_LINE_SEPARATOR);
                    }
                }
            }
            String result = sb.toString();
            if (result.endsWith(Constants.DIAMOND_LINE_SEPARATOR)) {
                result = result.substring(0, result.lastIndexOf(Constants.DIAMOND_LINE_SEPARATOR));
            }

            return result;

        }
        finally {
            strReader.close();
        }
    }


    private void realTimeNotify(String dataId, String group) {
        String taskType = dataId + "-" + group + "-pushit";
        RealTimeNotifyTask task = new RealTimeNotifyTask(dataId, group);
        task.setLastProcessTime(System.currentTimeMillis());
        task.setTaskInterval(2000L);
        this.taskManagerService.addPushitProcessor(taskType, realTimeNotifyTaskProcessor);
        this.taskManagerService.addPushitTask(taskType, task, false);
    }

}
