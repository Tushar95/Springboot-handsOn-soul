# ⚡ Resilience4j Demo with Spring Boot

This project demonstrates **Resilience4j** fault-tolerance patterns in a real-world Spring Boot setup.  
It includes **Circuit Breaker, Retry, Rate Limiter, Time Limiter, Bulkhead, and Fallbacks** with hands-on examples. 🚀

---

## 🔥 Features Covered
✅ CircuitBreaker – stop calling unhealthy services  
✅ Retry – auto retry failed calls  
✅ RateLimiter – control request floods  
✅ TimeLimiter – timeout for slow services  
✅ Bulkhead – prevent resource exhaustion  
✅ Fallback – graceful degradation  

---

## 📂 Project Structure
resilience4j-demo

📌 Description:
A Spring Boot project with two services:

Service A (client) → Calls Service B.

Service B (server) → Intentionally unstable (sometimes fails, delays, or throws errors).

You apply:

CircuitBreaker, Retry on Service A calling Service B

RateLimiter to protect Service B

TimeLimiter to cancel long requests

Fallbacks to provide default responses

┣ 📦 service-a (client, calls service-b with resilience4j)
┣ 📦 service-b (unstable service with random delays/failures)


## Resilience4j Components & How They Fit

Resilience4j provides different fault-tolerance patterns. Each one solves a specific issue in distributed, large-scale microservice architecture:

1. CircuitBreaker → Prevents cascading failures by stopping calls to a failing service until it recovers.

2. Retry → Automatically retries failed operations before giving up.

3. RateLimiter → Limits number of calls in a time window (protects services from overload).

4. Bulkhead → Restricts concurrent calls to isolate failures. (Thread-pool or semaphore isolation).

5. TimeLimiter → Sets max time a request can run (cancels slow calls).

6. Cache → Caches successful responses to reduce load on backend.

7. Fallback (with Spring Boot integration) → Provides alternative response when primary fails.

---

## 🔹 1. What `resilience4j-spring-boot3` actually is

* Think of `resilience4j-spring-boot3` as the **Spring Boot auto-configuration + annotation support layer**.
* It **does not include all the individual modules** (circuit breaker, retry, etc.) — it just wires them into Spring Boot if you add them.

👉 In other words:

* `resilience4j-spring-boot3` = *“Spring Boot glue code”*
* Actual resilience logic still comes from `resilience4j-circuitbreaker`, `resilience4j-retry`, `resilience4j-ratelimiter`, etc.

---

## 🔹 2. Why add `circuitbreaker` and `retry` explicitly?

Because:

* `resilience4j-spring-boot3` alone doesn’t contain the **implementation** of circuit breaker, retry, bulkhead, etc.
* If you don’t add `resilience4j-circuitbreaker`, the annotation `@CircuitBreaker` won’t work (because the core logic is missing).
* Same with `@Retry`, `@RateLimiter`, `@Bulkhead`, etc.

So **you always need at least two parts**:

1. **The core module(s)** (like `resilience4j-circuitbreaker`)
2. **The Spring Boot integration layer** (`resilience4j-spring-boot3`)

---

## 🔹 3. What’s the "core" of Resilience4j?

At its heart, Resilience4j is just a **set of functional interfaces + decorators** that wrap calls with fault-tolerant behavior.

* `resilience4j-core` is the **foundation** module — all others depend on it.
* Example: a **circuit breaker** is basically a `Function<T,R>` wrapper that checks if calls should pass through or fail-fast.
* Retry, Bulkhead, TimeLimiter, etc. are separate implementations built on top of `core`.

---

## 🔹 4. Practical dependency strategy

If you are using Spring Boot 3:

* ✅ Always include `resilience4j-spring-boot3`
* ✅ Add only the modules you really use (`circuitbreaker`, `retry`, etc.)
* ❌ Don’t add everything blindly unless you really need them

Example minimal setup (CircuitBreaker + Retry only):

