package com.dtstep.lighthouse.core.storage.engine.hbase;

import com.dtstep.lighthouse.common.constant.SysConst;
import com.dtstep.lighthouse.common.util.StringUtil;
import com.dtstep.lighthouse.core.config.LDPConfig;
import com.dtstep.lighthouse.core.storage.LdpGet;
import com.dtstep.lighthouse.core.storage.LdpIncrement;
import com.dtstep.lighthouse.core.storage.LdpPut;
import com.dtstep.lighthouse.core.storage.Result;
import com.dtstep.lighthouse.core.storage.engine.StorageEngine;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.MemoryCompactionPolicy;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.io.encoding.DataBlockEncoding;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.hadoop.hbase.util.Bytes;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class HBaseStorageEngine implements StorageEngine {

    private static final Logger logger = LoggerFactory.getLogger(HBaseStorageEngine.class);

    private static Admin hBaseAdmin = null;

    private static Compression.Algorithm algorithm = null;

    static {
        try{
            hBaseAdmin = getConnection().getAdmin();
        }catch (Exception ex){
            logger.error("hbase get admin error!",ex);
        }
        String compression = LDPConfig.getOrDefault(LDPConfig.KEY_DATA_COMPRESSION_TYPE,"zstd",String.class);
        if(StringUtil.isNotEmpty(compression)){
            switch (compression) {
                case "snappy":
                    algorithm = Compression.Algorithm.SNAPPY;
                    break;
                case "bzip2":
                    algorithm = Compression.Algorithm.BZIP2;
                    break;
                case "zstd":
                    algorithm = Compression.Algorithm.ZSTD;
                    break;
                case "gz":
                    algorithm = Compression.Algorithm.GZ;
                    break;
                case "lzo":
                    algorithm = Compression.Algorithm.LZO;
                    break;
                case "lzma":
                    algorithm = Compression.Algorithm.LZMA;
                    break;
                default:
                    algorithm = Compression.Algorithm.NONE;
                    break;
            }
        }
    }

    private static volatile Connection connection = null;

    public static Connection getConnection() throws Exception{
        if(connection == null || connection.isClosed()){
            synchronized (HBaseStorageEngine.class){
                if(connection == null || connection.isClosed()){
                    StopWatch stopWatch = new StopWatch();
                    stopWatch.start();
                    String zooQuorum = LDPConfig.getVal(LDPConfig.KEY_ZOOKEEPER_QUORUM);
                    String port = LDPConfig.getVal(LDPConfig.KEY_ZOOKEEPER_QUORUM_PORT);
                    Configuration hBaseConfiguration = HBaseConfiguration.create();
                    hBaseConfiguration.set("hbase.zookeeper.quorum",zooQuorum);
                    hBaseConfiguration.set("hbase.zookeeper.property.clientPort",port);
                    hBaseConfiguration.setInt("hbase.client.ipc.pool.size",10);
                    hBaseConfiguration.setInt("hbase.rpc.timeout",180000);
                    hBaseConfiguration.setInt("hbase.client.operation.timeout",240000);
                    hBaseConfiguration.setInt("hbase.client.scanner.timeout.period",180000);
                    connection = ConnectionFactory.createConnection(hBaseConfiguration);
                    logger.info("create hbase connection,thread:{},cost:{}",Thread.currentThread().getName(),stopWatch.getTime());
                }
            }
        }
        return connection;
    }

    public boolean isNameSpaceExist(String namespace) throws Exception {
        String [] namespaceArr = hBaseAdmin.listNamespaces();
        for(String dbNamespace : namespaceArr){
            if(dbNamespace.equals(namespace)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void createNamespace(String namespace) throws Exception {
        if(isNameSpaceExist(namespace)){
            return;
        }
        NamespaceDescriptor namespaceDescriptor = NamespaceDescriptor.create(namespace).build();
        hBaseAdmin.createNamespace(namespaceDescriptor);
        logger.info("create namespace {} success!",namespace);
    }

    @Override
    public void createTable(String tableName) throws Exception {
        int prePartitionsSize = SysConst._DBKeyPrefixArray.length;
        String [] keys = SysConst._DBKeyPrefixArray;
        byte[][] splitKeys = new byte[prePartitionsSize][];
        TreeSet<byte[]> rows = new TreeSet<>(Bytes.BYTES_COMPARATOR);
        for (int i = 0; i < prePartitionsSize; i++) {
            rows.add(Bytes.toBytes(keys[i]));
        }
        Iterator<byte[]> rowKeyIterator = rows.iterator();
        int i=0;
        while (rowKeyIterator.hasNext()) {
            byte[] tempRow = rowKeyIterator.next();
            rowKeyIterator.remove();
            splitKeys[i] = tempRow;
            i++;
        }
        TableDescriptorBuilder tableDescriptor = TableDescriptorBuilder.newBuilder(TableName.valueOf(tableName));
        ColumnFamilyDescriptor family = ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("f")).setMaxVersions(1)
                .setInMemory(false)
                .setBloomFilterType(BloomType.ROW)
                .setCompressTags(true)
                .setTimeToLive((int) TimeUnit.DAYS.toSeconds(30))
                .setCompactionCompressionType(algorithm)
                .setCompressionType(algorithm)
                .setBlocksize(16384)
                .setDataBlockEncoding(DataBlockEncoding.FAST_DIFF)
                .setInMemoryCompaction(MemoryCompactionPolicy.BASIC).build();
        tableDescriptor.setColumnFamily(family);
        try{
            hBaseAdmin.createTable(tableDescriptor.build(),splitKeys);
        }catch (Exception ex){
            logger.error("create hbase table,system error.metaName:{}",tableName,ex);
            throw ex;
        }
        boolean isResult;
        try{
            isResult = isTableExist(tableName);
        }catch (Exception ex){
            logger.error(String.format("create hbase table,check status error.metaName:%s",tableName),ex);
            throw ex;
        }
        if(!isResult) {
            logger.info("create hbase table error,table not created.metaName:{}",tableName);
            throw new Exception("create hbase table error!");
        }
    }

    @Override
    public boolean isTableExist(String tableName) throws Exception {
        TableName tableNameObj = TableName.valueOf(tableName);
        return hBaseAdmin.tableExists(tableNameObj);
    }

    @Override
    public void dropTable(String tableName) throws Exception {
        if(!isTableExist(tableName)){
            return;
        }
        TableName table = TableName.valueOf(tableName);
        hBaseAdmin.disableTable(table);
        hBaseAdmin.deleteTable(table);
    }

    @Override
    public void increment(String tableName, LdpIncrement ldpIncrement) throws Exception {
        String rowKey = ldpIncrement.getKey();
        String column = ldpIncrement.getColumn();
        long step = ldpIncrement.getStep();
        try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
            Increment increment = new Increment(Bytes.toBytes(rowKey));
            increment.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), step);
            increment.setTTL(ldpIncrement.getTtl());
            increment.setReturnResults(false);
            increment.setDurability(Durability.SYNC_WAL);
            table.increment(increment);
        }catch (Exception ex){
            logger.error("hbase put error,tableName:{}!",tableName,ex);
            throw ex;
        }
    }

    @Override
    public void increments(String tableName, List<LdpIncrement> ldpIncrements) throws Exception {
        if(CollectionUtils.isEmpty(ldpIncrements)){
            return;
        }
        try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
            List<Row> rows = Lists.newArrayListWithCapacity(ldpIncrements.size());
            for (LdpIncrement ldpIncrement : ldpIncrements) {
                String rowKey = ldpIncrement.getKey();
                String column = ldpIncrement.getColumn();
                long step = ldpIncrement.getStep();
                long ttl = ldpIncrement.getTtl();
                Increment increment = new Increment(Bytes.toBytes(rowKey));
                increment.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), step);
                increment.setTTL(ttl);
                increment.setReturnResults(false);
                increment.setDurability(Durability.SYNC_WAL);
                rows.add(increment);
            }
            table.batch(rows,null);
        } catch (Exception ex) {
            logger.error("batch increment error,tableName:{}!",tableName,ex);
            throw ex;
        }
    }

    @Override
    public void put(String tableName, LdpPut ldpPut) throws Exception {
        Object value = ldpPut.getData();
        String rowKey = ldpPut.getKey();
        String column = ldpPut.getColumn();
        try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
            org.apache.hadoop.hbase.client.Put dbPut = new org.apache.hadoop.hbase.client.Put(Bytes.toBytes(rowKey));
            if (value.getClass() == String.class) {
                dbPut.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), Bytes.toBytes(value.toString()));
            } else if (value.getClass() == Long.class) {
                dbPut.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), Bytes.toBytes((long) value));
            } else if (value.getClass() == Integer.class) {
                dbPut.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), Bytes.toBytes(Long.parseLong(value.toString())));
            }
            dbPut.setDurability(Durability.SYNC_WAL);
            table.put(dbPut);
        }catch (Exception ex){
            logger.error("hbase put error,metaName:{}!",tableName,ex);
            throw ex;
        }
    }

    @Override
    public void puts(String tableName, List<LdpPut> ldpPuts) throws Exception {

    }

    @Override
    public Result get(String tableName, LdpGet ldpGet) throws Exception {
        return null;
    }

    @Override
    public List<Result> gets(String tableName, List<LdpGet> ldpGets) throws Exception {
        return null;
    }

    @Override
    public List<Result> scan(String tableName, String startRow, String endRow, int limit) throws Exception {
        return null;
    }

    @Override
    public void delete(String tableName, String key) throws Exception {
    }

    @Override
    public void maxPuts(String tableName, List<LdpPut> ldpPuts) throws Exception {
    }

    @Override
    public void minPuts(String tableName, List<LdpPut> ldpPuts) throws Exception {
    }
}
