package corebancario.service;


import corebancario.dto.response.AccountResponse;
import corebancario.exception.BusinessException;
import corebancario.model.Account;
import corebancario.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public AccountResponse buscarMinhaConta(String cpfUsuarioLogado) {
        Account account = accountRepository.findByCustomerCpf(cpfUsuarioLogado)
                .orElseThrow(() -> new BusinessException("Conta não encontrada para o usuário autenticado."));

        return new AccountResponse(
                account.getId(),
                account.getAgencia(),
                account.getNumero(),
                account.getTipo(),
                account.getSaldo(),
                account.getCustomer().getNome()
        );
    }
}