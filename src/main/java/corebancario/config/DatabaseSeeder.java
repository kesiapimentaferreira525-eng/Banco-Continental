package corebancario.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import corebancario.model.Account;
import corebancario.model.Customer;
import corebancario.model.Transaction;
import corebancario.model.enums.AccountType;
import corebancario.model.enums.TransactionType;
import corebancario.repository.AccountRepository;
import corebancario.repository.CustomerRepository;
import corebancario.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Component
@Profile("dev")
public class DatabaseSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(CustomerRepository customerRepository,
                          AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) return;

        Random random = new Random();
        String senhaPadrao = passwordEncoder.encode("123456");

        List<Account> contasCriadas = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Customer customer = Customer.builder()
                    .nome("Cliente Teste " + (i + 1))
                    .cpf(String.format("%011d", i + 1))
                    .email("cliente" + (i + 1) + "@corebank.com")
                    .senha(senhaPadrao)
                    .build();

            customerRepository.save(customer);

            BigDecimal saldoInicial = BigDecimal.valueOf(500L + i * 100L)
                    .setScale(2, RoundingMode.HALF_UP);

            Account account = Account.builder()
                    .agencia("0001")
                    .numero(String.format("%06d-%d", random.nextInt(999999), random.nextInt(9)))
                    .tipo(AccountType.CORRENTE)
                    .saldo(saldoInicial)
                    .ativa(true)
                    .customer(customer)
                    .build();

            accountRepository.save(account);
            contasCriadas.add(account);

            // Registra o depósito inicial no extrato
            transactionRepository.save(Transaction.builder()
                    .valor(saldoInicial)
                    .tipo(TransactionType.DEPOSITO)
                    .contaDestino(account)
                    .build());
        }

        // Cria algumas transferências fictícias entre as primeiras contas
        for (int i = 0; i < 5; i++) {
            Account origem = contasCriadas.get(i);
            Account destino = contasCriadas.get(i + 1);
            BigDecimal valor = new BigDecimal("150.00");

            origem.setSaldo(origem.getSaldo().subtract(valor));
            destino.setSaldo(destino.getSaldo().add(valor));
            accountRepository.saveAll(List.of(origem, destino));

            transactionRepository.save(Transaction.builder()
                    .valor(valor)
                    .tipo(TransactionType.TRANSFERENCIA)
                    .contaOrigem(origem)
                    .contaDestino(destino)
                    .build());
        }

        Customer primeiroCliente = contasCriadas.get(0).getCustomer();
        System.out.println("======================================================");
        System.out.println("SEEDER CONCLUÍDO! 20 clientes e contas criados no MySQL.");
        System.out.println("Login de teste -> CPF: " + primeiroCliente.getCpf() + " | Senha: 123456");
        System.out.println("Conta destino para teste -> Número: " + contasCriadas.get(1).getNumero());
        System.out.println("======================================================");
    }
}