package com.galileo.api.role;

import com.galileo.api.config.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;

    @Cacheable(cacheNames = CacheNames.ROLES, key = "'catalogue'")
    public List<RoleResponse> findAll() {
        return new ArrayList<>(roleRepository.findAll().stream()
                .map(RoleResponse::from)
                .toList());
    }
}
