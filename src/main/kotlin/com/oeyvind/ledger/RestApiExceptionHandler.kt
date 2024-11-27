package com.oeyvind.ledger

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice(basePackages = ["com.oeyvind.ledger"])
class RestApiExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException, webRequest: WebRequest): ProblemDetail = run {
        super.createProblemDetail(
            e,
            HttpStatus.BAD_REQUEST,
            e.message ?: "Unexpected error occurred",
            null,
            emptyArray(),
            webRequest
        )
    }
}
