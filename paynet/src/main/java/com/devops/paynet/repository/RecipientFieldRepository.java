package com.devops.paynet.repository;

import com.devops.paynet.domain.RecipientField;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RecipientField entity.
 */
@Repository
public interface RecipientFieldRepository
  extends JpaRepository<RecipientField, Long>
{
  @Query(
    value = "select recipientField from RecipientField recipientField left join fetch recipientField.recipient",
    countQuery = "select count(recipientField) from RecipientField recipientField"
  )
  Page<RecipientField> findAllWithEagerRelationships(Pageable pageable);

  @Query(
    "select recipientField from RecipientField recipientField left join fetch recipientField.recipient where recipientField.id =:id"
  )
  Optional<RecipientField> findOneWithEagerRelationships(@Param("id") Long id);

  @Query(
    value = "select recipientField from RecipientField recipientField " +
    "left join fetch recipientField.recipient recipient " +
    "left join fetch recipient.category " +
    "where recipientField.lastModifiedDate > :updatedSince",
    countQuery = "select count(recipientField) from RecipientField recipientField where recipientField.lastModifiedDate > :updatedSince"
  )
  Page<RecipientField> findAllUpdatedSince(@Param("updatedSince") Instant updatedSince, Pageable pageable);

  boolean existsByRecipient_IdAndActiveTrue(Long recipientId);
}
