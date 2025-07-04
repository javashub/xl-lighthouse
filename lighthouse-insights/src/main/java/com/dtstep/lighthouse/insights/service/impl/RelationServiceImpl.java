package com.dtstep.lighthouse.insights.service.impl;
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
import com.dtstep.lighthouse.common.util.Md5Util;
import com.dtstep.lighthouse.common.entity.ListData;
import com.dtstep.lighthouse.common.entity.ResultCode;
import com.dtstep.lighthouse.insights.dao.RelationDao;
import com.dtstep.lighthouse.insights.dto.RelationDeleteParam;
import com.dtstep.lighthouse.insights.dto.RelationQueryParam;
import com.dtstep.lighthouse.common.enums.RelationTypeEnum;
import com.dtstep.lighthouse.insights.service.*;
import com.dtstep.lighthouse.insights.vo.ProjectVO;
import com.dtstep.lighthouse.insights.vo.RelationVO;
import com.dtstep.lighthouse.insights.vo.StatVO;
import com.dtstep.lighthouse.common.enums.ResourceTypeEnum;
import com.dtstep.lighthouse.common.modal.Relation;
import com.dtstep.lighthouse.insights.vo.ViewVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RelationServiceImpl implements RelationService {

    private static final Logger logger = LoggerFactory.getLogger(RelationServiceImpl.class);

    @Autowired
    private RelationDao relationDao;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private StatService statService;

    @Autowired
    private BaseService baseService;

    @Autowired
    private ViewService viewService;

    @Override
    public int batchCreate(List<Relation> relationList) {
        return relationDao.batchInsert(relationList);
    }

    @Override
    public ResultCode create(Relation relation) {
        String message = relation.getSubjectId() + "_" + relation.getRelationType().getRelationType()
                + "_" + relation.getResourceType().getResourceType() + "_" + relation.getResourceId();
        String hash = Md5Util.getMD5(message);
        relation.setHash(hash);
        LocalDateTime localDateTime = LocalDateTime.now();
        int result;
        if(isExist(hash)){
            relation.setUpdateTime(localDateTime);
            result = relationDao.update(relation);
        }else{
            relation.setHash(hash);
            relation.setCreateTime(localDateTime);
            relation.setUpdateTime(localDateTime);
            result = relationDao.insert(relation);
        }
        if(result > 0){
            return ResultCode.success;
        }else{
            return ResultCode.systemError;
        }
    }

    @Override
    public int delete(RelationDeleteParam deleteParam){
        return relationDao.delete(deleteParam);
    }

    @Override
    public int deleteById(Integer id) {
        return relationDao.deleteById(id);
    }

    @Override
    public boolean isExist(String hash) {
        return relationDao.isExist(hash);
    }

    private RelationVO translate(Relation relation) throws Exception{
        RelationVO relationVO = new RelationVO(relation);
        if(relation.getResourceType() == ResourceTypeEnum.Project){
            ProjectVO projectVO = projectService.queryById(relation.getResourceId());
            relationVO.setExtend(projectVO);
        }else if(relation.getResourceType() == ResourceTypeEnum.Stat){
            StatVO statVO = statService.queryById(relation.getResourceId());
            relationVO.setExtend(statVO);
        }
        return relationVO;
    }

    @Override
    public List<RelationVO> queryList(Integer relationId, RelationTypeEnum relationTypeEnum) throws Exception{
        List<Relation> relationList = relationDao.queryList(relationId,relationTypeEnum);
        List<Integer> statIdList = new ArrayList<>();
        List<Integer> projectIdList = new ArrayList<>();
        for(Relation relation : relationList){
            if(relation.getResourceType() == ResourceTypeEnum.Project){
                projectIdList.add(relation.getResourceId());
            }else if(relation.getResourceType() == ResourceTypeEnum.Stat){
                statIdList.add(relation.getResourceId());
            }
        }
        List<StatVO> statList = statService.queryByIds(statIdList);
        List<ProjectVO> projectList = projectService.queryByIds(projectIdList);
        List<RelationVO> voList = new ArrayList<>();
        for(Relation relation : relationList){
            RelationVO relationVO = new RelationVO(relation);
            if(relation.getResourceType() == ResourceTypeEnum.Project){
                ProjectVO project = projectList.stream().filter(x -> x.getId().intValue() == relation.getResourceId().intValue()).findFirst().orElse(null);
                relationVO.setExtend(project);
            }else if(relation.getResourceType() == ResourceTypeEnum.Stat){
                StatVO statVO = statList.stream().filter(x -> x.getId().intValue() == relation.getResourceId().intValue()).findFirst().orElse(null);
                relationVO.setExtend(statVO);
            }
            voList.add(relationVO);
        }
        return voList;
    }

    @Override
    public ListData<RelationVO> queryList(RelationQueryParam queryParam, Integer pageNum, Integer pageSize) throws Exception{
        PageHelper.startPage(pageNum,pageSize);
        PageInfo<Relation> pageInfo;
        try{
            List<Relation> relationList = relationDao.queryListByPage(queryParam);
            pageInfo = new PageInfo<>(relationList);
        }finally {
            PageHelper.clearPage();
        }
        List<RelationVO> voList = new ArrayList<>();
        List<Integer> statIdList = new ArrayList<>();
        List<Integer> projectIdList = new ArrayList<>();
        List<Integer> viewIdList = new ArrayList<>();
        for(Relation relation : pageInfo.getList()){
            if(relation.getResourceType() == ResourceTypeEnum.Project){
                projectIdList.add(relation.getResourceId());
            }else if(relation.getResourceType() == ResourceTypeEnum.Stat){
                statIdList.add(relation.getResourceId());
            }else if(relation.getResourceType() == ResourceTypeEnum.View){
                viewIdList.add(relation.getResourceId());
            }
        }
        List<StatVO> statList = statService.queryByIds(statIdList);
        List<ProjectVO> projectList = projectService.queryByIds(projectIdList);
        List<ViewVO> viewList = viewService.queryByIds(viewIdList);
        for(Relation relation : pageInfo.getList()){
            RelationVO relationVO = new RelationVO(relation);
            if(relation.getResourceType() == ResourceTypeEnum.Project){
                ProjectVO project = projectList.stream().filter(x -> x.getId().intValue() == relation.getResourceId().intValue()).findFirst().orElse(null);
                relationVO.setExtend(project);
            }else if(relation.getResourceType() == ResourceTypeEnum.Stat){
                StatVO statVO = statList.stream().filter(x -> x.getId().intValue() == relation.getResourceId().intValue()).findFirst().orElse(null);
                relationVO.setExtend(statVO);
            }else if(relation.getResourceType() == ResourceTypeEnum.View){
                ViewVO viewVO = viewList.stream().filter(x -> x.getId().intValue() == relation.getResourceId().intValue()).findFirst().orElse(null);
                relationVO.setExtend(viewVO);
            }
            voList.add(relationVO);
        }
        return ListData.newInstance(voList,pageInfo.getTotal(),pageNum,pageSize);
    }

    @Override
    public int count(RelationQueryParam queryParam) {
        return relationDao.count(queryParam);
    }

    @Override
    public Relation queryRelationByHash(String hash) {
        return relationDao.queryRelationByHash(hash);
    }
}
