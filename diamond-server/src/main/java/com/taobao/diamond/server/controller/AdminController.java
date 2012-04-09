/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.taobao.diamond.common.Constants;
import com.taobao.diamond.domain.ConfigInfo;
import com.taobao.diamond.domain.ConfigInfoEx;
import com.taobao.diamond.domain.GroupInfo;
import com.taobao.diamond.domain.Page;
import com.taobao.diamond.server.exception.ConfigServiceException;
import com.taobao.diamond.server.service.ACLService;
import com.taobao.diamond.server.service.AdminService;
import com.taobao.diamond.server.service.AggregationService;
import com.taobao.diamond.server.service.ConfigService;
import com.taobao.diamond.server.service.FlowControlService;
import com.taobao.diamond.server.service.GroupService;
import com.taobao.diamond.server.service.PushitService;
import com.taobao.diamond.server.service.TaskManagerService;
import com.taobao.diamond.server.service.task.RealTimeNotifyTask;
import com.taobao.diamond.server.service.task.RedisTask;
import com.taobao.diamond.server.service.task.processor.RealTimeNotifyTaskProcessor;
import com.taobao.diamond.server.service.task.processor.RedisTaskProcessor;
import com.taobao.diamond.server.utils.DiamondUtils;
import com.taobao.diamond.server.utils.GlobalCounter;
import com.taobao.diamond.utils.JSONUtils;


/**
 * ���������
 * 
 * @author boyan
 * @date 2010-5-6
 */
@Controller
@RequestMapping("/admin.do")
public class AdminController {

    int FORBIDDEN_403 = 403;

    @Autowired
    private AdminService adminService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private ACLService aCLService;

    @Autowired
    private FlowControlService flowControlService;

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    private PushitService pushitService;

    @Autowired
    private TaskManagerService taskManagerService;

    @Autowired
    private RedisTaskProcessor redisTaskProcessor;

    @Autowired
    private RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor;

    static final Log log = LogFactory.getLog(AdminController.class);
    static final Log updateLog = LogFactory.getLog("updateLog");
    static final Log deleteLog = LogFactory.getLog("deleteLog");


    // @RequestMapping(params = "method")
    // public String handlerRequest(HttpServletRequest
    // request,HttpServletResponse response,ModelMap modelmap) throws Exception{
    // String methodName = request.getParameter("mehod");
    // String id = request.getParameter("id");
    // String dataId = request.getParameter("dataId");
    // String group = request.getParameter("group");
    // String content = request.getParameter("content");
    // String pageNo = request.getParameter("pageNo");
    // String pageSize = request.getParameter("pageSize");
    // //FileItem fileItem = new DiskFileItem();
    // MultipartFile contentFile =null;// new CommonsMultipartFile(fileItem);
    // if(methodName.equals("postConfig")){
    // return postConfig(request,response,dataId,group,content,modelmap);
    // }
    // if(methodName.equals("deleteConfig")){
    // return deleteConfig(request,response,Integer.parseInt(id),modelmap);
    // }
    //
    // if(methodName.equals("upload")){
    // return upload(request,response,dataId,group,contentFile,modelmap);
    // }
    // if(methodName.equals("reupload")){
    // return reupload(request,response,dataId,group,contentFile,modelmap);
    // }
    // if(methodName.equals("updateConfig")){
    // return updateConfig(request,response,dataId,group,content,modelmap);
    // }
    // if(methodName.equals("listConfig")){
    // return
    // listConfig(request,response,dataId,group,Integer.parseInt(pageNo),Integer.parseInt(pageSize),modelmap);
    // }
    // if(methodName.equals("listConfigLike")){
    // return
    // listConfigLike(request,response,dataId,group,Integer.parseInt(pageNo),Integer.parseInt(pageSize),modelmap);
    // }
    // if(methodName.equals("getConfigInfo")){
    // return getConfigInfo(request,response,dataId,group,modelmap);
    // }
    // if(methodName.equals("listGroup")){
    // return listGroup(request,response,modelmap);
    // }
    // if(methodName.equals("addGroup")){
    // String address = request.getParameter("address");
    // return addGroup(request,response,address,dataId,group,modelmap);
    // }
    // if(methodName.equals("deleteGroup")){
    // return deleteGroup(request,response,Long.parseLong(id),modelmap);
    // }
    // if(methodName.equals("moveGroup")){
    // String newGroup = request.getParameter("newGroup");
    // return moveGroup(request,response,Long.parseLong(id),newGroup,modelmap);
    // }
    // return "404";
    // }

    private String getRemoteIP(HttpServletRequest request) {
        String remoteIP = request.getRemoteAddr();
        if (remoteIP.equals("127.0.0.1")) {
            remoteIP = request.getHeader("X-Real-IP");
        }
        return remoteIP;
    }


