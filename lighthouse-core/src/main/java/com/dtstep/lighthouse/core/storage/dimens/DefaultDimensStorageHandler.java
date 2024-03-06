package com.dtstep.lighthouse.core.storage.dimens;

import com.dtstep.lighthouse.common.constant.StatConst;
import com.dtstep.lighthouse.common.constant.SysConst;
import com.dtstep.lighthouse.common.entity.event.DimensBucket;
import com.dtstep.lighthouse.common.hash.HashUtil;
import com.dtstep.lighthouse.common.modal.Group;
import com.dtstep.lighthouse.common.util.Md5Util;
import com.dtstep.lighthouse.common.util.StringUtil;
import com.dtstep.lighthouse.core.hbase.HBaseClient;
import com.dtstep.lighthouse.core.rowkey.KeyGenerator;
import com.dtstep.lighthouse.core.rowkey.impl.DefaultKeyGenerator;
import com.dtstep.lighthouse.core.storage.LdpPut;
import com.dtstep.lighthouse.core.storage.LdpResult;
import com.dtstep.lighthouse.core.storage.engine.StorageEngineProxy;
import com.dtstep.lighthouse.core.wrapper.DimensDBWrapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.javatuples.Quartet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DefaultDimensStorageHandler implements DimensStorageHandler<DimensBucket, String> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDimensStorageHandler.class);

    private final KeyGenerator keyGenerator = new DefaultKeyGenerator();

    @Override
    public void put(List<DimensBucket> list) throws Exception {
        if(CollectionUtils.isEmpty(list)){
            return;
        }
        List<LdpPut> putList = new ArrayList<>();
        for(DimensBucket quartet : list){
            String rowKey = keyGenerator.dimensKey(quartet.getGroup(),quartet.getDimens(),quartet.getDimensValue());
            if(logger.isTraceEnabled()){
                logger.trace("save dimens,token:{},dimens:{},dimensValue:{},rowKey:{}",quartet.getGroup().getToken(),quartet.getDimens(),quartet.getDimensValue(),rowKey);
            }
            LdpPut ldpPut = LdpPut.with(rowKey,"d",quartet.getDimensValue(),quartet.getTtl());
            putList.add(ldpPut);
        }
        StorageEngineProxy.getInstance().puts(StatConst.DIMENS_STORAGE_TABLE,putList);
    }

    @Override
    public List<String> queryDimensList(Group group, String dimens, String lastDimensValue, int limit) throws Exception {
        int startIndex = Math.abs((int) (HashUtil.BKDRHash(group.getRandomId() + "_" + dimens) % SysConst._DBKeyPrefixArray.length));
        List<String> dimensList = new ArrayList<>();
        String startRow = null;
        if(!StringUtil.isEmpty(lastDimensValue)){
            startRow = keyGenerator.dimensKey(group,dimens,lastDimensValue);
        }
        String finalStartRow = startRow;
        for(int part = 0;part <SysConst._DIMENS_STORAGE_PRE_PARTITIONS_SIZE;part ++){
            int current = startIndex + part ;
            while (current >= SysConst._DBKeyPrefixArray.length){
                current = current - SysConst._DBKeyPrefixArray.length;
            }
            if(dimensList.size() >= limit){
                break;
            }
            String prefix = SysConst._DBKeyPrefixArray[current];
            String partStartRow = prefix + Md5Util.getMD5(group.getRandomId() + "_" + dimens) + ".";
            String partEndRow = prefix + Md5Util.getMD5(group.getRandomId() + "_" + dimens) + "|";
            if(finalStartRow != null && finalStartRow.compareTo(partEndRow) < 0){
                continue;
            }else if(finalStartRow != null && finalStartRow.compareTo(partStartRow) > 0){
                partStartRow = finalStartRow;
            }
            try{
                while (true){
                    List<LdpResult<String>> dbResults = StorageEngineProxy.getInstance().scan(StatConst.DIMENS_STORAGE_TABLE,partStartRow,partEndRow,(limit - dimensList.size()),String.class);
                    if(CollectionUtils.isEmpty(dbResults)){
                        break;
                    }
                    for(LdpResult<String> ldpResult : dbResults){
                        String dimensValue = ldpResult.getData();
                        if(StringUtil.isNotEmpty(dimensValue) && dimensList.size() < limit){
                            partStartRow = ldpResult.getKey();
                            dimensList.add(dimensValue);
                        }
                    }
                    if(dimensList.size() >= limit){
                        break;
                    }
                }
            }catch (Exception ex){
                logger.error("load dimens error!",ex);
            }
        }
        return dimensList;
    }
}
