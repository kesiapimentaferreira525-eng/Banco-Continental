package corebancario.repository;

import corebancario.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
        SELECT t FROM Transaction t
        LEFT JOIN FETCH t.contaOrigem
        LEFT JOIN FETCH t.contaDestino
        WHERE t.contaOrigem.id = :accountId OR t.contaDestino.id = :accountId
        ORDER BY t.dataHora DESC
    """)
    Page<Transaction> findStatementByAccountId(@Param("accountId") UUID accountId, Pageable pageable);
}