    /**
     * �����µ�������Ϣ
     * 
     * @param request
     * @param dataId
     * @param group
     * @param content
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=postConfig", method = RequestMethod.POST)
    public String postConfig(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("content") String content, ModelMap modelMap) {
        response.setCharacterEncoding("GBK");

        String remoteIp = this.getRemoteIP(request);
        boolean checkSuccess = true;
        String errorMessage = "��������";
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч��DataId";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч�ķ���";
        }
        if (!StringUtils.hasLength(content)) {
            checkSuccess = false;
            errorMessage = "��Ч������";
        }
        if (!checkSuccess) {
            modelMap.addAttribute("message", errorMessage);
            try {
                response.sendError(FORBIDDEN_403, errorMessage);
            }
            catch (IOException ioe) {
                log.error(ioe.getMessage(), ioe.getCause());
            }
            return "/admin/config/new";
        }

        // Ϊ�˼����ϰ汾sdk, �÷�����ԴIP��Դ�û�������Ϊrequest�е�Զ��IP
        // this.configService.addConfigInfo(dataId, group, content);
        this.configService.addConfigInfo(dataId, group, content, remoteIp, remoteIp);

        modelMap.addAttribute("message", "�ύ�ɹ�!");
        // ������������������Ӱ��������, ���������߳̿�ס
        try {
            // �������ݾۺ�
            this.aggregationService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("���ݾۺϳ���", e);
        }
        try {
            // ʵʱ֪ͨ,
            // TODO ������������
            this.realTimeNotify(dataId, group);
        }
        catch (Exception e) {
            log.error("ʵʱ֪ͨ����", e);
        }
        try {
            // ����ip�뷢��dataId��ӳ�䵽redis
            // TODO ������������
            this.addIpToDataIdAndGroup(remoteIp, dataId, group);
        }
        catch (Exception e) {
            log.error("redis����", e);
        }
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }


    /**
     * �����µ�������Ϣ, �÷�����ԴͷIP��Դͷ�û���Ϊ����
     * 
     * @date 2012-03-22
     */
    @RequestMapping(params = "method=postConfigNew", method = RequestMethod.POST)
    public String postConfig(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("content") String content, @RequestParam("src_ip") String srcIp,
            @RequestParam("src_user") String srcUser, ModelMap modelMap) {
        response.setCharacterEncoding("GBK");

        String remoteIp = this.getRemoteIP(request);
        boolean checkSuccess = true;
        String errorMessage = "��������";
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч��DataId";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч�ķ���";
        }
        if (!StringUtils.hasLength(content)) {
            checkSuccess = false;
            errorMessage = "��Ч������";
        }
        if (!checkSuccess) {
            modelMap.addAttribute("message", errorMessage);
            try {
                response.sendError(FORBIDDEN_403, errorMessage);
            }
            catch (IOException ioe) {
                log.error(ioe.getMessage(), ioe.getCause());
            }
            return "/admin/config/new";
        }

        // Ϊ�°汾��sdkʹ�õķ���, ʹ�ò����е�ԴͷIP��Դͷ�û�
        this.configService.addConfigInfo(dataId, group, content, srcIp, srcUser);

