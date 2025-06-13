# Architecture Overview

This document describes the architecture of the Android OpenTelemetry Demo Application - an astronomy equipment e-commerce shop that showcases comprehensive observability patterns in mobile applications.

## High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Android App   │────│  OTel Collector │────│   Honeycomb     │
│                 │    │                 │    │                 │
│ - UI (Compose)  │    │ - OTLP Receiver │    │ - Trace Storage │
│ - ViewModels    │    │ - Processing    │    │ - Analytics     │
│ - API Clients   │    │ - Export        │    │ - Dashboards    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │
         │                       │
         └───────────────────────┴──────────────────┐
                                                    │
                                          ┌─────────────────┐
                                          │     Jaeger      │
                                          │                 │
                                          │ - Local Traces  │
                                          │ - Debug UI      │
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
- **`ProductListViewModel`**: Manages product catalog with lifecycle-aware loading
- **`ProductDetailViewModel`**: Handles individual product data and recommendations
- **`CartViewModel`**: Shopping cart state and operations
- **`CheckoutInfoViewModel`**: Purchase flow management

#### 4. Service Layer
- **`ProductApiService`**: Product catalog API interactions
- **`CheckoutApiService`**: Order processing and payment
- **`RecommendationService`**: Product recommendations
- **`FetchHelpers`**: Shared HTTP request utilities

#### 5. Model Layer
- **`Product`**: Product data model
- **`CheckoutModels`**: Order and payment models
- **`PriceUsd`**: Currency handling

## OpenTelemetry Integration

### Telemetry Stack
- **Honeycomb Android SDK**: RUM (Real User Monitoring)
- **OpenTelemetry Android SDK**: Core instrumentation
- **Auto-Instrumentation**: Lifecycle, crashes, ANRs, network requests
- **Manual Instrumentation**: Business logic spans and events

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
- **Root Spans**: User interactions, UI events, standalone operations
- **Child Spans**: API calls, background operations triggered by user actions
- **Context Passing**: Explicit `Context.current()` usage in service calls

## Data Flow

### Product Loading Flow
```
UI Compose
    ↓ LaunchedEffect
ProductListViewModel.refreshProducts()
    ↓ First load: isRefresh=false | Subsequent: isRefresh=true
ProductListViewModel.loadProducts()
    ↓ With telemetry span
ProductApiService.fetchProducts()
    ↓ HTTP request with context
Backend API
    ↓ Response
UI State Update
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
Cart UI → CheckoutInfo UI → Payment Processing → Confirmation
    ↓           ↓                    ↓               ↓
CartViewModel → CheckoutViewModel → CheckoutApiService → UI Update
    ↓           ↓                    ↓               ↓
  Telemetry   Form State         Payment Span    Success Event
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
- **Environments**: Debug/Release with different telemetry endpoints
- **API Keys**: Local properties for Honeycomb configuration
- **Network**: OkHttp with auto-instrumentation
- **Collector**: Local OTLP endpoint for development

This architecture provides a production-ready example of observability patterns in Android applications while maintaining clean separation of concerns and testability.