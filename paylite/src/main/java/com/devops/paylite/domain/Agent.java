package com.devops.paylite.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;

/**
 * A Agent.
 */
@Entity
@Table(name = "agent")
@Getter
@Setter
@Builder
@SuppressWarnings("common-java:DuplicatedBlocks")
@NoArgsConstructor
@AllArgsConstructor
public class Agent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "login", nullable = false, unique = true)
    private String login;

    @NotNull
    @Column(name = "balance", nullable = false)
    private Long balance;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Agent)) {
            return false;
        }
        return getId() != null && getId().equals(((Agent) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Agent{" +
            "id=" + getId() +
            ", login='" + getLogin() + "'" +
            ", balance=" + getBalance() +
            "}";
    }
}
