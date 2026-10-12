package com.galileo.api.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

    boolean existsByDepartmentId(Long departmentId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByDepartmentIdAndNameIgnoreCase(Long departmentId, String name);

    boolean existsByDepartmentIdAndNameIgnoreCaseAndIdNot(Long departmentId, String name, Long id);

    @Query("""
            select t from TeamEntity t
            join fetch t.department d
            where (:departmentId is null or d.id = :departmentId)
              and (:query is null
                   or lower(t.name) like lower(concat('%', :query, '%'))
                   or lower(t.code) like lower(concat('%', :query, '%')))
            """)
    Page<TeamEntity> search(
            @Param("departmentId") Long departmentId,
            @Param("query") String query,
            Pageable pageable);
}
