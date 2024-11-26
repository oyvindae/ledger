package com.oeyvind.ledger.service.model

import com.oeyvind.ledger.model.TransactionEntryType
import java.math.BigDecimal

data class PendingEntry(
    val amount: BigDecimal,
    val fromAccountCode: String,
    val toAccountCode: String,
    val type: TransactionEntryType,
)