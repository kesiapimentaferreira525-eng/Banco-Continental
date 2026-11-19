package corebancario.repository;

import corebancario.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByNumero(String numero);
    Optional<Account> findByCustomerCpf(String cpf);
    boolean existsByNumero(String numero);
}