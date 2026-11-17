package corebancario.dto.request;

import java.math.BigDecimal;

public record TransferRequest(Long destinationAccountId, BigDecimal amount, String description) {
}
