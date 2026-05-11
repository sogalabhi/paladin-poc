# Paladin Java SDK - LFX Mentorship Application

**Applicant:** Abhijith Sogal  
**Project:** Paladin Java SDK  
**Date:** May 2026  
**To:** Matthew Whitehead (Paladin maintainer) and the LFDT Paladin team  
**Mentor:** Matthew Whitehead  

---

## How I Found Out About the Mentorship Program

I was looking for LFX projects related to Web3 or financial/banking systems. While browsing the LFDT projects, I found Paladin - a programmable privacy solution for EVM blockchains. The problem it solves (confidential transactions for DvP/PvP) matched exactly the kind of real-world use cases I care about. I joined the LFDT Discord to follow discussions and understand the community.

## Why I Am Interested in This Program

Three reasons. First, I am genuinely interested in Web3 and building SDKs - I enjoy creating developer tools that abstract away protocol complexity. Second, Paladin solves a real problem for financial enterprises, and I want to work on something that has actual production impact. Third, and most importantly, this is a **build from scratch** project. The Go and TypeScript SDKs exist, but Java has nothing. Starting from zero, designing the API, making the right architectural decisions - that is exactly the kind of challenge I want.

## Experience and Knowledge/Skills Applicable to This Program

| Area | Relevant Experience |
|------|---------------------|
| Java | Shipped Android app (2k+ downloads, Java); production API integration with lifecycle management and background concurrency |
| Async / HTTP / WebSocket | Built cross-chain relayer (Stellar + Polkadot) with JSON-RPC polling, event sync, and idempotency ledger - directly mirrors Paladin's WebSocket ack/nack and receipt subscription model |
| Fluent API design | Micro-PoC with `TxBuilder` and `PD012220` recovery implemented and tested with `MockWebServer` ([github.com/sogalabhi/paladin-poc](https://github.com/sogalabhi/paladin-poc)) |
| Open-source workflow | Maintainer of Osdag-web for 1+ year: reviews, merges, rebases, branch hygiene in multi-contributor environment |
| Blockchain / EVM | Polkaflow (3rd prize) - AssetHub RPC + compiler integration; FitStake - Lit Protocol signing + on-chain oracle; PokeWars - WebSocket sync before on-chain confirmation |

## Pre-Application Work

Before applying, I completed the following:

- Read the Go SDK (`pldclient`, `rpcclient`, `wsclient`, `query`) and TypeScript SDK (`paladin.ts`, `websocket.ts`, `domains/*`) in full.
- Found a parity gap: four dispatch-related methods missing from TypeScript (`ptx_queryDispatches`, `ptx_getDispatch`, `ptx_queryChainedDispatches`, `ptx_getChainedDispatch`). Implemented them and opened PR #1168.
- Verified idempotency clash recovery (`PD012220`) behavior in Go's `TxBuilder`.
- Built a micro-PoC demonstrating fluent `TxBuilder` and automatic idempotency recovery ([github.com/sogalabhi/paladin-poc](https://github.com/sogalabhi/paladin-poc)).

## My Goals

1. Deliver a production-grade Java SDK with namespace-aligned RPC modules (`ptx`, `keymgr`, `pstate`, `pgroup`, `transport`, `bidx`, `reg`) matching Go/TS parity.
2. Implement automatic idempotency clash recovery (`PD012220`) mirroring Go's behavior - critical for enterprise DvP/PvP reliability.
3. Build domain convenience layers for Noto, Zeto, and Pente with typed factory/instance helpers matching TypeScript's ergonomics.
4. Ship dual WebSocket clients (PTX receipts + privacy-group messages) with explicit ack/nack and transparent reconnect.
5. Publish to Maven Central with a developer guide, Javadoc, and a standalone `examples/` repository demonstrating a 3-phase atomic swap for Spring Boot applications.
6. Become a long-term Paladin contributor - PR #1168 is the start, not the extent.

Thank you for considering my application. The detailed technical proposal follows below.

Sincerely,

**Abhijith Sogal**  
B.Tech, National Institute of Technology Karnataka (NITK Surathkal)  
[github.com/sogalabhi](https://github.com/sogalabhi) | abhijithsogal@gmail.com | IST (UTC+5:30)

---

# Paladin Java SDK – Architecture & Implementation Plan

**LFDT Paladin Mentorship (Jun - Nov 2026, Part-Time)**  
**Mentor:** Matthew Whitehead  
**Applicant:** Abhijith Sogal

---

## SECTION 1: Executive Summary

I am applying for the LFX Mentorship 2026 position with the Paladin project, focusing specifically on developing a production-grade Java SDK. Having spent the weeks leading up to this application reading the Go and TypeScript SDKs in depth, analyzing the JSON-RPC semantics, and implementing a focused micro-PoC, I have arrived at a clear, technically grounded plan to deliver a Java SDK that matches Go's RPC completeness and TypeScript's domain helper ergonomics.

**Core Deliverable:** A Maven-published, Java 21+ SDK with namespace-aligned RPC modules (`ptx`, `keymgr`, `pstate`, `pgroup`, `transport`, `bidx`, `reg`), a fluent `TxBuilder` with automatic idempotency clash recovery, dual WebSocket clients with ack/nack and transparent reconnect, domain helpers for Noto/Zeto/Pente, a Testcontainers integration suite, and a developer guide with DvP examples.

**Stretch Goals (conditional):** Spring Boot starter, CLI tooling + terminal dashboard, and built-in observability (Micrometer metrics).

---

## SECTION 2: The Problem

Enterprises using Java/Spring Boot currently lack a native, simplified way to interact with Paladin's privacy layers. Without an SDK, developers must:

- Manually construct and parse JSON-RPC payloads for every operation (`ptx_*`, `keymgr_*`, `pstate_*`, `pgroup_*`, ...)
- Correlate async submission with receipts and domain-specific outcomes without typed helpers
- Run two separate WebSocket subscribe lifecycles - `ptx_subscribe` (PTX receipts/events; `ptx_ack` / `ptx_nack`) and `pgroup_subscribe` (privacy-group messages; `pgroup_ack` / `pgroup_nack`) - including reconnect and subscription re-registration
- Repeat boilerplate for identity strings (`"alice@node1"`), `DependsOn` ordering, idempotency keys, and ABI-/domain-aware transaction payloads

The Go and TypeScript SDKs already solve this for their ecosystems. This project brings the same parity to Java - the dominant language in enterprise financial systems.

**Pre-application contribution:** While mapping PTX wrappers, I found that Go exposed four dispatch-related wrappers missing in TypeScript (`ptx_queryDispatches`, `ptx_getDispatch`, `ptx_queryChainedDispatches`, `ptx_getChainedDispatch`). I implemented the TypeScript wrappers and opened PR #1168 before submitting this proposal.

---

## SECTION 3: Technical Background - Go & TypeScript SDK Analysis

I have read both SDKs in detail. The key findings:

- Go SDK (`sdk/go`) provides a fluent `TxBuilder`, automatic idempotency clash recovery (`PD012220` -> `GetTransactionByIdempotencyKey`), dispatch queries, and `storeABI` returning the hash.
- TypeScript SDK provides excellent domain helpers (`NotoFactory`, `ZetoFactory`, `PenteFactory`) and a cleaner separation of WebSocket clients, but lacks dispatch queries and idempotency clash recovery (partially fixed by PR #1168).
- WebSocket ack/nack and reconnect/resubscribe logic is identical in wire protocol; both require dual clients (PTX vs privacy-group).
- Query DSL is defined as `QueryJSON` in Go and `IQuery` in TS; both support nested `eq`, `neq`, comparisons, `in`, `or`, `limit`, `sort`.

The Java SDK will adopt the broader RPC coverage of Go (dispatch queries, idempotency clash recovery, `storeABI` return hash) and the domain helper ergonomics of TypeScript. Where the two differ, we prioritise the behaviour that is more complete or more useful for enterprise Java (e.g., Go's idempotency recovery > TS's silent ignore; TS's domain helpers > Go's minimal `solutils`).

---

## SECTION 4: Proposed Solution

### 4.1 Architectural Pillars

**Pillar A - Asynchronous, Reliable RPC**  
`java.net.http.HttpClient` + `CompletableFuture` for non-blocking HTTP JSON-RPC. Virtual threads compatible. SLF4J logging with MDC trace correlation.

**Pillar B - Resilient WebSocket Subscriptions**  
Separate `PaladinWebSocketClient` (PTX) and `PrivacyGroupWebSocketClient` (pgroup). Callback-based `ReceiptHandler` with `AckCallback`. Automatic ping/pong, exponential backoff reconnect, and re-subscription on reconnect.

**Pillar C - Data Modeling & Type Safety**  
Java Records for immutable DTOs, `BigInteger` for 256-bit EVM numeric fields, flexible JSON nodes via Jackson for dynamic domain data.

### 4.2 Client Shape - RPC Namespaces

One unified `PaladinClient` with namespace-aligned modules: `ptx()`, `keymgr()`, `pstate()`, `pgroup()`, `transport()`, `bidx()`, `reg()`, `domain()`, `debug()`.

### 4.3 Fluent Transaction Builder (`TxBuilder`)

```java
client.newTransaction()
    .privateTx()
    .domain("zeto")
    .function("transfer")
    .to("0x...")
    .inputs(1000)
    .idempotencyKey("payment-ref-123")
    .dependsOn(prevTxId)
    .send();
```

**Key features:** deferred error handling ("first error wins"), automatic `PD012220` recovery (calls `ptx_getTransactionByIdempotencyKey`), support for public vs private, ABI reference by hash, and `DependsOn` ordering.

### 4.4 Domain Helpers (Noto, Zeto, Pente)

Convenience layers that assemble `TransactionInput` payloads with bundled ABIs, matching TypeScript's `NotoFactory`/`NotoInstance`, `ZetoFactory`/`ZetoInstance`, and `PenteFactory`/`PentePrivacyGroup`.

### 4.5 Dual WebSocket Clients

- `PaladinWebSocketClient`: subscribes to receipt batches and blockchain events using `ptx_subscribe`, sends `ptx_ack`/`ptx_nack`.
- `PrivacyGroupWebSocketClient`: subscribes to privacy-group messages using `pgroup_subscribe`, sends `pgroup_ack`/`pgroup_nack`.
- Both implement automatic reconnect and re-subscription (saved `SubscriptionConfig` list).

### 4.6 State Store Query Builder

Fluent query builder that serialises to `QueryJSON`/`IQuery` format, supporting nested `eq`, `neq`, comparisons, `in`, `or`, `limit`, `sort`.

### 4.7 Proof of Concept - Micro-PoC

To validate the core architecture, a focused micro-PoC has been implemented at [github.com/sogalabhi/paladin-poc](https://github.com/sogalabhi/paladin-poc). It demonstrates:
- Fluent `TxBuilder` with deferred error handling.
- Automatic `PD012220` idempotency clash recovery (mocked with `MockWebServer`).
- A simple `ZetoHelper` that uses the builder to send a transfer.

The full SDK will build on this foundation.

---

## SECTION 5: Implementation Timeline (22 Weeks, Part-Time, Jun - Nov 2026)

| Weeks | Phase | Deliverable | Gate |
|-------|-------|-------------|------|
| 1-2 | Foundation | Scaffold; `PaladinClient`, HTTP transport, `ptx` and `keymgr` stubs | Core call executes against dev node |
| 3-4 | Fluent TxBuilder | `TxBuilder` with public/private, domain, function, inputs, idempotency key, `DependsOn`; unit tests | Builder compiles and produces correct JSON-RPC |
| 5-6 | Queries & State | Query builder + `pstate` module; `pstate_queryStates` and `pstate_queryNullifiers` | Query serialisation matches Go/TS specs |
| 7-8 | Privacy Groups | `pgroup` module: create group, send transaction, call, messaging stubs | Group creation and private call succeed |
| 9-10 | Dual WebSockets (PTX) | `PaladinWebSocketClient` with subscribe/unsubscribe, ack/nack, reconnect, re-subscribe | Receipt batches delivered and acked in integration test |
| 11-12 | Dual WebSockets (pgroup) | `PrivacyGroupWebSocketClient` with message subscriptions; callback interface | Message batches delivered and acked |
| 13-14 | Domain Helpers - Noto & Zeto | `NotoFactory`/`NotoInstance`, `ZetoFactory`/`ZetoInstance` with bundled ABIs and `TransactionFuture` | End-to-end mint/transfer on dev node |
| 15-16 | Domain Helper - Pente | `PenteFactory`-style group flows; private EVM transaction and call | Private group deployment and invocation work |
| 17-18 | Extended Namespaces | `transport`, `bidx`, `reg`, `domain`, `debug` coverage; `getTransactionDependencies` | All core RPC groups have typed wrappers |
| 19-20 | Testing & Examples | Testcontainers integration suite; DvP atomic swap example; Javadoc | >80% code coverage; example runs against live node |
| 21-22 | Release & Polish | Maven Central publication; CI/CD alignment; final parity checklist | Published artifact with reproducible build |

---

## SECTION 6: Technology Stack

| Feature | Status | Application |
|---------|--------|-------------|
| Virtual Threads | Java 21+ | Concurrent connections without blocking |
| Records | Java 16+ | Immutable DTOs |
| BigInteger | Stable | 256-bit EVM numeric fields |
| HttpClient | Java 11+ | Async HTTP JSON-RPC |
| WebSocket (Jakarta / Tyrus) | Stable | Real-time subscriptions |
| Jackson | Stable | JSON serde |
| SLF4J | Stable | Logging facade with MDC |
| JUnit 5 + Testcontainers | Stable | Unit and integration tests |

**Error handling:** `RpcException` for JSON-RPC errors, `TransportException` for network/timeout failures.

---

## SECTION 7: Testing Strategy

**Unit tests (JUnit 5):**
- JSON-RPC request/response serde (`QueryJSON`, transaction DTOs, listener configs)
- Idempotency clash handling (`PD012220`) and recovery path (mirror Go behaviour)
- WebSocket routing tests (subscribe handshake vs notification; ack/nack payloads)
- Domain-helper payload builders produce JSON matching TS reference shapes

**Integration tests (Testcontainers):**
- Each module (Zeto, Noto, Pente) tested against a live Paladin node in Docker
- End-to-end DvP flow tested as a complete scenario
- WebSocket event subscription tested for correct delivery, acknowledgment, and reconnection resilience
- Uses official `ghcr.io/lf-decentralized-trust-labs/paladin` images (published by `cross-build-images.yaml`)

---

## SECTION 8: Release Process

- **Build tool:** Maven (chosen for enterprise familiarity, predictable CI with `mvnw`). *(If maintainers prefer Gradle+Kotlin, can be migrated during onboarding without API changes.)*
- **Group ID:** `org.paladinprivacy`
- **Artifact ID:** `paladin-java-sdk`
- **Build command:** `./mvnw clean deploy`
- **CI integration:** New Maven job in GitHub Actions, parallel to `release-typescript-sdk.yaml`
- **Publishing:** `nexus-staging-maven-plugin` for OSSRH (two-step: deploy -> close -> release)

---

## SECTION 9: Risk Mitigation

| Risk | Mitigation |
|------|-------------|
| RPC method signatures differ from SDK docs | Weeks 1-2 include direct node/RPC source verification before interface freeze |
| WebSocket reconnect edge cases (disconnect mid-batch) | Integration tests cover: connection drop before subscription confirmation, mid-batch delivery interruption, and ack timeout after reconnect |
| Maven Central publication delays | Release pipeline and signing/staging tasks scaffolded before final sprint |
| Community feedback requires interface changes | WIP PRs opened from early phases to gather maintainer feedback before broad API freeze |

---

## SECTION 10: Success Metrics

- Namespace parity for core modules (`ptx`, `keymgr`, `pstate`, `pgroup`, `transport`) with typed DTOs
- Working dual-WebSocket clients with explicit ack/nack and reconnect/re-subscribe behavior
- End-to-end domain helper flows for Noto, Zeto, and Pente against live node environments
- Idempotency clash recovery (`PD012220`) behavior matching Go SDK semantics
- Published Maven Central artifact with reproducible CI pipeline
- Developer docs and examples sufficient for Spring Boot integration without raw JSON-RPC boilerplate

---

## SECTION 11: Beyond the Core - Stretch Goals (Conditional)

*The following are not part of the mandatory 22-week mentorship scope. They will be explored only after the core SDK (namespace parity, `TxBuilder`, idempotency recovery, dual WebSockets, domain helpers, Testcontainers, Maven Central release) is complete, merged, and reviewed.*

### 11.1 Spring Boot Starter (`paladin-spring-boot-starter`)

Auto-configuration via `application.yml` or `application.properties` (node URL, timeouts, WebSocket settings, retry policies). `@Bean` registration of `PaladinClient`, `Ptx`, `KeyManager`, etc., ready for dependency injection. Seamless integration with Spring's `@Async` for non-blocking transaction submission and `@Scheduled` for receipt polling. Health indicators and metrics (via Micrometer) for monitoring SDK connectivity and WebSocket status.

### 11.2 Developer CLI & Operations Tooling

A lightweight command-line interface (e.g., `paladin-cli`) that wraps the SDK's core functionality for operators, CI/CD pipelines, and quick debugging. Planned commands: `tx send`, `receipt get`, `ws subscribe receipts`, `config validate`, `query states`. Additionally, a simple terminal dashboard (using Textual or Lanterna) to visualise live WebSocket receipt streams, transaction status, and configurable health metrics.

### 11.3 Observability & Metrics (Optional)

Built-in support for Micrometer metrics: counters for RPC requests (success, failure, retry); histograms for receipt polling latency and WebSocket reconnect delays; span propagation for correlating asynchronous transaction flows across application boundaries.

These stretch goals will be pursued only with mentor approval and after the core deliverable is stable. They represent the natural evolution of a thin RPC wrapper into a full-featured enterprise library.

---

## SECTION 12: About Me

### 12.1 Background

Abhijith Sogal, B.Tech, National Institute of Technology Karnataka (NITK Surathkal). I am a software engineer focused on interoperability, developer tooling, and reliable application architecture. My work spans enterprise-style systems and blockchain infrastructure. I am also comfortable with modern typed ecosystems beyond Java (Dart/Flutter in Iris app development), which helps when porting SDK ergonomics across language communities while preserving protocol correctness.

### 12.2 Relevant Projects

**Iris - Enterprise Financial & Academic ERP**  
Core app-team contributor for a college ERP serving 20,000+ downloads. The mobile frontend is in Dart/Flutter, which shares strong similarities with Java (object-oriented, statically typed, garbage-collected). Building a production-grade Flutter app taught me clean architecture, async state management, and rigorous API contract design.

**Polar Bridge - Cross-Chain Lending Protocol** (3rd prize)  
A cross-chain collateralized lending protocol (Stellar Soroban + Paseo EVM). Implemented a relayer that polls Stellar events (JSON-RPC) and maintains an idempotency ledger - directly mirrors Paladin's WebSocket ack/nack and receipt subscription model.

**FitStake - Web3 Fitness dApp** (ETHOnline 2025)  
Decentralised mobile app with Strava verification, Lit Protocol as a decentralised signer, and Envio indexer. Designed an oracle that verifies activity data and triggers on-chain verification - applicable to Paladin's receipt/event listeners.

**PokeWars - Multiplayer Pokémon Shooting Game** (ETHGlobal New Delhi)  
Real-time multiplayer game on Polygon with WebSocket sync. Implemented optimistic WebSocket updates before on-chain confirmation - directly applicable to designing responsive WebSocket subscriptions with ack/nack.

**Polkaflow - Visual Blockchain Workflow Builder** (3rd prize)  
Drag-and-drop workflow builder for PolkaVM and AssetHub generating Solidity/Rust contracts via AI or manual logic. Deep experience with RPC interactions (AssetHub), contract deployment pipelines, and building developer tooling that abstracts complex blockchain operations.

**Java Native Android Application - Arjun Guruji**  
Shipped Java Android app with 2,000+ downloads, covering lifecycle management, background concurrency, and production API integration.

### 12.3 Open-Source and Collaboration

- Maintainer of [Osdag-web](https://github.com/sogalabhi/Osdag-web) for 1+ year: reviews, merges, rebases, branch hygiene in multi-contributor environment.
- Profile links: [github.com/sogalabhi](https://github.com/sogalabhi) | [git.iris.nitk.ac.in/sogalabhi](https://git.iris.nitk.ac.in/sogalabhi)
- Active hackathon participant: ETHGlobal New Delhi, Polkadot Global Series 2024, ETHOnline 2025.

### 12.4 Pre-Application Work

- Built micro-PoC demonstrating fluent `TxBuilder` and automatic `PD012220` idempotency clash recovery ([github.com/sogalabhi/paladin-poc](https://github.com/sogalabhi/paladin-poc)).
- Opened PR #1168 adding four missing dispatch wrappers to TypeScript SDK.
- Read Go SDK modules (`pldclient`, `rpcclient`, `wsclient`, `query`) and TypeScript SDK modules (`paladin.ts`, `websocket.ts`, `domains/*`).
- Verified idempotency recovery behavior in Go `TxBuilder`.
- Reviewed doc-site architecture docs (`atomic_interop.md`, `noto.md`).

---

## SECTION 13: Availability and Commitment

- **Timezone:** IST (UTC+5:30)
- **Schedule:** Part-time from June to November 2026. During the summer break (June-August) I can commit 35-40 hours per week. During the academic term (September-November) I will commit 15-20 hours per week, with flexibility around exams.
- **Overlap:** Available from 8:00 AM IST to 10:00 PM IST, providing 4-5 hours overlap with EU business hours and 2-3 hours with US East Coast.
- **Communication:** Weekly video check-ins with written status updates; respond to Slack messages within a few hours on weekdays.

---

## SECTION 14: Approaches Deliberately Discarded

A well-designed enterprise SDK is defined as much by what it omits as what it includes.

**1. Java Flow API (Reactive Streams) for WebSockets**  
Using `Publisher`/`Subscriber` for event backpressure was rejected because it misaligns with Paladin's wire protocol: `request(n)` is a local demand signal, while `ptx_ack` is a strict wire-level acknowledgment.

**2. Core SDK `DvPManager` Abstraction**  
A fixed three-phase orchestration engine inside the core SDK was rejected. Such an engine would predefine stages like "deploy swap contract -> lock delivery asset -> lock payment asset -> execute atomic swap", which assumes every DvP scenario follows the same rigid pattern. Existing SDKs correctly expose only primitives (`sendTransaction`, `DependsOn`, receipt polling, idempotency keys). The Java SDK will follow the same philosophy, providing building blocks and a documented example.

**3. Client-Side Cryptography**  
Java-side EIP-712 signing, Babyjubjub arithmetic, and HD derivation were rejected; proving/signing boundaries remain inside the Paladin node.

**4. Preview-Feature Dependencies**  
Structured Concurrency (JEP 505) and Scoped Values (JEP 506) were excluded because they require `--enable-preview`, which enterprise production deployments cannot accept.

---

## SECTION 15: Closing Statement

Enterprise Java developers building on EVM infrastructure today often choose between hand-rolling fragile JSON-RPC clients or deferring privacy capabilities entirely. The Go and TypeScript SDKs solved this for their ecosystems. I have read both in depth, contributed a parity fix before applying, and designed a Java SDK plan that is predictable, enterprise-ready, and focused on protocol-accurate delivery without unnecessary abstractions. I know what needs to be built and why, and I want to build it.

Let's make Paladin accessible to every Java developer.

**Abhijith Sogal**  
May 2026
