package com.devops.paylite.repository;

import com.devops.paylite.domain.PaynetRecipientFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaynetRecipientFieldValueRepository extends JpaRepository<PaynetRecipientFieldValue, Long> {
}
