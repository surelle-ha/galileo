package com.galileo.api.team;

import com.galileo.api.common.domain.BaseEntity;
import com.galileo.api.department.DepartmentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "teams",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_department_name",
                columnNames = {"department_id", "name"}
        )
)
@NoArgsConstructor
public class TeamEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    public TeamEntity(String code, String name, String description, DepartmentEntity department) {
        update(code, name, description, department);
    }

    public void update(String code, String name, String description, DepartmentEntity department) {
        this.code = code.strip().toUpperCase();
        this.name = name.strip();
        this.description = description == null ? null : description.strip();
        this.department = department;
    }
}
