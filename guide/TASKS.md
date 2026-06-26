# Codex Task

Please create the initial Cynexus Maven multi-module project structure.

## Requirements

Use Java 21 and Maven.

Create a parent Maven project named cynexus.

GroupId: com.cynexus  
Version: 1.0.0-SNAPSHOT

Use these versions:

- Spring Boot 4.0.x
- Spring AI 2.0.x
- Spring Cloud 2025.1.x
- Spring Cloud Alibaba 2025.1.0.0
- MyBatis-Plus Spring Boot 4 starter
- JobRunr Spring Boot 4 starter

## Create Modules

Create:

- cynexus-common
    - cynexus-common-core
    - cynexus-common-web
    - cynexus-common-security
    - cynexus-common-db
    - cynexus-common-cloud

- cynexus-ai
    - cynexus-ai-api
    - cynexus-ai-common
    - cynexus-ai-domain
    - cynexus-ai-infra
    - cynexus-ai-application
        - cynexus-ai-application-controller
    - cynexus-ai-starter

- cynexus-market
    - cynexus-market-api
    - cynexus-market-common
    - cynexus-market-domain
    - cynexus-market-infra
    - cynexus-market-application
        - cynexus-market-application-controller
    - cynexus-market-starter

## Starter Classes

Create:

- com.cynexus.ai.CynexusAiApplication
- com.cynexus.market.CynexusMarketApplication

Use:

@SpringBootApplication(scanBasePackages = "com.cynexus")

For AI service also enable Feign clients.

## Ports

- cynexus-ai: 18081
- cynexus-market: 18082

## Config

Create application.yml for each starter.

Use Nacos config import style:

spring.config.import:
- optional:nacos:cynexus-ai.yml
- optional:nacos:cynexus-common.yml

For market:

spring.config.import:
- optional:nacos:cynexus-market.yml
- optional:nacos:cynexus-common.yml

Configure:

- spring.application.name
- server.port
- nacos server addr env
- postgres datasource env
- redis env
- actuator health/info/prometheus

## Do Not Create Yet

Do not create:

- cynexus-document
- cynexus-strategy
- cynexus-gateway
- cynexus-admin
- cynexus-oss
- cynexus-user
- cynexus-iam
- Jenkins config
- Elasticsearch config
- RabbitMQ config
- Prometheus config

## Goal

The project should compile with:

mvn clean package -DskipTests