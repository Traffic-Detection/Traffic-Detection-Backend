package com.gwuy.sba301.trafficdetectionbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class UserRoleId implements Serializable {
    @Serial
    private static final long serialVersionUID = -7757621873995890265L;
    @NotNull
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;


}