package com.oeyvind.ledger.model.entity

import com.oeyvind.ledger.model.Ledger
import com.oeyvind.ledger.model.TransactionEntryType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.*


@Entity
@Table(name = "pending_transaction_entries")
class PendingTransactionEntry(
    @Id override val id: UUID = UUID.randomUUID(),
    @Column(name = "created_at") val createdAt: Instant,
    @ManyToOne @JoinColumn(name = "account_id") val account: Account,
    @ManyToOne @JoinColumn(name = "transaction_id") val transaction: Transaction,
    @ManyToOne @JoinColumn(name = "main_transaction_id") val mainTransaction: Transaction,
    @Column(name = "amount_signed") override val amountSigned: BigDecimal,
    @Column(name = "sequence") val sequence: Int,
    @Column(name = "preceding_entry_id") val precedingEntryId: UUID?,
    @Enumerated @Column(name = "type") val type: TransactionEntryType,
    @Enumerated @Column(name = "ledger") val ledger: Ledger,
    @OneToOne @JoinColumn(name = "reverses_entry_id") val reverses: PendingTransactionEntry? = null,
) : TransactionEntry