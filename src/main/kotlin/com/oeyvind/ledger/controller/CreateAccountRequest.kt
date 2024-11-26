package com.oeyvind.ledger.controller

import com.oeyvind.ledger.model.Ledger

data class CreateAccountRequest(
    val code: String,
    val name: String,
    val ledger: Ledger
)