        modelMap.addAttribute("message", "�ύ�ɹ�!");
        // ������������������Ӱ��������, ���������߳̿�ס
        try {
            // �������ݾۺ�
            this.aggregationService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("���ݾۺϳ���", e);
        }
        try {
            // ʵʱ֪ͨ,
            // TODO ������������
            this.realTimeNotify(dataId, group);
        }
        catch (Exception e) {
            log.error("ʵʱ֪ͨ����", e);
        }
        try {
            // ����ip�뷢��dataId��ӳ�䵽redis
            // TODO ������������
            this.addIpToDataIdAndGroup(remoteIp, dataId, group);
        }
        catch (Exception e) {
            log.error("redis����", e);
        }
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }


    @RequestMapping(params = "method=deleteConfig", method = RequestMethod.GET)
    public String deleteConfig(HttpServletRequest request, HttpServletResponse response, @RequestParam("id") long id,
            ModelMap modelMap) {
        // ����id��ѯ����������
        ConfigInfo configInfo = this.configService.getPersistService().findConfigInfoByID(id);
        if (configInfo == null) {
            deleteLog.warn("ɾ��ʧ��, Ҫɾ�������ݲ�����, id=" + id);
            modelMap.addAttribute("message", "ɾ��ʧ��, Ҫɾ�������ݲ�����, id=" + id);
            return "/admin/config/list";
        }
        String dataId = configInfo.getDataId();
        String group = configInfo.getGroup();
        String content = configInfo.getContent();
        String sourceIP = this.getRemoteIP(request);
        // ɾ������
        this.configService.removeConfigInfo(id);
        // ��¼ɾ����־, AOP��ʽ�ļ�¼�����¼dataId����Ϣ, �����������ٴμ�¼
        deleteLog.warn("����ɾ���ɹ�\ndataId=" + dataId + "\ngroup=" + group + "\ncontent=\n" + content + "\nsrc ip="
                + sourceIP);
        modelMap.addAttribute("message", "ɾ���ɹ�!");
        try {
            // �������ݾۺ�
            this.aggregationService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("���ݾۺϳ���", e);
        }
        return "/admin/config/list";
    }


    @RequestMapping(params = "method=deleteConfigByDataIdGroup", method = RequestMethod.GET)
    public String deleteConfigByDataIdGroup(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group, ModelMap modelMap) {

        // �������,Ŀ��������־�м�¼��������
        ConfigInfo configInfo = this.configService.findConfigInfo(dataId, group);
        if (configInfo == null) {
            deleteLog.warn("ɾ��ʧ��, Ҫɾ�������ݲ�����, dataId=" + dataId + ",group=" + group);
            modelMap.addAttribute("message", "ɾ��ʧ��, Ҫɾ�������ݲ�����, dataId=" + dataId + ",group=" + group);
            return "/admin/config/list";
        }
        String content = configInfo.getContent();
        String sourceIP = this.getRemoteIP(request);
        // ɾ������
        this.configService.removeConfigInfo(dataId, group);
        deleteLog.warn("����ɾ���ɹ�\ndataId=" + dataId + "\ngroup=" + group + "\ncontent=\n" + content + "\nsrc ip="
                + sourceIP);
        modelMap.addAttribute("message", "ɾ���ɹ�!");

        try {
            // �������ݾۺ�
            this.aggregationService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("���ݾۺϳ���", e);
        }
        return "/admin/config/list";
    }


    /**
     * �ϴ������ļ�
     * 
     * @param request
     * @param dataId
     * @param group
     * @param contentFile
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=upload", method = RequestMethod.POST)
    public String upload(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("contentFile") MultipartFile contentFile, ModelMap modelMap) {
        response.setCharacterEncoding("GBK");

        String remoteIp = request.getHeader("X-Real-IP");
        if (remoteIp == null || remoteIp.isEmpty()) {
            remoteIp = request.getRemoteAddr();
        }
        boolean checkSuccess = true;
        String errorMessage = "��������";
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч��DataId";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч�ķ���";
        }
        String content = getContentFromFile(contentFile);
        if (!StringUtils.hasLength(content)) {
            checkSuccess = false;
            errorMessage = "��Ч������";
        }
        if (!checkSuccess) {
            modelMap.addAttribute("message", errorMessage);
            return "/admin/config/upload";
        }

        // Ϊ�˼����ϰ汾sdk, �÷�����ԴIP��Դ�û�������Ϊrequest�е�Զ��IP
        // this.configService.addConfigInfo(dataId, group, content);
        this.configService.addConfigInfo(dataId, group, content, remoteIp, remoteIp);
        modelMap.addAttribute("message", "�ύ�ɹ�!");
        try {
            // �������ݾۺ�
            this.aggregationService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("���ݾۺϳ���", e);
        }
        try {
            // ʵʱ֪ͨ
            this.realTimeNotify(dataId, group);
        }
        catch (Exception e) {
            log.error("ʵʱ֪ͨ����", e);
        }
        try {
            // ����ip�뷢��dataId��ӳ�䵽redis
            this.addIpToDataIdAndGroup(remoteIp, dataId, group);
        }
        catch (Exception e) {
            log.error("redis����", e);
        }
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }


    /**
     * �����ϴ������ļ�
     * 
     * @param request
     * @param dataId
     * @param group
     * @param contentFile
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=reupload", method = RequestMethod.POST)
    public String reupload(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("contentFile") MultipartFile contentFile, ModelMap modelMap) {
        response.setCharacterEncoding("GBK");

        String remoteIp = getRemoteIP(request);

        boolean checkSuccess = true;
        String errorMessage = "��������";
        String content = getContentFromFile(contentFile);
        ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч��DataId";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч�ķ���";
        }
        if (!StringUtils.hasLength(content)) {
            checkSuccess = false;
            errorMessage = "��Ч������";
        }
        if (!checkSuccess) {
            modelMap.addAttribute("message", errorMessage);
            modelMap.addAttribute("configInfo", configInfo);
            return "/admin/config/edit";
        }

        // ��ѯ����, Ŀ��������־�м�¼�����µ����ݵ�����
        ConfigInfo oldConfigInfo = this.configService.findConfigInfo(dataId, group);
        if (oldConfigInfo == null) {
            updateLog.warn("�������ݳ���,Ҫ���µ����ݲ�����,dataId=" + dataId + ",group=" + group);
            modelMap.addAttribute("message", "�������ݳ���,Ҫ���µ����ݲ�����,dataId=" + dataId + ",group=" + group);
            return listConfig(request, response, dataId, group, 1, 20, modelMap);
        }
        String oldContent = oldConfigInfo.getContent();

        // Ϊ�˼����ϰ汾sdk, �÷�����ԴIP��Դ�û�������Ϊrequest�е�Զ��IP
        // this.configService.updateConfigInfo(dataId, group, content);
        this.configService.updateConfigInfo(dataId, group, content, remoteIp, remoteIp);

        // ��¼������־
        updateLog.warn("�������ݳɹ�\ndataId=" + dataId + "\ngroup=" + group + "\noldContent=\n" + oldContent
                + "\nnewContent=\n" + content + "\nsrc ip=" + remoteIp);
        modelMap.addAttribute("message", "���³ɹ�!");
        try {
            // �������ݾۺ�
            this.aggregationService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("���ݾۺϳ���", e);
        }
        try {
            // ʵʱ֪ͨ
            this.realTimeNotify(dataId, group);
        }
        catch (Exception e) {
            log.error("ʵʱ֪ͨ����", e);
        }
        try {
            // ����ip�뷢��dataId��ӳ�䵽redis
            this.addIpToDataIdAndGroup(remoteIp, dataId, group);
        }
        catch (Exception e) {
            log.error("redis����", e);
        }
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }


    private String getContentFromFile(MultipartFile contentFile) {
        try {
            String charset = Constants.ENCODE;
            final String content = new String(contentFile.getBytes(), charset);
            return content;
        }
        catch (Exception e) {
            throw new ConfigServiceException(e);
        }
    }


    /**
     * ����������Ϣ
     * 
     * @param request
     * @param dataId
     * @param group
     * @param content
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=updateConfig", method = RequestMethod.POST)
    public String updateConfig(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("content") String content, ModelMap modelMap) {
        response.setCharacterEncoding("GBK");

        String remoteIp = getRemoteIP(request);

        ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
        boolean checkSuccess = true;
        String errorMessage = "��������";
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч��DataId";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч�ķ���";
        }
        if (!StringUtils.hasLength(content)) {
            checkSuccess = false;
            errorMessage = "��Ч������";
        }
        if (!checkSuccess) {
            modelMap.addAttribute("message", errorMessage);
            modelMap.addAttribute("configInfo", configInfo);
            return "/admin/config/edit";
        }

        // ������,Ŀ����Ϊ������־�м�¼�����µ����ݵ�����
        ConfigInfo oldConfigInfo = this.configService.findConfigInfo(dataId, group);
        if (oldConfigInfo == null) {
            updateLog.warn("�������ݳ���,Ҫ���µ����ݲ�����, dataId=" + dataId + ",group=" + group);
            modelMap.addAttribute("message", "�������ݳ���, Ҫ���µ����ݲ�����, dataId=" + dataId + ",group=" + group);
            return listConfig(request, response, dataId, group, 1, 20, modelMap);
        }
        String oldContent = oldConfigInfo.getContent();

        // Ϊ�˼����ϰ汾sdk, �÷�����ԴIP��Դ�û�������Ϊrequest�е�Զ��IP
        // this.configService.updateConfigInfo(dataId, group, content);
        this.configService.updateConfigInfo(dataId, group, content, remoteIp, remoteIp);

        // ��¼������־
        updateLog.warn("�������ݳɹ�\ndataId=" + dataId + "\ngroup=" + group + "\noldContent=\n" + oldContent
                + "\nnewContent=\n" + content + "\nsrc ip=" + remoteIp);
        modelMap.addAttribute("message", "�ύ�ɹ�!");
        try {
            // �������ݾۺ�
            this.aggregationService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("���ݾۺϳ���", e);
        }
        try {
            // ʵʱ֪ͨ
            this.realTimeNotify(dataId, group);
        }
        catch (Exception e) {
            log.error("ʵʱ֪ͨ����", e);
        }
        try {
            // ����ip�뷢��dataId��ӳ�䵽redis
            this.addIpToDataIdAndGroup(remoteIp, dataId, group);
        }
        catch (Exception e) {
            log.error("redis����", e);
        }
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }


    @RequestMapping(params = "method=updateConfigNew", method = RequestMethod.POST)
    public String updateConfig(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("content") String content, @RequestParam("src_ip") String srcIp,
            @RequestParam("src_user") String srcUser, ModelMap modelMap) {
        response.setCharacterEncoding("GBK");

        String remoteIp = getRemoteIP(request);

        ConfigInfo configInfo = new ConfigInfo(dataId, group, content);
        boolean checkSuccess = true;
        String errorMessage = "��������";
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч��DataId";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            checkSuccess = false;
            errorMessage = "��Ч�ķ���";
        }
        if (!StringUtils.hasLength(content)) {
            checkSuccess = false;
            errorMessage = "��Ч������";
        }
        if (!checkSuccess) {
            modelMap.addAttribute("message", errorMessage);
            modelMap.addAttribute("configInfo", configInfo);
            return "/admin/config/edit";
        }

        // ������,Ŀ����Ϊ������־�м�¼�����µ����ݵ�����
        ConfigInfo oldConfigInfo = this.configService.findConfigInfo(dataId, group);
        if (oldConfigInfo == null) {
            updateLog.warn("�������ݳ���,Ҫ���µ����ݲ�����, dataId=" + dataId + ",group=" + group);
            modelMap.addAttribute("message", "�������ݳ���, Ҫ���µ����ݲ�����, dataId=" + dataId + ",group=" + group);
            return listConfig(request, response, dataId, group, 1, 20, modelMap);
        }
        String oldContent = oldConfigInfo.getContent();

        // Ϊ�°汾��sdkʹ�õķ���, ʹ�ò����е�ԴͷIP��Դͷ�û�
        this.configService.updateConfigInfo(dataId, group, content, srcIp, srcUser);

        // ��¼������־
        updateLog.warn("�������ݳɹ�\ndataId=" + dataId + "\ngroup=" + group + "\noldContent=\n" + oldContent
                + "\nnewContent=\n" + content + "\nsrc ip=" + srcIp + "\nsrc user=" + srcUser);
        modelMap.addAttribute("message", "�ύ�ɹ�!");
        try {
            // �������ݾۺ�
            this.aggregationService.aggregation(dataId, group);
        }
        catch (Exception e) {
            log.error("���ݾۺϳ���", e);
        }
        try {
            // ʵʱ֪ͨ
            this.realTimeNotify(dataId, group);
        }
        catch (Exception e) {
            log.error("ʵʱ֪ͨ����", e);
        }
        try {
            // ����ip�뷢��dataId��ӳ�䵽redis
            this.addIpToDataIdAndGroup(remoteIp, dataId, group);
        }
        catch (Exception e) {
            log.error("redis����", e);
        }
        return listConfig(request, response, dataId, group, 1, 20, modelMap);
    }


    /**
     * ��ѯ������Ϣ
     * 
     * @param request
     * @param dataId
     * @param group
     * @param pageNo
     * @param pageSize
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=listConfig", method = RequestMethod.GET)
    public String listConfig(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("pageNo") int pageNo, @RequestParam("pageSize") int pageSize, ModelMap modelMap) {
        Page<ConfigInfo> page = this.configService.findConfigInfo(pageNo, pageSize, group, dataId);
        String accept = request.getHeader("Accept");
        if (accept != null && accept.indexOf("application/json") >= 0) {
            try {
                String json = JSONUtils.serializeObject(page);
                modelMap.addAttribute("pageJson", json);
            }
            catch (Exception e) {
                log.error("���л�page�������", e);
            }
            return "/admin/config/list_json";
        }
        else {
            modelMap.addAttribute("dataId", dataId);
            modelMap.addAttribute("group", group);
            modelMap.addAttribute("page", page);
            return "/admin/config/list";
        }
    }


    /**
     * ģ����ѯ������Ϣ
     * 
     * @param request
     * @param dataId
     * @param group
     * @param pageNo
     * @param pageSize
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=listConfigLike", method = RequestMethod.GET)
    public String listConfigLike(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group,
            @RequestParam("pageNo") int pageNo, @RequestParam("pageSize") int pageSize, ModelMap modelMap) {
        if (!StringUtils.hasLength(dataId) && !StringUtils.hasLength(group)) {
            modelMap.addAttribute("message", "ģ����ѯ����������һ����ѯ����");
            return "/admin/config/list";
        }
        Page<ConfigInfo> page = this.configService.findConfigInfoLike(pageNo, pageSize, group, dataId);
        String accept = request.getHeader("Accept");
        if (accept != null && accept.indexOf("application/json") >= 0) {
            try {
                String json = JSONUtils.serializeObject(page);
                modelMap.addAttribute("pageJson", json);
            }
            catch (Exception e) {
                log.error("���л�page�������", e);
            }
            return "/admin/config/list_json";
        }
        else {
            modelMap.addAttribute("page", page);
            modelMap.addAttribute("dataId", dataId);
            modelMap.addAttribute("group", group);
            modelMap.addAttribute("method", "listConfigLike");
            return "/admin/config/list";
        }
    }


    /**
     * �鿴������Ϣ����
     * 
     * @param request
     * @param dataId
     * @param group
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=detailConfig", method = RequestMethod.GET)
    public String getConfigInfo(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataId") String dataId, @RequestParam("group") String group, ModelMap modelMap) {
        dataId = dataId.trim();
        group = group.trim();
        ConfigInfo configInfo = this.configService.findConfigInfo(dataId, group);
        modelMap.addAttribute("configInfo", configInfo);
        return "/admin/config/edit";
    }


    /**
     * �������е�ַ����ӳ����Ϣ
     * 
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=listGroup", method = RequestMethod.GET)
    public String listGroup(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {

        ConcurrentHashMap<String, ConcurrentHashMap<String, GroupInfo>> allAddressGroupMap =
                groupService.getAllAddressGroupMapping();
        modelMap.addAttribute("groupMap", allAddressGroupMap);

        List<GroupInfo> groupInfo = new ArrayList<GroupInfo>();
        for (Entry<String, ConcurrentHashMap<String, GroupInfo>> dataEntry : allAddressGroupMap.entrySet()) {
            for (Entry<String, GroupInfo> inner : dataEntry.getValue().entrySet()) {
                groupInfo.add(inner.getValue());
            }
        }
        String accept = request.getHeader("Accept");
        if (accept != null && accept.indexOf("application/json") >= 0) {
            try {
                String json = JSONUtils.serializeObject(groupInfo);
                modelMap.addAttribute("groupJson", json);
            }
            catch (Exception e) {
                log.error("���л�group�������", e);
            }
            return "/admin/group/list_json";
        }
        modelMap.addAttribute("groupMap", allAddressGroupMap);
        return "/admin/group/list";
    }


    /**
     * ����µĵ�ַ����������ӳ��
     * 
     * @param address
     * @param group
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=addGroup", method = RequestMethod.POST)
    public String addGroup(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("address") String address, @RequestParam("dataId") String dataId,
            @RequestParam("group") String group, ModelMap modelMap) {
        if (!StringUtils.hasLength(address) || DiamondUtils.hasInvalidChar(address.trim())) {
            modelMap.addAttribute("message", "��Ч�ĵ�ַ");
            return "/admin/group/new";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            modelMap.addAttribute("message", "��Ч�ķ�������");
            return "/admin/group/new";
        }
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            modelMap.addAttribute("message", "��Ч��dataId");
            return "/admin/group/new";
        }

        String remoteIp = this.getRemoteIP(request);
        // Ϊ�˼����ϰ汾sdk, �÷�����ԴIP��Դ�û�������Ϊrequest�е�Զ��IP
        // boolean result = groupService.addAddress2GroupMapping(address,
        // dataId, group);
        boolean result = groupService.addAddress2GroupMapping(address, dataId, group, remoteIp, remoteIp);
        if (!result) {
            modelMap.addAttribute("message", "��ַ�Ѵ��ڣ��뽫�õ�ַ�ƶ����·���");
        }
        else {
            modelMap.addAttribute("message", "��ӳɹ�!");
        }
        return listGroup(request, response, modelMap);
    }


    /**
     * �����µķ������, �÷�����ԴͷIP��Դͷ�û���Ϊ����
     * 
     */
    @RequestMapping(params = "method=addGroupNew", method = RequestMethod.POST)
    public String addGroup(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("address") String address, @RequestParam("dataId") String dataId,
            @RequestParam("group") String group, @RequestParam("src_ip") String srcIp,
            @RequestParam("src_user") String srcUser, ModelMap modelMap) {
        if (!StringUtils.hasLength(address) || DiamondUtils.hasInvalidChar(address.trim())) {
            modelMap.addAttribute("message", "��Ч�ĵ�ַ");
            return "/admin/group/new";
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group.trim())) {
            modelMap.addAttribute("message", "��Ч�ķ�������");
            return "/admin/group/new";
        }
        if (!StringUtils.hasLength(dataId) || DiamondUtils.hasInvalidChar(dataId.trim())) {
            modelMap.addAttribute("message", "��Ч��dataId");
            return "/admin/group/new";
        }

        boolean result = groupService.addAddress2GroupMapping(address, dataId, group, srcIp, srcUser);
        if (!result) {
            modelMap.addAttribute("message", "��ַ�Ѵ��ڣ��뽫�õ�ַ�ƶ����·���");
        }
        else {
            modelMap.addAttribute("message", "��ӳɹ�!");
        }
        return listGroup(request, response, modelMap);
    }


    /**
     * ɾ��������Ϣ
     * 
     * @param address
     * @param group
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=deleteGroup", method = RequestMethod.GET)
    public String deleteGroup(HttpServletRequest request, HttpServletResponse response, @RequestParam("id") long id,
            ModelMap modelMap) {
        groupService.removeAddress2GroupMapping(id);
        modelMap.addAttribute("message", "ɾ���ɹ�!");
        return listGroup(request, response, modelMap);

    }


    /**
     * ����ַ�ƶ����µķ���
     * 
     * @param address
     * @param oldGroup
     * @param newGroup
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=moveGroup", method = RequestMethod.GET)
    public String moveGroup(HttpServletRequest request, HttpServletResponse response, @RequestParam("id") long id,
            @RequestParam("newGroup") String newGroup, ModelMap modelMap) {
        newGroup = newGroup.trim();
        if (!StringUtils.hasLength(newGroup) || DiamondUtils.hasInvalidChar(newGroup)) {
            modelMap.addAttribute("message", "��Ч���·�������");
            return listGroup(request, response, modelMap);
        }

        String remoteIp = this.getRemoteIP(request);
        // ������
        if (this.groupService.updateAddress2GroupMapping(id, newGroup, remoteIp, remoteIp)) {
            modelMap.addAttribute("message", "�ƶ�����ɹ�!");
        }
        else {
            modelMap.addAttribute("message", "�ƶ�����ʧ��!");
        }
        return listGroup(request, response, modelMap);
    }


    /**
     * ����ַ�ƶ����µķ���, ʹ�ò����е�ԴͷIP��Դͷ�û�
     * 
     * @param request
     * @param response
     * @param id
     * @param newGroup
     * @param srcIp
     * @param srcUser
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=moveGroupNew", method = RequestMethod.GET)
    public String moveGroup(HttpServletRequest request, HttpServletResponse response, @RequestParam("id") long id,
            @RequestParam("newGroup") String newGroup, @RequestParam("src_ip") String srcIp,
            @RequestParam("src_user") String srcUser, ModelMap modelMap) {
        newGroup = newGroup.trim();
        if (!StringUtils.hasLength(newGroup) || DiamondUtils.hasInvalidChar(newGroup)) {
            modelMap.addAttribute("message", "��Ч���·�������");
            return listGroup(request, response, modelMap);
        }

        if (this.groupService.updateAddress2GroupMapping(id, newGroup, srcIp, srcUser)) {
            modelMap.addAttribute("message", "�ƶ�����ɹ�!");
        }
        else {
            modelMap.addAttribute("message", "�ƶ�����ʧ��!");
        }
        return listGroup(request, response, modelMap);
    }


    /**
     * ���´����ݿ����������Ϣ
     * 
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=reloadGroup", method = RequestMethod.GET)
    public String reloadGroupInfo(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
        this.groupService.loadGroupInfo();
        modelMap.addAttribute("message", "���سɹ�!");
        return listGroup(request, response, modelMap);
    }


    /**
     * չʾ�����û�
     * 
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=listUser", method = RequestMethod.GET)
    public String listUser(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
        Map<String, String> userMap = this.adminService.getAllUsers();
        modelMap.addAttribute("userMap", userMap);
        return "/admin/user/list";
    }


    /**
     * ����û�
     * 
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=addUser", method = RequestMethod.POST)
    public String addUser(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("userName") String userName, @RequestParam("password") String password, ModelMap modelMap) {
        if (!StringUtils.hasLength(userName) || DiamondUtils.hasInvalidChar(userName.trim())) {
            modelMap.addAttribute("message", "��Ч���û���");
            return listUser(request, response, modelMap);
        }
        if (!StringUtils.hasLength(password) || DiamondUtils.hasInvalidChar(password.trim())) {
            modelMap.addAttribute("message", "��Ч������");
            return "/admin/user/new";
        }
        if (this.adminService.addUser(userName, password))
            modelMap.addAttribute("message", "��ӳɹ�!");
        else
            modelMap.addAttribute("message", "���ʧ��!");
        return listUser(request, response, modelMap);
    }


    /**
     * ɾ���û�
     * 
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=deleteUser", method = RequestMethod.GET)
    public String deleteUser(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("userName") String userName, ModelMap modelMap) {
        if (!StringUtils.hasLength(userName) || DiamondUtils.hasInvalidChar(userName.trim())) {
            modelMap.addAttribute("message", "��Ч���û���");
            return listUser(request, response, modelMap);
        }
        if (this.adminService.removeUser(userName)) {
            modelMap.addAttribute("message", "ɾ���ɹ�!");
        }
        else {
            modelMap.addAttribute("message", "ɾ��ʧ��!");
        }
        return listUser(request, response, modelMap);
    }


    /**
     * ��������
     * 
     * @param userName
     * @param password
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=changePassword", method = RequestMethod.GET)
    public String changePassword(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("userName") String userName, @RequestParam("password") String password, ModelMap modelMap) {

        userName = userName.trim();
        password = password.trim();

        if (!StringUtils.hasLength(userName) || DiamondUtils.hasInvalidChar(userName.trim())) {
            modelMap.addAttribute("message", "��Ч���û���");
            return listUser(request, response, modelMap);
        }
        if (!StringUtils.hasLength(password) || DiamondUtils.hasInvalidChar(password.trim())) {
            modelMap.addAttribute("message", "��Ч��������");
            return listUser(request, response, modelMap);
        }
        if (this.adminService.updatePassword(userName, password)) {
            modelMap.addAttribute("message", "���ĳɹ�,�´ε�¼���������룡");
        }
        else {
            modelMap.addAttribute("message", "����ʧ��!");
        }
        return listUser(request, response, modelMap);
    }


    @RequestMapping(params = "method=setRefuseRequestCount", method = RequestMethod.POST)
    public String setRefuseRequestCount(@RequestParam("count") long count, ModelMap modelMap) {
        if (count <= 0) {
            modelMap.addAttribute("message", "�Ƿ��ļ���");
            return "/admin/count";
        }
        GlobalCounter.getCounter().set(count);
        modelMap.addAttribute("message", "���óɹ�!");
        return getRefuseRequestCount(modelMap);
    }


    @RequestMapping(params = "method=getRefuseRequestCount", method = RequestMethod.GET)
    public String getRefuseRequestCount(ModelMap modelMap) {
        modelMap.addAttribute("count", GlobalCounter.getCounter().get());
        return "/admin/count";
    }


    /**
     * �����ļ������û���Ϣ
     * 
     * @param modelMap
     * @return
     */
    @RequestMapping(params = "method=reloadUser", method = RequestMethod.GET)
    public String reloadUser(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
        this.adminService.loadUsers();
        modelMap.addAttribute("message", "���سɹ�!");
        return listUser(request, response, modelMap);
    }


    public AdminService getAdminService() {
        return adminService;
    }


    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }


    public GroupService getGroupService() {
        return groupService;
    }


    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }


    public ConfigService getConfigService() {
        return configService;
    }


    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }


    public TaskManagerService getTaskManagerService() {
        return taskManagerService;
    }


    public void setTaskManagerService(TaskManagerService taskManagerService) {
        this.taskManagerService = taskManagerService;
    }


    public ACLService getaCLService() {
        return aCLService;
    }


    public void setaCLService(ACLService aCLService) {
        this.aCLService = aCLService;
    }


    public FlowControlService getFlowControlService() {
        return flowControlService;
    }


    public void setFlowControlService(FlowControlService flowControlService) {
        this.flowControlService = flowControlService;
    }


    public PushitService getPushitService() {
        return pushitService;
    }


    public void setPushitService(PushitService pushitService) {
        this.pushitService = pushitService;
    }


    public AggregationService getAggregationService() {
        return aggregationService;
    }


    public void setAggregationService(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }


    public RedisTaskProcessor getRedisTaskProcessor() {
        return redisTaskProcessor;
    }


    public void setRedisTaskProcessor(RedisTaskProcessor redisTaskProcessor) {
        this.redisTaskProcessor = redisTaskProcessor;
    }


    public RealTimeNotifyTaskProcessor getRealTimeNotifyTaskProcessor() {
        return realTimeNotifyTaskProcessor;
    }


    public void setRealTimeNotifyTaskProcessor(RealTimeNotifyTaskProcessor realTimeNotifyTaskProcessor) {
        this.realTimeNotifyTaskProcessor = realTimeNotifyTaskProcessor;
    }


    // =========================== �������� ============================== //

    @RequestMapping(params = "method=batchQuery", method = RequestMethod.POST)
    public String batchQuery(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("dataIds") String dataIds, @RequestParam("group") String group, ModelMap modelMap) {

        if (!StringUtils.hasLength(dataIds) || DiamondUtils.hasInvalidChar(dataIds)) {
            throw new IllegalArgumentException("������ѯ, ��Ч��dataIds");
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group)) {
            throw new IllegalArgumentException("������ѯ, ��Ч��group");
        }

        // �ֽ�dataId
        String[] dataIdArray = dataIds.split(Constants.WORD_SEPARATOR);
        group = group.trim();

        List<ConfigInfoEx> configInfoExList = new ArrayList<ConfigInfoEx>();
        for (String dataId : dataIdArray) {
            ConfigInfoEx configInfoEx = new ConfigInfoEx();
            configInfoEx.setDataId(dataId);
            configInfoEx.setGroup(group);
            try {
                // ��ѯ���ݿ�
                ConfigInfo configInfo = this.configService.findConfigInfo(dataId, group);
                if (configInfo == null) {
                    // û���쳣, ˵����ѯ�ɹ�, �����ݲ�����, ���ò����ڵ�״̬��
                    configInfoEx.setStatus(Constants.BATCH_QUERY_NONEXISTS);
                    configInfoEx.setMessage("query data does not exist");
                }
                else {
                    // û���쳣, ˵����ѯ�ɹ�, �������ݴ���, ���ô��ڵ�״̬��
                    String content = configInfo.getContent();
                    configInfoEx.setContent(content);
                    configInfoEx.setStatus(Constants.BATCH_QUERY_EXISTS);
                    configInfoEx.setMessage("query success");
                }
            }
            catch (Exception e) {
                log.error("������ѯ, �ڲ�ѯ���dataIdʱ����, dataId=" + dataId + ",group=" + group, e);
                // �����쳣, �����쳣״̬��
                configInfoEx.setStatus(Constants.BATCH_OP_ERROR);
                configInfoEx.setMessage("query error: " + e.getMessage());
            }
            configInfoExList.add(configInfoEx);
        }

        String json = null;
        try {
            json = JSONUtils.serializeObject(configInfoExList);
        }
        catch (Exception e) {
            log.error("������ѯ������л�����, json=" + json, e);
        }
        modelMap.addAttribute("json", json);

        return "/admin/config/batch_result";
    }


    @RequestMapping(params = "method=batchAddOrUpdate", method = RequestMethod.POST)
    public String batchAddOrUpdate(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("allDataIdAndContent") String allDataIdAndContent, @RequestParam("group") String group,
            @RequestParam("src_ip") String srcIp, @RequestParam("src_user") String srcUser, ModelMap modelMap) {

        if (!StringUtils.hasLength(allDataIdAndContent) || DiamondUtils.hasInvalidChar(allDataIdAndContent)) {
            throw new IllegalArgumentException("����д����, ��Ч��allDataIdAndContent");
        }
        if (!StringUtils.hasLength(group) || DiamondUtils.hasInvalidChar(group)) {
            throw new IllegalArgumentException("����д����, ��Ч��group");
        }

        String[] dataIdAndContentArray = allDataIdAndContent.split(Constants.LINE_SEPARATOR);
        group = group.trim();

        List<ConfigInfoEx> configInfoExList = new ArrayList<ConfigInfoEx>();
        for (String dataIdAndContent : dataIdAndContentArray) {
            String dataId = dataIdAndContent.substring(0, dataIdAndContent.indexOf(Constants.WORD_SEPARATOR));
            String content = dataIdAndContent.substring(dataIdAndContent.indexOf(Constants.WORD_SEPARATOR) + 1);
            ConfigInfoEx configInfoEx = new ConfigInfoEx();
            configInfoEx.setDataId(dataId);
            configInfoEx.setGroup(group);
            configInfoEx.setContent(content);

            try {
                // ��ѯ���ݿ�
                ConfigInfo configInfo = this.configService.findConfigInfo(dataId, group);
                if (configInfo == null) {
                    // ���ݲ�����, ����
                    // this.configService.addConfigInfo(dataId, group, content);
                    this.configService.addConfigInfo(dataId, group, content, srcIp, srcUser);

                    // �����ɹ�, ����״̬��
                    configInfoEx.setStatus(Constants.BATCH_ADD_SUCCESS);
                    configInfoEx.setMessage("add success");
                }
                else {
                    // ���ݴ���, ����
                    // this.configService.updateConfigInfo(dataId, group,
                    // content);
                    this.configService.updateConfigInfo(dataId, group, content, srcIp, srcUser);
                    // ���³ɹ�, ����״̬��
                    configInfoEx.setStatus(Constants.BATCH_UPDATE_SUCCESS);
                    configInfoEx.setMessage("update success");
                }
            }
            catch (Exception e) {
                log.error("����д��������ʱ����, dataId=" + dataId + ",group=" + group + ",content=" + content, e);
                // �����쳣, �����쳣״̬��
                configInfoEx.setStatus(Constants.BATCH_OP_ERROR);
                configInfoEx.setMessage("batch write error: " + e.getMessage());
            }
            configInfoExList.add(configInfoEx);
        }

        String json = null;
        try {
            json = JSONUtils.serializeObject(configInfoExList);
        }
        catch (Exception e) {
            log.error("����д����������л�����, json=" + json, e);
        }
        modelMap.addAttribute("json", json);

        return "/admin/config/batch_result";
    }


    private void addIpToDataIdAndGroup(String remoteIp, String dataId, String group) {
        RedisTask task = new RedisTask(remoteIp, dataId + Constants.WORD_SEPARATOR + group);
        this.taskManagerService.addRedisProcessor(remoteIp + "-redis", redisTaskProcessor);
        this.taskManagerService.addRedisTask(remoteIp + "-redis", task, false);
    }


    private void realTimeNotify(String dataId, String group) {
        RealTimeNotifyTask task = new RealTimeNotifyTask(dataId, group);
        task.setLastProcessTime(System.currentTimeMillis());
        task.setTaskInterval(2000L);
        this.taskManagerService.addPushitProcessor(dataId + "-" + group + "-pushit", realTimeNotifyTaskProcessor);
        this.taskManagerService.addPushitTask(dataId + "-" + group + "-pushit", task, false);
    }

    static class FlowControlObject {
        boolean effective = false;
        String path = "";
    }

}
