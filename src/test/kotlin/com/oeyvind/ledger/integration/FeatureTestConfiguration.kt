package com.oeyvind.ledger.integration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import

@TestConfiguration
@Import(
    PostgresTestConfiguration::class,
)
class FeatureTestConfiguration
