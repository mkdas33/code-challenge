package com.db.awmd.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import java.math.BigDecimal;

@Data
public class Transaction {

    @NonNull
    @NotEmpty
    private final String accountFromId;

    @NonNull
    @NotEmpty
    private final String accountToId;

    @NonNull
    @Min(value = 0, message = "Transaction amount must be positive.")
    private final BigDecimal amount;

    @JsonCreator
    public Transaction(@JsonProperty("accountFromId") String accountFromId,
                       @JsonProperty("accountToId") String accountToId,
                       @JsonProperty("amount") BigDecimal amount) {
        this.accountFromId = accountFromId;
        this.accountToId = accountToId;
        this.amount = amount;
    }

}
