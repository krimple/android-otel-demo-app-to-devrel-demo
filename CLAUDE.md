# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android OpenTelemetry demonstration application showcasing observability features in mobile apps. The app simulates an astronomy equipment e-commerce shop with comprehensive telemetry instrumentation.

## Common Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Install debug version to connected device/emulator
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### Infrastructure Setup
```bash
# Start OpenTelemetry collector and Jaeger
docker compose build
docker compose up

# View telemetry data
# Jaeger UI: http://localhost:16686
# Collector logs: docker compose logs collector
```

### Required Configuration

Create `local.properties` with your Honeycomb API key:
```properties
rum.access.token=your_honeycomb_api_key
```

## Architecture Overview

### OpenTelemetry Integration
- **Application Entry**: `OtelDemoApplication.kt` initializes Honeycomb SDK
- **Service Name**: "android-otel-demo"
- **Dual Backends**: Local Jaeger + Honeycomb.io
- **Instrumentation Types**: Automatic (lifecycle, crashes, ANRs, slow renders) + Manual (custom spans/events)
- **Avoid** Do not use GlobalOpenTelemetry for any API calls

### Key Components
- **Main Flow**: `MainActivity.kt` → `AstronomyShopActivity.kt` (shopping flow)
- **Product Data**: `/src/main/assets/products.json` (10 astronomy products)
- **Navigation**: Jetpack Compose with `Navigation.kt` handling screen transitions
- **State Management**: MVVM pattern with ViewModels:
  - `ProductListViewModel.kt` - Product listing with lifecycle-aware loading and currency support
  - `ProductDetailViewModel.kt` - Individual product details with currency support
  - `CartViewModel.kt` - Shopping cart operations  
  - `CheckoutInfoViewModel.kt` - Checkout flow management with shipping cost calculation
  - `CurrencyViewModel.kt` - Currency selection and management
- **API Services**: RESTful API clients with comprehensive telemetry:
  - `ProductApiService.kt` - Product catalog with currency parameter support
  - `CurrencyApiService.kt` - Available currencies fetching
  - `ShippingApiService.kt` - Shipping cost calculation via checkout preview
  - `CheckoutApiService.kt` - Order placement with currency and shipping support
  - `FetchHelpers.kt` - Unified HTTP client with session baggage propagation for complete user journey tracking

### Demo Test Scenarios
- **Crash**: Add exactly 10 "National Park Foundation Explorascope" items
- **ANR**: Add exactly 9 "National Park Foundation Explorascope" items  
- **Slow Render**: Add any quantity of "The Comet Book" items
- **Manual Instrumentation**: Click the OpenTelemetry logo button

### Build System
- **Gradle**: Kotlin DSL with version catalogs (`gradle/libs.versions.toml`)
- **Min SDK**: 26, **Target SDK**: 34, **Compile SDK**: 35
- **UI**: Jetpack Compose with Material 3
- **ByteBuddy**: Used for OkHttp auto-instrumentation

### Key Dependencies
- OpenTelemetry Android SDK (0.11.0-alpha)
- Honeycomb Android SDK (0.0.11)
- OpenTelemetry Core (1.49.0)
- OkHttp with auto-instrumentation
- Coil (Image loading and caching)

## HTTP Client Architecture

### Session-Aware Request Handling
All API services now use `FetchHelpers.executeRequestWithBaggage()` to ensure complete user journey tracking:

- **Unified Session Context**: All HTTP requests include session baggage headers (`session.id=${sessionId}`)
- **Complete User Journey**: Product browsing, currency changes, shipping calculations, and checkout all linked to the same session
- **Enhanced Observability**: Full traceability from initial app launch through order completion
- **Distributed Tracing**: Session context propagates across all service boundaries for end-to-end visibility

### API Services Integration
- **ProductApiService**: Product catalog requests now include session context for user journey correlation
- **CurrencyApiService**: Currency selection events tied to user session for conversion analysis
- **ShippingApiService**: Shipping calculations linked to shopping session for checkout flow insights
- **CheckoutApiService & CartApiService**: Continue to use session context for order processing

This architecture change enables rich analytics like:
- Time-to-purchase analysis from first product view to checkout
- Currency selection impact on user behavior
- Shopping cart abandonment patterns
- Cross-service performance correlation

## Telemetry Patterns

