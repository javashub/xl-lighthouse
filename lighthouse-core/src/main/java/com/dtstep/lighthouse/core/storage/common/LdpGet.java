package com.dtstep.lighthouse.core.storage.common;

import java.util.Objects;

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
public class LdpGet {

    private String key;

    private String column;

    public static LdpGet with(String key,String column){
        LdpGet ldpGet = new LdpGet();
        ldpGet.setKey(key);
        ldpGet.setColumn(column);
        return ldpGet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LdpGet myObject = (LdpGet) o;
        return Objects.equals(key, myObject.key) && Objects.equals(column, myObject.column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, column);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }
}
