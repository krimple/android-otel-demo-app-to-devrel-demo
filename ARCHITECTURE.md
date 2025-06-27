# Architecture Overview

This document describes the architecture of the Android OpenTelemetry Demo Application - an astronomy equipment e-commerce shop that showcases comprehensive observability patterns in mobile applications.

## High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Android App   │────│   Honeycomb     │    │  OTel Collector │
│                 │    │                 │    │   (Optional)    │
│ - UI (Compose)  │    │ - RUM SDK       │    │                 │
│ - ViewModels    │    │ - Trace Storage │    │ - OTLP Receiver │
│ - API Clients   │    │ - Analytics     │    │ - Debug Export  │
│ - Manual Spans  │    │ - Dashboards    │    │ - Local Dev     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         └───────────────────────┴───────────────────────┘
                                 │
                       ┌─────────────────┐
                       │  Backend APIs   │
                       │                 │
                       │ - Product API   │
                       │ - Currency API  │
                       │ - Shipping API  │
                       │ - Checkout API  │
                       └─────────────────┘
```

## Application Architecture

### MVVM Pattern with Jetpack Compose

The app follows Model-View-ViewModel (MVVM) architecture:

```
┌─────────────────┐
│      View       │  ← Jetpack Compose UI
│   (Composables) │
└─────────┬───────┘
          │
┌─────────▼───────┐
│   ViewModel     │  ← Business Logic & State Management
│                 │
│ - StateFlow     │
│ - Coroutines    │
│ - Telemetry     │
└─────────┬───────┘
          │
