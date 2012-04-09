/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 * Authors:
 *   leiwen <chrisredfield1985@126.com> , boyan <killme2008@gmail.com>
 */
package com.taobao.diamond.server.utils;

/**
 * ����HSF��dataId�Ĺ�����
 * 
 * @author leiwen.zh
 * 
 */
public class HSFDataIdUtils {

    public static final String ADDR_SUFFIX = ".ADDRESS";
    public static final String DIAMOND_ROUT_SUFFIX = ".ROUTINGRULE";
    public static final String CONFSRV_ROUT_SUFFIX = ".ROUTING.RULE";
    public static final String DIAMOND_AGGR_SUFFIX = ".FORCONSUMER";


    /**
     * ��config server�˵�HSF��ַ���õ�dataId�����޸�, ��ĩβ���.ADDRESS
     * 
     * @param addrDataId
     *            config server�˵�HSF��ַ���õ�dataId
     * @return �޸ĺ��dataId
     */
    private static String c2dModifyAddress(String addrDataId) {
        StringBuilder result = new StringBuilder(addrDataId);
        result.append(ADDR_SUFFIX);
        return result.toString();
    }


    /**
     * ��diamond�˵�HSF��ַ���õȴ�dataId�����޸�, ȥ��ĩβ��.ADDRESS
     * 
     * @param addrDataId
     *            diamond�˵�HSF��ַ���õ�dataId
     * @return �޸ĺ��dataId
     */
    private static String d2cModifyAddress(String addrDataId) {
        return addrDataId.substring(0, addrDataId.indexOf(ADDR_SUFFIX));
    }


    /**
     * ��config server�˵�HSF·�ɹ������õ�dataId�����޸�, ��ROUTING.RULE��"."ȥ��
     * 
     * @param routDataId
     *            config server�˵�HSF·�ɹ������õ�dataId
     * @return �޸ĺ��dataId
     */
    private static String c2dModifyRoutingRule(String routDataId) {
        return routDataId.replace(CONFSRV_ROUT_SUFFIX, DIAMOND_ROUT_SUFFIX);
    }


    /**
     * ��diamond�˵�HSF·�ɹ������õ�dataId�����޸�, ��ROUTINGRULE��ΪROUTING.RULE
     * 
     * @param routDataId
     *            diamond�˵�HSF·�ɹ������õ�dataId
     * @return �޸ĺ��dataId
     */
    private static String d2cModifyRoutingRule(String routDataId) {
        return routDataId.replace(DIAMOND_ROUT_SUFFIX, CONFSRV_ROUT_SUFFIX);
    }


    public static String d2cDataIdMapping(String dataId) {
        if (dataId.endsWith(HSFDataIdUtils.ADDR_SUFFIX)) {
            dataId = HSFDataIdUtils.d2cModifyAddress(dataId);
        }
        if (dataId.endsWith(HSFDataIdUtils.DIAMOND_ROUT_SUFFIX)) {
            dataId = HSFDataIdUtils.d2cModifyRoutingRule(dataId);
        }
        return dataId;
    }
    
    
    public static String c2dDataIdMapping(String dataId, boolean isUrl) {
        if (dataId.endsWith(HSFDataIdUtils.CONFSRV_ROUT_SUFFIX)) {
            dataId = HSFDataIdUtils.c2dModifyRoutingRule(dataId);
        }
        if ( isUrl ) {
            dataId = c2dModifyAddress(dataId);
        }
        
        return dataId;
    }
}
