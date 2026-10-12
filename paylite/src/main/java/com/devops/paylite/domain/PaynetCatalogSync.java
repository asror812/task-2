package com.devops.paylite.domain;

import com.devops.paylite.domain.enumeration.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "paynet_catalog_sync")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaynetCatalogSync {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SyncStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "categories_processed")
    private Long categoriesProcessed;

    @Column(name = "recipients_processed")
    private Long recipientsProcessed;

    @Column(name = "fields_processed")
    private Long fieldsProcessed;

    @Column(name = "values_processed")
    private Long valuesProcessed;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}
