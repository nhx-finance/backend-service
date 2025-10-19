package com.javaguy.nhxserver.model.entity;

import com.javaguy.nhxserver.model.enums.ERole;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;

    public Role(ERole roleEnum) {
        this.name = roleEnum;
    }
}