### Automatic Instrumentation
- Activity/Fragment lifecycle monitoring
- Crash and ANR detection
- Slow render detection  
- HTTP client instrumentation (OkHttp)

### Manual Instrumentation Examples
- Custom spans: `MainOtelButton.kt:MainOtelButton()`
- Navigation events: `Navigation.kt:navigateToDestination()`
- Order placement: `AstronomyShopActivity.kt:placeOrder()`
- Cart operations: `Cart.kt:emptyCart()`
- Currency operations: `CurrencyApiService.kt:fetchCurrencies()`
- Shipping calculations: `ShippingApiService.kt:getShippingCost()`

### Currency and Shipping Features

#### Multi-Currency Support
- **Available Currencies**: 33+ currencies fetched from `/api/currency` endpoint
- **Currency Storage**: Persistent selection via SharedPreferences with "USD" default
- **Dynamic Pricing**: All product APIs support `?currencyCode=` parameter for real-time price conversion
- **Currency UI Components**:
  - **CurrencyToggle**: Quick switch between common currencies (USD, EUR, GBP, JPY) in product list header
  - **CurrencyBottomSheet**: Full currency picker with search functionality for all available currencies
  - **Real-time Updates**: Price changes propagate immediately across all product screens

#### Currency Formatting and Display
- **Localized Symbols**: Major currencies display with proper symbols (USD: $, EUR: €, GBP: £, JPY: ¥)
- **Fallback Format**: Less common currencies display as "CODE amount" (e.g., "SEK 123.45")
- **Consistent Formatting**: `Money.formatCurrency()` extension handles all currency display logic
- **Price Integration**: Product cards, detail screens, and checkout all use consistent formatting

#### Shipping Cost Calculation
- **Preview Method**: Uses checkout API with preview flag to calculate shipping without placing order
- **Real-time Calculation**: Triggered automatically when shipping info becomes complete
- **Fallback Handling**: Graceful degradation when shipping calculation API unavailable
- **Currency-Aware**: Shipping costs calculated and displayed in selected currency
- **State Management**: Loading states, error handling, and cost display in CheckoutInfoViewModel

#### Architecture Components
- **CurrencyApiService.kt**: Fetches available currencies with full OpenTelemetry instrumentation
- **CurrencyViewModel.kt**: Manages currency state, persistence, and loading with proper error handling
- **ShippingApiService.kt**: Calculates shipping costs via checkout API preview mechanism
- **CurrencyComponents.kt**: Reusable UI components for currency selection and display
- **Enhanced ViewModels**: ProductListViewModel and ProductDetailViewModel updated for currency support

#### Client-Side Cart Architecture
- **Mobile Optimization**: Cart state maintained on device (vs server-side sessions in web frontend)
- **Immediate Updates**: Cart operations trigger immediate UI updates without server round-trips
- **Currency Consistency**: Cart totals and shipping costs always calculated in selected currency
- **Checkout Integration**: Cart contents and currency passed to checkout API for order placement

#### Comprehensive Telemetry
All currency and shipping operations include detailed OpenTelemetry instrumentation:
- **Currency Operations**: 
  - `currency.code` attribute on product loading and selection spans
  - `currencies.count` attribute when fetching available currencies
  - Currency change events tracked as discrete user interactions
- **Shipping Operations**:
  - `shipping.cost.value` and `shipping.cost.currency` attributes
  - `shipping.calculation.method` (preview vs actual) differentiation
  - Shipping calculation timing and error rate monitoring
- **Order Operations**:
  - Complete order context including selected currency and calculated shipping
  - Currency-aware order totals and tax calculations

### API Endpoints
The Android app integrates with the following REST API endpoints:
- **GET /api/currency** - Fetch available currencies (33+ supported)
- **GET /api/products?currencyCode={code}** - Fetch product catalog with pricing in specified currency
- **GET /api/products/{id}?currencyCode={code}** - Fetch individual product with currency-converted pricing
- **POST /api/checkout?currencyCode={code}** - Place order with currency-specific pricing and shipping costs
- **GET /images/products/{picture}** - Product images served relative to API base URL

**Base URL Configuration**:
- Local Development: `http://10.0.2.2:9191` (Android emulator)
- Production: `https://www.zurelia.honeydemo.io`
- Configured via `OtelDemoApplication.apiEndpoint`

### Collector Configuration
- **Receivers**: OTLP gRPC (4317) and HTTP (4318)
- **Exporters**: Debug console + Honeycomb
- **Data Types**: Traces, logs, metrics, events

