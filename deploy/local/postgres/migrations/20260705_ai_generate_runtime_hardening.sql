\set ON_ERROR_STOP on

BEGIN;

CREATE SCHEMA IF NOT EXISTS cynexus_appl_ai;

CREATE TABLE IF NOT EXISTS cynexus_appl_ai.ai_execution_log (
    id bigint PRIMARY KEY,
    execution_no varchar(96) NOT NULL,
    capability varchar(128) NOT NULL,
    prompt_key varchar(128),
    prompt_version varchar(64),
    provider_code varchar(64),
    model_code varchar(128),
    status varchar(32) NOT NULL,
    input_tokens integer NOT NULL DEFAULT 0,
    output_tokens integer NOT NULL DEFAULT 0,
    total_tokens integer NOT NULL DEFAULT 0,
    latency_ms bigint NOT NULL DEFAULT 0,
    request_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    response_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    error_message text,
    extra jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ck_ai_execution_log_status CHECK (status IN ('SUCCEEDED', 'FAILED'))
);

ALTER TABLE cynexus_appl_ai.ai_execution_log
    ADD COLUMN IF NOT EXISTS capability varchar(128),
    ADD COLUMN IF NOT EXISTS prompt_key varchar(128),
    ADD COLUMN IF NOT EXISTS prompt_version varchar(64),
    ADD COLUMN IF NOT EXISTS provider_code varchar(64),
    ADD COLUMN IF NOT EXISTS total_tokens integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS error_message text,
    ADD COLUMN IF NOT EXISTS extra jsonb NOT NULL DEFAULT '{}'::jsonb;

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_execution_log_execution_no_alive
    ON cynexus_appl_ai.ai_execution_log (execution_no)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_ai_execution_log_capability_created_alive
    ON cynexus_appl_ai.ai_execution_log (capability, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_ai_execution_log_provider_model_alive
    ON cynexus_appl_ai.ai_execution_log (provider_code, model_code, created_at DESC)
    WHERE deleted_at IS NULL;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id,
    prompt_key,
    name,
    scenario_code,
    prompt_version,
    status,
    system_prompt,
    user_prompt_template,
    output_schema,
    variables,
    tags,
    extra,
    remark
) VALUES (
    910003000000000001,
    'knowledge_base_document_generate',
    'Knowledge Base Document Generate',
    'knowledge_base_document_generate',
    'v1',
    'active',
    '你是 Cynexus 知识库 Markdown 文档整理助手。
任务：根据用户输入的一段原始文本，提炼出适合保存到知识库的文档草稿。

要求：
- title：简洁明确，优先中文，最多 40 个字。
- description：一句话概括文档用途，最多 120 个字。
- tags：3 到 8 个标签，使用短词，不要重复。
- categories：1 到 3 个分类，偏业务主题，不要和 tags 完全重复。
- content：输出完整 Markdown 正文，结构清晰，保留原文中的关键事实、步骤、列表、代码或表格信息。
- 不要编造输入中没有的事实。
- 列表为空时输出 []，字符串为空时输出 ""。
- 严格输出合法 JSON，不要添加 Markdown 代码块或额外解释。',
    '{{formatInstructions}}

原始文本：
{{sourceText}}',
    '{"type":"object","required":["title","description","tags","categories","content"],"properties":{"title":{"type":"string"},"description":{"type":"string"},"tags":{"type":"array","items":{"type":"string"}},"categories":{"type":"array","items":{"type":"string"}},"content":{"type":"string"}}}'::jsonb,
    '["formatInstructions","sourceText"]'::jsonb,
    '["knowledge-base","structured-output","markdown"]'::jsonb,
    '{"capability":"knowledge_base_document_generate"}'::jsonb,
    'Knowledge Base Writing 页面 AI Generate 的结构化输出 prompt。'
)
ON CONFLICT DO NOTHING;

UPDATE cynexus_appl_ai.ai_usage_bindings
SET params = COALESCE(params, '{}'::jsonb) || '{"prompt_version":"v1","fallback_chain":["gpt-5.4"],"quality":"balanced","cost_tier":"standard"}'::jsonb,
    updated_at = now()
WHERE capability = 'knowledge_base_document_generate'
  AND deleted_at IS NULL;

COMMIT;
