# Paladin Java SDK (Micro-PoC)

> **Status:** Proof of Concept for LFX Mentorship Application (Jun-Nov 2026)

This repository contains a focused Proof of Concept (PoC) demonstrating the core architectural philosophy proposed for the Paladin Java SDK.

## Scope: Depth over Breadth

This PoC implements a small but **production‑grade** slice of the `ptx` namespace, prioritising:

1. **Fluent `TxBuilder`** - guided, chainable, IDE‑friendly transaction construction.
2. **Automatic `PD012220` Recovery** - transparent fallback to `ptx_getTransactionByIdempotencyKey` for idempotency clashes.

The remaining RPC methods (`pstate_*`, `pgroup_*`, WebSockets, domain helpers) are design‑aligned in the proposal and will be incrementally delivered during the mentorship, maintaining the same quality standard.

## Design: Fluent Transaction Builder

The builder pattern provides compile‑time safety, deferred error validation, and a self‑documenting API.

**Example usage:**

```java
client.newTransaction()
    .privateTx()
    .domain("zeto")
    .function("transfer")
    .to("0x...")
    .inputs(1000)
    .idempotencyKey("payment-ref-123")
    .send();
```

## 🛡️ Resilience: Idempotency Recovery

In distributed financial systems, network failures can cause duplicate transaction submissions. Paladin nodes reject duplicates with error code `PD012220` (Idempotency Clash).

This PoC implements the same recovery behaviour as the Go SDK: when `PD012220` is detected, the client automatically calls `ptx_getTransactionByIdempotencyKey` and returns the existing transaction ID - ensuring the calling application sees a success without any retry logic.

```mermaid
sequenceDiagram
    participant App as Application
    participant Builder as TxBuilder
    participant Client as PaladinClient
    participant Mock as MockWebServer

    App->>Builder: .privateTx().domain("zeto")...
    App->>Builder: .function("transfer").inputs(1000)
    App->>Builder: .idempotencyKey("unique-123")
    App->>Builder: .send()

    Builder->>Client: build JSON-RPC request
    Client->>Mock: POST /jsonrpc (id=1)
    
    alt First attempt (idempotency key unused)
        Mock-->>Client: {"result": {"txId": "new-tx-456"}}
        Client-->>App: CompletableFuture completed with "new-tx-456"
    else Duplicate (PD012220)
        Mock-->>Client: {"error": {"code": "PD012220", "message": "..."}}
        Client->>Client: detect PD012220, key present
        Client->>Mock: ptx_getTransactionByIdempotencyKey("unique-123")
        Mock-->>Client: {"result": {"txId": "existing-tx-789"}}
        Client-->>App: CompletableFuture completed with "existing-tx-789"
    end
```

## The Payoff: Domain Helpers
The ultimate goal of this architecture is to make life easy for the application developer. Because the core `TxBuilder` handles type safety and error recovery, building higher-level Domain Helpers (like Zeto or Noto) becomes trivial. 

Instead of dealing with raw RPC maps, developers can just do this:
```java
ZetoHelper zeto = new ZetoHelper(client);
zeto.transfer("alice@node1", "bob@node2", 500, "invoice-123").join();
```

## Running the Tests

The PoC uses `MockWebServer` to simulate the `PD012220` error path deterministically, without requiring a live Paladin node.

```bash
mvn clean test
```

## Project Vision (Beyond the PoC)

This PoC is the foundation for a complete Paladin Java SDK. The full SDK will include:

```mermaid
flowchart TB
    subgraph App [Application Layer]
        A1[Spring Boot App]
        A2[Plain Java App]
    end

    subgraph SDK [Paladin Java SDK]
        B1["PaladinClient"]
        B2["TxBuilder<br/>(fluent + idempotency)"]
        B3["Domain Helpers<br/>Zeto / Noto / Pente"]
        B4["Dual WebSocket Clients<br/>PTX + pgroup subscriptions"]
        B5["Query Builder for pstate"]
    end

    subgraph RPC [RPC Transport]
        C1["HTTP JSON-RPC<br/>java.net.http.HttpClient"]
        C2["WebSocket<br/>Jakarta / Tyrus"]
    end

    subgraph Node [Paladin Node]
        D1["ptx_* / keymgr_* / pstate_* / pgroup_*"]
    end

    App --> SDK
    SDK --> RPC
    RPC --> Node
```

**Implementation Roadmap (22 weeks, part-time):**

```mermaid
gantt
    title Paladin Java SDK - 22 Week Timeline
    dateFormat  YYYY-MM-DD
    axisFormat  %b %d
    
    section Foundation
    HTTP + core modules       :a1, 2026-06-01, 21d
    Fluent TxBuilder          :a2, after a1, 14d
    
    section Core RPC
    Query + pstate module     :a3, after a2, 14d
    Privacy groups (pgroup)   :a4, after a3, 14d
    
    section WebSocket
    PTX WebSocket + ack/nack  :a5, after a4, 14d
    pgroup WebSocket          :a6, after a5, 14d
    
    section Domain Helpers
    Noto + Zeto               :a7, after a6, 21d
    Pente                     :a8, after a7, 14d
    
    section Release
    Extended namespaces       :a9, after a8, 14d
    Testcontainers + examples :a10, after a9, 14d
    Maven Central release     :a11, after a10, 14d
```

## Next Steps (Mentorship Scope)

- Full `ptx` parity (dispatch queries, blockchain event listeners)
- Domain helpers for Noto, Zeto, and Pente
- Dual WebSocket clients with ack/nack and auto‑reconnect
- State store query builder
- Testcontainers integration suite
- Maven Central publication
