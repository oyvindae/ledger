package com.oeyvind.ledger.service

import com.oeyvind.ledger.controller.LedgerBalance
import com.oeyvind.ledger.model.entity.Account
import com.oeyvind.ledger.model.repository.PendingTransactionEntryRepository
import com.oeyvind.ledger.model.repository.SettledTransactionEntryRepository
import org.springframework.stereotype.Service

@Service
class AccountBalanceService(
    val pendingTransactionEntryRepository: PendingTransactionEntryRepository,
    val settledTransactionEntryRepository: SettledTransactionEntryRepository
) {

    fun calculateAccountBalance(account: Account) = run {
        val pendingDebit = pendingTransactionEntryRepository.calculatePendingDebit(account.id)
        val settledDebit = settledTransactionEntryRepository.calculateSumDebit(account)
        val settledCredit = settledTransactionEntryRepository.calculateSumCredit(account)
        val settledBalance = settledCredit - settledDebit
        val availableBalance = settledBalance - pendingDebit
        LedgerBalance(
            settledCredit = settledCredit,
            settledDebit = settledDebit,
            pendingDebit = pendingDebit,
            availableBalance = availableBalance,
        )
    }
}