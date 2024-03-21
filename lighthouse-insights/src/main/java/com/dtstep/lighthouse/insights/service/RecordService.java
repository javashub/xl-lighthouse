package com.dtstep.lighthouse.insights.service;

import com.dtstep.lighthouse.common.entity.ListData;
import com.dtstep.lighthouse.insights.dto.RecordQueryParam;
import com.dtstep.lighthouse.common.modal.Record;

public interface RecordService {

    int create(Record record);

    ListData<Record> queryList(RecordQueryParam queryParam,Integer pageNum,Integer pageSize);

    ListData<Record> queryStatLimitList(Integer statId,Integer pageNum,Integer pageSize);
}
