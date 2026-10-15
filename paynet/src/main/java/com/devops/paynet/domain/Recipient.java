package com.devops.paynet.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * A Recipient.
 */
@Entity
@Table(name = "recipient")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Recipient extends AbstractAuditingEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "sequenceGenerator"
    )
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @NotNull
    @Size(max = 50)
    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @ManyToOne(optional = false)
    @NotNull
    private Category category;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "recipient")
    private List<RecipientField> recipientFields;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getActive() {
        return this.active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Category getCategory() {
        return this.category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<RecipientField> getRecipientFields() {
        return this.recipientFields;
    }

    public void setRecipientFields(List<RecipientField> recipientFields) {
        this.recipientFields = recipientFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Recipient)) {
            return false;
        }
        return getId() != null && getId().equals(((Recipient) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Recipient{" +
                "id=" + getId() +
                ", name='" + getName() + "'" +
                ", code='" + getCode() + "'" +
                ", active='" + getActive() + "'" +
                "}";
    }
}
