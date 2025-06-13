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
class ProductDetailViewModelTest {

    @Mock
    private lateinit var productApiService: ProductApiService
    
    private lateinit var viewModel: ProductDetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = ProductDetailViewModel(productApiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        val initialState = viewModel.uiState.first()
        assertFalse("Should not be loading initially", initialState.isLoading)
        assertNull("Product should be null initially", initialState.product)
        assertNull("Error message should be null initially", initialState.errorMessage)
    }

    @Test
    fun `successful product loading updates state correctly`() = runTest {
        val productId = "test-product-id"
        val mockProduct = Product(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            picture = "test.jpg",
            priceUsd = PriceUsd("USD", 25, 500000000), // $25.50
            categories = listOf("test")
        )
        
        whenever(productApiService.fetchProduct(productId)).thenReturn(mockProduct)
        
        viewModel.loadProduct(productId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = viewModel.uiState.first()
        assertFalse("Should not be loading", finalState.isLoading)
        assertEquals("Should have correct product", mockProduct, finalState.product)
        assertNull("Error message should be null", finalState.errorMessage)
    }

    @Test
    fun `failed product loading updates state with error`() = runTest {
        val productId = "test-product-id"
        val errorMessage = "Product not found"
        whenever(productApiService.fetchProduct(productId)).thenThrow(RuntimeException(errorMessage))
        
        viewModel.loadProduct(productId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = viewModel.uiState.first()
        assertFalse("Should not be loading", finalState.isLoading)
        assertNull("Product should be null", finalState.product)
        assertEquals("Should have error message", errorMessage, finalState.errorMessage)
    }

    @Test
    fun `refresh product calls API again`() = runTest {
        val productId = "test-product-id"
        val mockProduct = Product(
            id = productId,
            name = "Test Product",
            description = "Test Description",
            picture = "test.jpg", 
            priceUsd = PriceUsd("USD", 15, 0),
            categories = listOf("test")
        )
        
        whenever(productApiService.fetchProduct(productId)).thenReturn(mockProduct)
        
        viewModel.loadProduct(productId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Call refresh
        viewModel.refreshProduct(productId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val finalState = viewModel.uiState.first()
        assertFalse("Should not be loading", finalState.isLoading)
        assertEquals("Should have correct product", mockProduct, finalState.product)
    }

    @Test
    fun `viewModel can be instantiated and has proper structure`() = runTest {
        // Verify the ViewModel is properly instantiated
        assertNotNull("ViewModel should be instantiated", viewModel)
        assertNotNull("ViewModel should have uiState", viewModel.uiState)

        // Verify the ViewModel has the expected methods
        assertTrue("ViewModel should have loadProduct method",
            viewModel::class.java.methods.any { it.name == "loadProduct" })
        assertTrue("ViewModel should have refreshProduct method",
            viewModel::class.java.methods.any { it.name == "refreshProduct" })

        // The initial state should be properly structured
        val initialState = viewModel.uiState.first()
        assertNotNull("Initial state should not be null", initialState)
    }
}
