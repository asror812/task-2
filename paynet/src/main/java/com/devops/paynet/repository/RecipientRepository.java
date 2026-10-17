package com.devops.paynet.repository;

import com.devops.paynet.domain.Recipient;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Recipient entity.
 */
@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Long> {
  @Query(
    value = "select recipient from Recipient recipient left join fetch recipient.category",
    countQuery = "select count(recipient) from Recipient recipient"
  )
  Page<Recipient> findAllWithEagerRelationships(Pageable pageable);

  @Query(
    "select recipient from Recipient recipient left join fetch recipient.category where recipient.id =:id"
  )
  Optional<Recipient> findOneWithEagerRelationships(@Param("id") Long id);

  @Query(
    "select distinct recipient from Recipient recipient" +
    " left join fetch recipient.category " +
    "left join fetch recipient.recipientFields " +
    "where recipient.id =:id"
  )
  Optional<Recipient> findOneWithRecipientFields(@Param("id") Long id);

  @Query(
    value = "select recipient from Recipient recipient left join fetch recipient.category where recipient.lastModifiedDate > :updatedSince",
    countQuery = "select count(recipient) from Recipient recipient where recipient.lastModifiedDate > :updatedSince"
  )
  Page<Recipient> findAllUpdatedSince(@Param("updatedSince") Instant updatedSince, Pageable pageable);

  @Query(
    "select distinct recipient from Recipient recipient " +
    "left join fetch recipient.category " +
    "left join fetch recipient.recipientFields field " +
    "left join field.values value"
  )
  List<Recipient> findCatalog();

  @Query(
    "select distinct recipient from Recipient recipient " +
    "left join fetch recipient.category " +
    "left join fetch recipient.recipientFields field " +
    "left join field.values value " +
    "where recipient.lastModifiedDate > :updatedSince " +
    "or field.lastModifiedDate > :updatedSince " +
    "or value.lastModifiedDate > :updatedSince"
  )
  List<Recipient> findCatalogUpdatedSince(@Param("updatedSince") Instant updatedSince);
}
