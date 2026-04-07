# RateShield: Distributed Rate Limiter

RateShield is a production-style distributed rate limiter built with Spring Boot, Redis, and a React frontend. It demonstrates how multiple backend instances can enforce consistent request limits by using Redis as a shared coordination layer instead of local in-memory counters.

This project is designed to be both technically solid and interview-ready. It focuses on algorithm design, distributed systems correctness, Redis atomicity, clean backend architecture, and an interactive frontend for simulation and comparison.

## Repository Structure

```text
rateshield-distributed-rate-limiter/
+-- backend/
¦   +-- pom.xml
¦   +-- Dockerfile
¦   +-- src/
¦   +-- target/
+-- frontend/
¦   +-- package.json
¦   +-- src/
¦   +-- vite.config.ts
+-- docker-compose.yml
+-- render.yaml
+-- readme.md
```

## Tech Stack

- Backend: Java 21, Spring Boot
- Data store: Redis
- Frontend: React, Vite, TypeScript, Tailwind CSS
- Charts: Recharts
- Containerization: Docker, Docker Compose
- Cloud deployment: Render Blueprint (`render.yaml`)

## What This Project Demonstrates

- Distributed rate limiting with shared state in Redis
- Three common rate limiting algorithms:
  - Fixed Window
  - Sliding Window
  - Token Bucket
- Redis atomic operations using Lua scripts
- Clean backend layering with strategy-based algorithm selection
- Frontend simulation, dashboard metrics, and side-by-side algorithm comparison
- Dockerized local environment for backend + Redis
- Render deployment for backend, frontend, and managed Redis

## High-Level Architecture

```text
Client / Frontend
        |
        v
Spring Boot API
        |
        v
RateLimitingService
        |
        v
RateLimiter Strategy
  |        |         |
  v        v         v
Fixed   Sliding   Token Bucket
Window   Window
  \        |         /
   \       |        /
    \      |       /
           Redis
```

### Backend Flow

1. A request hits the simulation API.
2. The service creates a rate limit policy and normalized request model.
3. The service chooses the correct algorithm implementation.
4. The algorithm talks to Redis.
5. Redis evaluates the request atomically.
6. The backend returns whether the request is allowed or blocked, how many tokens/requests remain, and when retry is possible.

### Frontend Flow

1. The simulator page sends requests to `POST /simulate`.
2. The API service normalizes request payloads.
3. The frontend records request outcomes in a local metrics store.
4. The dashboard reads those events and renders charts.
5. The compare page runs the same workload across all algorithms and presents the outcomes side by side.

## Backend Architecture

### Packages

```text
backend/src/main/java/com/rateshield
+-- config
+-- controller
+-- dto
+-- exception
+-- model
+-- ratelimiter
¦   +-- core
¦   +-- fixedwindow
¦   +-- slidingwindow
¦   +-- tokenbucket
+-- service
```

### Design Choices

- `controller`: transport layer only
- `service`: orchestration and logging
- `ratelimiter.core`: common algorithm contract
- `ratelimiter.*`: algorithm-specific implementations
- `config`: Redis and app configuration
- `dto`: request and response contracts
- `model`: internal domain objects

This separation keeps the code testable, readable, and easy to explain in interviews.

## Algorithms

### 1. Fixed Window

Fixed window keeps one counter per subject and per time bucket.

Example:
- limit = 5 requests per 60 seconds
- Redis key represents the current 60-second bucket
- each request increments the bucket counter
- if count > 5, reject

Why use it:
- simple
- fast
- low memory usage

Limitation:
- burst problem near window boundaries
- a client can send requests at the end of one bucket and again at the start of the next

### 2. Sliding Window

Sliding window keeps individual request timestamps in a Redis sorted set.

Example:
- remove timestamps older than `now - window`
- count remaining timestamps
- allow only if count is still below the limit

Why use it:
- much fairer than fixed window
- avoids hard bucket resets

Trade-off:
- higher Redis cost
- more memory usage
- more per-request work

### 3. Token Bucket

Token bucket stores available tokens and last refill time.

