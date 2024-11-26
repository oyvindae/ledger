package com.oeyvind.ledger.controller

import com.oeyvind.ledger.model.Ledger
import java.time.Instant

data class AccountResponse(
    val code: String,
    val name: String,
    val ledger: Ledger,
    val createdAt: Instant
)