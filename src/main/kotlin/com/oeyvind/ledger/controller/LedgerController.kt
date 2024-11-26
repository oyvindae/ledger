package com.oeyvind.ledger.controller

import com.oeyvind.ledger.model.entity.Banana
import com.oeyvind.ledger.model.repository.BananaRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LedgerController(private val bananaRepository: BananaRepository) {

    @GetMapping("/test")
    fun test(@RequestParam("title") title: String = "test") = run {
        bananaRepository.save(Banana(title = title))
        title
    }

//    @PostMapping()
}