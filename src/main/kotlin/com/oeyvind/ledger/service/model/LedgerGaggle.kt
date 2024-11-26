package com.oeyvind.ledger.service.model

import com.oeyvind.ledger.model.entity.TransactionEntry
import java.math.BigDecimal

class LedgerGaggle<T : TransactionEntry>() {
    private val entries: MutableList<T> = mutableListOf<T>()

    fun preceedingEntryUuid() = entries.lastOrNull()?.id

    fun add(createDebit: (TransactionEntryParams) -> T, createCredit: (TransactionEntryParams) -> T) {
        // debit before credit is the accounting standard
        val debit = add(createDebit)
        val credit = add(createCredit)

        require(debit.amountSigned < BigDecimal.ZERO) { "Debit amount not negative" }
        require(credit.amountSigned > BigDecimal.ZERO) { "Credit amount not positive" }
        require(credit.amountSigned == debit.amountSigned.negate()) { "Debit amount of ${debit.amountSigned} does not equal credit amount of $credit.amountSigned" }
    }

    fun add(create: (TransactionEntryParams) -> T) = run {
        val params = TransactionEntryParams(
            precedingEntryId = preceedingEntryUuid(),
            sequence = entries.size
        )
        val entry = create(params)
        entries += entry
        entry
    }

    fun honk(): List<T> = entries.toList()
}