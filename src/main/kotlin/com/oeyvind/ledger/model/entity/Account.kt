package com.oeyvind.ledger.model.entity

import com.oeyvind.ledger.model.Ledger
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@Entity
@Table(name = "accounts")
class Account(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "created_at") val createdAt: Instant,
    @Version var version: Int = 0,
    @Column(name = "code") var code: String,
    @Column(name = "name") var name: String,
    @Enumerated @Column(name = "ledger") val ledger: Ledger,
    @Column(name = "pending_credit") var pendingCredit: BigDecimal = BigDecimal.ZERO,
    @Column(name = "pending_debit") var pendingDebit: BigDecimal = BigDecimal.ZERO,
    @Column(name = "settled_credit") var settledCredit: BigDecimal = BigDecimal.ZERO,
    @Column(name = "settled_debit") var settledDebit: BigDecimal = BigDecimal.ZERO,
) {
    fun settledBalance() = settledCredit - settledDebit

    fun availableBalance() = settledBalance() - pendingDebit
}