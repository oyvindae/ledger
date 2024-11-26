package com.oeyvind.ledger.model.repository

import com.oeyvind.ledger.model.entity.Banana
import org.springframework.data.jpa.repository.JpaRepository

interface BananaRepository : JpaRepository<Banana, Long>{
}