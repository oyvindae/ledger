package com.oeyvind.ledger.model.repository

import com.oeyvind.ledger.model.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TransactionRepository : JpaRepository<Transaction, UUID> {
    fun findByExternalTransactionId(externalId: String): Transaction?
    fun findByParentTransactionAndReversal(transaction: Transaction, reversal: Boolean): List<Transaction>
}