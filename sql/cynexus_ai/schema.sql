CREATE TABLE IF NOT EXISTS ai_provider_config (
    id BIGINT PRIMARY KEY,
    provider_code VARCHAR(64) NOT NULL,
    provider_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_provider_code UNIQUE (provider_code)
);

CREATE TABLE IF NOT EXISTS ai_model_config (
    id BIGINT PRIMARY KEY,
    model_code VARCHAR(128) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    model_name VARCHAR(128),
    base_url VARCHAR(512),
    api_key_secret_ref VARCHAR(256),
    support_tool_call BOOLEAN NOT NULL DEFAULT FALSE,
    support_structured_output BOOLEAN NOT NULL DEFAULT FALSE,
    support_vision BOOLEAN NOT NULL DEFAULT FALSE,
    max_context_tokens INTEGER,
    default_temperature NUMERIC(4, 2),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_model_code UNIQUE (model_code)
);

CREATE TABLE IF NOT EXISTS ai_scenario_config (
    id BIGINT PRIMARY KEY,
    scenario_code VARCHAR(128) NOT NULL,
    scenario_name VARCHAR(128) NOT NULL,
    default_model_code VARCHAR(128) NOT NULL,
    fallback_model_code VARCHAR(128),
    prompt_version VARCHAR(64) NOT NULL,
    max_tool_calls INTEGER NOT NULL DEFAULT 6,
    require_tool_call BOOLEAN NOT NULL DEFAULT FALSE,
    require_json_output BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_scenario_code UNIQUE (scenario_code)
);

CREATE TABLE IF NOT EXISTS ai_prompt_template (
    id BIGINT PRIMARY KEY,
    scenario_code VARCHAR(128) NOT NULL,
    prompt_version VARCHAR(64) NOT NULL,
    system_prompt TEXT NOT NULL,
    user_prompt_template TEXT,
    output_schema_json JSONB,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_prompt_version UNIQUE (scenario_code, prompt_version)
);

CREATE TABLE IF NOT EXISTS ai_tool_definition (
    id BIGINT PRIMARY KEY,
    tool_code VARCHAR(128) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_tool_code UNIQUE (tool_code)
);

CREATE TABLE IF NOT EXISTS ai_tool_binding (
    id BIGINT PRIMARY KEY,
    scenario_code VARCHAR(128) NOT NULL,
    tool_code VARCHAR(128) NOT NULL,
    max_call_count INTEGER NOT NULL DEFAULT 3,
    timeout_ms INTEGER NOT NULL DEFAULT 3000,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_tool_binding UNIQUE (scenario_code, tool_code)
);

CREATE TABLE IF NOT EXISTS ai_execution_log (
    id BIGINT PRIMARY KEY,
    execution_no VARCHAR(64) NOT NULL,
    scenario_code VARCHAR(128) NOT NULL,
    model_code VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_tokens INTEGER NOT NULL DEFAULT 0,
    output_tokens INTEGER NOT NULL DEFAULT 0,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    request_json JSONB,
    response_json JSONB,
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_execution_no UNIQUE (execution_no)
);

CREATE TABLE IF NOT EXISTS ai_tool_call_log (
    id BIGINT PRIMARY KEY,
    execution_no VARCHAR(64) NOT NULL,
    tool_code VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    request_json JSONB,
    response_json JSONB,
    error_message TEXT,
    created_by VARCHAR(64) NOT NULL DEFAULT 'self',
    updated_by VARCHAR(64) NOT NULL DEFAULT 'self',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