```kotlin
dependencies {
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
}
```

---

✅ **In short**:

* `resilience4j-spring-boot3` = Spring Boot auto-configuration & annotations
* `resilience4j-circuitbreaker`, `resilience4j-retry`, etc. = actual resilience mechanisms
* You need both.

---

1. **Visibility into the CircuitBreaker state** (Closed → Open → Half-Open).
2. **Logs/metrics about retries and failures**.

Let me break this down step by step:

---

## 🔹 1. Checking CircuitBreaker State

Resilience4j gives you the `CircuitBreakerRegistry` and `CircuitBreaker` instances directly:

```kotlin
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.stereotype.Component

@Component
class CircuitBreakerInspector(val registry: CircuitBreakerRegistry) {

    fun checkState() {
        val cb = registry.circuitBreaker("myService")
        println("State: ${cb.state}")
    }
}
```

States you can see:

* `CLOSED` → normal working
* `OPEN` → all calls are blocked
* `HALF_OPEN` → testing if the service has recovered

---

## 🔹 2. Why the Circuit Breaker Opened (Failure Reasons)

Resilience4j counts:

* **Failed calls** (exceptions)
* **Slow calls** (if configured with a slow-call threshold)
* **Success calls**

You can get these details:

```kotlin
val cb = registry.circuitBreaker("myService")
val metrics = cb.metrics

println("Failure rate: ${metrics.failureRate}")
println("Number of failed calls: ${metrics.numberOfFailedCalls}")
println("Number of successful calls: ${metrics.numberOfSuccessfulCalls}")
println("Number of slow calls: ${metrics.numberOfSlowCalls}")
```

---

## 🔹 3. How many retries happened?

Use `RetryRegistry`:

```kotlin
import io.github.resilience4j.retry.RetryRegistry

@Component
class RetryInspector(val registry: RetryRegistry) {

    fun checkRetries() {
        val retry = registry.retry("myService")
        val metrics = retry.metrics

        println("Number of successful calls without retry: ${metrics.numberOfSuccessfulCallsWithoutRetryAttempt}")
        println("Number of successful calls with retry: ${metrics.numberOfSuccessfulCallsWithRetryAttempt}")
        println("Number of failed calls with retry: ${metrics.numberOfFailedCallsWithRetryAttempt}")
    }
}
```

---

## 🔹 4. Logging Events (Best Practice)

Instead of polling metrics, you can subscribe to **events**:

### Circuit Breaker Events

```kotlin
cb.eventPublisher
    .onStateTransition { e -> println("State transition: ${e.stateTransition}") }
    .onError { e -> println("Error recorded: ${e.throwable}") }
    .onSuccess { e -> println("Success: ${e.elapsedDuration}") }
```

### Retry Events

```kotlin
val retry = registry.retry("myService")

retry.eventPublisher
    .onRetry { e -> println("Retry attempt: ${e.numberOfRetryAttempts}, due to: ${e.lastThrowable?.message}") }
    .onSuccess { e -> println("Call succeeded after retry") }
```

---

## 🔹 5. Observability via Actuator

If you add:

```kotlin
implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
implementation("io.github.resilience4j:resilience4j-micrometer:2.2.0")
implementation("io.micrometer:micrometer-registry-prometheus")
```

➡ You automatically get **Spring Boot Actuator endpoints**:

* `/actuator/metrics/resilience4j.circuitbreaker.calls`
* `/actuator/metrics/resilience4j.retry.calls`
* `/actuator/circuitbreakers` (shows all breakers + states)

This is the cleanest way in production.

---

## ✅ Summary

* **Check state** → via `CircuitBreakerRegistry`
* **Reason for breaking** → look at `metrics.failureRate`, failed/slow calls
* **Retries count** → via `RetryRegistry.metrics`
* **Best logging** → subscribe to event publishers (`onRetry`, `onStateTransition`, etc.)
* **Prod visibility** → enable Resilience4j Actuator + Micrometer

