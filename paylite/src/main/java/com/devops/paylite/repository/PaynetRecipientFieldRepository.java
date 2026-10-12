package com.devops.paylite.repository;

import com.devops.paylite.domain.PaynetRecipientField;
import com.devops.paylite.domain.enumeration.RecipientFieldMethod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaynetRecipientFieldRepository extends JpaRepository<PaynetRecipientField, Long> {
    List<PaynetRecipientField> findAllByRecipientPaynetIdAndActiveTrueAndMethodOrderBySortOrderAscIdAsc(
        Long recipientPaynetId,
        RecipientFieldMethod method
    );
}
