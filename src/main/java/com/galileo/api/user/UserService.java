package com.galileo.api.user;

import com.galileo.api.common.api.PageResponse;
import com.galileo.api.common.exception.ConflictException;
import com.galileo.api.common.exception.ResourceNotFoundException;
import com.galileo.api.config.CacheNames;
import com.galileo.api.role.RoleEntity;
import com.galileo.api.role.RoleName;
import com.galileo.api.role.RoleRepository;
import com.galileo.api.team.TeamEntity;
import com.galileo.api.team.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeamService teamService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @CachePut(cacheNames = CacheNames.USERS, key = "#result.id()")
    public UserResponse create(UserCreateRequest request) {
        ensureEmailAvailable(request.email(), null);
        UserEntity user = new UserEntity(
                request.firstName(),
                request.lastName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.jobTitle(),
                resolveTeam(request.teamId()),
                resolveRoles(request.roles())
        );
        return UserResponse.from(userRepository.save(user));
    }

    public PageResponse<UserResponse> findAll(Long teamId, String query, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.strip();
        return PageResponse.from(userRepository.search(teamId, normalizedQuery, pageable)
                .map(UserResponse::from));
    }

    @Cacheable(cacheNames = CacheNames.USERS, key = "#id")
    public UserResponse findById(Long id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional
    @CachePut(cacheNames = CacheNames.USERS, key = "#id")
    public UserResponse update(Long id, UserUpdateRequest request) {
        UserEntity user = requireUser(id);
        ensureEmailAvailable(request.email(), id);
        user.updateProfile(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.jobTitle(),
                resolveTeam(request.teamId()),
                request.enabled()
        );
        return UserResponse.from(user);
    }

    @Transactional
    @CachePut(cacheNames = CacheNames.USERS, key = "#id")
    public UserResponse assignRoles(Long id, RoleAssignmentRequest request) {
        UserEntity user = requireUser(id);
        user.assignRoles(resolveRoles(request.roles()));
        return UserResponse.from(user);
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.USERS, key = "#id")
    public void changePassword(Long id, PasswordChangeRequest request) {
        UserEntity user = requireUser(id);
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.USERS, key = "#id")
    public void delete(Long id) {
        userRepository.delete(requireUser(id));
    }

    public UserEntity requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private TeamEntity resolveTeam(Long teamId) {
        return teamId == null ? null : teamService.requireTeam(teamId);
    }

    private Set<RoleEntity> resolveRoles(Set<RoleName> roleNames) {
        var roles = roleRepository.findAllByNameIn(roleNames);
        if (roles.size() != roleNames.size()) {
            Set<RoleName> found = roles.stream()
                    .map(RoleEntity::getName)
                    .collect(java.util.stream.Collectors.toSet());
            Set<RoleName> missing = new HashSet<>(roleNames);
            missing.removeAll(found);
            throw new IllegalArgumentException("Unknown roles: " + missing);
        }
        return new HashSet<>(roles);
    }

    private void ensureEmailAvailable(String email, Long excludedId) {
        boolean exists = excludedId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, excludedId);
        if (exists) {
            throw new ConflictException("Email already exists: " + email);
        }
    }
}
