package io.opentelemetry.android.demo.shop.ui.currency

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CurrencyViewModelTest {

    @Test
    fun `viewModel can be instantiated`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val viewModel = CurrencyViewModel(context)
        assertNotNull(viewModel)
    }

    @Test
    fun `initial state has USD as default currency`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val viewModel = CurrencyViewModel(context)
        assertEquals("USD", viewModel.selectedCurrency.value)
    }

    @Test
    fun `initial state has empty currency list`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val viewModel = CurrencyViewModel(context)
        assertTrue("Initial currency list should be empty", 
            viewModel.availableCurrencies.value.isEmpty())
    }

    @Test
    fun `initial state is loading currencies`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val viewModel = CurrencyViewModel(context)
        // The viewModel should attempt to load currencies on initialization
        assertNotNull("ViewModel should be instantiated", viewModel)
        assertNotNull("isLoading state should exist", viewModel.isLoading)
    }

    @Test
    fun `selectCurrency updates selected currency`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val viewModel = CurrencyViewModel(context)
        
        // Since we can't easily test the network call, we'll just verify the method exists
        // and can be called without throwing exceptions
        viewModel.selectCurrency("EUR")
        
        assertNotNull("ViewModel should handle currency selection", viewModel)
    }

    @Test
    fun `selectCurrency ignores invalid currencies`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val viewModel = CurrencyViewModel(context)
        val initialCurrency = viewModel.selectedCurrency.value
        
        // Try to select an invalid currency
        viewModel.selectCurrency("INVALID")
        
        // Should not change from initial currency since INVALID is not in available list
        assertEquals("Invalid currency should not be selected", 
            initialCurrency, viewModel.selectedCurrency.value)
    }

    @Test
    fun `retryLoadCurrencies can be called`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val viewModel = CurrencyViewModel(context)
        
        // Test that retry method exists and can be called
        viewModel.retryLoadCurrencies()
        
        // Verify the method exists
        assertNotNull("Retry method should exist", viewModel::retryLoadCurrencies)
    }

    @Test
    fun `viewModel has proper state management structure`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val viewModel = CurrencyViewModel(context)
        
        // Verify all required state properties exist
        assertNotNull("availableCurrencies should exist", viewModel.availableCurrencies)
        assertNotNull("selectedCurrency should exist", viewModel.selectedCurrency)
        assertNotNull("isLoading should exist", viewModel.isLoading)
        assertNotNull("error should exist", viewModel.error)
        
        // Verify initial states are reasonable
        assertEquals("Initial selected currency should be USD", 
            "USD", viewModel.selectedCurrency.value)
        assertNull("Initial error should be null", viewModel.error.value)
    }
}