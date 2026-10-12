package com.devops.paynet.repository;

import com.devops.paynet.domain.RecipientField;
import java.util.List;
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
  default Optional<RecipientField> findOneWithEagerRelationships(Long id) {
    return this.findOneWithToOneRelationships(id);
  }

  default List<RecipientField> findAllWithEagerRelationships() {
    return this.findAllWithToOneRelationships();
  }

  default Page<RecipientField> findAllWithEagerRelationships(
    Pageable pageable
  ) {
    return this.findAllWithToOneRelationships(pageable);
  }

  @Query(
    value = "select recipientField from RecipientField recipientField left join fetch recipientField.recipient",
    countQuery = "select count(recipientField) from RecipientField recipientField"
  )
  Page<RecipientField> findAllWithToOneRelationships(Pageable pageable);

  @Query(
    "select recipientField from RecipientField recipientField left join fetch recipientField.recipient"
  )
  List<RecipientField> findAllWithToOneRelationships();

  @Query(
    "select recipientField from RecipientField recipientField left join fetch recipientField.recipient where recipientField.id =:id"
  )
  Optional<RecipientField> findOneWithToOneRelationships(@Param("id") Long id);
}