┌─────────▼───────┐
│     Model       │  ← Data Layer
│                 │
│ - API Services  │
│ - Data Models   │
│ - Repository    │
└─────────────────┘
```

### Key Architectural Components

#### 1. Application Layer
- **`OtelDemoApplication`**: Application entry point, initializes OpenTelemetry SDK
- **`MainActivity`**: Main entry point, launches shopping flow
- **`SessionId`**: Global session management

#### 2. UI Layer (Jetpack Compose)
- **Navigation**: Declarative navigation with `Navigation.kt`
- **Screens**: 
  - `ProductList` - Product catalog browsing
  - `ProductDetails` - Individual product information
  - `Cart` - Shopping cart management
  - `CheckoutInfo` - Purchase flow
  - `CheckoutConfirmation` - Order completion

#### 3. ViewModel Layer
- **`ProductListViewModel`**: Manages product catalog with lifecycle-aware loading and currency support
- **`ProductDetailViewModel`**: Handles individual product data with currency conversion
- **`CartViewModel`**: Shopping cart state and operations
- **`CheckoutInfoViewModel`**: Purchase flow management with shipping cost calculation
- **`CurrencyViewModel`**: Currency selection and management

#### 4. Service Layer
- **`ProductApiService`**: Product catalog API interactions with currency parameter support
- **`CheckoutApiService`**: Order processing and payment with currency and shipping support
- **`CurrencyApiService`**: Available currencies fetching with comprehensive telemetry
- **`ShippingApiService`**: Shipping cost calculation via checkout preview API
- **`FetchHelpers`**: Shared HTTP request utilities with telemetry integration

#### 5. Model Layer
- **`Product`**: Product data model with currency-aware pricing
- **`CheckoutModels`**: Order and payment models with currency and shipping support
- **`Money`**: Multi-currency support with localized formatting
- **`Currency`**: Currency data model for 33+ supported currencies
- **`ShippingInfo`**: Shipping address and cost calculation models

## OpenTelemetry Integration

### Telemetry Stack
- **Honeycomb Android SDK (0.0.11)**: Primary RUM (Real User Monitoring) backend
- **OpenTelemetry Android SDK (0.11.0-alpha)**: Core instrumentation framework
- **OpenTelemetry Core (1.49.0)**: Telemetry APIs and context management
- **Auto-Instrumentation**: Activity lifecycle, crashes, ANRs, slow renders, HTTP requests (OkHttp)
- **Manual Instrumentation**: Business logic spans, user interactions, API boundaries
- **ByteBuddy**: Runtime instrumentation for OkHttp auto-instrumentation

### Instrumentation Patterns

#### 1. Automatic Instrumentation
- **Activity Lifecycle**: App start, screen transitions, backgrounding
- **Network Requests**: HTTP client auto-instrumentation via OkHttp
- **Performance**: Slow renders, ANRs, crash detection

#### 2. Manual Instrumentation
- **Business Operations**: Cart operations, checkout flow, product loading
- **User Interactions**: Button clicks, navigation events
- **API Boundaries**: Service calls with proper context propagation

#### 3. Span Hierarchy Design
```
User Session (Root)
├── Activity Lifecycle Spans (Auto)
├── User Interaction Spans (Manual Root)
│   ├── ViewModel Operations (Manual)
│   │   └── API Calls (Manual with context)
│   │       └── HTTP Requests (Auto)
│   └── Navigation Events (Manual)
└── Background Operations (Manual)
```

### Context Propagation Strategy

#### Context Management Principles
- **Root Spans**: User interactions, UI events, standalone operations that should start new traces
- **Child Spans**: API calls, background operations triggered by user actions that should inherit context
- **Context Isolation**: Clean coroutine launches without context propagation for isolated operations
- **Context Cleanup**: Proper use of `makeCurrent().use { }` blocks for automatic context restoration

#### Critical Context Management Patterns

**✅ Correct Pattern - Isolated Operations:**
```kotlin
// Clean coroutine launch for independent operations
coroutineScope.launch {
    placeOrder(...)  // Starts its own trace context
}
```

**❌ Incorrect Pattern - Context Leakage:**
```kotlin
// AVOID: Propagates ambient context to coroutine
coroutineScope.launch(Context.current().asContextElement()) {
    placeOrder(...)  // Inherits and contaminates trace context
}
```

**✅ Correct Pattern - Context Scoping:**
```kotlin
// Proper span context management with automatic cleanup
span?.makeCurrent()?.use {
    // Operation runs with span as current context
    // Context automatically restored when block exits
}
```

#### Context Leak Prevention
- **Coroutine Isolation**: Use `launch { }` without context elements for independent operations
- **Span Lifecycle**: Both `makeCurrent().use { }` for context management AND `span.end()` for span cleanup
- **Background Operations**: Explicit parent context passing rather than ambient context capture

#### Resolved Issues: Trace Accumulation Fix

**Problem Identified**: Trace `349eff370100498d2fc05fd58be5c903` contained 152 spans instead of the expected 3-5 Android spans, indicating context leakage where checkout operations contaminated subsequent user interactions.

**Root Cause**: In `AstronomyShopActivity.kt`, the checkout operation was launched with:
```kotlin
coroutineScope.launch(Context.current().asContextElement()) {
    placeOrder(...)
}
```

This pattern captured the ambient OpenTelemetry context and propagated it to the coroutine, causing all subsequent operations in that scope to inherit the checkout trace context.

**Solution Applied**: Changed to clean coroutine launch:
```kotlin
coroutineScope.launch {
    placeOrder(...)
}
```

**Results**: 
- Trace `0a568b117ce51d6e2afc1a545425e884` now contains only 3 Android spans as expected
- Each checkout operation creates an isolated trace that doesn't contaminate future operations
- Proper trace boundaries maintained between different user interactions

**Key Learning**: The `use` block from `makeCurrent()` only handles context restoration, not span ending. Both context management (`makeCurrent().use{}`) and span lifecycle management (`span.end()`) are required for proper cleanup.

### Resource Management with Use Blocks

The application employs Kotlin's `use` blocks for automatic resource management, particularly for OpenTelemetry spans:

#### Pattern Implementation
```kotlin
// Automatic span lifecycle management
tracer.spanBuilder("operation-name")
    .startSpan()
    .use { span ->
        // Span is automatically closed when block exits
        span.setAttribute("key", "value")
        span.setStatus(StatusCode.OK)
        // Perform operation
        return result
    }
