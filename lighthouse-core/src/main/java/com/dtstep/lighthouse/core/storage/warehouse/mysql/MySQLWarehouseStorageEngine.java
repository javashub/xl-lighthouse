package com.dtstep.lighthouse.core.storage.warehouse.mysql;
/*
 * Copyright (C) 2022-2025 XueLing.雪灵
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.dtstep.lighthouse.common.exception.InitializationException;
import com.dtstep.lighthouse.common.hash.HashUtil;
import com.dtstep.lighthouse.common.util.JsonUtil;
import com.dtstep.lighthouse.common.util.StringUtil;
import com.dtstep.lighthouse.core.config.LDPConfig;
import com.dtstep.lighthouse.core.dao.DBConnectionSource;
import com.dtstep.lighthouse.core.dao.RDBMSConfiguration;
import com.dtstep.lighthouse.core.lock.RedissonLock;
import com.dtstep.lighthouse.core.storage.common.*;
import com.dtstep.lighthouse.core.storage.warehouse.WarehouseStorageEngine;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MySQLWarehouseStorageEngine implements WarehouseStorageEngine {

    private static final Logger logger = LoggerFactory.getLogger(MySQLWarehouseStorageEngine.class);

    private static final BasicDataSource basicDataSource;

    private static final RDBMSConfiguration mySQLConfiguration;

    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    static {
        String driverClassName = LDPConfig.getVal("warehouse.storage.engine.javax.jdo.option.driverClassName");
        String connectionUrl = LDPConfig.getVal("warehouse.storage.engine.javax.jdo.option.ConnectionURL");
        String connectionUserName = LDPConfig.getVal("warehouse.storage.engine.javax.jdo.option.ConnectionUserName");
        String connectionPassword = LDPConfig.getVal("warehouse.storage.engine.javax.jdo.option.ConnectionPassword");
        try{
            Validate.isTrue(StringUtil.isNotEmpty(driverClassName));
            Validate.isTrue(StringUtil.isNotEmpty(connectionUrl));
            Validate.isTrue(StringUtil.isNotEmpty(connectionUserName));
            Validate.isTrue(StringUtil.isNotEmpty(connectionPassword));
            mySQLConfiguration = new RDBMSConfiguration(driverClassName,connectionUrl,connectionUserName,connectionPassword);
            basicDataSource = DBConnectionSource.getBasicDataSource(mySQLConfiguration);
            logger.info("Database[{}] connection initialization completed!",mySQLConfiguration.getDatabase());
        }catch (Exception ex){
            logger.error("init mysql warehouse connection error!",ex);
            throw new InitializationException("init mysql warehouse connection error!");
        }
    }

    private Connection getConnection() throws Exception {
        Connection conn = connectionHolder.get();
        if (conn == null || conn.isClosed()) {
            conn = basicDataSource.getConnection();
            connectionHolder.set(conn);
        }
        return conn;
    }

    private void closeConnection() throws Exception {
        Connection conn = connectionHolder.get();
        if (conn != null) {
            try{
                conn.close();
            }catch (Exception ex){
                ex.printStackTrace();
            }finally {
                connectionHolder.remove();
            }
        }
    }

    @Override
    public String getDefaultNamespace() {
        return null;
    }

    @Override
    public void createNamespaceIfNotExist(String namespace) throws Exception {}

    @Override
    public void createResultTable(String tableName) throws Exception {
        Connection connection = null;
        Statement statement = null;
        try{
            connection = getConnection();
            String sql = String.format("CREATE TABLE IF NOT EXISTS %s ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "k VARCHAR(200) NOT NULL, "
                    + "v bigint NOT NULL DEFAULT '0', "
                    + "upd_time timestamp NOT NULL, "
                    + "exp_time timestamp NOT NULL, "
                    + "UNIQUE KEY `k_UNIQUE` (`k`), "
                    + "KEY `index_exp_time` (`exp_time`)"
                    + ") ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb3",tableName);
            statement = connection.createStatement();
            statement.execute(sql);
            logger.info("Mysql Table '{}' created successfully!",tableName);
        } catch (SQLException ex) {
            logger.error("Mysql Table '{}' created failed!",tableName,ex);
            ex.printStackTrace();
        } finally {
            release(null,statement,connection);
        }
    }

    @Override
    public void createDimensTable(String tableName) throws Exception {
        Connection connection = null;
        Statement statement = null;
        try{
            connection = getConnection();
            String sql = String.format("CREATE TABLE IF NOT EXISTS %s ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "k VARCHAR(200) NOT NULL, "
                    + "v VARCHAR(800) NOT NULL, "
                    + "upd_time timestamp NOT NULL, "
                    + "exp_time timestamp NOT NULL, "
                    + "UNIQUE KEY `k_UNIQUE` (`k`), "
                    + "KEY `index_exp_time` (`exp_time`)"
                    + ") ENGINE=InnoDB AUTO_INCREMENT=100000 DEFAULT CHARSET=utf8mb3",tableName);
            statement = connection.createStatement();
            statement.execute(sql);
            logger.info("Mysql Table '{}' created successfully!",tableName);
        } catch (SQLException ex) {
            logger.error("Mysql Table '{}' created failed!",tableName,ex);
            ex.printStackTrace();
        } finally {
            release(null,statement,connection);
        }
    }

    @Override
    public boolean isTableExist(String tableName) throws Exception {
        String query = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try{
            connection = getConnection();
            ps = connection.prepareStatement(query);
            ps.setString(1, mySQLConfiguration.getDatabase());
            ps.setString(2, tableName);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }catch (Exception ex){
            logger.error("check mysql table exist error!",ex);
        }finally {
            release(rs,ps,connection);
        }
        return false;
    }

    @Override
    public void dropTable(String tableName) throws Exception {
        String sql = String.format("DROP TABLE %s",tableName);
        Connection connection = null;
        Statement statement = null;
        try{
            connection = getConnection();
            statement = connection.createStatement();
            statement.execute(sql);
            logger.info("drop table:{} successfully!",tableName);
        }catch (Exception ex){
            logger.error("drop table:{} error!",tableName,ex);
            ex.printStackTrace();
        }finally {
            release(null,statement,connection);
        }
    }

    @Override
    public void put(String tableName, LdpPut ldpPut) throws Exception {
        Object value = ldpPut.getData();
        String sql = "INSERT ignore INTO " + tableName + " (`k`, `v`, `exp_time`, `upd_time`) VALUES (?, ?, ?, ?) on duplicate key update v = ?,exp_time = ? , upd_time = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        long current = System.currentTimeMillis();
        try{
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1,getDBKey(ldpPut.getKey(), ldpPut.getColumn()));
            if(value.getClass() == String.class){
                ps.setString(2,ldpPut.getData().toString());
            }else if(value.getClass() == Long.class){
                ps.setLong(2,(Long)ldpPut.getData());
            }else {
                throw new IllegalArgumentException(String.format("Current type(%s) not supported!",value.getClass()));
            }
            ps.setTimestamp(3,new Timestamp(current + ldpPut.getTtl()));
            ps.setTimestamp(4, new Timestamp(current));
            if (value.getClass() == String.class) {
                ps.setString(5, ldpPut.getData().toString());
            } else {
                ps.setLong(5, (Long) ldpPut.getData());
            }
            ps.setTimestamp(6,new Timestamp(current + ldpPut.getTtl()));
            ps.setTimestamp(7, new Timestamp(current));
            ps.executeUpdate();
        }catch (Exception ex){
            logger.error("put data to mysql error,tableName:{}!",tableName,ex);
            ex.printStackTrace();
        }finally {
            release(null,ps,connection);
        }
    }

    private static final String MYSQL_PUTS_LOCK_PREFIX = "MYSQL_PUTS_LOCK_PREFIX";

    @Override
    public void puts(String tableName, List<LdpPut> ldpPuts) throws Exception {
        String sql = "INSERT ignore INTO " + tableName + " (`k`, `v`, `exp_time`, `upd_time`) VALUES (?, ?, ?, ?) on duplicate key update v = ?,exp_time = ? , upd_time = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        long current = System.currentTimeMillis();
        Map<Long,List<LdpPut>> map = ldpPuts.stream().collect(Collectors.groupingBy(x -> HashUtil.BKDRHash(getDBKey(x.getKey(),x.getColumn())) % batchSalt));
        for(Long object : map.keySet()){
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<LdpPut> subList = map.get(object);
            String lockKey = MYSQL_PUTS_LOCK_PREFIX + "_" + tableName + "_" +  + object;
            boolean isLock = RedissonLock.tryLock(lockKey,8,3,TimeUnit.MINUTES);
            if(isLock){
                try{
                    connection = getConnection();
                    ps = connection.prepareStatement(sql);
                    for (LdpPut ldpPut : subList) {
                        Object value = ldpPut.getData();
                        ps.setString(1, getDBKey(ldpPut.getKey(), ldpPut.getColumn()));
                        if (value.getClass() == String.class) {
                            ps.setString(2, ldpPut.getData().toString());
                        } else if (value.getClass() == Long.class) {
                            ps.setLong(2, (Long) ldpPut.getData());
                        } else {
                            throw new IllegalArgumentException(String.format("Current type(%s) not supported!", value.getClass()));
                        }
                        ps.setTimestamp(3,new Timestamp(current + ldpPut.getTtl()));
                        ps.setTimestamp(4, new Timestamp(current));
                        if (value.getClass() == String.class) {
                            ps.setString(5, ldpPut.getData().toString());
                        } else {
                            ps.setLong(5, (Long) ldpPut.getData());
                        }
                        ps.setTimestamp(6,new Timestamp(current + ldpPut.getTtl()));
                        ps.setTimestamp(7, new Timestamp(current));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    ps.clearBatch();
                } catch (Exception ex){
                    logger.error("puts data to mysql error,tableName:{},putsSize:{}!",tableName,subList.size(),ex);
                    ex.printStackTrace();
                } finally {
                    release(null,ps,connection);
                    try{
                        RedissonLock.unLock(lockKey);
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }else{
                logger.error("try lock failed,thread unable to acquire lock,this batch data may be lost,cost:{}ms!",stopWatch.getTime());
            }
        }
    }

    private static final String MYSQL_INCREMENT_PUT_LOCK = "MYSQL_INCREMENT_PUT_LOCK";

    @Override
    public void increment(String tableName, LdpIncrement ldpIncrement) throws Exception {
        Validate.notNull(ldpIncrement);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Connection connection = null;
        PreparedStatement ps = null;
        long current = System.currentTimeMillis();
        String sql = "INSERT INTO " + tableName + "(`k`,`v`,`exp_time`,`upd_time`) values (?, ?, ?, ?) on duplicate key update `v` = `v` + ?, `exp_time` = ? , `upd_time` = ?";
        String dbKey = getDBKey(ldpIncrement.getKey(),ldpIncrement.getColumn());
        String lockKey = MYSQL_INCREMENT_PUT_LOCK + "_" + tableName + "_" + dbKey;
        boolean isLock = RedissonLock.tryLock(lockKey,8,3, TimeUnit.MINUTES);
        try {
            if(isLock){
                connection = getConnection();
                ps = connection.prepareStatement(sql);
                ps.setString(1, dbKey);
                ps.setLong(2,ldpIncrement.getStep());
                ps.setTimestamp(3,new Timestamp(current + ldpIncrement.getTtl()));
                ps.setTimestamp(4,new Timestamp(current));
                ps.setLong(5,ldpIncrement.getStep());
                ps.setTimestamp(6,new Timestamp(current + ldpIncrement.getTtl()));
                ps.setTimestamp(7,new Timestamp(current));
                ps.executeUpdate();
            }else{
                logger.error("try lock failed,thread unable to acquire lock,this batch data may be lost,cost:{}ms!",stopWatch.getTime());
            }
        } catch (Exception ex) {
            logger.error("increment process error!",ex);
            ex.printStackTrace();
        } finally {
            release(null,ps,connection);
            try{
                RedissonLock.unLock(lockKey);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    private static final String MYSQL_INCREMENTS_LOCK_PREFIX = "MYSQL_INCREMENTS_PUT_LOCK";

    @Override
    public void increments(String tableName, List<LdpIncrement> ldpIncrements) throws Exception {
        long current = System.currentTimeMillis();
        String sql = "INSERT INTO " + tableName + "(`k`,`v`,`exp_time`,`upd_time`) values (?, ?, ?, ?) on duplicate key update `v` = `v` + ?, `exp_time` = ? , `upd_time` = ?";
        Map<Long,List<LdpIncrement>> map = ldpIncrements.stream().collect(Collectors.groupingBy(x -> HashUtil.BKDRHash(getDBKey(x.getKey(),x.getColumn())) % batchSalt));
        for(Long object : map.keySet()){
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<LdpIncrement> subList = map.get(object);
            String lockKey = MYSQL_INCREMENTS_LOCK_PREFIX + "_" + tableName + "_" +  + object;
            boolean isLock = RedissonLock.tryLock(lockKey,8,3, TimeUnit.MINUTES);
            if(isLock){
                Connection connection = null;
                PreparedStatement ps = null;
                try {
                    connection = getConnection();
                    ps = connection.prepareStatement(sql);
                    for(LdpIncrement ldpIncrement : subList){
                        ps.setString(1, getDBKey(ldpIncrement.getKey(),ldpIncrement.getColumn()));
                        ps.setLong(2,ldpIncrement.getStep());
                        ps.setTimestamp(3,new Timestamp(current + ldpIncrement.getTtl()));
                        ps.setTimestamp(4,new Timestamp(current));
                        ps.setLong(5,ldpIncrement.getStep());
                        ps.setTimestamp(6,new Timestamp(current + ldpIncrement.getTtl()));
                        ps.setTimestamp(7,new Timestamp(current));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    ps.clearBatch();
                } catch (Exception ex) {
                    logger.error("increments process error!",ex);
                    ex.printStackTrace();
                } finally {
                    release(null,ps,connection);
                    try{
                        RedissonLock.unLock(lockKey);
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }else{
                logger.error("try lock failed,thread unable to acquire lock,this batch data may be lost,cost:{}ms!",stopWatch.getTime());
            }
        }
    }

    private static final int batchSalt = 4;

    private static final String MYSQL_COMPARE_PUT_LOCK_PREFIX = "MYSQL_COMPARE_PUT_LOCK_PREFIX";

    @Override
    public void putsWithCompare(String tableName, CompareOperator compareOperator, List<LdpPut> ldpPuts) throws Exception {
        if(CollectionUtils.isEmpty(ldpPuts)){
            return;
        }
        Map<Long,List<LdpPut>> map = ldpPuts.stream().collect(Collectors.groupingBy(x -> HashUtil.BKDRHash(getDBKey(x.getKey(),x.getColumn())) % batchSalt));
        for(Long object : map.keySet()){
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            String lockKey = MYSQL_COMPARE_PUT_LOCK_PREFIX + "_" + tableName + "_" + compareOperator + "_" + object;
            boolean isLock = RedissonLock.tryLock(lockKey,8,3, TimeUnit.MINUTES);
            if(isLock){
                try{
                    List<LdpPut> subList = map.get(object);
                    List<LdpGet> getList = new ArrayList<>();
                    for(LdpPut ldpPut:subList){
                        LdpGet ldpGet = new LdpGet();
                        ldpGet.setKey(ldpPut.getKey());
                        ldpGet.setColumn(ldpPut.getColumn());
                        getList.add(ldpGet);
                    }
                    List<LdpResult<Long>> dbResults = gets(tableName,getList,Long.class);
                    Map<String,Long> dbValueMap = null;
                    if(CollectionUtils.isNotEmpty(dbResults)){
                        dbValueMap = dbResults.stream().filter(x -> x.getData() != null).collect(Collectors.toMap(x -> getDBKey(x.getKey(),x.getColumn()), LdpResult::getData));
                    }
                    List<LdpPut> filterPuts = new ArrayList<>();
                    for(LdpPut ldpPut : subList){
                        String rowKey = ldpPut.getKey();
                        String column = ldpPut.getColumn();
                        String aggregateKey = getDBKey(rowKey,column);
                        if(compareOperator == CompareOperator.GREATER){
                            if(MapUtils.isEmpty(dbValueMap) || !dbValueMap.containsKey(aggregateKey) || (Long)ldpPut.getData() > dbValueMap.get(aggregateKey)){
                                filterPuts.add(ldpPut);
                            }
                        }else{
                            if(MapUtils.isEmpty(dbValueMap) || !dbValueMap.containsKey(aggregateKey) || (Long)ldpPut.getData() < dbValueMap.get(aggregateKey)){
                                filterPuts.add(ldpPut);
                            }
                        }
                    }
                    puts(tableName,filterPuts);
                }catch (Exception ex){
                    logger.error("batch put error!",ex);
                }finally {
                    RedissonLock.unLock(lockKey);
                }
            }else{
                logger.error("try lock failed,thread unable to acquire lock,this batch data may be lost,cost:{}ms!",stopWatch.getTime());
            }
        }
    }

    @Override
    public <R> LdpResult<R> get(String tableName, LdpGet ldpGet, Class<R> clazz) throws Exception {
        String sql = "SELECT v FROM " + tableName + " WHERE `k` = ?";
        String ldpKey = ldpGet.getKey();
        String ldpColumn = ldpGet.getColumn();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        LdpResult<R> ldpResult = new LdpResult<>();
        ldpResult.setKey(ldpKey);
        ldpResult.setColumn(ldpColumn);
        try{
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1,getDBKey(ldpKey,ldpColumn));
            rs = ps.executeQuery();
            R result = null;
            while (rs.next()){
                if(clazz == Long.class){
                    result = clazz.cast(rs.getString(1));
                }else if(clazz == String.class) {
                    result = clazz.cast(rs.getLong(1));
                }
            }
            ldpResult.setData(result);
        }catch (Exception ex){
            logger.error("get data from mysql error!",ex);
        }finally {
            release(rs,ps,connection);
        }
        return ldpResult;
    }

    private static final int BATCH_GET_SIZE = 200;

    private <R> List<LdpResult<R>> partGets(String tableName, List<LdpGet> ldpGets, Class<R> clazz) throws Exception {
        String placeholders = String.join(", ", Collections.nCopies(ldpGets.size(), "?"));
        String sql = "SELECT `k`,`v`,`upd_time` FROM " + tableName + " WHERE k IN("+placeholders+")";
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<LdpResult<R>> ldpResults = new ArrayList<>();
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            for (int i = 0; i < ldpGets.size(); i++) {
                LdpGet ldpGet = ldpGets.get(i);
                String tempKey = getDBKey(ldpGet.getKey(),ldpGet.getColumn());
                ps.setString(i + 1, tempKey);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                LdpResult<R> ldpResult = new LdpResult<>();
                R r = null;
                String dbKey = rs.getString("k");
                if(clazz == Long.class){
                    r = clazz.cast(rs.getLong("v"));
                }else if(clazz == String.class){
                    r = clazz.cast(rs.getString("v"));
                }
                Timestamp updTime = rs.getTimestamp("upd_time");
                String key;
                String column = null;
                if(dbKey.contains(";")){
                    key = dbKey.split(";")[0];
                    column = dbKey.split(";")[1];
                }else{
                    key = dbKey;
                }
                ldpResult.setKey(key);
                ldpResult.setColumn(column);
                ldpResult.setData(r);
                ldpResult.setTimestamp(updTime.getTime());
                ldpResults.add(ldpResult);
            }
        } catch (Exception ex){
            logger.error("query data info error!",ex);
            ex.printStackTrace();
        } finally {
            release(rs,ps,connection);
        }
        return ldpResults;
    }

    private void release(ResultSet rs,Statement statement,Connection connection){
        try{
            if(rs != null){
                rs.close();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        try{
            if(statement != null){
                statement.close();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        try{
            closeConnection();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private static final ExecutorService pool = Executors.newFixedThreadPool(5,new BasicThreadFactory.Builder().namingPattern("MySql-WarehouseEngine-schedule-pool-%d").build());

    @Override
    public <R> List<LdpResult<R>> gets(String tableName, List<LdpGet> ldpGets, Class<R> clazz) throws Exception {
        int totalSize = ldpGets.size();
        int loopSize = totalSize % BATCH_GET_SIZE == 0 ? totalSize / BATCH_GET_SIZE : totalSize / BATCH_GET_SIZE + 1;
        ArrayList<Future<List<LdpResult<R>>>> results = new ArrayList<>();
        for (int loop = 0; loop < loopSize; loop++)
        {
            int end = Math.min((loop + 1) * BATCH_GET_SIZE, totalSize);
            List<LdpGet> partGets = ldpGets.subList(loop * BATCH_GET_SIZE, end);
            MysqlGetterThread<R> hBaseGetterThread = new MysqlGetterThread<>(tableName,partGets,clazz);
            synchronized (pool)
            {
                Future<List<LdpResult<R>>> result = pool.submit(hBaseGetterThread);
                results.add(result);
            }
        }
        List<LdpResult<R>> totalResult = new ArrayList<>();
        for (Future<List<LdpResult<R>>> subResults : results){
            List<LdpResult<R>> ldpResults = subResults.get();
            if(CollectionUtils.isNotEmpty(ldpResults)){
                totalResult.addAll(ldpResults);
            }
        }
        return totalResult;
    }

    @Override
    public <R> List<LdpResult<R>> scan(String tableName, String startRow, String endRow, int limit, Class<R> clazz) throws Exception {
        String sql = "SELECT `k` , `v` ,`upd_time` from " + tableName + " where k >= ? and k < ? order by k asc limit ?";
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<LdpResult<R>> ldpResults = new ArrayList<>();
        try{
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1,startRow);
            ps.setString(2,endRow);
            ps.setInt(3,limit);
            rs = ps.executeQuery();
            while (rs.next()) {
                LdpResult<R> ldpResult = new LdpResult<>();
                R r = null;
                String dbKey = rs.getString("k");
                if(clazz == Long.class){
                    r = clazz.cast(rs.getLong("v"));
                }else if(clazz == String.class){
                    r = clazz.cast(rs.getString("v"));
                }
                Timestamp updTime = rs.getTimestamp("upd_time");
                String key;
                String column = null;
                if(dbKey.contains(";")){
                    key = dbKey.split(";")[0];
                    column = dbKey.split(";")[1];
                }else{
                    key = dbKey;
                }
                ldpResult.setKey(key);
                ldpResult.setColumn(column);
                ldpResult.setData(r);
                ldpResult.setTimestamp(updTime.getTime());
                ldpResults.add(ldpResult);
            }
        }catch (Exception ex){
            logger.error("scan mysql data error!",ex);
            ex.printStackTrace();
        }finally {
            release(rs,ps,connection);
        }
        return ldpResults;
    }

    @Override
    public void delete(String tableName, String key) throws Exception {
        String sql = "DELETE FROM " + tableName + " WHERE k = ?";
        Connection connection = null;
        PreparedStatement ps = null;
        try{
            connection = getConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1,key);
            ps.executeUpdate();
        }catch (Exception ex){
            logger.error("delete mysql data error!",ex);
            ex.printStackTrace();
        }finally {
            release(null,ps,connection);
        }
    }

    @Override
    public void deletes(String tableName, List<String> keyList) throws Exception {
        if (keyList == null || keyList.isEmpty()) {
            logger.warn("Key list is empty, nothing to delete.");
            return;
        }
        StringBuilder sql = new StringBuilder("DELETE FROM " + tableName + " WHERE k IN (");
        sql.append("?,".repeat(keyList.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = getConnection();
            ps = connection.prepareStatement(sql.toString());
            for (int i = 0; i < keyList.size(); i++) {
                ps.setString(i + 1, keyList.get(i));
            }
            ps.executeUpdate();
        } catch (Exception ex) {
            logger.error("Batch delete mysql data error!", ex);
            ex.printStackTrace();
        } finally {
            release(null, ps, connection);
        }
    }

    @Override
    public boolean isAppendable(String tableName) throws Exception {
        return true;
    }

    @Override
    public long getTableMaxValidPeriod() throws Exception {
        return TimeUnit.DAYS.toSeconds(120);
    }

    private static String getDBKey(String ldpKey, String ldpColumn){
        if(StringUtil.isNotEmpty(ldpColumn)){
            return ldpKey + ";" + ldpColumn;
        }else{
            return ldpKey;
        }
    }

    private class MysqlGetterThread<R> implements Callable<List<LdpResult<R>>> {

        private final String tableName;

        private final List<LdpGet> getList;

        private final Class<R> clazz;

        public MysqlGetterThread(String tableName,List<LdpGet> getList,Class<R> clazz){
            this.getList = getList;
            this.tableName = tableName;
            this.clazz = clazz;
        }

        @Override
        public List<LdpResult<R>> call() throws Exception {
            return partGets(tableName,getList,clazz);
        }
    }
}
