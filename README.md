# Nexus Traffic Manager

**Version**: `0.0.1-SNAPSHOT`  
**Developed By**: Pradeep Kushwah
**Last Updated**: June 20, 2025

---

## Overview

Nexus Traffic Manager is a robust Spring Cloud Gateway designed to **route**, **secure**, and **monitor** microservice
traffic within the Nexus ecosystem. It serves as the **entry point** for all API requests, offering advanced features
like:

- ✅ Dynamic routing
- 🚦 Custom rate limiting (MongoDB + Bucket4j)
- 🔁 Retry policies
- ⚡ Reactive architecture
- 🪵 Integrated logging and debugging

---

# Technical and Functional Explanation

The Nexus Traffic Manager operates as a reactive API Gateway built on Spring Cloud Gateway, designed to enhance the
scalability and resilience of microservice architectures. Below is a detailed breakdown of its functionality and
technical underpinnings:

- **Communication with Nexus Server (Eureka) for Service Discovery**:
  The gateway integrates with the **Nexus Server**, an Eureka-based service registry, to dynamically discover available
  microservices. Upon startup, it registers itself with the Nexus Server and periodically synchronizes with the registry
  to fetch the latest service instances (e.g., IP addresses and ports). This process relies on the
  `spring-cloud-starter-netflix-eureka-client` dependency, which enables the gateway to query the Nexus Server at
  `http://localhost:8761/eureka/` for service metadata. Post-discovery, the gateway maps logical service IDs (e.g.,
  `lb://pigeon`) to physical endpoints, ensuring requests are routed to healthy instances based on Eureka’s health
  checks.

- **Dynamic Routing to Microservices (e.g., PIGEON Service)**:
  Leveraging Spring Cloud Gateway’s routing capabilities, the gateway dynamically routes incoming HTTP requests to
  appropriate microservices based on path predicates. For instance, a request to `/pigeon/graphql` is matched using a
  `Path=/pigeon/graphql` predicate and forwarded to the PIGEON service, resolved via the Nexus Server as `lb://pigeon`.
  The `RewritePath` filter transforms the URL (e.g., `/pigeon/graphql` to `/graphql`) to align with the target service’s
  endpoint (`http://localhost:8085/graphql`). This feature ensures flexibility, allowing the addition of new
  microservices without gateway reconfiguration.

- **Custom Rate Limiting with MongoDB-backed Configuration using Bucket4j**:
  The gateway implements a custom rate-limiting mechanism using the **Bucket4j** library, with configurations stored in
  a MongoDB collection (`rate_limit_config`). Each rate limit policy is defined by fields such as `requestsPerMinute`,
  `timeWindow`, `timeUnit`, and `burstCapacity`, persisted as JSON documents. The `RateLimiterFilter` queries MongoDB at
  runtime to instantiate `Bucket` objects, enforcing limits (e.g., 100 requests per minute with a 10-request burst).
  This approach provides persistence and scalability, allowing administrators to update limits centrally without
  redeploying the gateway.

- **Support for Retries, Burst Capacity, and Priority-based Limits**:
  The gateway enhances resilience with a retry policy configurable via `customAttributes` (e.g., `maxRetries: 2`,
  `retryDelay: 500ms`), enabling clients to retry requests after rate limit exceedances. **Burst capacity** allows
  temporary spikes beyond the base limit (e.g., 10 extra requests), managed by Bucket4j’s bandwidth configuration. *
  *Priority-based limits** leverage the `priority` field (e.g., 1 for high-priority clients) to adjust rate limits
  dynamically, ensuring fair resource allocation across clients.

- **Reactive Architecture with Spring WebFlux**:
  Built on **Spring WebFlux**, the gateway adopts a non-blocking, event-driven architecture using the reactive Netty
  server. This enables handling thousands of concurrent connections efficiently, with the
  `web-application-type: reactive` setting in `application.yaml`. The `RateLimiterFilter` integrates with WebFlux’s
  reactive chain, using `Mono.defer` for asynchronous retry logic, ensuring high throughput and low latency even under
  load.

- **Integrated Logging and Debugging**:
  The gateway incorporates **SLF4J** with DEBUG-level logging (e.g., `org.springframework.cloud.gateway: DEBUG`) to
  provide detailed insights into routing, filtering, and rate-limiting decisions. Configured in `application.yaml`, logs
  capture filter invocations (e.g., `RateLimiterFilter invoked`) and errors, aiding developers in troubleshooting. The
  `reactor.netty.http.client` and `reactor.netty.http.server` log levels further expose network-level details, enhancing
  debuggability.

---

## 🧠 Architecture Overview

