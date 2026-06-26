# Cynexus Project Brief

## Project Name

cynexus

Cynexus is a personal intelligent workspace platform. AI is only one part of the system.

## Positioning

Cynexus is not only an AI system. It will include:

- AI orchestration
- market analysis
- document / knowledge base
- strategy / backtesting
- personal workspace
- future admin panel

## Architecture

Use DDD + Maven multi-module + microservice-ready structure.

Initial services:

- cynexus-ai
- cynexus-market
- cynexus-common

Future services:

- cynexus-document
- cynexus-strategy
- cynexus-oss
- cynexus-admin
- cynexus-gateway
- cynexus-iam, only if multi-user support is needed later

## Naming

Root project: cynexus  
GroupId: com.cynexus  
Java package root: com.cynexus  
Service names:

- cynexus-ai
- cynexus-market

## Tech Stack

- JDK 21
- Spring Boot 4.0.x
- Spring AI 2.0.x
- Spring Cloud 2025.1.x
- Spring Cloud Alibaba 2025.1.0.0
- Nacos 3.1.1
- PostgreSQL
- Redis
- Elasticsearch later
- MyBatis-Plus
- JobRunr first, XXL-JOB optional later
- RabbitMQ optional later
- Docker Compose for local development
- Jenkins later
- Prometheus + Grafana later

## Local Development

First stage runs on Windows local machine.

Use:

- Windows
- IntelliJ IDEA
- JDK 21
- Maven
- Docker Desktop
- WSL2 backend

First Docker Compose core services:

- PostgreSQL
- Redis
- Nacos

Do not add Jenkins, Elasticsearch, RabbitMQ, Prometheus, Grafana in the first runnable version unless explicitly requested.

## Security

This is a single-user personal system for now.

Do not create user-service or auth-service in the first version.

Use single-user mode:

- no login/register
- no RBAC
- no tenant system
- optional admin token later
- keep created_by / updated_by fields with value self

Future service name for full auth should be cynexus-iam, not cynexus-user.

## ID Strategy

Use BIGINT primary key.

Prefer distributed ID / Snowflake style ID.

Do not use UUID as default primary key.

Use public_id or code only when resource IDs are exposed externally.

## First Phase Modules

Create only:

- cynexus-common
- cynexus-ai
- cynexus-market

Do not create document, strategy, gateway, admin, oss yet.

## Maven Module Style

Follow the existing devs-phere style:

Each business service is a Maven aggregate module.

Example:

cynexus-ai
- cynexus-ai-api
- cynexus-ai-common
- cynexus-ai-domain
- cynexus-ai-infra
- cynexus-ai-application
    - cynexus-ai-application-controller
- cynexus-ai-starter

cynexus-market
- cynexus-market-api
- cynexus-market-common
- cynexus-market-domain
- cynexus-market-infra
- cynexus-market-application
    - cynexus-market-application-controller
- cynexus-market-starter

Common modules:

cynexus-common
- cynexus-common-core
- cynexus-common-web
- cynexus-common-security
- cynexus-common-db
- cynexus-common-cloud

## AI Service Responsibility

cynexus-ai is responsible for:

- AI scenario execution
- model routing
- model adapter
- prompt management
- tool calling
- structured JSON output
- output validation
- fallback model
- AI execution logs
- token/cost statistics later

cynexus-ai should not directly own market data logic.

## Market Service Responsibility

cynexus-market is responsible for:

- market overview
- sector rank
- stock daily feature
- stock intraday feature
- money flow feature
- market feature compression

cynexus-ai calls cynexus-market through tools / remote client.

## AI Model Design

Use:

- Adapter Pattern
- Strategy Pattern
- Factory / Router
- DB based model config later

Core concepts:

- AiModelAdapter
- AiModelAdapterRegistry
- DynamicChatClientFactory
- ModelRouter
- ToolRegistry
- ScenarioExecutionAppService
- AgentExecutor
- OutputValidator

## AI Config Storage

AI business config should be stored in PostgreSQL later:

- ai_provider_config
- ai_model_config
- ai_scenario_config
- ai_prompt_template
- ai_tool_definition
- ai_tool_binding
- ai_execution_log
- ai_tool_call_log

Nacos should be used for infrastructure config and service discovery, not for large prompt content.

## Nacos

Use Nacos for:

- service registration
- service discovery
- environment config
- shared infrastructure config

Use spring.config.import instead of old bootstrap style.

## First Runnable Goal

Build a minimal runnable version:

1. cynexus-ai-starter starts on port 18081
2. cynexus-market-starter starts on port 18082
3. both register to Nacos
4. PostgreSQL and Redis connection config exists
5. cynexus-ai can call cynexus-market later through Feign
6. project compiles successfully