package com.galileo.api.team;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Teams nested under departments")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TEAM_WRITE')")
    @Operation(summary = "Create a team in a department")
    public ApiResponse<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
        return ApiResponse.of("Team created", teamService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TEAM_READ')")
    @Operation(summary = "Page teams, optionally filtering by department and search text")
    public ApiResponse<PageResponse<TeamResponse>> findAll(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ApiResponse.of("Teams retrieved", teamService.findAll(departmentId, query, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TEAM_READ')")
    @Operation(summary = "Get a team")
    public ApiResponse<TeamResponse> findById(@PathVariable Long id) {
        return ApiResponse.of("Team retrieved", teamService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TEAM_WRITE')")
    @Operation(summary = "Replace a team or move it to another department")
    public ApiResponse<TeamResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TeamRequest request) {
        return ApiResponse.of("Team updated", teamService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TEAM_WRITE')")
    @Operation(summary = "Delete an empty team")
    public void delete(@PathVariable Long id) {
        teamService.delete(id);
    }
}
