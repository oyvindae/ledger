package com.oeyvind.ledger.controller

import com.oeyvind.ledger.service.LedgerService
import com.oeyvind.ledger.service.model.SimplePendingTransactionRequest
import com.oeyvind.ledger.service.model.SimpleReversePendingTransactionRequest
import com.oeyvind.ledger.model.entity.Transaction
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/transactions")
class TransactionController(private val ledgerService: LedgerService) {

    @PutMapping
    fun getOrCreatePendingTransaction(@RequestBody request: SimplePendingTransactionRequest): Transaction = run {
        ledgerService.createPendingTransaction(request)
    }

    @PutMapping("/{transaction-id}/settle")
    fun settleTransaction(@PathVariable("transaction-id") transactionId: String) = run {
        ledgerService.settleTransaction(transactionId)
    }

    @PutMapping("/{transaction-id}/reverse")
    fun reverseTransaction(@PathVariable("transaction-id") transactionId: String, @RequestBody request: SimpleReversePendingTransactionRequest) = run {
        ledgerService.reversePendingTransaction(transactionId, request)
    }
}