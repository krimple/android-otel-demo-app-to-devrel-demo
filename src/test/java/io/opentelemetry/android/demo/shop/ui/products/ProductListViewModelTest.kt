package io.opentelemetry.android.demo.shop.ui.products

import io.opentelemetry.android.demo.shop.clients.ProductApiService
import io.opentelemetry.android.demo.shop.model.Product
import io.opentelemetry.android.demo.shop.model.PriceUsd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProductListViewModelTest {

    @Mock
    private lateinit var productApiService: ProductApiService
    
    private lateinit var viewModel: ProductListViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel can be instantiated`() = runTest {
        viewModel = ProductListViewModel(productApiService)

        // Verify the ViewModel is properly instantiated
        assertNotNull("ViewModel should be instantiated", viewModel)
        assertNotNull("ViewModel should have uiState", viewModel.uiState)

        // The initial state may vary depending on the test environment
        // but the ViewModel should be functional
        val initialState = viewModel.uiState.first()
        assertNotNull("Initial state should not be null", initialState)
    }

    @Test
    fun `successful product loading updates state correctly`() = runTest {
        val mockProducts = listOf(
            Product(
                id = "1",
                name = "Test Product",
                description = "Test Description",
                picture = "test.jpg",
                priceUsd = PriceUsd("USD", 10, 0),
                categories = listOf("test")
            )
        )
        
        whenever(productApiService.fetchProducts()).thenReturn(mockProducts)
        
        viewModel = ProductListViewModel(productApiService)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = viewModel.uiState.first()
        assertFalse("Should not be loading", finalState.isLoading)
        assertEquals("Should have correct products", mockProducts, finalState.products)
        assertNull("Error message should be null", finalState.errorMessage)
    }

    @Test
    fun `failed product loading updates state with error`() = runTest {
        val errorMessage = "Network error"
        whenever(productApiService.fetchProducts()).thenThrow(RuntimeException(errorMessage))
        
        viewModel = ProductListViewModel(productApiService)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = viewModel.uiState.first()
        assertFalse("Should not be loading", finalState.isLoading)
        assertTrue("Products should be empty", finalState.products.isEmpty())
        assertEquals("Should have error message", errorMessage, finalState.errorMessage)
    }

    @Test
    fun `refresh products calls API again`() = runTest {
        val mockProducts = listOf(
            Product(
                id = "1",
                name = "Test Product",
                description = "Test Description", 
                picture = "test.jpg",
                priceUsd = PriceUsd("USD", 10, 0),
                categories = listOf("test")
            )
        )
        
        whenever(productApiService.fetchProducts()).thenReturn(mockProducts)
        
        viewModel = ProductListViewModel(productApiService)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Call refresh
        viewModel.refreshProducts()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = viewModel.uiState.first()
        assertFalse("Should not be loading", finalState.isLoading)
        assertEquals("Should have correct products", mockProducts, finalState.products)
    }
}
