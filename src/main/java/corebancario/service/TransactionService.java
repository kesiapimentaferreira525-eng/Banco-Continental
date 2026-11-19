package corebancario.service;


import corebancario.dto.request.DepositWithdrawRequest;
import corebancario.dto.request.TransferRequest;
import corebancario.dto.response.TransactionResponse;
import corebancario.exception.BusinessException;
import corebancario.model.Account;
import corebancario.model.Transaction;
import corebancario.model.enums.TransactionType;
import corebancario.repository.AccountRepository;
import corebancario.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse depositar(String cpf, DepositWithdrawRequest request) {
        Account conta = buscarContaPorCpf(cpf);
        conta.setSaldo(conta.getSaldo().add(request.valor()));
        accountRepository.save(conta);

        Transaction tx = Transaction.builder()
                .valor(request.valor())
                .tipo(TransactionType.DEPOSITO)
                .contaDestino(conta)
                .build();

        return toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public TransactionResponse sacar(String cpf, DepositWithdrawRequest request) {
        Account conta = buscarContaPorCpf(cpf);

        if (conta.getSaldo().compareTo(request.valor()) < 0) {
            throw new BusinessException("Saldo insuficiente para realizar o saque.");
        }

        conta.setSaldo(conta.getSaldo().subtract(request.valor()));
        accountRepository.save(conta);

        Transaction tx = Transaction.builder()
                .valor(request.valor())
                .tipo(TransactionType.SAQUE)
                .contaOrigem(conta)
                .build();

        return toResponse(transactionRepository.save(tx));
    }

    @Transactional
    public TransactionResponse transferir(String cpfOrigem, TransferRequest request) {
        Account origem = buscarContaPorCpf(cpfOrigem);
        Account destino = accountRepository.findByNumero(request.contaDestinoNumero())
                .orElseThrow(() -> new BusinessException("Conta de destino não encontrada."));

        if (origem.getNumero().equals(destino.getNumero())) {
            throw new BusinessException("Não é possível transferir para a própria conta.");
        }

        if (!destino.isAtiva()) {
            throw new BusinessException("A conta de destino está inativa.");
        }

        if (origem.getSaldo().compareTo(request.valor()) < 0) {
            throw new BusinessException("Saldo insuficiente para realizar a transferência.");
        }

        origem.setSaldo(origem.getSaldo().subtract(request.valor()));
        destino.setSaldo(destino.getSaldo().add(request.valor()));

        accountRepository.save(origem);
        accountRepository.save(destino);

        Transaction tx = Transaction.builder()
                .valor(request.valor())
                .tipo(TransactionType.TRANSFERENCIA)
                .contaOrigem(origem)
                .contaDestino(destino)
                .build();

        return toResponse(transactionRepository.save(tx));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> consultarExtrato(String cpf, Pageable pageable) {
        Account conta = buscarContaPorCpf(cpf);
        return transactionRepository.findStatementByAccountId(conta.getId(), pageable)
                .map(this::toResponse);
    }

    private Account buscarContaPorCpf(String cpf) {
        return accountRepository.findByCustomerCpf(cpf)
                .orElseThrow(() -> new BusinessException("Conta não encontrada."));
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getTipo(),
                tx.getValor(),
                tx.getContaOrigem() != null ? tx.getContaOrigem().getNumero() : null,
                tx.getContaDestino() != null ? tx.getContaDestino().getNumero() : null,
                tx.getDataHora()
        );
    }
}