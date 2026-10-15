package com.devops.paynet.repository;

import com.devops.paynet.domain.RecipientFieldValue;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipientFieldValueRepository extends JpaRepository<RecipientFieldValue, Long> {

    @EntityGraph(attributePaths = { "recipientField", "parent", "parent.recipientField" })
    Optional<RecipientFieldValue> findOneByRecipientField_IdAndValueAndActiveTrue(Long recipientFieldId, String value);

    @EntityGraph(attributePaths = { "recipientField", "parent" })
    List<RecipientFieldValue> findAllByRecipientField_IdAndParentIsNullAndActiveTrueOrderBySortOrderAsc(Long recipientFieldId);

    @EntityGraph(attributePaths = { "recipientField", "parent" })
    List<RecipientFieldValue> findAllByRecipientField_IdAndParent_IdAndActiveTrueOrderBySortOrderAsc(Long recipientFieldId, Long parentId);

    @EntityGraph(attributePaths = { "recipientField", "parent" })
    List<RecipientFieldValue> findAllByRecipientField_IdInAndLastModifiedDateAfterOrderByRecipientField_IdAscSortOrderAsc(
        List<Long> recipientFieldIds,
        Instant updatedSince
    );

    @EntityGraph(attributePaths = { "recipientField", "parent" })
    List<RecipientFieldValue> findAllByRecipientField_IdInOrderByRecipientField_IdAscSortOrderAsc(List<Long> recipientFieldIds);
}
