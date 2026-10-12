package com.galileo.api.department;

import com.galileo.api.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "departments")
@NoArgsConstructor
public class DepartmentEntity extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    public DepartmentEntity(String code, String name, String description) {
        update(code, name, description);
    }

    public void update(String code, String name, String description) {
        this.code = code.strip().toUpperCase();
        this.name = name.strip();
        this.description = description == null ? null : description.strip();
    }
}
