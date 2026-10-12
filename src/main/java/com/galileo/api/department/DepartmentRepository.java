package com.galileo.api.department;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            select d from DepartmentEntity d
            where lower(d.name) like lower(concat('%', :query, '%'))
               or lower(d.code) like lower(concat('%', :query, '%'))
            """)
    Page<DepartmentEntity> search(@Param("query") String query, Pageable pageable);
}
