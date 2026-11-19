package corebancario.dto.response;


import corebancario.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType tipo,
        BigDecimal valor,
        String contaOrigem,
        String contaDestino,
        LocalDateTime dataHora
) {}