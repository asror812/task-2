package com.devops.paylite.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "paynet_category")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaynetCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paynet_id", nullable = false, unique = true)
    private Long paynetId;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, name = "last_synced_at")
    private Instant lastSyncedAt;
}