---

# ⚡ Resilience4j Patterns — Best Practices

## 1. **CircuitBreaker (CB)**

* **Use for:** External dependencies that may fail repeatedly (APIs, DB, message broker).
* **Best practice:**

    * One CB per external dependency (e.g., `paymentService`, `shippingAPI`).
    * Configure in `application.yml`.
    * Always define a **fallback method**.

---

## 2. **Retry**

* **Use for:** Transient failures (e.g., network glitches, temporary 500s).
* **Best practice:**

    * Keep retry attempts low (2–3 max).
    * Combine with **CircuitBreaker** to avoid hammering a dead system.
    * Always have a **backoff policy** (like exponential delay).

Example config:

```yaml
resilience4j.retry:
  instances:
    paymentService:
      maxAttempts: 3
      waitDuration: 500ms
      enableExponentialBackoff: true
```

Usage:

```java
@Retry(name = "paymentService", fallbackMethod = "retryFallback")
public String callPayment() {
    return restTemplate.getForObject("https://api.example.com/pay", String.class);
}
```

---

## 3. **RateLimiter**

* **Use for:**

    * Protecting **your downstream service** from being overwhelmed.
    * Handling **third-party APIs with quotas** (e.g., 1000 req/min).
* **Best practice:**

    * Set `limitForPeriod` and `limitRefreshPeriod` according to API quota.
    * Apply **per-client or per-user** if you expose APIs.

Config:

```yaml
resilience4j.ratelimiter:
  instances:
    paymentService:
      limitForPeriod: 10
      limitRefreshPeriod: 1s
      timeoutDuration: 500ms
```

Usage:

```java
@RateLimiter(name = "paymentService", fallbackMethod = "rateLimitFallback")
public String callPayment() {
    return restTemplate.getForObject("https://api.example.com/pay", String.class);
}
```

---

## 4. **TimeLimiter**

* **Use for:** Ensuring you **don’t wait too long** on a slow API.
* **Best practice:**

    * Wrap async calls (`CompletableFuture`, `Mono`, etc).
    * Always define a timeout smaller than your service SLA.
    * Combine with CB to avoid flooding a slow dependency.

Config:

```yaml
resilience4j.timelimiter:
  instances:
    paymentService:
      timeoutDuration: 2s
      cancelRunningFuture: true
```

Usage:

```java
@TimeLimiter(name = "paymentService")
public CompletableFuture<String> callPayment() {
    return CompletableFuture.supplyAsync(() ->
        restTemplate.getForObject("https://api.example.com/pay", String.class)
    );
}
```

---

# 🚀 Enterprise Best Practices (All Together)

✅ **Layer patterns wisely**:

* `TimeLimiter` → fail fast if it’s too slow.
* `Retry` → retry transient failures.
* `CircuitBreaker` → trip if too many failures.
* `RateLimiter` → ensure you don’t overload yourself or hit quota.

✅ **YAML-driven config**, not hardcoded. Keeps it flexible.

✅ **Fallbacks for each pattern**:

* CB → alternative response / cache.
* Retry → final fallback if max attempts fail.
* RateLimiter → “Too many requests, please retry later.”
* TimeLimiter → return cached/default response.

✅ **One config per external system** (not per method).

✅ **Metrics & Observability**:

* Expose via Actuator → Prometheus → Grafana.
* Monitor open CBs, retry counts, throttled requests.

---

### 🔄 Example Composite Usage

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "cbFallback")
@Retry(name = "paymentService")
@RateLimiter(name = "paymentService")
@TimeLimiter(name = "paymentService")
public CompletableFuture<String> callPayment() {
    return CompletableFuture.supplyAsync(() ->
        restTemplate.getForObject("https://api.example.com/pay", String.class)
    );
}

public CompletableFuture<String> cbFallback(Throwable e) {
    return CompletableFuture.completedFuture("Payment fallback response");
}
```

