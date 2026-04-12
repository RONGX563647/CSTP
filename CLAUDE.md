# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AiSale is a campus second-hand trading platform with a Vue 3 + TypeScript frontend and Spring Boot 3.3.4 backend. The platform uses an offline pickup/face-to-face transaction model with no logistics delivery.

**Tech Stack:**
- **Backend**: Spring Boot 3.3.4, Spring Data JPA, Spring Security, JWT (jjwt 0.12.6), Hutool 5.8.23, Maven
- **Frontend**: Vue 3.5, Vite 6.2, TypeScript 5.8, Element Plus 2.8, Pinia 3.0, Vue Router 5.0
- **Database**: H2 (development), PostgreSQL (production)
- **Java Version**: 17

## Commands

### Backend (Spring Boot)

```bash
cd backend

# Run application (dev mode with H2)
mvn spring-boot:run

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=OrderServiceTest,OrderReviewServiceTest

# Build package
mvn clean package

# View API docs
# Open http://localhost:8090/swagger-ui.html
```

### Frontend (Vue 3)

```bash
cd frontend

# Install dependencies
npm install

# Run dev server (port 3000, proxies /api to backend:8090)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### API Testing

```bash
cd backend/tests
python api_full_coverage_test.py
```

## Architecture

### Backend Structure

```
backend/src/main/java/com/aisale/backend/
├── config/          # Security, AI, Knife4j configurations
├── controller/      # REST controllers (user + admin endpoints)
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities (User, Admin, Product, Order, OrderReview, Address)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter, SecurityConfig
├── service/         # Business logic layer
└── util/            # Utilities (JwtUtil, JwtRequestUtils)
```

### Frontend Structure

```
frontend/src/
├── api/             # API client (axios-based)
├── layouts/         # Layout components (MobileLayout, AdminLayout, AuthLayout)
├── router/          # Vue Router configuration
├── stores/          # Pinia stores (auth store)
├── utils/           # Utilities (request.ts - axios interceptor)
└── views/
    ├── user/        # User-facing H5 pages (mobile-first)
    └── admin/       # Admin management pages (desktop)
```

### Key Design Patterns

**Entity Relationships:**
- `User` → `Address` (one-to-many)
- `User` → `Product` (one-to-many, as seller)
- `User` → `Order` (one-to-many, as buyer/seller)
- `Order` → `OrderReview` (one-to-many)
- `Order` → `OrderLog` (one-to-many)

**Order State Machine:**
```
PENDING_PAYMENT → PENDING_PICKUP → PENDING_CONFIRM → PENDING_REVIEW → COMPLETED
       ↓                  ↓                ↓
   CANCELLED          CANCELLED        CANCELLED
```

**Security:**
- JWT-based authentication with Bearer token prefix
- Roles: USER, ADMIN, SUPER_ADMIN
- Public endpoints: `/api/auth/**`, `/api/user/products/public/**`
- User endpoints require authentication: `/api/user/**`
- Admin endpoints require ADMIN/SUPER_ADMIN role: `/api/admin/**`

**API Response Format:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

## Test Accounts (DataInitializer)

| Username | Password | Role | Notes |
|----------|----------|------|-------|
| superadmin | 123456 | SUPER_ADMIN | Full admin access |
| admin | 123456 | ADMIN | Regular admin |
| zhangsan | 123456 | USER | Has 2 addresses, sells products |
| lisi | 123456 | USER | Has 1 address, sells products |
| wangwu | 123456 | USER | No addresses, buyer only |

## Configuration

**application.yml:**
- Backend runs on port 8090
- H2 console available at `/h2-console`
- Active profile: `dev`
- AI chat disabled by default (`AI_CHAT_ENABLED=false`)

**vite.config.ts:**
- Frontend dev server runs on port 3000
- Proxies `/api/*` requests to `http://localhost:8090`

**Environment Variables (.env):**
- `OPENAI_API_KEY` - OpenAI API key
- `OPENAI_BASE_URL` - OpenAI base URL (optional)

## Common Development Tasks

**Adding a new entity:**
1. Create entity class in `entity/` with JPA annotations
2. Create repository interface extending `JpaRepository`
3. Create DTOs in `dto/` for request/response
4. Create service class with business logic
5. Create controller with REST endpoints
6. Add API methods in frontend `api/` folder
7. Update `SecurityConfig` if new endpoints need special access rules

**Modifying order flow:**
- Order logic is in `OrderService.java` with transactional methods
- State transitions are validated before each operation
- Order history is logged via `OrderLog` entity
- Frontend order operations are in `UserOrderController` (user) and `AdminOrderController` (admin)

**Frontend-page routing:**
- User routes: `/user/*` with `MobileLayout` wrapper
- Admin routes: `/admin/*` with `AdminLayout` wrapper
- Auth guard checks `requiresAuth` and `role` meta fields
- Token stored as `"Bearer <token>"` in localStorage
