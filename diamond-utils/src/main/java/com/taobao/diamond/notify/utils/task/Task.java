/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.notify.utils.task;

/**
 * @author huali
 *
 */
public abstract class Task {
	/**
	 * һ���������δ���ļ������λ�Ǻ���
	 */
	private long taskInterval;
	
	/**
	 * �����ϴα������ʱ�䣬�ú����ʾ
	 */
	private long lastProcessTime;
	
	public abstract void merge(Task task);
	
	public void setTaskInterval(long interval){
		this.taskInterval = interval;
	}
	
	public long getTaskInterval(){
		return this.taskInterval;
	}
	
	public void setLastProcessTime(long lastProcessTime){
		this.lastProcessTime = lastProcessTime;
	}
	
	public long getLastProcessTime(){
		return this.lastProcessTime;
	}
	
	/**
	 * TaskManager �жϵ�ǰ�Ƿ���Ҫ�������Task���������Override�������ʵ���Լ����߼�
	 * @return
	 */
	public boolean shouldProcess(){
		return (System.currentTimeMillis() - this.lastProcessTime >= this.taskInterval);
	}
	
}
