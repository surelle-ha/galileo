package com.galileo.api.department;

import com.galileo.api.common.api.PageResponse;
import com.galileo.api.common.exception.ConflictException;
import com.galileo.api.common.exception.ResourceNotFoundException;
import com.galileo.api.config.CacheNames;
import com.galileo.api.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;

    @Transactional
    @CachePut(cacheNames = CacheNames.DEPARTMENTS, key = "#result.id()")
    public DepartmentResponse create(DepartmentRequest request) {
        ensureUnique(request, null);
        DepartmentEntity saved = departmentRepository.save(new DepartmentEntity(
                request.code(), request.name(), request.description()));
        return DepartmentResponse.from(saved);
    }

    public PageResponse<DepartmentResponse> findAll(String query, Pageable pageable) {
        var departments = query == null || query.isBlank()
                ? departmentRepository.findAll(pageable)
                : departmentRepository.search(query.strip(), pageable);
        return PageResponse.from(departments.map(DepartmentResponse::from));
    }

    @Cacheable(cacheNames = CacheNames.DEPARTMENTS, key = "#id")
    public DepartmentResponse findById(Long id) {
        return DepartmentResponse.from(requireDepartment(id));
    }

    @Transactional
    @Caching(
            put = @CachePut(cacheNames = CacheNames.DEPARTMENTS, key = "#id"),
            evict = {
                    @CacheEvict(cacheNames = CacheNames.TEAMS, allEntries = true),
                    @CacheEvict(cacheNames = CacheNames.USERS, allEntries = true)
            }
    )
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        DepartmentEntity department = requireDepartment(id);
        ensureUnique(request, id);
        department.update(request.code(), request.name(), request.description());
        return DepartmentResponse.from(department);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.DEPARTMENTS, key = "#id")
    public void delete(Long id) {
        DepartmentEntity department = requireDepartment(id);
        if (teamRepository.existsByDepartmentId(id)) {
            throw new ConflictException("Move or delete the department's teams before deleting it");
        }
        departmentRepository.delete(department);
    }

    public DepartmentEntity requireDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    private void ensureUnique(DepartmentRequest request, Long excludedId) {
        boolean duplicateCode = excludedId == null
                ? departmentRepository.existsByCodeIgnoreCase(request.code())
                : departmentRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), excludedId);
        boolean duplicateName = excludedId == null
                ? departmentRepository.existsByNameIgnoreCase(request.name())
                : departmentRepository.existsByNameIgnoreCaseAndIdNot(request.name(), excludedId);
        if (duplicateCode) {
            throw new ConflictException("Department code already exists: " + request.code());
        }
        if (duplicateName) {
            throw new ConflictException("Department name already exists: " + request.name());
        }
    }
}
