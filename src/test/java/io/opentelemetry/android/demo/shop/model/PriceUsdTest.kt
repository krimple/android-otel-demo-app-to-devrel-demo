package io.opentelemetry.android.demo.shop.model

import org.junit.Test
import org.junit.Assert.*

class PriceUsdTest {

    @Test
    fun `PriceUsd can be instantiated`() {
        val price = PriceUsd("USD", 10, 500000000)
        assertNotNull(price)
        assertEquals("USD", price.currencyCode)
        assertEquals(10L, price.units)
        assertEquals(500000000L, price.nanos)
    }

    @Test
    fun `formatCurrency displays USD correctly`() {
        val price = PriceUsd("USD", 25, 990000000)
        val formatted = price.formatCurrency()
        assertEquals("$25.99", formatted)
    }

    @Test
    fun `formatCurrency displays EUR correctly`() {
        val price = PriceUsd("EUR", 30, 500000000)
        val formatted = price.formatCurrency()
        assertEquals("€30.50", formatted)
    }

    @Test
    fun `formatCurrency displays GBP correctly`() {
        val price = PriceUsd("GBP", 22, 750000000)
        val formatted = price.formatCurrency()
        assertEquals("£22.75", formatted)
    }

    @Test
    fun `formatCurrency displays JPY correctly without decimals`() {
        val price = PriceUsd("JPY", 1500, 0)
        val formatted = price.formatCurrency()
        assertEquals("¥1500", formatted)
    }

    @Test
    fun `formatCurrency displays unknown currency with code`() {
        val price = PriceUsd("CAD", 35, 250000000)
        val formatted = price.formatCurrency()
        assertEquals("CAD 35.25", formatted)
    }

    @Test
    fun `formatCurrency handles zero values`() {
        val price = PriceUsd("USD", 0, 0)
        val formatted = price.formatCurrency()
        assertEquals("$0.00", formatted)
    }

    @Test
    fun `formatCurrency handles large values`() {
        val price = PriceUsd("USD", 1000, 0)
        val formatted = price.formatCurrency()
        assertEquals("$1000.00", formatted)
    }

    @Test
    fun `formatCurrency handles nanos conversion correctly`() {
        val price = PriceUsd("USD", 0, 100000000) // 0.1 units
        val formatted = price.formatCurrency()
        assertEquals("$0.10", formatted)
    }

    @Test
    fun `formatCurrency handles complex nanos conversion`() {
        val price = PriceUsd("USD", 10, 123456789) // 10.123456789 units
        val formatted = price.formatCurrency()
        assertEquals("$10.12", formatted) // Should round to 2 decimal places
    }
}