Example:
- bucket has a maximum capacity
- tokens refill steadily over time
- each request consumes one token
- if no token is available, reject

Why use it:
- allows short bursts
- still enforces long-term rate
- strong practical choice for APIs

Trade-off:
- slightly more complex logic
- needs atomic refill + consume behavior

## Redis Design

Redis is the core of the distributed system. Without Redis, each backend instance would count requests independently and users could bypass limits by hitting different servers.

### Key Naming Strategy

Keys are namespaced by algorithm and subject.

Examples:

```text
rateshield:fixed:user:123:/api/search:29589720
rateshield:sliding:client:demo_key:/api/orders
rateshield:token:api_key:key_abc:/api/payments
```

### Data Structures

#### Strings
Used for fixed window counters.

Why:
- simple counter semantics
- low memory footprint
- very fast

#### Sorted Sets
Used for sliding window timestamps.

Why:
- each request is stored with a score equal to its timestamp
- old requests can be removed efficiently
- active requests can be counted accurately

#### Hash-style state via Lua access
Used for token bucket state.

Why:
- store token count and last refill time together
- update refill and consumption atomically

## Why Atomicity Matters

Rate limiting is a correctness problem, not just a counting problem.

If two servers check the same subject at nearly the same time, naive multi-command Redis logic can let both requests through when only one should pass.

Examples of concurrency issues:
- two requests both see the same remaining slot
- TTL is not set after increment because of a partial failure
- two servers spend the same token in a token bucket
- sliding window cleanup and insert operations interleave incorrectly

### How This Project Solves It

This project uses Redis Lua scripts so multi-step logic executes atomically.

That means operations like these happen as one unit:
- increment + expire for fixed window
- cleanup + count + add + expire for sliding window
- refill + consume + persist + expire for token bucket

This is one of the strongest distributed systems talking points in the project.

## Frontend Structure

```text
frontend/src
+-- app
+-- components
¦   +-- dashboard
¦   +-- layout
¦   +-- simulation
¦   +-- ui
+-- lib
+-- pages
+-- router
+-- services
¦   +-- api
+-- types
```

### Why It Is Organized This Way

- `pages`: route-level screens only
- `components`: reusable UI and feature building blocks
- `services/api`: backend communication isolated from UI
- `router`: route definitions only
- `lib`: local metrics persistence and helper logic
- `types`: shared frontend contracts

This keeps the UI scalable as new pages are added for metrics, policy management, and advanced visualizations.

## Available Frontend Pages

- `/`: project overview
- `/simulate`: send repeated requests using one selected algorithm
- `/dashboard`: view allowed vs blocked totals and charts
- `/compare`: run the same workload across all three algorithms

## API

### Simulation Endpoint

`POST /simulate`

Example request:

```json
{
  "algorithm": "TOKEN_BUCKET",
  "subjectType": "CLIENT",
  "subjectId": "demo-client",
  "resource": "/api/search",
  "limit": 5,
  "windowSizeInSeconds": 60
}
```

Example response:

```json
{
  "success": true,
  "message": "Simulation completed",
  "data": {
    "status": "ALLOWED",
    "allowed": true,
    "remainingTokens": 4,
    "retryAfterSeconds": 0,
    "message": "Request allowed using token bucket"
  },
  "error": null
}
```

## Running the Project

### Option 1: Backend + Redis with Docker

Prerequisites:
- Docker
- Docker Compose

Run from the repository root:

```bash
docker compose up --build
```

This starts:
- Redis on `localhost:6379`
- Spring Boot backend on `localhost:8080`

### Option 2: Run Backend Locally

Prerequisites:
- Java 21
- Maven
- Redis running locally on port `6379`

Run backend:

```bash
cd backend
mvn spring-boot:run
```

Environment variables supported:
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_TIMEOUT`
- `PORT`
- `SERVER_PORT`
- `CORS_ALLOWED_ORIGINS`

### Option 3: Run Frontend Locally

Prerequisites:
- Node.js 20+
- npm

Install and run:

```bash
cd frontend
npm install
npm run dev
```

Vite dev server runs on:
- `http://localhost:5173`

