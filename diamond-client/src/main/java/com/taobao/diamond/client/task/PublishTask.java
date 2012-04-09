/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.client.task;

import java.util.ArrayList;
import java.util.List;

import com.taobao.diamond.notify.utils.task.Task;


/**
 * ������������, �����������ݺ�ȫ������
 * 
 * @author leiwen.zh
 * 
 */
public class PublishTask extends Task {
    private String dataId;
    private String group;
    private List<String> contents = new ArrayList<String>();
    private int count;


    public PublishTask(String dataId, String group, String content) {
        this.dataId = dataId;
        this.group = group;
        contents.add(content);
    }


    public String getDataId() {
        return dataId;
    }


    public String getGroup() {
        return group;
    }


    public List<String> getContents() {
        return contents;
    }


    public int getCount() {
        return count;
    }


    public void setCount(int count) {
        if(count == Integer.MAX_VALUE) {
            this.count = 0;
        }
        this.count = count;
    }


    @Override
    public void merge(Task task) {
        // ����ͬdataId��group�����ݺϲ�
        PublishTask anotherTask = (PublishTask) task;
        if (this.getDataId().equals(anotherTask.getDataId()) && this.getGroup().equals(anotherTask.getGroup())) {
            // �������������ȫ�����ӵ���������
            this.contents.addAll(anotherTask.contents);
        }
    }
}