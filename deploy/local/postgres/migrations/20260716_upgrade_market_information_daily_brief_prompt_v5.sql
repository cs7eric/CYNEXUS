\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES
    (910003000000000103, 'market_information_daily_brief', 'Market Information Daily Brief',
     'market_information', 'v5', 'active',
     '你是 Cynexus 中国 A 股市场信息简报生成器，负责把一次当前输入快照中的已完成事件分析，整理成一份面向研究和复盘的市场级信息简报。你不是新闻搜索器、行情终端或投资顾问。输入只代表系统在指定时间窗口内筛选出的部分市场信息，不等于完整市场事实；你只能使用输入内容，不能联网、补充外部背景、猜测指数或价格，也不能给出投资建议、买卖结论或未被输入支持的因果关系。\n\n先理解输入背景，再生成简报：analysisWindow 是本次快照的时间范围和事件数量，sourceCoverage 是来源覆盖统计，events 是已完成的事件分析。每个 event 的 clusterId 是事件引用，title/category/时间和 analysis 共同描述事件背景；analysis 中的 eventFacts、informationDelta、whyItMatters、transmissionPaths、affectedScopes、scenarios、evidence、risks、uncertainties 是可用信息。summary_only、unknown 或缺失字段必须明确保留，不能用常识补齐。\n\n简报必须先回答“这批信息在讲什么”，再回答“哪些事件值得关注、为什么、影响可能如何传导、哪些地方仍不确定”。背景介绍不是外部宏观叙事：只能从输入中的时间范围、事件分类、事件事实、信息变化、影响范围和来源覆盖中归纳。不要把一条事件标题改写成没有背景的口号，也不要把推测写成事实。\n\n输出规则：只输出一个合法 JSON 对象，不要 Markdown、代码围栏、前后解释或注释。使用简体中文。headline 是对本次输入主线的准确概括，不要使用“市场信息简报”这类空泛标题。executiveSummary 用 3 至 5 句完整中文说明：本次快照覆盖什么时间和信息范围、主要事件或主题是什么、它们为什么值得关注、当前判断的边界是什么；若输入不足，要直接说明不足。keyEvents 只选输入中最重要且有分析依据的事件，每项 summary 必须包含事件背景、已知信息变化和关注原因，不能只重复标题；每项只能引用输入中的 clusterId。policySignals、industrySignals、companySignals 和 riskSignals 只能从事件分析归纳，必须通过 eventReferences 标明依据；没有足够证据时输出空数组或 unknown，不要强行填充。marketTone 只表示这批输入呈现的整体信息方向，不代表指数涨跌预测。uncertainties 要写出具体未知项、证据缺口或可能改变判断的条件。sourceCoverage 和 analysisWindow 必须原样保留输入值。eventReferences 只能使用输入中存在的 clusterId，并覆盖简报正文实际引用的事件。disclaimer 必须使用固定值。',
     '以下是本次市场信息快照。请把它当作唯一事实来源，先梳理背景和信息主线，再生成结构化简报。\n\n当前事件分析输入：\n{{analysis_input}}',
     '{"type":"object","required":["headline","executiveSummary","marketTone","keyEvents","policySignals","industrySignals","companySignals","riskSignals","uncertainties","sourceCoverage","analysisWindow","eventReferences","disclaimer"]}'::jsonb,
     '["analysis_input"]'::jsonb, '["market-information","daily-brief"]'::jsonb,
     '{"capability":"market_information_daily_brief"}'::jsonb, 'Market Information 市场简报 v5：强调输入背景、信息主线、事件依据和不确定性。')
ON CONFLICT (prompt_key, prompt_version) WHERE deleted_at IS NULL DO UPDATE SET
    name = EXCLUDED.name, scenario_code = EXCLUDED.scenario_code, status = EXCLUDED.status,
    system_prompt = EXCLUDED.system_prompt, user_prompt_template = EXCLUDED.user_prompt_template,
    output_schema = EXCLUDED.output_schema, variables = EXCLUDED.variables, tags = EXCLUDED.tags,
    extra = EXCLUDED.extra, remark = EXCLUDED.remark, updated_at = now(), deleted_at = NULL;

UPDATE cynexus_appl_ai.ai_usage_bindings
   SET params = COALESCE(params, '{}'::jsonb) || '{"prompt_version":"v5","schema_version":"v2"}'::jsonb,
       prompt_id = (SELECT id FROM cynexus_appl_ai.ai_prompt
                     WHERE prompt_key = 'market_information_daily_brief'
                       AND prompt_version = 'v5'
                       AND deleted_at IS NULL),
       updated_at = now()
 WHERE capability = 'market_information_daily_brief'
   AND deleted_at IS NULL;

COMMIT;
