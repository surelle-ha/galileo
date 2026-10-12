package com.galileo.api.user;

import com.galileo.api.common.domain.BaseEntity;
import com.galileo.api.role.RoleEntity;
import com.galileo.api.team.TeamEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "app_users")
@NoArgsConstructor
public class UserEntity extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(nullable = false)
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private TeamEntity team;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    public UserEntity(
            String firstName,
            String lastName,
            String email,
            String passwordHash,
            String jobTitle,
            TeamEntity team,
            Set<RoleEntity> roles) {
        updateProfile(firstName, lastName, email, jobTitle, team, true);
        this.passwordHash = passwordHash;
        this.roles = new HashSet<>(roles);
    }

    public void updateProfile(
            String firstName,
            String lastName,
            String email,
            String jobTitle,
            TeamEntity team,
            boolean enabled) {
        this.firstName = firstName.strip();
        this.lastName = lastName.strip();
        this.email = email.strip().toLowerCase();
        this.jobTitle = jobTitle == null ? null : jobTitle.strip();
        this.team = team;
        this.enabled = enabled;
    }

    public void assignRoles(Set<RoleEntity> roles) {
        this.roles.clear();
        this.roles.addAll(roles);
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
