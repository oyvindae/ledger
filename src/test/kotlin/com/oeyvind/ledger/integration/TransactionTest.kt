package com.oeyvind.ledger.integration

import com.oeyvind.ledger.controller.CreateAccountRequest
import com.oeyvind.ledger.controller.LedgerBalance
import com.oeyvind.ledger.controller.SearchResult
import com.oeyvind.ledger.controller.TransactionResponse
import com.oeyvind.ledger.model.Ledger
import com.oeyvind.ledger.model.TransactionEntryType
import com.oeyvind.ledger.model.entity.Transaction
import com.oeyvind.ledger.service.model.SimplePendingTransactionRequest
import com.oeyvind.ledger.service.model.SimpleReversePendingTransactionRequest
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import java.math.BigDecimal
import java.net.URI

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(FeatureTestConfiguration::class)
@AutoConfigureMockMvc
class TransactionTest(
    @Autowired private val mvc: MockMvc,
    @Autowired private val testRestTemplate: TestRestTemplate,
) {

    @Test
    fun `test pending transactions, reversals, settlements and balances`() {
        testRestTemplate.put("/accounts", CreateAccountRequest("stripe", "Stripe", Ledger.GATEWAY))
        testRestTemplate.put("/accounts", CreateAccountRequest("oeyvind", "Oeyvind", Ledger.CUSTOMER))
        testRestTemplate.put("/accounts", CreateAccountRequest("pierre", "Pierre", Ledger.CUSTOMER))

        testRestTemplate.put(
            "/transactions",
            SimplePendingTransactionRequest("tx-1", BigDecimal(100), "stripe", "oeyvind", TransactionEntryType.PAYMENT)
        )
        testRestTemplate.put("/transactions/tx-1/settle", null)

        testRestTemplate.getForEntity("/accounts/oeyvind/balance", LedgerBalance::class.java).body.shouldNotBeNull {
            settledCredit.intValueExact() shouldBe 100
            settledDebit.intValueExact() shouldBe 0
            pendingDebit.intValueExact() shouldBe 0
            availableBalance.intValueExact() shouldBe 100
        }

        testRestTemplate.put(
            "/transactions",
            SimplePendingTransactionRequest("tx-2", BigDecimal(40), "oeyvind", "pierre", TransactionEntryType.PAYMENT)
        )
        testRestTemplate.getForEntity("/accounts/oeyvind/balance", LedgerBalance::class.java).body.shouldNotBeNull {
            settledCredit.intValueExact() shouldBe 100
            settledDebit.intValueExact() shouldBe 0
            pendingDebit.intValueExact() shouldBe 40
            availableBalance.intValueExact() shouldBe 60
        }
        testRestTemplate.put(
            "/transactions/tx-2/reverse",
            SimpleReversePendingTransactionRequest("r-tx-2", BigDecimal(30))
        )
        testRestTemplate.getForEntity("/accounts/oeyvind/balance", LedgerBalance::class.java).body.shouldNotBeNull {
            settledCredit.intValueExact() shouldBe 100
            settledDebit.intValueExact() shouldBe 0
            pendingDebit.intValueExact() shouldBe 10
            availableBalance.intValueExact() shouldBe 90
        }

        testRestTemplate.put(
            "/transactions",
            SimplePendingTransactionRequest("tx-3", BigDecimal(40), "oeyvind", "pierre", TransactionEntryType.PAYMENT)
        )
        testRestTemplate.getForEntity("/accounts/oeyvind/balance", LedgerBalance::class.java).body.shouldNotBeNull {
            settledCredit.intValueExact() shouldBe 100
            settledDebit.intValueExact() shouldBe 0
            pendingDebit.intValueExact() shouldBe 50
            availableBalance.intValueExact() shouldBe 50
        }

        testRestTemplate.put(
            "/transactions/tx-3/reverse",
            SimpleReversePendingTransactionRequest("r-tx-3", BigDecimal(15))
        )
        testRestTemplate.getForEntity("/accounts/oeyvind/balance", LedgerBalance::class.java).body.shouldNotBeNull {
            settledCredit.intValueExact() shouldBe 100
            settledDebit.intValueExact() shouldBe 0
            pendingDebit.intValueExact() shouldBe 35
            availableBalance.intValueExact() shouldBe 65
        }

        testRestTemplate.put("/transactions/tx-2/settle", null)
        testRestTemplate.getForEntity("/accounts/oeyvind/balance", LedgerBalance::class.java).body.shouldNotBeNull {
            settledCredit.intValueExact() shouldBe 100
            settledDebit.intValueExact() shouldBe 10
            pendingDebit.intValueExact() shouldBe 25
            availableBalance.intValueExact() shouldBe 65
        }

        testRestTemplate.put("/transactions/tx-3/settle", null)
        testRestTemplate.getForEntity("/accounts/oeyvind/balance", LedgerBalance::class.java).body.shouldNotBeNull {
            settledCredit.intValueExact() shouldBe 100
            settledDebit.intValueExact() shouldBe 35
            pendingDebit.intValueExact() shouldBe 0
            availableBalance.intValueExact() shouldBe 65
        }

        testRestTemplate.getForEntity("/accounts/oeyvind/transactions", SearchResult::class.java).body.shouldNotBeNull {
            data shouldBe listOf(
                TransactionResponse("tx-1", true),
                TransactionResponse("tx-2", true),
                TransactionResponse("tx-3", true)
            )
        }
    }

    @Test
    fun `test creating pending transaction twice`() {
        testRestTemplate.put("/accounts", CreateAccountRequest("stripe", "Stripe", Ledger.GATEWAY))
        testRestTemplate.put("/accounts", CreateAccountRequest("oeyvind", "Oeyvind", Ledger.CUSTOMER))
        testRestTemplate.put("/accounts", CreateAccountRequest("pierre", "Pierre", Ledger.CUSTOMER))

        val requestEntity = RequestEntity(
            SimplePendingTransactionRequest(
                "tx-1",
                BigDecimal(100),
                "stripe",
                "oeyvind",
                TransactionEntryType.PAYMENT
            ), HttpMethod.PUT, URI("/transactions")
        )

        // Attempt creating transaction twice
        testRestTemplate.exchange<Transaction>(requestEntity).run {
            statusCode shouldBe HttpStatus.OK
            body?.externalTransactionId shouldBe "tx-1"
        }
        testRestTemplate.exchange<Transaction>(requestEntity).run {
            statusCode shouldBe HttpStatus.OK
            body?.externalTransactionId shouldBe "tx-1"
        }

        // Attempt settlement twice
        testRestTemplate.exchange<Transaction>(RequestEntity(null, HttpMethod.PUT, URI("/transactions/tx-1/settle")))
            .run {
                statusCode shouldBe HttpStatus.OK
                body?.externalTransactionId shouldBe "tx-1"
            }
        testRestTemplate.exchange<Transaction>(RequestEntity(null, HttpMethod.PUT, URI("/transactions/tx-1/settle")))
            .run {
                statusCode shouldBe HttpStatus.OK
                body?.externalTransactionId shouldBe "tx-1"
            }

        testRestTemplate.getForEntity("/accounts/oeyvind/balance", LedgerBalance::class.java).body.shouldNotBeNull {
            settledCredit.intValueExact() shouldBe 100
            settledDebit.intValueExact() shouldBe 0
            pendingDebit.intValueExact() shouldBe 0
            availableBalance.intValueExact() shouldBe 100
        }
    }
}