package com.oeyvind.ledger.controller

import com.oeyvind.ledger.model.entity.Account
import com.oeyvind.ledger.model.repository.AccountRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/accounts")
class AccountsController(private val accountRepository: AccountRepository) {

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
}