package com.oeyvind.ledger.model.repository

import com.oeyvind.ledger.model.entity.Account
import com.oeyvind.ledger.model.entity.SettledTransactionEntry
import com.oeyvind.ledger.model.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.util.*

interface SettledTransactionEntryRepository : JpaRepository<SettledTransactionEntry, UUID> {
    @Query("select coalesce(sum(abs(p.amountSigned)),0) from SettledTransactionEntry p where p.account = :account and p.amountSigned < 0")
    fun calculateSumDebit(account: Account): BigDecimal

    @Query("select coalesce(sum(abs(p.amountSigned)),0) from SettledTransactionEntry p where p.account = :account and p.amountSigned > 0")
    fun calculateSumCredit(account: Account): BigDecimal

    @Query("select e.transaction from SettledTransactionEntry e where e.account=:account order by e.createdAt asc limit 100")
    fun findTransactions(account: Account): List<Transaction>
}