The Nexus Traffic Manager operates as a reactive API Gateway, orchestrating communication between the **Nexus Server** (
Eureka-based service registry) and downstream microservices like the PIGEON service. Below is a textual description of
the flow, followed by a placeholder for an image-based flow diagram:

### Flow Description

1. **Service Discovery**: The gateway registers with the Nexus Server at `http://localhost:8761/eureka/` and fetches
   service metadata (e.g., instances, status) using the `spring-cloud-starter-netflix-eureka-client`.
2. **Request Routing**: Incoming requests (e.g., `/pigeon/graphql`) are matched via path predicates and routed to the
   PIGEON service (`lb://pigeon`), resolved to `http://localhost:8085/graphql` by the Nexus Server.
3. **Rate Limiting & Processing**: The `RateLimiterFilter` applies custom limits (e.g., 100 requests/min) using
   Bucket4j, with configurations from MongoDB, before forwarding the request.
4. **Response**: The PIGEON service processes the request (e.g., GraphQL query) and returns a response via the gateway.

### Flow Diagram

![Nexus Traffic Manager Flow Diagram](Flow-Gateway-Eureka-Server-Client.png)

- **Eureka Server (Nexus Server)**: Manages service registration/discovery
- **Traffic Manager (Nexus Traffic Manager)**: Handles routing, rate-limiting, and rewriting
- **PIGEON Service (Client Service)**: GraphQL backend service (`http://localhost:8085/graphql`)

---

## 🔧 Prerequisites

- **Java**: JDK 17+
- **Maven**: 3.6.0+
- **MongoDB**: Running at `localhost:27017` (configurable)
- **IDE**: IntelliJ IDEA (recommended)

---

## 🚀 Setup Instructions

### Clone the Repository

```bash
git clone <repository-url>**
cd Nexus-Traffic-Manager**
```

---

## Configure Environment

### - **Start MongoDB**: Update spring.data.mongodb.uri in application.yaml if using a different host/port.

### - **Install dependencies**: mvn clean install

## Import into IntelliJ

- **Open IntelliJ IDEA.**

- **Select File** > Open and choose **pom.xml.**

- **Wait for indexing and dependency resolution.**

---

## ⚙️ Configure application.yaml

### - Edit  **src/main/resources/application.yaml**:

### 📁 `application.yaml` Configuration

```yaml
server:
  port: 8090

spring:
  application:
    name: NexusTrafficManager
  main:
    web-application-type: reactive
  data:
    mongodb:
      uri: mongodb://localhost:27017/nexus_traffic_manager

  cloud:
    gateway:
      discovery:
        locator:
          enabled: true  # Enable Eureka discovery
      routes:
        - id: pigeon-service
          uri: lb://pigeon  # Eureka-resolved service
          predicates:
            - Path=/pigeon/graphql
          filters:
            - name: RateLimiterFilter
            - RewritePath=/pigeon/(?<segment>.*), /${segment}

  logging:
    level:
      org.springframework.cloud.gateway: DEBUG
      com.daimlertrucksasia.it.dsc.nexus.traffic.manager: DEBUG
      reactor.netty.http.client: DEBUG
      reactor.netty.http.server: DEBUG

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
```

---

## ▶️ Run the Application

### - **Right-click: NexusTrafficManagerApplication.java and select Run.**

### - **Or use**:Maven CLI

```bash
mvn spring-boot:run
```

---

## Verify Setup

### Start the Eureka server at http://localhost:8761.

- **Ensure the PIGEON service is registered with Eureka.**
- **Test the endpoint**:

```shell
curl --location 'http://localhost:8090/pigeon/graphql' \
--header 'X-Client-Id: test-client' \
--header 'Content-Type: application/json' \
--data '{"query":"mutation CreateMsgTemplate {...}"}'
````

---

### - **Check IntelliJ console for DEBUG logs.**

## Configuration

### 📊 -**Rate Limiting**:

#### Managed in MongoDB (rate_limit_config collection). Example:

```json
{
  "clientId": "test-client",
  "route": "/pigeon/graphql",
  "requestsPerMinute": 100,
  "timeWindow": 60,
  "timeUnit": "SECONDS",
  "burstCapacity": 10,
  "priority": 1,
  "status": "ACTIVE",
  "customAttributes": {
    "maxRetries": "2",
    "retryDelay": "500"
  }
}
```

# 📦 Usage

#### 1- **Access the gateway at http://localhost:8090**

#### 2- **Use the X-Client-Id header for rate limiting.**

#### 3- **Monitor logs for rate limit enforcement and retries.**

# 📬 Contact

### Developer: Pradeep Kushwah

### Email: kushwahpradeep531@gmail.com

### Organization: 


