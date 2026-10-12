package com.devops.paylite.repository;

import com.devops.paylite.domain.PaynetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PaynetCategoryRepository extends JpaRepository<PaynetCategory, Long> {
    List<PaynetCategory> findAllByPaynetIdIn(Collection<Long> paynetIds);
}
