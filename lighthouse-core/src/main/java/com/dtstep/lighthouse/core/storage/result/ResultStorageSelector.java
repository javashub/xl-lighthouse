package com.dtstep.lighthouse.core.storage.result;
/*
 * Copyright (C) 2022-2024 XueLing.雪灵
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
import com.dtstep.lighthouse.common.entity.calculate.MicroBucket;
import com.dtstep.lighthouse.common.entity.stat.StatExtEntity;
import com.dtstep.lighthouse.common.entity.view.StatValue;
import com.dtstep.lighthouse.core.storage.result.impl.DefaultResultStorageHandler;

import java.util.LinkedHashMap;
import java.util.List;

public class ResultStorageSelector {

    private static final ResultStorageHandler<MicroBucket,StatValue> resultStorageHandler = new DefaultResultStorageHandler();

    public static void increment(List<MicroBucket> bucketList) throws Exception{
        resultStorageHandler.increment(bucketList);
    }

    public static void maxPut(List<MicroBucket> bucketList) throws Exception{
        resultStorageHandler.maxPut(bucketList);
    }

    public static void minPut(List<MicroBucket> bucketList) throws Exception{
        resultStorageHandler.minPut(bucketList);
    }

    public static void put(List<MicroBucket> bucketList) throws Exception {
        resultStorageHandler.put(bucketList);
    }

    public static LinkedHashMap<String,StatValue> queryWithDimensList(StatExtEntity statExtEntity, List<String> dimensValueList, long batchTime) throws Exception{
//        return resultStorageEngine.queryWithDimensList(statExtEntity,dimensValueList,batchTime);
        return null;
    }
}
