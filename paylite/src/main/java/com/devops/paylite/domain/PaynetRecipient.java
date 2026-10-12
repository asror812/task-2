package com.devops.paylite.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "paynet_recipient")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaynetRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paynet_id", nullable = false, unique = true)
    private Long paynetId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private PaynetCategory category;

    @Column(nullable = false, name = "last_synced_at")
    private Instant lastSyncedAt;
}
