package com.dtstep.lighthouse.core.functions;
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
import com.dtstep.lighthouse.common.aggregator.EventPool;
import com.dtstep.lighthouse.common.aggregator.SlotsGroup;
import com.dtstep.lighthouse.common.constant.StatConst;
import com.dtstep.lighthouse.common.entity.event.LimitBucket;
import com.dtstep.lighthouse.core.storage.limit.LimitStorageSelector;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class LimitStorageThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(LimitStorageThread.class);

    private static final long _threadConsumePeriod = TimeUnit.SECONDS.toMillis(90);

    private static final long _maximumBacklogPeriod =  TimeUnit.MINUTES.toMillis(3);

    private final EventPool<LimitBucket> eventPool;

    private static final int batchSize = 20000;

    LimitStorageThread(EventPool<LimitBucket> eventPool){
        this.eventPool = eventPool;
    }

    @Override
    public void run() {
        IntStream.range(0, eventPool.slotSize()).forEach(this::consumer);
    }

    public void consumer(int slot) {
        try{
            SlotsGroup.SlotWrapper<LimitBucket> slotWrapper = eventPool.take(slot);
            while (slotWrapper.size() > batchSize * StatConst.backlog_factor
                    || System.currentTimeMillis() - slotWrapper.getLastAccessTime() > _threadConsumePeriod
                    || System.currentTimeMillis() - slotWrapper.getHeadElementTime() > _maximumBacklogPeriod
            ) {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                List<LimitBucket> events = slotWrapper.getEvents(batchSize);
                if(CollectionUtils.isEmpty(events)){
                    break;
                }
                LimitStorageSelector.limit(events);
                long cost = stopWatch.getTime();
                logger.info("process limit events,thread:{},slot:{},process size:{},remaining size:{},capacity:{},accessTime:{},cost:{}ms",
                        Thread.currentThread().getName(),slot,events.size(),slotWrapper.size(),slotWrapper.getCapacity(),slotWrapper.getLastAccessTime(),cost);
                if(cost > _threadConsumePeriod){
                    logger.warn("batch processing limit events takes too long, and may cause message delays" +
                            ",thread:{},slot:{},cost:{}",Thread.currentThread().getName(),slot,String.format("[%sms > %sms]",cost,_threadConsumePeriod));
                }
            }
        }catch (Exception ex){
            logger.error("process limit events error!",ex);
        }
    }
}
