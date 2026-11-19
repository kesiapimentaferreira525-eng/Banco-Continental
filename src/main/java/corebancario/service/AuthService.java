package corebancario.service;


import corebancario.repository.AccountRepository;
import corebancario.repository.CustomerRepository;
import corebancario.dto.request.LoginRequest;
import corebancario.dto.request.RegisterRequest;
import corebancario.dto.response.TokenResponse;
import corebancario.exception.BusinessException;
import corebancario.model.Account;
import corebancario.model.Customer;
import corebancario.model.enums.AccountType;
import corebancario.security.TokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(CustomerRepository customerRepository,
                       AccountRepository accountRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       TokenService tokenService) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public void registrar(RegisterRequest request) {
        String cpfLimpo = request.cpf().replaceAll("\\D", "");

        if (customerRepository.existsByCpfOrEmail(cpfLimpo, request.email())) {
            throw new BusinessException("CPF ou e-mail já cadastrado no sistema.");
        }

        Customer customer = Customer.builder()
                .nome(request.nome())
                .cpf(cpfLimpo)
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .build();

        customerRepository.save(customer);

        String numero = String.format("%06d-%d", new Random().nextInt(1_000_000), new Random().nextInt(10));
        Account account = Account.builder()
                .agencia("0001")
                .numero(numero)
                .tipo(AccountType.CORRENTE)
                .saldo(BigDecimal.ZERO)
                .ativa(true)
                .customer(customer)
                .build();
        accountRepository.save(account);
    }

    public TokenResponse autenticar(LoginRequest request) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(
                request.cpf().replaceAll("\\D", ""), request.senha());
        authenticationManager.authenticate(authenticationToken);

        Customer customer = customerRepository.findByCpf(request.cpf().replaceAll("\\D", ""))
                .orElseThrow(() -> new BusinessException("Cliente não encontrado."));
        String token = tokenService.gerarToken(customer);

        return new TokenResponse(token, "Bearer", tokenService.getExpirationSeconds());
    }
}