/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.service.task;

import java.util.ArrayList;
import java.util.List;

import com.taobao.diamond.notify.utils.task.Task;


/**
 * ��������, ������������, ɾ������, ͬ������ʱʹ��
 * 
 * @author leiwen.zh
 * 
 */
public class ConfigInfoTask extends Task {

    private String dataId;
    private String group;
    private List<String> contents = new ArrayList<String>();
    // ����ʧ�ܴ���
    private int failCount;


    public ConfigInfoTask(String dataId, String group, String content) {
        this.dataId = dataId;
        this.group = group;
        this.contents.add(content);
    }


    public ConfigInfoTask(String dataId, String group, List<String> contents) {
        this.dataId = dataId;
        this.group = group;
        this.contents = contents;
    }


    public String getDataId() {
        return dataId;
    }


    public void setDataId(String dataId) {
        this.dataId = dataId;
    }


    public String getGroup() {
        return group;
    }


    public void setGroup(String group) {
        this.group = group;
    }


    public List<String> getContents() {
        return contents;
    }


    public int getFailCount() {
        return failCount;
    }


    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }


    @Override
    public void merge(Task task) {
        // ����ͬdataId��group�����ݺϲ�
        ConfigInfoTask anotherTask = (ConfigInfoTask) task;
        if (this.getDataId().equals(anotherTask.getDataId()) && this.getGroup().equals(anotherTask.getGroup())) {
            // �������������ȫ�����ӵ���������
            this.contents.addAll(anotherTask.contents);
        }
    }


    @Override
    public String toString() {
        return this.getClass().getName() + ": dataId=" + dataId + ",group=" + group;
    }

}
