package com.oeyvind.ledger.service.model

import java.util.*

data class TransactionEntryParams(
    val precedingEntryId: UUID?,
    val sequence: Int
)