The frontend proxies `/simulate` and `/api` to the backend on `http://localhost:8080` when `VITE_API_BASE_URL` is not set.

## Deploying on Render

This repository now includes a Render Blueprint at [render.yaml](./render.yaml).

### What the Blueprint Creates

- `rateshield-backend`: Docker-based web service
- `rateshield-frontend`: static site built from `frontend/`
- `rateshield-redis`: Render Key Value instance

### Files Added for Render

- [render.yaml](./render.yaml)
- [frontend/.env.example](./frontend/.env.example)
- backend CORS support in `backend/src/main/java/com/rateshield/config/WebConfig.java`
- backend `PORT` support in `backend/src/main/resources/application.yml`
- frontend API base URL support in `frontend/src/services/api/client.ts`

### Render Deployment Steps

1. Push this repository to GitHub.
2. In Render, choose `New +` -> `Blueprint`.
3. Connect the GitHub repository.
4. Render will detect [render.yaml](./render.yaml).
5. Create the resources.
6. When Render prompts for `VITE_API_BASE_URL`, set it to your backend public URL.

Example:

```text
https://your-backend-service.onrender.com
```

7. After the backend is live, redeploy the frontend if needed so the final API URL is embedded into the Vite build.

### Important Render Notes

- The backend binds to `PORT`, which Render provides automatically.
- The backend CORS configuration currently defaults to `*` for easy portfolio deployment.
  Tighten this later to your exact frontend domain if you want stricter production security.
- The frontend is a static site, so `VITE_API_BASE_URL` must be known at build time.
- The Render Redis instance is wired into the backend automatically through `REDIS_HOST` and `REDIS_PORT` in the Blueprint.

## Testing

Unit tests are included for the three algorithms.

Covered areas:
- allow path
- block path
- invalid Redis response edge cases
- correct Redis key and argument wiring

Run tests:

```bash
cd backend
mvn test
```

Note:
In restricted environments without Maven dependency access, tests may not execute until dependencies are available locally.

## Interview Talking Points

### System Design
- “This project solves distributed rate limiting by moving request state into Redis so all backend nodes share the same source of truth.”
- “I used a strategy-based design so the service layer stays stable while algorithms can be swapped independently.”

### Redis and Correctness
- “I chose Redis structures based on algorithm needs: strings for counters, sorted sets for rolling timestamps, and token state managed atomically through Lua.”
- “The important part is not just counting requests but preserving correctness under concurrency.”
- “Lua scripts were used to make multi-step Redis workflows atomic.”

### Algorithm Trade-offs
- “Fixed window is simple and fast but can be bursty at boundaries.”
- “Sliding window is fairer but costs more in memory and Redis operations.”
- “Token bucket is often the most practical production choice because it supports burst tolerance with long-term control.”

### Frontend/Product Thinking
- “I built a simulator, dashboard, and comparison page so the system is not just implemented, but observable and explainable.”
- “The frontend architecture separates routing, page composition, reusable components, and API access for maintainability.”

### Cloud / Deployment Story
- “I containerized the backend, added a Render Blueprint, and wired the frontend, backend, and managed Redis into one deployable setup.”
- “I adapted the backend to cloud runtime constraints by binding to Render’s `PORT` and adding CORS support for a separately hosted frontend.”

### Resume-Ready Summary
- Built a distributed rate limiter using Spring Boot and Redis with support for fixed window, sliding window, and token bucket algorithms.
- Designed Redis key strategy and atomic Lua-script execution to prevent race conditions under concurrent load.
- Developed a React + TypeScript frontend with simulation, dashboard, and side-by-side algorithm comparison views.
- Dockerized backend and Redis for reproducible local development and added Render deployment support with managed infrastructure configuration.

## What To Improve Next

Good next steps for taking this project further:
- add integration tests with real Redis
- add per-tenant or per-endpoint policy management
- add Micrometer / Prometheus metrics
- add multi-instance backend deployment in Docker Compose
- lock down CORS to the exact frontend domain in Render
- add persistence for policies instead of simulation-only policy creation

## License

This project is currently for educational and portfolio use.
