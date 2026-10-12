package com.devops.paynet.repository;

import com.devops.paynet.domain.Recipient;
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
  default Optional<Recipient> findOneWithEagerRelationships(Long id) {
    return this.findOneWithToOneRelationships(id);
  }

  default Optional<Recipient> findOneWithFields(Long id) {
    return this.findOneWithRecipientFields(id);
  }

  default List<Recipient> findAllWithEagerRelationships() {
    return this.findAllWithToOneRelationships();
  }

  default Page<Recipient> findAllWithEagerRelationships(Pageable pageable) {
    return this.findAllWithToOneRelationships(pageable);
  }

  @Query(
    value = "select recipient from Recipient recipient left join fetch recipient.category",
    countQuery = "select count(recipient) from Recipient recipient"
  )
  Page<Recipient> findAllWithToOneRelationships(Pageable pageable);

  @Query(
    "select recipient from Recipient recipient left join fetch recipient.category"
  )
  List<Recipient> findAllWithToOneRelationships();

  @Query(
    "select recipient from Recipient recipient left join fetch recipient.category where recipient.id =:id"
  )
  Optional<Recipient> findOneWithToOneRelationships(@Param("id") Long id);

  @Query(
    "select distinct recipient from Recipient recipient left join fetch recipient.category left join fetch recipient.recipientFields where recipient.id =:id"
  )
  Optional<Recipient> findOneWithRecipientFields(@Param("id") Long id);
}
