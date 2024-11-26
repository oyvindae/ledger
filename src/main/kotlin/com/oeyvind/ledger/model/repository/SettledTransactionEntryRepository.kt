package com.oeyvind.ledger.model.repository

import com.oeyvind.ledger.model.entity.SettledTransactionEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface SettledTransactionEntryRepository : JpaRepository<SettledTransactionEntry, UUID> {
}