package com.galileo.api.team;

import com.galileo.api.common.api.PageResponse;
import com.galileo.api.common.exception.ConflictException;
import com.galileo.api.common.exception.ResourceNotFoundException;
import com.galileo.api.config.CacheNames;
import com.galileo.api.department.DepartmentEntity;
import com.galileo.api.department.DepartmentService;
import com.galileo.api.user.UserRepository;
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
public class TeamService {

    private final TeamRepository teamRepository;
    private final DepartmentService departmentService;
    private final UserRepository userRepository;

    @Transactional
    @CachePut(cacheNames = CacheNames.TEAMS, key = "#result.id()")
    public TeamResponse create(TeamRequest request) {
        ensureUnique(request, null);
        DepartmentEntity department = departmentService.requireDepartment(request.departmentId());
        TeamEntity team = new TeamEntity(
                request.code(), request.name(), request.description(), department);
        return TeamResponse.from(teamRepository.save(team));
    }

    public PageResponse<TeamResponse> findAll(Long departmentId, String query, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.strip();
        return PageResponse.from(teamRepository.search(departmentId, normalizedQuery, pageable)
                .map(TeamResponse::from));
    }

    @Cacheable(cacheNames = CacheNames.TEAMS, key = "#id")
    public TeamResponse findById(Long id) {
        return TeamResponse.from(requireTeam(id));
    }

    @Transactional
    @Caching(
            put = @CachePut(cacheNames = CacheNames.TEAMS, key = "#id"),
            evict = @CacheEvict(cacheNames = CacheNames.USERS, allEntries = true)
    )
    public TeamResponse update(Long id, TeamRequest request) {
        TeamEntity team = requireTeam(id);
        ensureUnique(request, id);
        DepartmentEntity department = departmentService.requireDepartment(request.departmentId());
        team.update(request.code(), request.name(), request.description(), department);
        return TeamResponse.from(team);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.TEAMS, key = "#id")
    public void delete(Long id) {
        TeamEntity team = requireTeam(id);
        if (userRepository.existsByTeamId(id)) {
            throw new ConflictException("Move or delete the team's users before deleting it");
        }
        teamRepository.delete(team);
    }

    public TeamEntity requireTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", id));
    }

    private void ensureUnique(TeamRequest request, Long excludedId) {
        boolean duplicateCode = excludedId == null
                ? teamRepository.existsByCodeIgnoreCase(request.code())
                : teamRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), excludedId);
        boolean duplicateName = excludedId == null
                ? teamRepository.existsByDepartmentIdAndNameIgnoreCase(
                        request.departmentId(), request.name())
                : teamRepository.existsByDepartmentIdAndNameIgnoreCaseAndIdNot(
                        request.departmentId(), request.name(), excludedId);
        if (duplicateCode) {
            throw new ConflictException("Team code already exists: " + request.code());
        }
        if (duplicateName) {
            throw new ConflictException("Team name already exists in this department: " + request.name());
        }
    }
}
