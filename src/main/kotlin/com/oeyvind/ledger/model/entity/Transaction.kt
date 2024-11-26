package com.oeyvind.ledger.model.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.*


@Entity
@Table(name = "transactions")
class Transaction(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "created_at") val createdAt: Instant,
    @Version var version: Int = 0,
    @Column(name = "reversal") val reversal: Boolean,
    @Column(name = "settled") var settled: Boolean,
    @Column(name = "external_transaction_id") val externalTransactionId: String,
    @ManyToOne @JoinColumn(name = "parent_transaction_id") val parentTransaction: Transaction? = null,
) {

}