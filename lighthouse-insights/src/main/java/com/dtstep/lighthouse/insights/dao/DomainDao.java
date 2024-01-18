package com.dtstep.lighthouse.insights.dao;

import com.dtstep.lighthouse.insights.modal.Domain;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainDao {

    int insert(Domain domain);

    Domain queryById(Integer id);

    Domain queryDefault();
}