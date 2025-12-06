# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

This is a Spring Boot Maven project using Java 17. Use these commands for development:

- **Build and package:** `mvn clean install`
- **Run tests:** `mvn test` 
- **Run single test:** `mvn test -Dtest=AuthSessionRedisApplicationTests`
- **Run application:** `mvn spring-boot:run`
- **Build Docker image:** `docker build -t auth-java-service .`
- **Run with Docker Compose:** `docker-compose up --build`
- **Run with Podman Compose:** `podman-compose up --build`

## Architecture Overview

This is a session authentication service that uses Redis for session storage. Key architectural components:

### Core Structure
- **Package:** `com.plainprog.auth_session_redis`
- **Main Application:** `AuthSessionRedisApplication.java`
- **Configuration:** `configs/` package with `SecurityConfig` and `RedisConfig`
- **Controller:** `SessionController` with two main endpoints
- **Service:** `SessionExplorerService` for session data management
- **Models:** `SessionData` and `BasicUserInfoDTO`

### SessionExplorerService Methods
- `getSessionData(String sessionId)` - Retrieves session data from Redis by session ID
- `deleteSession(String sessionId)` - Deletes session from Spring Session repository and Redis
- `getSessionDataBatch(List<String> sessionIds)` - Batch retrieves multiple sessions using Redis pipelining for performance

### Key Design Decisions
- **Security:** Only allows requests from localhost (127.0.0.1, ::1) - modify `SecurityConfig.java:21-26` to change this
- **Redis Integration:** Uses MessagePack serialization for efficient session storage
- **Session Management:** Spring Session with Redis backend on custom port 5050
- **Authentication:** Header-based user/authorities for session initiation

### API Endpoints
- `POST /api/session/validate` - Validates existing session (authenticated endpoint)
- `POST /api/session/initiate` - Creates new session with user data and authorities
- `DELETE /api/session/terminate/{sessionId}` - Terminates session by ID (unprotected backend endpoint)
- `POST /api/session/validate-batch` - Validates multiple sessions in batch (unprotected backend endpoint)

### Configuration Notes
- Application runs on port 5060 (configurable via `server.port`)
- Redis connection via environment variable `REDIS_AUTH_HOST` (defaults to localhost)
- Session timeout: 1800 seconds (30 minutes)
- Redis uses custom port 5050 to avoid conflicts

### Dependencies
- Spring Boot 3.4.4 with Security and Web
- Spring Session Data Redis
- Redis Jedis client
- MessagePack for serialization