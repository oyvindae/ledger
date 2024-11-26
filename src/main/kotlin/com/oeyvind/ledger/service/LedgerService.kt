package com.oeyvind.ledger.service

import com.oeyvind.ledger.model.Ledger
import com.oeyvind.ledger.model.entity.Account
import com.oeyvind.ledger.model.entity.PendingTransactionEntry
import com.oeyvind.ledger.model.entity.SettledTransactionEntry
import com.oeyvind.ledger.model.entity.Transaction
import com.oeyvind.ledger.model.repository.AccountRepository
import com.oeyvind.ledger.model.repository.PendingTransactionEntryRepository
import com.oeyvind.ledger.model.repository.SettledTransactionEntryRepository
import com.oeyvind.ledger.model.repository.TransactionRepository
import com.oeyvind.ledger.service.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Service
class LedgerService(
    private val accountRepository: AccountRepository,
    private val pendingTransactionEntryRepository: PendingTransactionEntryRepository,
    private val settledTransactionEntryRepository: SettledTransactionEntryRepository,
    private val transactionRepository: TransactionRepository
) {
    // archived pending entries? Or opposite direction for now? need to create anyway
    // delete pending entries for PoC, but point out they should be archived

    @Transactional
    fun settleTransaction(transactionId: String): Transaction = run {
        val pendingTransaction = transactionRepository.findByExternalTransactionId(transactionId)
        require(pendingTransaction != null) { "Transaction $transactionId not found" }
        require(!pendingTransaction.reversal) { "Transaction is reversal" }
        if (pendingTransaction.settled) return pendingTransaction

        // find other reversals
        val reversalTransactions = transactionRepository.findByParentTransactionAndReversal(pendingTransaction, true)
        val reversalEntries = reversalTransactions.flatMap { pendingTransactionEntryRepository.findByTransaction(it) }
        val entryReversalAmountMap = reversalEntries
            .groupBy { it.reverses?.id }
            .mapValues { it.value.sumOf { it.amountAbsolute() } }

        val pendingEntries = pendingTransactionEntryRepository.findByTransaction(pendingTransaction)
        require(pendingEntries.size % 2 == 0) { "Odd number of entries" }

        val accounts = pendingEntries
            .map { it.account }
            .distinct()

        val settledGaggle = LedgerGaggle<SettledTransactionEntry>()
        val finalReversal = transactionRepository.save(
            Transaction(
                createdAt = Instant.now(),
                reversal = true,
                settled = true,
                externalTransactionId = UUID.randomUUID().toString(),
                parentTransaction = pendingTransaction,
                )
        )
        val pendingGaggle = LedgerGaggle<PendingTransactionEntry>()

        val (evenEntries, oddEntries) = pendingEntries.sortedBy { it.sequence }.partition { it.sequence % 2 == 0 }
        evenEntries.zip(oddEntries).forEach { (debitEntry, creditEntry) ->
            // TODO: This didn't subtract pending amounts for some reason
            settledGaggle.add(
                createDebit = { params ->
                    val reversedDebitAmount = entryReversalAmountMap.get(debitEntry.id) ?: BigDecimal.ZERO
                    val settledAmountSigned = debitEntry.amountSigned + reversedDebitAmount
                    SettledTransactionEntry(
                        createdAt = Instant.now(),
                        account = debitEntry.account,
                        transaction = debitEntry.transaction,
                        amountSigned = settledAmountSigned,
                        sequence = params.sequence,
                        precedingEntryId = params.precedingEntryId,
                        type = debitEntry.type,
                        ledger = debitEntry.ledger,
                    )

                },
                createCredit = { params ->
                    val reversedCreditAmount = entryReversalAmountMap.get(creditEntry.id) ?: BigDecimal.ZERO
                    val settledAmountSigned = creditEntry.amountSigned - reversedCreditAmount
                    SettledTransactionEntry(
                        createdAt = Instant.now(),
                        account = creditEntry.account,
                        transaction = creditEntry.transaction,
                        amountSigned = settledAmountSigned,
                        sequence = params.sequence,
                        precedingEntryId = params.precedingEntryId,
                        type = creditEntry.type,
                        ledger = creditEntry.ledger,
                    )
                }
            )
        }

        val settledEntries = settledGaggle.honk()

        val (settledEvenEntries, settledOddEntries) = settledEntries.sortedBy { it.sequence }.partition { it.sequence % 2 == 0 }
        settledEvenEntries.zip(settledOddEntries).forEach { (debitEntry, creditEntry) ->
            // need to swap settled vs pending
            pendingGaggle.add(
                createDebit = { params ->
                    PendingTransactionEntry(
                        createdAt = Instant.now(),
                        account = creditEntry.account,
                        transaction = finalReversal,
                        amountSigned = -creditEntry.amountSigned,
                        sequence = params.sequence,
                        precedingEntryId = params.precedingEntryId,
                        type = creditEntry.type,
                        ledger = creditEntry.ledger,
                    )
                },
                createCredit = { params ->
                    PendingTransactionEntry(
                        createdAt = Instant.now(),
                        account = debitEntry.account,
                        transaction = finalReversal,
                        amountSigned = -debitEntry.amountSigned,
                        sequence = params.sequence,
                        precedingEntryId = params.precedingEntryId,
                        type = debitEntry.type,
                        ledger = debitEntry.ledger,
                    )
                }
            )
        }

        accounts.forEach { account ->
            val matchingEntries = settledEntries.filter { it.account.id == account.id }
            val debit = matchingEntries.filter { it.amountSigned < BigDecimal.ZERO }.sumOf { it.amountAbsolute() }
            val credit = matchingEntries.filter { it.amountSigned > BigDecimal.ZERO }.sumOf { it.amountAbsolute() }
            account.settledDebit += debit
            account.settledCredit += credit
            account.pendingDebit -= debit
            account.pendingCredit -= credit
            account
        }

        settledTransactionEntryRepository.saveAll(settledEntries)
        pendingTransactionEntryRepository.saveAll(pendingGaggle.honk())
        accountRepository.saveAll(accounts)

        pendingTransaction.settled = true
        transactionRepository.save(pendingTransaction)
    }

    @Transactional
    fun reversePendingTransaction(
        pendingTransactionId: String,
        request: SimpleReversePendingTransactionRequest
    ): Transaction = run {
        val current = transactionRepository.findByExternalTransactionId(request.transactionId)
        if (current != null) return current

        val pendingTransaction = transactionRepository.findByExternalTransactionId(pendingTransactionId)
        if (pendingTransaction == null) throw IllegalArgumentException("Transaction not found")
        if (pendingTransaction.settled) throw IllegalArgumentException("Transaction already settled")
        val pendingEntries = pendingTransactionEntryRepository.findByTransaction(pendingTransaction)
        require(pendingEntries.size == 2) {
            "Only support reversing simple transactions for now"
        }
        val debitEntry = pendingEntries.find { it.amountSigned < BigDecimal.ZERO }!!
        val creditEntry = pendingEntries.find { it.amountSigned > BigDecimal.ZERO }!!

        val accounts = listOf(debitEntry, creditEntry)
            .map { it.account }

        // find other reversals
        val reversalTransactions = transactionRepository.findByParentTransactionAndReversal(pendingTransaction, true)
        val reversalEntries = reversalTransactions.flatMap { pendingTransactionEntryRepository.findByTransaction(it) }
        val reversedDebit = reversalEntries.filter { it.amountSigned < BigDecimal.ZERO }.sumOf { it.amountAbsolute() }
        val reversedCredit = reversalEntries.filter { it.amountSigned > BigDecimal.ZERO }.sumOf { it.amountAbsolute() }

        require(debitEntry.amountAbsolute() >= request.amount + reversedDebit) {
            "TBD"
        }
        require(creditEntry.amountAbsolute() >= request.amount + reversedCredit) {
            "TBD"
        }

        val reversalTransaction = transactionRepository.save(
            Transaction(
                createdAt = Instant.now(),
                reversal = true,
                settled = true,
                externalTransactionId = request.transactionId,
                parentTransaction = pendingTransaction
            )
        )
        val gaggle = LedgerGaggle<PendingTransactionEntry>()
        gaggle.add(
            createDebit = { params ->
                PendingTransactionEntry(
                    createdAt = Instant.now(),
                    account = creditEntry.account,
                    transaction = reversalTransaction,
                    amountSigned = -request.amount,
                    sequence = params.sequence,
                    precedingEntryId = params.precedingEntryId,
                    type = creditEntry.type,
                    ledger = creditEntry.ledger,
                    reverses = creditEntry
                )
            },
            createCredit = { params ->
                PendingTransactionEntry(
                    createdAt = Instant.now(),
                    account = debitEntry.account,
                    transaction = reversalTransaction,
                    amountSigned = request.amount,
                    sequence = params.sequence,
                    precedingEntryId = params.precedingEntryId,
                    type = debitEntry.type,
                    ledger = debitEntry.ledger,
                    reverses = debitEntry
                )
            }
        )

        val entries = gaggle.honk()

        accounts.forEach { account ->
            val matchingEntries = entries.filter { it.account.id == account.id }
            val debit = matchingEntries.filter { it.amountSigned < BigDecimal.ZERO }.sumOf { it.amountAbsolute() }
            val credit = matchingEntries.filter { it.amountSigned > BigDecimal.ZERO }.sumOf { it.amountAbsolute() }
            account.pendingCredit -= debit
            account.pendingDebit -= credit
            account
        }

        pendingTransactionEntryRepository.saveAll(entries)
        accountRepository.saveAll(accounts)

        reversalTransaction
    }

    @Transactional
    fun createPendingTransaction(request: SimplePendingTransactionRequest): Transaction = run {
        createPendingTransaction(
            PendingTransactionRequest(
                request.transactionId,
                listOf(
                    PendingEntry(
                        request.amount,
                        fromAccountCode = request.fromAccountCode,
                        toAccountCode = request.toAccountCode,
                        type = request.type
                    )
                )
            )
        )
    }

    // TODO: Postgres triggers/functions to validate
    @Transactional
    fun createPendingTransaction(request: PendingTransactionRequest): Transaction = run {

        val current = transactionRepository.findByExternalTransactionId(request.transactionId)
        if (current != null) return current // TODO: Check status?

        val accounts = request
            .entries
            .flatMap { listOf(it.fromAccountCode, it.toAccountCode) }
            .distinct()
            .map { findAccountByCode(it) }

        val transaction = transactionRepository.save(
            Transaction(
                createdAt = Instant.now(),
                reversal = false,
                settled = false,
                externalTransactionId = request.transactionId,
            )
        )
        val gaggle = LedgerGaggle<PendingTransactionEntry>()
        request.entries.forEach { e ->
            gaggle.add(
                createDebit = { params ->
                    val account = findAccountByCode(e.fromAccountCode)
                    PendingTransactionEntry(
                        createdAt = Instant.now(),
                        account = account,
                        transaction = transaction,
                        amountSigned = -e.amount,
                        sequence = params.sequence,
                        precedingEntryId = params.precedingEntryId,
                        type = e.type,
                        ledger = account.ledger
                    )
                },
                createCredit = { params ->
                    val account = findAccountByCode(e.toAccountCode)
                    PendingTransactionEntry(
                        createdAt = Instant.now(),
                        account = account,
                        transaction = transaction,
                        amountSigned = e.amount,
                        sequence = params.sequence,
                        precedingEntryId = params.precedingEntryId,
                        type = e.type,
                        ledger = account.ledger
                    )
                }
            )
        }

        val entries = gaggle.honk()

        accounts.forEach { account ->
            val matchingEntries = entries.filter { it.account.id == account.id }
            val debit = matchingEntries.filter { it.amountSigned < BigDecimal.ZERO }.sumOf { it.amountAbsolute() }
            val credit = matchingEntries.filter { it.amountSigned > BigDecimal.ZERO }.sumOf { it.amountAbsolute() }
            if (!hasAvailableBalance(account, debit, credit)) {
                throw IllegalArgumentException("Insufficient funds")
            }
            account.pendingCredit += credit
            account.pendingDebit += debit
            account
        }

        pendingTransactionEntryRepository.saveAll(entries)
        accountRepository.saveAll(accounts)

        transaction
    }

    private fun hasAvailableBalance(account: Account, debit: BigDecimal, credit: BigDecimal) = run {
        when (account.ledger) {
            Ledger.CUSTOMER -> account.availableBalance() + (credit - debit) >= BigDecimal.ZERO
            Ledger.GATEWAY -> true
        }
    }

    private fun findAccountByCode(code: String): Account = run {
        val account = accountRepository.findByCode(code)
        require(account != null) { "Account with id $code not found" }
        account
    }
}