package corebancario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import corebancario.dto.request.DepositWithdrawRequest;
import corebancario.dto.request.TransferRequest;
import corebancario.dto.response.TransactionResponse;
import corebancario.model.Customer;
import corebancario.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Transações", description = "Depósitos, saques, transferências e extrato")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    @Operation(summary = "Realizar depósito na própria conta")
    public ResponseEntity<TransactionResponse> depositar(
            @AuthenticationPrincipal Customer usuarioLogado,
            @RequestBody @Valid DepositWithdrawRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.depositar(usuarioLogado.getCpf(), request));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Realizar saque da própria conta")
    public ResponseEntity<TransactionResponse> sacar(
            @AuthenticationPrincipal Customer usuarioLogado,
            @RequestBody @Valid DepositWithdrawRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.sacar(usuarioLogado.getCpf(), request));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transferir valores para outra conta")
    public ResponseEntity<TransactionResponse> transferir(
            @AuthenticationPrincipal Customer usuarioLogado,
            @RequestBody @Valid TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transferir(usuarioLogado.getCpf(), request));
    }

    @GetMapping("/statement")
    @Operation(summary = "Consultar extrato paginado da conta")
    public ResponseEntity<Page<TransactionResponse>> consultarExtrato(
            @AuthenticationPrincipal Customer usuarioLogado,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(transactionService.consultarExtrato(usuarioLogado.getCpf(), pageable));
    }
}