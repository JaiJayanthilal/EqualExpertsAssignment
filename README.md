# 🛒 Shopping Cart Application

## 📌 Overview

This is a simple Java-based shopping cart application built as part of the Equal Experts coding assignment.

The application supports:

* Adding products to the cart
* Fetching product prices from an external API
* Calculating subtotal, tax, and total
* Handling failures using retry and circuit breaker patterns
* Optimizing performance using caching strategies
* test

---

## ⚙️ Features

### 🧾 Cart Capabilities

* Add product with quantity
* Maintain cart state
* Calculate:

    * **Subtotal**
    * **Tax (12.5%)**
    * **Total payable**
* Rounds values to 2 decimal places

---

### 🌐 Price API Integration

* Fetches product prices from:

  ```
  https://equalexperts.github.io/backend-take-home-test-data/{product}.json
  ```
* Supported products:

    * cheerios
    * cornflakes
    * frosties
    * shreddies
    * weetabix

---

## 🧠 Design Approach

### 🔹 Abstraction

A common interface is used:

```java
PriceProvider
```

This allows switching between different implementations without changing the cart logic.

---

## 🔄 Price Service Implementations

### 1️⃣ SimplePriceService

* Implements **manual retry logic**
* Implements **custom circuit breaker**
* Demonstrates:

    * Low-level control over failure handling
    * Understanding of retry + circuit breaker mechanics

---

### 2️⃣ ResilientPriceService

* Uses Resilience4j
* Implements:

    * Circuit Breaker
    * Retry mechanism
* Demonstrates:

    * Production-grade resilience
    * Clean functional composition using Supplier

---

### 💡 Note

Both implementations are intentionally kept separate to **demonstrate two approaches**:

* Manual implementation (learning purpose)
* Library-based implementation (production-ready)

---

## 🧠 Caching Strategies

To avoid repeated API calls, multiple caching strategies are implemented:

### 1️⃣ LRUCache

* Uses `LinkedHashMap`
* Simple, non-thread-safe implementation
* Demonstrates basic LRU eviction

---

### 2️⃣ ConcurrentLRUCache

* Uses:

    * `ConcurrentHashMap`
    * `ConcurrentLinkedDeque`
    * `ReentrantLock`
* Thread-safe implementation
* Demonstrates:

    * Custom concurrency handling
    * Manual eviction logic

---

### 3️⃣ CaffeineCache

* Uses Caffeine
* Production-grade caching
* Features:

    * High concurrency
    * Automatic eviction
    * Atomic loading

---

### 💡 Note

All three cache implementations are intentionally included to:

* Compare approaches
* Demonstrate trade-offs between simplicity and performance

---

## 🔁 Resilience Design

### Manual Approach (SimplePriceService)

* Custom circuit breaker implementation
* Retry loop with delay

### Library Approach (ResilientPriceService)

* Uses Resilience4j:

    * CircuitBreaker
    * Retry
* Configurable:

    * Failure thresholds
    * Retry attempts
    * HALF_OPEN behavior

---

## 🧪 Testing

* Unit tests cover:

    * Cart calculations
    * Price service logic
    * Cache behavior

---

## ⚖️ Design Trade-offs

| Area         | Approach          | Reason                                        |
| ------------ | ----------------- | --------------------------------------------- |
| Resilience   | Manual + Library  | Demonstrate understanding vs production usage |
| Caching      | 3 implementations | Compare strategies                            |
| Architecture | Interface-based   | Enables flexibility and testing               |
| Frameworks   | None              | Requirement constraint                        |

---

## 🚀 How to Run

1. Clone repository
2. Build using Maven:

   ```
   mvn clean install
   ```
3. Run main class:

   ```
   ShoppingCartApp
   ```

---

## 🎯 Key Learnings

* Implementation of **LRU cache**
* Handling concurrency in caching
* Understanding **Circuit Breaker pattern**
* Using **Resilience4j** for production-ready systems
* Applying **clean architecture principles**

---

## 📌 Final Note

This project intentionally includes multiple implementations for:

* Learning
* Comparison
* Demonstration during technical discussions

In a real-world production system:

* A single standardized approach (e.g., Caffeine + Resilience4j) would be preferred

---
