package com.oeyvind.ledger.service.model


data class PendingTransactionRequest(
    val transactionId: String,
    val entries: List<PendingEntry>
)


