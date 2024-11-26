package com.oeyvind.ledger.model.entity

import java.math.BigDecimal
import java.util.UUID

interface TransactionEntry {
    val id: UUID
    val amountSigned: BigDecimal
}