## Known Issues & Diagnostics

### Clock Drift in Distributed Traces

**Issue**: Android device and server infrastructure clocks are not synchronized, causing timing anomalies in distributed traces where:
- Parent spans appear to end before child spans start
- Server-side spans (ingress, proxy) show different timing than client spans
- Total trace time extends beyond root span duration

**Example**: Trace `61fd682f3248c4a89e8994db179702f0` shows:
- Android `GET` span (315.5ms) ends before server `ingress` span (9.9ms) appears to start
- Root `loadProducts` (327ms) vs actual trace duration (~500ms)

**Root Cause**: Android devices sync with cellular/WiFi network time, while server infrastructure may use different NTP sources.

**Mitigation Strategies**:
- Focus on relative timing within each service rather than absolute cross-service timing
- Use logical parent-child relationships for causality tracking
- Consider adding server-side timestamp correlation in API responses
- Accept 100-200ms drift as normal for mobile-to-server traces

**Clock Sync Options**:
- **Android**: Limited control - devices auto-sync with network time
- **Server**: Ensure NTP configuration in containers/infrastructure
- **Docker**: Mount host time with `/etc/localtime:/etc/localtime:ro`
- **OTel Collector**: Add timestamp processor for normalization

### Span Context Guidelines

**Standalone Events**: User-initiated actions that represent the start of new interactions should create root spans without parent context. Examples:
- **UI event handlers** (`MainOtelButton.kt:generateClickEvent()`) - Button clicks/taps are interaction origins
- **ViewModel state operations** (`CartViewModel.kt:addProduct()`) - User-triggered state changes like "Add to Cart"
- **User-triggered operations** (navigation events, cart operations) - Discrete user actions
- **Event origination points** - Where new user journeys begin

**When NOT to add parent context**:
- ✅ Button click handlers - these are trace origins, not continuations
- ✅ ViewModel state mutations - cart operations, user preferences changes
- ✅ User gesture responses - new interaction starts here
- ✅ Timer/scheduled events - independent operations
- ✅ Standalone analytics events - discrete measurements

**When to use parent context**:
- ✅ API calls triggered by user actions - inherit from the triggering operation
- ✅ Background operations spawned from UI - connect to originating event
- ✅ Service-to-service calls - maintain distributed trace context

**Conclusion**: Not every span needs a parent. Root spans are correct and necessary for marking the beginning of user interactions and standalone events.

## ViewModel Lifecycle Management

### ProductListViewModel Behavior
The `ProductListViewModel` implements smart loading to prevent duplicate API calls:

- **First Load**: `refreshProducts()` called by UI triggers `loadProducts(isRefresh=false)`
- **Subsequent Loads**: `refreshProducts()` called by UI triggers `loadProducts(isRefresh=true)`
- **Internal Tracking**: `hasLoadedOnce` flag distinguishes between initial load and refresh
- **Telemetry**: `is_refresh` attribute properly tracks user interaction patterns

This prevents double-loading when UI components (like `LaunchedEffect`) automatically trigger refresh on screen load while maintaining proper telemetry distinction between first-time loading and user-initiated refreshes.

## Image Loading Architecture

### Image Service Integration
The app uses a unified image loading system that dynamically adapts to the current environment:

- **URL Construction**: `ImageLoader.kt:getImageUrl()` constructs image URLs using the same base endpoint as the API
- **Environment Awareness**: Images served from `/images/products/` path relative to `API_ENDPOINT` 
- **Local Development**: `http://10.0.2.2:9191/images/products/$picture`
- **Production**: `https://www.zurelia.honeydemo.io/images/products/$picture`

### Caching Behavior
- **Library**: Coil3 (`AsyncImage` composables) with caching disabled
- **Cache Strategy**: Both memory and disk caching disabled to ensure fresh image fetches
- **Product Detail Flow**: 
  - Product data refreshes on each navigation (API call)
  - Images are re-fetched on every display for real-time updates
  - Prioritizes fresh content over performance optimization

### Components
- **ImageLoader.kt**: Central image URL construction with environment detection
- **ProductCard.kt**: Uses `AsyncImage` for product thumbnails in lists
- **ProductDetails.kt**: Uses `AsyncImage` for full-size product images
- **Telemetry**: HTTP requests for images appear in traces with proper status codes