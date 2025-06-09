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
- **State Management**: ViewModels for cart (`CartViewModel.kt`) and checkout (`CheckoutInfoViewModel.kt`)

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

### Collector Configuration
- **Receivers**: OTLP gRPC (4317) and HTTP (4318)
- **Exporters**: Debug console + Honeycomb
- **Data Types**: Traces, logs, metrics, events