```

#### Benefits
- **Automatic Cleanup**: Spans are guaranteed to be closed even if exceptions occur
- **Exception Safety**: Proper span status setting on both success and failure paths
- **Resource Efficiency**: Prevents span leaks and ensures proper telemetry data
- **Code Clarity**: Clear scope boundaries for telemetry operations

#### Usage Patterns
- **API Service Calls**: All HTTP operations wrapped in use blocks for span management
- **ViewModel Operations**: Business logic operations with proper span lifecycle
- **Background Tasks**: Long-running operations with guaranteed span closure
- **Error Handling**: Automatic span status setting on exceptions

#### Implementation Examples
```kotlin
// In API services
suspend fun fetchProducts(): List<Product> {
    return tracer.spanBuilder("fetchProducts")
        .startSpan()
        .use { span ->
            span.setAttribute("operation", "product_fetch")
            // HTTP call automatically instrumented
            apiClient.getProducts()
        }
}

// In ViewModels
fun loadData() {
    tracer.spanBuilder("loadData")
        .startSpan()
        .use { span ->
            span.setAttribute("user_action", "data_load")
            try {
                // Business logic
                span.setStatus(StatusCode.OK)
            } catch (e: Exception) {
                span.setStatus(StatusCode.ERROR, e.message)
                throw e
            }
        }
}
```

This pattern ensures robust telemetry collection while maintaining clean, readable code that automatically handles resource cleanup.

## Multi-Currency and Shipping Architecture

### Currency Support System
The application implements comprehensive multi-currency support with real-time pricing and localized formatting:

#### Currency Management
- **33+ Supported Currencies**: Fetched dynamically from `/api/currency` endpoint
- **Persistent Selection**: User currency preference stored in SharedPreferences with USD default
- **Real-time Conversion**: All product APIs support `?currencyCode=` parameter for dynamic pricing
- **Localized Display**: Major currencies (USD, EUR, GBP, JPY) show proper symbols; others use "CODE amount" format

#### UI Components
- **CurrencyToggle**: Quick switcher for common currencies in product list header
- **CurrencyBottomSheet**: Full currency picker with search functionality
- **Consistent Formatting**: `Money.formatCurrency()` extension handles all display logic

#### Architecture Integration
- **CurrencyApiService**: Fetches available currencies with full OpenTelemetry instrumentation
- **CurrencyViewModel**: Manages currency state, persistence, and loading with error handling
- **Enhanced ViewModels**: ProductListViewModel and ProductDetailViewModel updated for currency support

### Shipping Cost Calculation
The application calculates shipping costs using a preview-based approach:

#### Calculation Method
- **Preview API**: Uses checkout API with preview flag to calculate shipping without placing orders
- **Real-time Updates**: Triggered automatically when shipping information becomes complete
- **Currency Integration**: Shipping costs calculated and displayed in selected currency
- **Graceful Degradation**: Fallback handling when shipping API unavailable

#### Implementation Components
- **ShippingApiService**: Calculates costs via checkout API preview mechanism
- **CheckoutInfoViewModel**: Manages shipping state, loading, and cost display
- **Client-side Cart**: Mobile-optimized cart state maintained on device vs server sessions

### Comprehensive Telemetry Integration
All currency and shipping operations include detailed OpenTelemetry instrumentation:

#### Currency Telemetry
- **Product Loading**: `currency.code` attribute on all product-related spans
- **Currency Fetching**: `currencies.count` attribute when loading available currencies
- **User Actions**: Currency selection events tracked as discrete user interactions

#### Shipping Telemetry
- **Cost Calculation**: `shipping.cost.value` and `shipping.cost.currency` attributes
- **Method Tracking**: `shipping.calculation.method` (preview vs actual) differentiation
- **Performance Monitoring**: Shipping calculation timing and error rate tracking

#### Order Telemetry
- **Complete Context**: Currency and shipping data included in all order-related spans
- **Order Totals**: Currency-aware order totals and tax calculations
- **Business Metrics**: Order value, currency distribution, and shipping cost analytics

## Data Flow

### Product Loading Flow
```
UI Compose
    ↓ LaunchedEffect
