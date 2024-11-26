package com.oeyvind.ledger.model.repository

import com.oeyvind.ledger.model.entity.PendingTransactionEntry
import com.oeyvind.ledger.model.entity.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PendingTransactionEntryRepository : JpaRepository<PendingTransactionEntry, UUID> {
    fun findByTransaction(tx: Transaction): List<PendingTransactionEntry>
}