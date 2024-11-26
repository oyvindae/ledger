package com.oeyvind.ledger.controller

import java.math.BigDecimal

data class LedgerBalance(
    val settledCredit: BigDecimal,
    val settledDebit: BigDecimal,
    val pendingDebit: BigDecimal,
    val availableBalance: BigDecimal
)