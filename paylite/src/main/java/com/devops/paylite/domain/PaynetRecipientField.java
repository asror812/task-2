package com.devops.paylite.domain;

import com.devops.paylite.domain.enumeration.RecipientFieldType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paynet_recipient_field")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaynetRecipientField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paynet_id", nullable = false, unique = true)
    private Long paynetId;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "label", length = 100, nullable = false)
    private String label;

    @Column(name = "validation_regex", length = 255)
    private String validationRegex;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "required", nullable = false)
    private Boolean required;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RecipientFieldType type;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private PaynetRecipient recipient;

    @OneToMany(mappedBy = "recipientField", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    private List<PaynetRecipientFieldValue> values = new ArrayList<>();

    @Column(nullable = false, name = "last_synced_at")
    private Instant lastSyncedAt;
}
