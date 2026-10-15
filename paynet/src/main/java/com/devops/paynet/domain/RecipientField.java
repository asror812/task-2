package com.devops.paynet.domain;

import com.devops.paynet.domain.enumeration.RecipientFieldMethod;
import com.devops.paynet.domain.enumeration.RecipientFieldType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * A RecipientField.
 */
@Entity
@Table(
  name = "recipient_field",
  uniqueConstraints = @UniqueConstraint(
    name = "ux_recipient_field__recipient_name",
    columnNames = { "recipient_id", "name" }
  )
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecipientField extends AbstractAuditingEntity<Long> {

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
  @Size(max = 50)
  @Column(name = "name", length = 50, nullable = false)
  private String name;

  @NotNull
  @Size(max = 100)
  @Column(name = "label", length = 100, nullable = false)
  private String label;

  @Size(max = 255)
  @Column(name = "validation_regex", length = 255)
  private String validationRegex;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false)
  private RecipientFieldMethod method;

  @NotNull
  @Column(name = "active", nullable = false)
  private Boolean active;

  @NotNull
  @Column(name = "required", nullable = false)
  private Boolean required;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private RecipientFieldType type;

  @NotNull
  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;

  @ManyToOne(optional = false)
  @NotNull
  private Recipient recipient;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "recipientField")
  @OrderBy("sortOrder ASC, id ASC")
  private List<RecipientFieldValue> values;

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

  public String getLabel() {
    return this.label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getValidationRegex() {
    return this.validationRegex;
  }

  public void setValidationRegex(String validationRegex) {
    this.validationRegex = validationRegex;
  }

  public RecipientFieldMethod getMethod() {
    return this.method;
  }

  public void setMethod(RecipientFieldMethod method) {
    this.method = method;
  }

  public Boolean getActive() {
    return this.active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Boolean getRequired() {
    return this.required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public RecipientFieldType getType() {
    return this.type;
  }

  public void setType(RecipientFieldType type) {
    this.type = type;
  }

  public Integer getSortOrder() {
    return this.sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Recipient getRecipient() {
    return this.recipient;
  }

  public void setRecipient(Recipient recipient) {
    this.recipient = recipient;
  }

  public List<RecipientFieldValue> getValues() {
    return values;
  }

  public void setValues(List<RecipientFieldValue> values) {
    this.values = values;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RecipientField)) {
      return false;
    }
    return id != null && id.equals(((RecipientField) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  // prettier-ignore
  @Override
    public String toString() {
        return "RecipientField{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", label='" + getLabel() + "'" +
            ", validationRegex='" + getValidationRegex() + "'" +
            ", method='" + getMethod() + "'" +
            ", active='" + getActive() + "'" +
            ", required='" + getRequired() + "'" +
            ", type='" + getType() + "'" +
            ", sortOrder=" + getSortOrder() +
            "}";
    }
}
