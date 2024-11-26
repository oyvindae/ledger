package com.oeyvind.ledger.model.repository

import com.oeyvind.ledger.model.entity.Account
import com.oeyvind.ledger.model.entity.PendingTransactionEntry
import com.oeyvind.ledger.model.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.UUID

interface PendingTransactionEntryRepository : JpaRepository<PendingTransactionEntry, UUID> {
    fun findByTransaction(tx: Transaction): List<PendingTransactionEntry>

    @Query("select coalesce(sum(abs(debit)),0)\n" +
            "from (select sum(pe.amount_signed) as debit\n" +
            "      from transactions t\n" +
            "               join pending_transaction_entries pe on pe.main_transaction_id = t.id and t.settled is false and\n" +
            "                                                      pe.account_id = :accountId \n" +
            "      group by t.id\n" +
            "      having sum(pe.amount_signed) < 0)",
        nativeQuery = true
        )
    fun calculatePendingDebit(@Param("accountId") accountId: UUID): BigDecimal
}