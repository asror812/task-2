package com.devops.paylite.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AgentDTO implements Serializable {

    private Long id;

    @NotNull
    private String login;

    @NotNull
    private Long balance;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AgentDTO agentDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, agentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AgentDTO{" +
            "id=" + getId() +
            ", login='" + getLogin() + "'" +
            ", balance=" + getBalance() +
            "}";
    }
}
