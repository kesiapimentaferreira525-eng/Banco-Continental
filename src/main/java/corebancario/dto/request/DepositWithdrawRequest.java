package corebancario.dto.request;

import java.math.BigDecimal;

public record DepositWithdrawRequest(BigDecimal amount, String description) {
}
