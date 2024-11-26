package com.oeyvind.ledger.model.repository

import com.oeyvind.ledger.model.entity.Account
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AccountRepository : JpaRepository<Account, UUID> {
    fun findByCode(code: String): Account?
}