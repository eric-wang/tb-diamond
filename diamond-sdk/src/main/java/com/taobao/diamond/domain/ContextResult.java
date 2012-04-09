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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ����dataId,groupName��ȷ��ѯ���صĶ���
 * 
 * @filename ContextResult.java
 * @author libinbin.pt
 * @datetime 2010-7-15 ����06:49:12
 */
/**
 * 
 * @filename ContextResult.java
 * @author libinbin.pt
 * @param <T>
 * @datetime 2010-7-16 ����05:48:54
 */
@SuppressWarnings("serial")
public class ContextResult implements Serializable {
	private boolean isSuccess = false;    		// �Ƿ�ɹ�
	private int statusCode; 			  		// ״̬��
	private String statusMsg = ""; 			  	// ״̬��Ϣ
	private ConfigInfo configInfo=null;         // ���ö������[���ݣ�dataId��groupName]
	private GroupInfo groupInfo=null;
	private String receiveResult=null;	  		// �ش���Ϣ
	private List<GroupInfo> receive;
	private ConcurrentHashMap<String,ConcurrentHashMap<String,GroupInfo>> map;

    
    public ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> getMap() {
        return map;
    }

    public void setMap(ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> map) {
        this.map = map;
    }

    public List<GroupInfo> getReceive() {
        return receive;
    }
    
    public void setReceive(List<GroupInfo> receive) {
		this.receive = receive;
	}

	public GroupInfo getGroupInfo() {
		return groupInfo;
	}

	public void setGroupInfo(GroupInfo groupInfo) {
		this.groupInfo = groupInfo;
	}

	public ContextResult() {

	}
	public boolean isSuccess() {
		return isSuccess;
	}


	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
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


	public ConfigInfo getConfigInfo() {
		return configInfo;
	}

	public void setConfigInfo(ConfigInfo configInfo) {
		this.configInfo = configInfo;
	}


	public String getReceiveResult() {
		return receiveResult;
	}


	public void setReceiveResult(String receiveResult) {
		this.receiveResult = receiveResult;
	}

	
	@Override
	public String toString() {
		return "[" + 
				"statusCode="+statusCode+
				",isSuccess="+isSuccess+
				",statusMsg="+statusMsg+
				",receiveResult="+receiveResult+
				",[groupInfo="+groupInfo+ "]"+
				",[configInfo="+configInfo+ "]]";
	}

}
