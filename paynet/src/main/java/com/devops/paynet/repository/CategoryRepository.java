package com.devops.paynet.repository;

import com.devops.paynet.domain.Category;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Category entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Page<Category> findAllByLastModifiedDateAfter(Instant updatedSince, Pageable pageable);
}
