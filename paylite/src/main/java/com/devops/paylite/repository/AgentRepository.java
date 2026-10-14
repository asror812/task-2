package com.devops.paylite.repository;

import com.devops.paylite.domain.Agent;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Agent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByLogin(String login);

    Page<Agent> findAllByOrderByLoginAsc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select agent from Agent agent where agent.login = :login")
    Optional<Agent> findByLoginForUpdate(@Param("login") String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select agent from Agent agent where agent.id = :id")
    Optional<Agent> findByIdForUpdate(@Param("id") Long id);
}
