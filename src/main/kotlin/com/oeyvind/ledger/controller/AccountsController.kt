package com.oeyvind.ledger.controller

import com.oeyvind.ledger.model.entity.Account
import com.oeyvind.ledger.model.entity.SettledTransactionEntry
import com.oeyvind.ledger.model.entity.Transaction
import com.oeyvind.ledger.model.repository.AccountRepository
import com.oeyvind.ledger.service.AccountBalanceService
import com.oeyvind.ledger.service.LedgerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/accounts")
class AccountsController(
    private val accountRepository: AccountRepository,
    private val accountBalanceService: AccountBalanceService,
    private val ledgerService: LedgerService,
) {

    @PutMapping
    fun upsertAccount(@RequestBody request: CreateAccountRequest) = run {
        val result = when (val account = accountRepository.findByCode(request.code)) {
            null -> accountRepository.save(
                Account(
                    createdAt = Instant.now(),
                    code = request.code,
                    name = request.name,
                    ledger = request.ledger,
                )
            )

            else -> account
        }
        ResponseEntity.ok(
            AccountResponse(
                createdAt = result.createdAt,
                code = result.code,
                ledger = result.ledger,
                name = result.name
            )
        )
    }

    @GetMapping("/{account-code}/balance")
    fun getBalance(@PathVariable("account-code") code: String): LedgerBalance = run {
        val account = getAccountOrFail(code)
        accountBalanceService.calculateAccountBalance(account)
    }

    @GetMapping("/{account-code}/transactions")
    fun listTransactions(@PathVariable("account-code") code: String): SearchResult = run {
        val account = getAccountOrFail(code)
        SearchResult(
            ledgerService.listTransactions(account).map { TransactionResponse(it.externalTransactionId, it.settled) }
        )
    }

    private fun getAccountOrFail(code: String): Account = run {
        val account = accountRepository.findByCode(code)
        require(account != null) { "Account not found" }
        account
    }
}