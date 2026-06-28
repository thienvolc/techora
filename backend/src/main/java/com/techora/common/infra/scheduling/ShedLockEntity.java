package com.techora.common.infra.scheduling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "shedlock")
@Getter
@Setter
@NoArgsConstructor
public class ShedLockEntity {
    @Id
    @Column(length = 64)
    private String name;

    @Column(nullable = false)
    private Instant lockUntil;

    @Column(nullable = false)
    private Instant lockedAt;

    @Column(nullable = false)
    private String lockedBy;
}
