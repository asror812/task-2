package com.devops.paylite.repository;

import com.devops.paylite.domain.PaynetRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PaynetRecipientRepository extends JpaRepository<PaynetRecipient, Long> {
    List<PaynetRecipient> findAllByPaynetIdIn(Collection<Long> paynetIds);

    List<PaynetRecipient> findAllByActiveTrueOrderByNameAsc();
}
