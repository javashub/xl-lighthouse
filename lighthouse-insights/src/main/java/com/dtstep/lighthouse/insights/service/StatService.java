package com.dtstep.lighthouse.insights.service;

import com.dtstep.lighthouse.commonv2.insights.ListData;
import com.dtstep.lighthouse.insights.dto.StatDto;
import com.dtstep.lighthouse.insights.dto.StatQueryParam;
import com.dtstep.lighthouse.insights.modal.Stat;

import java.util.List;

public interface StatService {

    int create(Stat stat);

    ListData<StatDto> queryList(StatQueryParam queryParam, Integer pageNum, Integer pageSize);
}