ProductListViewModel.refreshProducts()
    ↓ First load: isRefresh=false | Subsequent: isRefresh=true
ProductListViewModel.loadProducts()
    ↓ With telemetry span + currency.code attribute
ProductApiService.fetchProducts(currencyCode)
    ↓ HTTP request with ?currencyCode= parameter
Backend API
    ↓ Currency-converted pricing response
UI State Update with localized formatting
```

### Shopping Cart Flow
```
Product Detail UI
    ↓ User tap "Add to Cart"
CartViewModel.addProduct()
    ↓ Create root span (user action)
Cart State Update
    ↓ Telemetry attributes
UI Recomposition
```

### Checkout Flow
```
Cart UI → CheckoutInfo UI → Shipping Calculation → Payment Processing → Confirmation
    ↓           ↓                    ↓                      ↓               ↓
CartViewModel → CheckoutViewModel → ShippingApiService → CheckoutApiService → UI Update
    ↓           ↓                    ↓                      ↓               ↓
  Telemetry   Form + Currency    Shipping Span        Payment + Currency  Success Event
```

### Currency Selection Flow
```
Product List UI
    ↓ User taps currency toggle
CurrencyViewModel.selectCurrency()
    ↓ Persistence + telemetry event
SharedPreferences.save()
    ↓ State propagation
ProductListViewModel.loadProducts(newCurrency)
    ↓ Real-time price updates
UI Recomposition with new prices
```

## Performance Considerations

### ViewModel Lifecycle Management
- **Smart Loading**: Prevents duplicate API calls on screen transitions
- **State Preservation**: ViewModels survive configuration changes
- **Memory Management**: Proper coroutine scope handling

### Telemetry Performance
- **Minimal Overhead**: Strategic span creation for business value
- **Async Processing**: Non-blocking telemetry operations
- **Context Efficiency**: Careful context propagation to avoid memory leaks

### UI Performance
- **Jetpack Compose**: Declarative UI with efficient recomposition
- **State Management**: Optimized StateFlow usage
- **Image Loading**: Efficient asset handling

## Testing Strategy

### Unit Tests
- **ViewModel Logic**: Business logic and state management
- **API Services**: Network interactions and error handling
- **Telemetry**: Span creation and context propagation

### Test Coverage
- ProductListViewModel: Lifecycle behavior and API integration
- ProductDetailViewModel: Product loading and recommendations
- CheckoutApiService: Payment processing and error scenarios
- FetchHelpers: HTTP utilities and telemetry integration

## Build and Development

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM with StateFlow
- **Async**: Coroutines + Flow
- **Build**: Gradle with Kotlin DSL
- **Testing**: JUnit + Mockito + Robolectric

### Configuration
- **Environments**: Debug/Release with configurable API and telemetry endpoints
- **API Keys**: `otel.properties` file for Honeycomb configuration (HONEYCOMB_API_KEY, SERVICE_NAME, etc.)
- **Backend APIs**: Configurable base URL (local: `http://10.0.2.2:9191`, production: `https://www.zurelia.honeydemo.io`)
- **Network**: OkHttp with ByteBuddy auto-instrumentation
- **Telemetry**: Direct Honeycomb integration via Android SDK
- **Local Development**: Optional OTel Collector with Docker Compose for debugging

### API Endpoints
The Android app integrates with the following REST API endpoints:
- **GET /api/currency** - Fetch available currencies (33+ supported)
- **GET /api/products?currencyCode={code}** - Product catalog with currency-converted pricing
- **GET /api/products/{id}?currencyCode={code}** - Individual product with currency pricing
- **POST /api/checkout?currencyCode={code}** - Order placement with currency and shipping
- **GET /images/products/{picture}** - Product images served relative to API base URL

### Local Development Infrastructure
Optional Docker Compose setup provides:
- **OTel Collector**: OTLP receiver (gRPC:4317, HTTP:4318) with debug export to Honeycomb
- **Environment Variables**: `HONEYCOMB_API_KEY` for collector configuration

This architecture provides a production-ready example of observability patterns in Android applications while maintaining clean separation of concerns and testability.