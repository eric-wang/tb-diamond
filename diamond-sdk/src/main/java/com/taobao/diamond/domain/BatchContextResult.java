/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * ������ķ��ؽ��
 * 
 * @author leiwen.zh
 * 
 */
public class BatchContextResult<T> implements Serializable {

    private static final long serialVersionUID = -5170746311067772091L;

    // �������Ƿ�ɹ�
    private boolean success = true;
    // ���󷵻ص�״̬��
    private int statusCode;
    // �û��ɶ��ķ�����Ϣ
    private String statusMsg;
    // ���صĽ����
    private List<T> result;


    public BatchContextResult() {
        this.result = new ArrayList<T>();
    }


    public boolean isSuccess() {
        return success;
    }


    public void setSuccess(boolean success) {
        this.success = success;
    }


    public int getStatusCode() {
        return statusCode;
    }


    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }


    public String getStatusMsg() {
        return statusMsg;
    }


    public void setStatusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }


    public List<T> getResult() {
        return result;
    }


    @Override
    public String toString() {
        return "BatchContextResult [success=" + success + ", statusCode=" + statusCode + ", statusMsg=" + statusMsg
                + ", result=" + result + "]";
    }

}
