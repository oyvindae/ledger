package com.oeyvind.ledger.service.model

import java.math.BigDecimal

data class SimpleReversePendingTransactionRequest(
    val transactionId: String,
    val amount: BigDecimal,
)