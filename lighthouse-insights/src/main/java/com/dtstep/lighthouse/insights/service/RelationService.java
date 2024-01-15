package com.dtstep.lighthouse.insights.service;

import com.dtstep.lighthouse.insights.enums.RelationTypeEnum;
import com.dtstep.lighthouse.insights.modal.Relation;

import java.util.List;

public interface RelationService {

    int batchCreate(List<Relation> relationList);

    boolean isExist(String hash);

    List<Relation> queryList(Integer relationId, RelationTypeEnum relationTypeEnum);
}
