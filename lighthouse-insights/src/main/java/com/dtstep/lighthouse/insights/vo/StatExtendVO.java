package com.dtstep.lighthouse.insights.vo;
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
import com.dtstep.lighthouse.common.modal.RenderConfig;
import com.dtstep.lighthouse.insights.dto.CommonlyFilterConfigParam;

import java.util.List;

public class StatExtendVO extends StatVO {

    private RenderConfig renderConfig;

    private List<CommonlyFilterConfigParam> commonlyFilters;

    public List<CommonlyFilterConfigParam> getCommonlyFilters() {
        return commonlyFilters;
    }

    public void setCommonlyFilters(List<CommonlyFilterConfigParam> commonlyFilters) {
        this.commonlyFilters = commonlyFilters;
    }

    public StatExtendVO(StatVO stat) {
        super(stat);
    }

    public RenderConfig getRenderConfig() {
        return renderConfig;
    }

    public void setRenderConfig(RenderConfig renderConfig) {
        this.renderConfig = renderConfig;
    }
}
