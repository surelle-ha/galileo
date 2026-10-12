package com.galileo.api.user;

import com.galileo.api.common.api.ApiResponse;
import com.galileo.api.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Users assigned to teams and RBAC roles")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_WRITE') and hasAuthority('ROLE_ASSIGN')")
    @Operation(summary = "Create a user with one or more roles")
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.of("User created", userService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Page users, optionally filtering by team and search text")
    public ApiResponse<PageResponse<UserResponse>> findAll(
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return ApiResponse.of("Users retrieved", userService.findAll(teamId, query, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    @Operation(summary = "Get a user")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.of("User retrieved", userService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Operation(summary = "Replace a user's profile or team assignment")
    public ApiResponse<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.of("User updated", userService.update(id, request));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    @Operation(summary = "Replace a user's RBAC roles")
    public ApiResponse<UserResponse> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody RoleAssignmentRequest request) {
        return ApiResponse.of("Roles updated", userService.assignRoles(id, request));
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("#id.toString() == authentication.tokenAttributes['userId'].toString() " +
            "or hasAuthority('USER_WRITE')")
    @Operation(summary = "Reset a user's password")
    public void changePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Operation(summary = "Delete a user")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
