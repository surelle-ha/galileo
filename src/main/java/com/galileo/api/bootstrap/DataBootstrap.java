package com.galileo.api.bootstrap;

import com.galileo.api.config.BootstrapProperties;
import com.galileo.api.role.Permission;
import com.galileo.api.role.RoleEntity;
import com.galileo.api.role.RoleName;
import com.galileo.api.role.RoleRepository;
import com.galileo.api.user.UserEntity;
import com.galileo.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataBootstrap implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        Map<RoleName, RoleDefinition> definitions = definitions();
        definitions.forEach((name, definition) -> {
            RoleEntity role = roleRepository.findByName(name)
                    .orElseGet(() -> new RoleEntity(name, definition.description(), definition.permissions()));
            role.synchronize(definition.description(), definition.permissions());
            roleRepository.save(role);
        });

        if (userRepository.findByEmailIgnoreCase(properties.adminEmail()).isEmpty()) {
            RoleEntity adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
            UserEntity admin = new UserEntity(
                    "Galileo",
                    "Administrator",
                    properties.adminEmail(),
                    passwordEncoder.encode(properties.adminPassword()),
                    "Platform Administrator",
                    null,
                    Set.of(adminRole)
            );
            userRepository.save(admin);
            log.warn("Bootstrap administrator created for {}. Change its password before shared use.",
                    properties.adminEmail());
        }
    }

    private Map<RoleName, RoleDefinition> definitions() {
        Map<RoleName, RoleDefinition> roles = new LinkedHashMap<>();
        roles.put(RoleName.ADMIN, new RoleDefinition(
                "Full platform administration",
                EnumSet.allOf(Permission.class)));
        roles.put(RoleName.DEPARTMENT_MANAGER, new RoleDefinition(
                "Manage departments, teams, and user placement",
                EnumSet.of(
                        Permission.DEPARTMENT_READ,
                        Permission.DEPARTMENT_WRITE,
                        Permission.TEAM_READ,
                        Permission.TEAM_WRITE,
                        Permission.USER_READ,
                        Permission.USER_WRITE,
                        Permission.ROLE_READ)));
        roles.put(RoleName.TEAM_LEAD, new RoleDefinition(
                "Read the organization and user directory",
                EnumSet.of(
                        Permission.DEPARTMENT_READ,
                        Permission.TEAM_READ,
                        Permission.USER_READ)));
        roles.put(RoleName.USER, new RoleDefinition(
                "Read organization and user directory data",
                EnumSet.of(
                        Permission.DEPARTMENT_READ,
                        Permission.TEAM_READ,
                        Permission.USER_READ)));
        return roles;
    }

    private record RoleDefinition(String description, Set<Permission> permissions) {
    }
}
