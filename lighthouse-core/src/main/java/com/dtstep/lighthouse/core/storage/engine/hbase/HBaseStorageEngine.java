package com.dtstep.lighthouse.core.storage.engine.hbase;

import com.dtstep.lighthouse.common.constant.SysConst;
import com.dtstep.lighthouse.common.hash.HashUtil;
import com.dtstep.lighthouse.common.util.StringUtil;
import com.dtstep.lighthouse.core.config.LDPConfig;
import com.dtstep.lighthouse.core.lock.RedLock;
import com.dtstep.lighthouse.core.storage.*;
import com.dtstep.lighthouse.core.storage.CompareOperator;
import com.dtstep.lighthouse.core.storage.engine.StorageEngine;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.compress.Compression;
import org.apache.hadoop.hbase.io.encoding.DataBlockEncoding;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
            long ttl = ldpIncrement.getTtl();
            Validate.isTrue(ttl != 0);
            increment.setTTL(ttl);
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
                Validate.isTrue(ttl != 0);
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
                dbPut.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), Bytes.toBytes((Long) value));
            } else {
                throw new IllegalArgumentException(String.format("Current type(%s) not supported!",value.getClass()));
            }
            long ttl = ldpPut.getTtl();
            Validate.isTrue(ttl != 0);
            dbPut.setTTL(ttl);
            dbPut.setDurability(Durability.SYNC_WAL);
            table.put(dbPut);
        }catch (Exception ex){
            logger.error("hbase put error,metaName:{}!",tableName,ex);
            throw ex;
        }
    }

    @Override
    public void puts(String tableName, List<LdpPut> ldpPuts) throws Exception {
        if(CollectionUtils.isEmpty(ldpPuts)){
            return;
        }
        try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
            List<Put> puts = Lists.newArrayListWithCapacity(ldpPuts.size());
            for (LdpPut ldpPut : ldpPuts) {
                String rowKey = ldpPut.getKey();
                String column = ldpPut.getColumn();
                Object value = ldpPut.getData();
                long ttl = ldpPut.getTtl();
                Validate.isTrue(ttl != 0);
                Put put = new Put(Bytes.toBytes(rowKey));
                put.setDurability(Durability.SYNC_WAL);
                if (value.getClass() == String.class) {
                    put.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), Bytes.toBytes(value.toString()));
                } else if (value.getClass() == Long.class) {
                    put.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), Bytes.toBytes((Long) value));
                } else {
                    throw new IllegalArgumentException(String.format("Current type(%s) not supported!",value.getClass()));
                }
                put.setTTL(ttl);
                puts.add(put);
            }
            table.put(puts);
        } catch (Exception ex) {
            logger.error("hbase batch put error,tableName:{}!",tableName,ex);
            throw ex;
        }
    }

    @Override
    public <R> LdpResult<R> get(String tableName, LdpGet ldpGet, Class<R> clazz) throws Exception {
        String rowKey = ldpGet.getKey();
        String column = ldpGet.getColumn();
        Result dbResult;
        try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            dbResult = table.get(get);
        } catch (Exception ex) {
            logger.error("hbase get error!",ex);
            throw ex;
        }
        Cell cell = dbResult.getColumnLatestCell(Bytes.toBytes("f"),Bytes.toBytes(column));
        LdpResult<R> ldpResult = new LdpResult<>();
        if(cell != null){
            byte[] b = CellUtil.cloneValue(cell);
            long timestamp = cell.getTimestamp();
            R data = null;
            if(clazz == Long.class || clazz == long.class){
                data = clazz.cast(Bytes.toLong(b));
            }else if(clazz == String.class){
                data = clazz.cast(Bytes.toString(b));
            }else if(clazz == Integer.class || clazz == int.class){
                data = clazz.cast(Bytes.toInt(b));
            }else if(clazz == Double.class || clazz == double.class){
                data = clazz.cast(Bytes.toDouble(b));
            }else if(clazz == Float.class || clazz == float.class){
                data = clazz.cast(Bytes.toFloat(b));
            }else if(clazz == Boolean.class || clazz == boolean.class){
                data = clazz.cast(Bytes.toBoolean(b));
            }
            ldpResult.setData(data);
            ldpResult.setTimestamp(timestamp);
        }
        ldpResult.setKey(rowKey);
        ldpResult.setColumn(column);
        return ldpResult;
    }

    @Override
    public <R> List<LdpResult<R>> gets(String tableName, List<LdpGet> ldpGets, Class<R> clazz) throws Exception {
        List<Get> getList = new ArrayList<>();
        for(LdpGet ldpGet : ldpGets){
            String rowKey = ldpGet.getKey();
            String column = ldpGet.getColumn();
            Get get = new Get(Bytes.toBytes(rowKey));
            get.addColumn(Bytes.toBytes("f"),Bytes.toBytes(column));
            getList.add(get);
        }
        Result[] dbResults;
        try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
            dbResults = table.get(getList);
        } catch (Exception ex) {
            logger.error("hbase get error!",ex);
            throw ex;
        }
        List<LdpResult<R>> resultList = new ArrayList<>();
        for(int i=0;i<dbResults.length;i++){
            String column = ldpGets.get(i).getColumn();
            String key = ldpGets.get(i).getKey();
            Result dbResult = dbResults[i];
            LdpResult<R> ldpResult;
            if(dbResult != null){
                ldpResult = new LdpResult<>();
                Cell cell = dbResult.getColumnLatestCell(Bytes.toBytes("f"),Bytes.toBytes(column));
                if(cell != null){
                    byte[] b = CellUtil.cloneValue(cell);
                    long timestamp = cell.getTimestamp();
                    R data = null;
                    if(clazz == Long.class || clazz == long.class){
                        data = clazz.cast(Bytes.toLong(b));
                    }else if(clazz == String.class){
                        data = clazz.cast(Bytes.toString(b));
                    }else if(clazz == Integer.class || clazz == int.class){
                        data = clazz.cast(Bytes.toInt(b));
                    }else if(clazz == Double.class || clazz == double.class){
                        data = clazz.cast(Bytes.toDouble(b));
                    }else if(clazz == Float.class || clazz == float.class){
                        data = clazz.cast(Bytes.toFloat(b));
                    }else if(clazz == Boolean.class || clazz == boolean.class){
                        data = clazz.cast(Bytes.toBoolean(b));
                    }
                    ldpResult.setData(data);
                    ldpResult.setTimestamp(timestamp);
                }
                ldpResult.setKey(key);
                ldpResult.setColumn(column);
                resultList.add(ldpResult);
            }
        }
        return resultList;
    }

    @Override
    public <R> List<LdpResult<R>> scan(String tableName, String startRow, String endRow,String column, int limit,Class<R> clazz) throws Exception {
        List<LdpResult<R>> resultList = new ArrayList<>();
        try(Table table = getConnection().getTable(TableName.valueOf(tableName))){
            Scan scan = new Scan();
            scan.setStartRow(Bytes.toBytes(startRow + "."));
            scan.setStopRow(Bytes.toBytes(endRow  + "|"));
            scan.setMaxResultSize(limit);
            scan.setCaching(20);
            scan.setBatch(100);
            try (ResultScanner scanner = table.getScanner(scan)) {
                int count = 0;
                for (Result dbResult = scanner.next(); dbResult != null; dbResult = scanner.next()) {
                    String rowKey = Bytes.toString(dbResult.getRow());
                    LdpResult<R> ldpResult = new LdpResult<>();
                    Cell cell = dbResult.getColumnLatestCell(Bytes.toBytes("f"),Bytes.toBytes(column));
                    if(cell != null){
                        byte[] b = CellUtil.cloneValue(cell);
                        long timestamp = cell.getTimestamp();
                        R data = null;
                        if(clazz == Long.class || clazz == long.class){
                            data = clazz.cast(Bytes.toLong(b));
                        }else if(clazz == String.class){
                            data = clazz.cast(Bytes.toString(b));
                        }else if(clazz == Integer.class || clazz == int.class){
                            data = clazz.cast(Bytes.toInt(b));
                        }else if(clazz == Double.class || clazz == double.class){
                            data = clazz.cast(Bytes.toDouble(b));
                        }else if(clazz == Float.class || clazz == float.class){
                            data = clazz.cast(Bytes.toFloat(b));
                        }else if(clazz == Boolean.class || clazz == boolean.class){
                            data = clazz.cast(Bytes.toBoolean(b));
                        }
                        ldpResult.setData(data);
                        ldpResult.setTimestamp(timestamp);
                    }
                    ldpResult.setKey(rowKey);
                    count++;
                    if (limit != -1 && count > limit) {
                        break;
                    }
                    resultList.add(ldpResult);
                }
            } catch (Exception ex) {
                logger.error("hbase scan error!",ex);
            }
        }catch (Exception ex){
            logger.error("hbase scan error!",ex);
            throw ex;
        }
        return resultList;
    }

    @Override
    public void delete(String tableName, String key) throws Exception {
        try(Table table = getConnection().getTable(TableName.valueOf(tableName))){
            Delete delete = new Delete(Bytes.toBytes(key));
            table.delete(delete);
        }catch (Exception ex){
            logger.error("delete table {} data error!",tableName,ex);
            throw ex;
        }
    }

    private static final int batchSalt = 4;

    private static final String COMPARE_PUT_LOCK_PREFIX = "COMPARE_PUT_LOCK";

    @Override
    public void putsWithCompare(String tableName, CompareOperator compareOperator, List<LdpPut> ldpPuts) throws Exception {
        if(CollectionUtils.isEmpty(ldpPuts)){
            return;
        }
        Map<Long,List<LdpPut>> map = ldpPuts.stream().collect(Collectors.groupingBy(x -> HashUtil.BKDRHash(x.getKey() + "_" + x.getColumn()) % batchSalt));
        for(Long object : map.keySet()){
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            String lockKey = COMPARE_PUT_LOCK_PREFIX + "_" + compareOperator + "_" + object;
            boolean isLock = RedLock.tryLock(lockKey,8,3, TimeUnit.MINUTES);
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
                        dbValueMap = dbResults.stream().filter(x -> x.getData() != null).collect(Collectors.toMap(x -> x.getKey() + ";" + x.getColumn(), LdpResult::getData));
                    }
                    List<Put> puts = Lists.newArrayList();
                    for(LdpPut ldpPut : subList){
                        String rowKey = ldpPut.getKey();
                        String column = ldpPut.getColumn();
                        Object value = ldpPut.getData();
                        long ttl = ldpPut.getTtl();
                        Validate.isTrue(ttl != 0);
                        String aggregateKey = rowKey + ";" + column;
                        if(compareOperator == CompareOperator.GREATER){
                            if(MapUtils.isEmpty(dbValueMap) || !dbValueMap.containsKey(aggregateKey) || (Long)ldpPut.getData() > dbValueMap.get(aggregateKey)){
                                Put put = new Put(Bytes.toBytes(rowKey));
                                if (value.getClass() == String.class) {
                                    put.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), Bytes.toBytes(value.toString()));
                                } else if (value.getClass() == Long.class) {
                                    put.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), Bytes.toBytes((Long) value));
                                } else {
                                    throw new IllegalArgumentException(String.format("Current type(%s) not supported!",value.getClass()));
                                }
                                put.setTTL(ttl);
                                put.setDurability(Durability.SYNC_WAL);
                                puts.add(put);
                            }
                        }else{
                            if(MapUtils.isEmpty(dbValueMap) || !dbValueMap.containsKey(aggregateKey) || (Long)ldpPut.getData() < dbValueMap.get(aggregateKey)){
                                Put put = new Put(Bytes.toBytes(rowKey));
                                if (value.getClass() == String.class) {
                                    put.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), Bytes.toBytes(value.toString()));
                                } else if (value.getClass() == Long.class) {
                                    put.addColumn(Bytes.toBytes("f"), Bytes.toBytes(column), Bytes.toBytes((Long) value));
                                } else {
                                    throw new IllegalArgumentException(String.format("Current type(%s) not supported!",value.getClass()));
                                }
                                put.setTTL(ttl);
                                put.setDurability(Durability.SYNC_WAL);
                                puts.add(put);
                            }
                        }
                    }
                    try (Table table = getConnection().getTable(TableName.valueOf(tableName))) {
                        table.put(puts);
                    }catch (Exception ex) {
                        logger.error("execute batch put error,tableName:{}!",tableName,ex);
                        throw ex;
                    }
                }catch (Exception ex){
                    logger.error("batch put error!",ex);
                }finally {
                    RedLock.unLock(lockKey);
                }
            }else{
                logger.error("try lock failed,thread unable to acquire lock,this batch data may be lost,cost:{}ms!",stopWatch.getTime());
            }
        }
    }
}
