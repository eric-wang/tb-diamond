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

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


/**
 * Redis����ʹ��Redis��Java�ͻ���Jedis
 * 
 * @author leiwen
 * 
 */
public class RedisService {

    private JedisPool pool;


    public RedisService(String redisServerIp, String redisServerPort) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxActive(100);
        poolConfig.setMaxWait(1000);
        pool = new JedisPool(poolConfig, redisServerIp, Integer.parseInt(redisServerPort));
    }


    /**
     * ����һ��valueֵ��key��Ӧ�ļ�����
     */
    public void add(String key, String value) {
        Jedis client = null;
        try {
            client = pool.getResource();
            client.sadd(key, value);
            client.save();
        }
        finally {
            if (client != null) {
                client.disconnect();
                pool.returnResource(client);
            }
        }
    }


    /**
     * ����һ��value��key��Ӧ�ļ�����
     */
    public void addAll(String key, String[] values) {
        Jedis client = null;
        try {
            client = pool.getResource();
            for (String value : values) {
                client.sadd(key, value);
            }
            client.save();
        }
        finally {
            if (client != null) {
                client.disconnect();
                pool.returnResource(client);
            }
        }
    }


    /**
     * ��key��Ӧ�ļ����е�valueֵɾ��
     */
    public void remove(String key, String value) {
        Jedis client = null;
        try {
            client = pool.getResource();
            client.srem(key, value);
            client.save();
        }
        finally {
            if (client != null) {
                client.disconnect();
                pool.returnResource(client);
            }
        }
    }


    /*
     * ��ȡkey��Ӧ�ļ���
     */
    public Set<String> get(String key) {
        Set<String> result = new HashSet<String>();
        Jedis client = null;
        try {
            client = pool.getResource();
            result = client.smembers(key);
            return result;
        }
        finally {
            if (client != null) {
                client.disconnect();
                pool.returnResource(client);
            }
        }
    }


    public void close() {
        pool.destroy();
    }
}
