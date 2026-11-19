package corebancario.dto.response;

import corebancario.model.enums.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String agencia,
        String numero,
        AccountType tipo,
        BigDecimal saldo,
        String titularNome
) {}
