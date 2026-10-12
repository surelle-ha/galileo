package com.galileo.api.user;

import com.galileo.api.role.RoleName;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record RoleAssignmentRequest(@NotEmpty Set<RoleName> roles) {
}
