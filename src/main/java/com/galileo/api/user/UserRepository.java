package com.galileo.api.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByTeamId(Long teamId);

    @Query("""
            select u from UserEntity u
            where (:teamId is null or u.team.id = :teamId)
              and (:query is null
                   or lower(u.firstName) like lower(concat('%', :query, '%'))
                   or lower(u.lastName) like lower(concat('%', :query, '%'))
                   or lower(u.email) like lower(concat('%', :query, '%')))
            """)
    Page<UserEntity> search(
            @Param("teamId") Long teamId,
            @Param("query") String query,
            Pageable pageable);
}
