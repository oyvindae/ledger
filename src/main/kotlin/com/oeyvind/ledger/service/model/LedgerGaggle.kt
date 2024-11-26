package com.oeyvind.ledger.service.model

import com.oeyvind.ledger.model.entity.TransactionEntry
import java.math.BigDecimal
import java.util.*

// Apparently a collection of ledger entries is referred to as a "gaggle"

class LedgerGaggle<T : TransactionEntry>() {
    private val entries: MutableList<T> = mutableListOf<T>()

    fun preceedingEntryUuid(): UUID? = entries.lastOrNull()?.id

    fun add(createDebit: (TransactionEntryParams) -> T, createCredit: (TransactionEntryParams) -> T) {
        // debit before credit is the accounting standard
        val debit = add(createDebit)
        val credit = add(createCredit)

        require(debit.isDebit()) { "Debit amount not negative" }
        require(credit.isCredit()) { "Credit amount not positive" }
        require(credit.amountAbsolute() == debit.amountAbsolute()) {
            "Debit amount of ${debit.amountAbsolute()} does not equal credit amount of ${credit.amountAbsolute()}"
        }
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

    fun honk(): List<T> = run {
        val totalDebit = entries.filter { it.isDebit() }.sumOf { it.amountAbsolute() }
        val totalCredit = entries.filter { it.isCredit() }.sumOf { it.amountAbsolute() }
        require(totalDebit == totalCredit) {
            "Transaction is unbalanced. Debit: $totalDebit Credit: $totalCredit"
        }
        entries.toList()
    }
}