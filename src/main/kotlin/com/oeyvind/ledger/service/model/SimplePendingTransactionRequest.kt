package com.oeyvind.ledger.service.model

import com.oeyvind.ledger.model.TransactionEntryType
import java.math.BigDecimal

data class SimplePendingTransactionRequest(
    val transactionId: String,
    val amount: BigDecimal,
    val fromAccountCode: String,
    val toAccountCode: String,
    val type: TransactionEntryType
)