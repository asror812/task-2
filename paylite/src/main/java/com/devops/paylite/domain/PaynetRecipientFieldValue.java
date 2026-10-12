package com.devops.paylite.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "paynet_recipient_field_value")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaynetRecipientFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paynet_id", nullable = false, unique = true)
    private Long paynetId;

    @Column(name = "field_value", length = 100, nullable = false)
    private String value;

    @Column(name = "label", length = 150, nullable = false)
    private String label;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_field_id", nullable = false)
    @JsonIgnoreProperties(value = { "recipient", "values" }, allowSetters = true)
    private PaynetRecipientField recipientField;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnoreProperties(value = { "recipientField", "parent" }, allowSetters = true)
    private PaynetRecipientFieldValue parent;

    @Column(nullable = false, name = "last_synced_at")
    private Instant lastSyncedAt;
}
