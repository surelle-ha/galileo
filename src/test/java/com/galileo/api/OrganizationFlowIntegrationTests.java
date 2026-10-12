package com.galileo.api;

import com.galileo.api.department.DepartmentRequest;
import com.galileo.api.department.DepartmentService;
import com.galileo.api.role.RoleName;
import com.galileo.api.security.AuthenticationService;
import com.galileo.api.security.LoginRequest;
import com.galileo.api.team.TeamRequest;
import com.galileo.api.team.TeamService;
import com.galileo.api.user.UserCreateRequest;
import com.galileo.api.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationFlowIntegrationTests {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void createsDepartmentTeamAndUserHierarchy() {
        var department = departmentService.create(new DepartmentRequest(
                "ENG", "Engineering", "Builds Galileo"));
        var team = teamService.create(new TeamRequest(
                "PLATFORM", "Platform", "Shared services", department.id()));
        var user = userService.create(new UserCreateRequest(
                "Ada",
                "Lovelace",
                "ada@galileo.test",
                "CorrectHorse123!",
                "Engineer",
                team.id(),
                Set.of(RoleName.USER)
        ));

        assertThat(user.team()).isNotNull();
        assertThat(user.team().id()).isEqualTo(team.id());
        assertThat(user.team().departmentId()).isEqualTo(department.id());
        assertThat(user.roles()).containsExactly(RoleName.USER);
        assertThat(departmentService.findAll("engin", PageRequest.of(0, 10)).totalElements())
                .isEqualTo(1);
        assertThat(teamService.findAll(department.id(), "plat", PageRequest.of(0, 10)).totalElements())
                .isEqualTo(1);
        assertThat(userService.findAll(team.id(), "ada", PageRequest.of(0, 10)).totalElements())
                .isEqualTo(1);
    }

    @Test
    void bootstrapAdministratorCanReceiveVerifiedJwt() {
        var response = authenticationService.login(
                new LoginRequest("admin@test.local", "TestAdmin123!"));
        var jwt = jwtDecoder.decode(response.accessToken());

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwt.getSubject()).isEqualTo("admin@test.local");
        assertThat(jwt.getClaimAsStringList("roles")).contains("ADMIN");
        assertThat(jwt.getClaimAsStringList("permissions")).contains("ROLE_ASSIGN");
    }
}
