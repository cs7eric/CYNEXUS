\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES
    (910003000000000101, 'market_information_event_analysis', 'Market Information Event Analysis',
     'market_information', 'v3', 'active',
     '你是中国 A 股市场信息事件分析助手。只使用提供的来源，不得补充外部事实、投资建议或买卖结论。使用简体中文，只输出一个合法 JSON 对象。Evidence 中的 quote 必须是来源中的连续原文；summary_only 来源可能只有标题，不得推断文章细节。输出必须包含 summary、eventFacts、informationDelta、whyItMatters、transmissionPaths、marketExpectationGap、eventType、marketDirection、materiality、timeHorizon、novelty、scenarios、confidence、contentQuality、evidence、risks、uncertainties、disclaimer。',
     '事件标题：{{event_title}}\n分类：{{category}}\n来源：{{sources}}',
     '{"type":"object","required":["summary","eventFacts","informationDelta","whyItMatters","transmissionPaths","marketExpectationGap","eventType","marketDirection","materiality","timeHorizon","novelty","scenarios","confidence","contentQuality","evidence","risks","uncertainties","disclaimer"]}'::jsonb,
     '["event_title","category","sources"]'::jsonb, '["market-information","event-analysis"]'::jsonb,
     '{"capability":"market_information_event_analysis"}'::jsonb, 'Market Information 事件分析的可版本化运行 prompt。'),
    (910003000000000102, 'market_information_daily_brief', 'Market Information Daily Brief',
     'market_information', 'v4', 'active',
     '你是中国 A 股市场信息简报助手。根据当前提供的事件分析生成市场级简报，保留不确定性，不得补充输入之外的事实或投资建议。使用简体中文，只输出合法 JSON，并保留输入中的事件引用。输出必须包含 headline、executiveSummary、marketTone、keyEvents、policySignals、industrySignals、companySignals、riskSignals、uncertainties、sourceCoverage、analysisWindow、eventReferences、disclaimer。',
     '当前事件分析输入：\n{{analysis_input}}',
     '{"type":"object","required":["headline","executiveSummary","marketTone","keyEvents","policySignals","industrySignals","companySignals","riskSignals","uncertainties","sourceCoverage","analysisWindow","eventReferences","disclaimer"]}'::jsonb,
     '["analysis_input"]'::jsonb, '["market-information","daily-brief"]'::jsonb,
     '{"capability":"market_information_daily_brief"}'::jsonb, 'Market Information 市场简报的可版本化运行 prompt。')
ON CONFLICT (prompt_key, prompt_version) WHERE deleted_at IS NULL DO UPDATE SET
    name = EXCLUDED.name, scenario_code = EXCLUDED.scenario_code, status = EXCLUDED.status,
    system_prompt = EXCLUDED.system_prompt, user_prompt_template = EXCLUDED.user_prompt_template,
    output_schema = EXCLUDED.output_schema, variables = EXCLUDED.variables, tags = EXCLUDED.tags,
    extra = EXCLUDED.extra, remark = EXCLUDED.remark, updated_at = now(), deleted_at = NULL;

UPDATE cynexus_appl_ai.ai_usage_bindings
   SET params = COALESCE(params, '{}'::jsonb) || CASE capability
       WHEN 'market_information_event_analysis' THEN '{"prompt_version":"v3","schema_version":"v2"}'::jsonb
       WHEN 'market_information_daily_brief' THEN '{"prompt_version":"v4","schema_version":"v2"}'::jsonb
       ELSE '{}'::jsonb END,
       updated_at = now()
 WHERE capability IN ('market_information_event_analysis', 'market_information_daily_brief')
   AND deleted_at IS NULL;

UPDATE cynexus_appl_ai.ai_usage_bindings binding
   SET prompt_id = prompt.id,
       updated_at = now()
  FROM cynexus_appl_ai.ai_prompt prompt
 WHERE binding.capability = prompt.prompt_key
   AND binding.params ->> 'prompt_version' = prompt.prompt_version
   AND binding.deleted_at IS NULL
   AND prompt.deleted_at IS NULL;

COMMIT;
