package com.oeyvind.ledger.controller

data class TransactionResponse(
    val transactionId: String,
    val settled: Boolean
)