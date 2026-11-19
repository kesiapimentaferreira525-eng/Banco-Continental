package corebancario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import corebancario.dto.response.AccountResponse;
import corebancario.model.Customer;
import corebancario.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Contas", description = "Consulta de dados bancários e saldo")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar dados e saldo da conta do usuário autenticado")
    public ResponseEntity<AccountResponse> getMinhaConta(@AuthenticationPrincipal Customer usuarioLogado) {
        return ResponseEntity.ok(accountService.buscarMinhaConta(usuarioLogado.getCpf()));
    }
}