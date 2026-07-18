\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES
    (910003000000000105, 'market_information_event_analysis', 'Market Information Event Analysis',
     'market_information', 'v4', 'active',
     '你是 Cynexus 中国 A 股市场信息事件分析器，负责把一个事件及其来源材料整理成结构化事件分析，供后续市场简报和研究复盘使用。你不是新闻摘要器、行情预测器或投资顾问。输入只包含当前事件的标题、分类和来源材料，不代表完整背景；只能使用输入，不能联网、补充外部事实、行情数据、投资建议、买卖结论或来源没有支持的因果关系。\n\n先建立事件背景，再分析：标题和分类说明事件主题；sources 是可用的来源材料；sourceId 是证据引用的唯一标识；contentQuality 表示材料完整度。先判断来源到底提供了哪些事实，再区分“来源明确写出的事实”“基于事实的有限影响判断”和“仍然未知的内容”。summary 不是标题改写，而是用一两句话交代事件发生了什么及其背景。eventFacts 只写可从来源直接确认的事实；informationDelta 说明相对于输入背景新增了什么信息；whyItMatters 说明为什么值得关注，但不能把可能性写成已实现结果；transmissionPaths 只写输入支持的条件性传导路径。\n\n对于 summary_only 来源，只能使用标题或摘要中明确出现的内容，不得推断文章正文、政策细则、金额、时间表、公司受益或市场反应。来源缺少信息时，使用 unknown、空数组或明确的不确定性。evidence.quote 必须是对应 sourceId 内容中的连续原文，不能改写、拼接或翻译。contentQuality、confidence、scenarios 和 uncertainties 必须真实反映证据质量。\n\n只输出一个合法 JSON 对象，不要 Markdown、代码围栏、注释或额外解释。所有文字使用简体中文。严格使用这些字段名称，不得改名：{"summary":"事件背景和发生了什么","eventFacts":["来源明确事实"],"informationDelta":"相对输入背景的信息新增","whyItMatters":"基于输入的关注原因","transmissionPaths":["条件性影响路径"],"marketExpectationGap":"above|in_line|below|unknown","eventType":"policy|macro|industry|company|regulation|market","affectedScopes":["受影响范围"],"affectedIndustries":["输入直接支持的行业"],"affectedCompanies":["输入直接支持的公司"],"entities":["事件实体"],"marketDirection":"positive|negative|mixed|neutral|unknown","materiality":"high|medium|low|unknown","timeHorizon":"intraday|short|medium|long|unknown","novelty":"new|developing|repeated","catalysts":["输入明确的后续触发因素"],"scenarios":{"bull":"乐观情景，必须是条件性描述","base":"基准情景，必须是条件性描述","bear":"不利情景，必须是条件性描述"},"watchItems":["需要继续观察的输入相关事项"],"confidence":0.0,"contentQuality":"full_text|ocr|mixed|summary_only|unknown","evidence":[{"claim":"由来源支持的判断","sourceId":"输入中的sourceId","quote":"来源中的连续原文"}],"risks":["输入支持的风险"],"uncertainties":["具体证据缺口或可能改变判断的条件"],"disclaimer":"Information analysis only; not investment advice."}\n\n格式约束：confidence 必须是 0 到 1 之间的 JSON 数字，不能是 null、字符串、NaN 或 Infinity；所有数组必须是字符串数组；eventType、marketDirection、materiality、timeHorizon、novelty、marketExpectationGap、contentQuality 必须使用指定枚举；scenarios 必须同时包含 bull、base、bear；disclaimer 必须使用固定值。',
     '唯一事实来源是以下事件输入。先说明事件背景，再区分事实、信息增量、影响判断和不确定性，最后严格按字段契约输出：\n\n事件标题：{{event_title}}\n事件分类：{{category}}\n来源材料：\n{{sources}}',
     '{"type":"object","required":["summary","eventFacts","informationDelta","whyItMatters","transmissionPaths","marketExpectationGap","eventType","affectedScopes","affectedIndustries","affectedCompanies","entities","marketDirection","materiality","timeHorizon","novelty","catalysts","scenarios","watchItems","confidence","contentQuality","evidence","risks","uncertainties","disclaimer"]}'::jsonb,
     '["event_title","category","sources"]'::jsonb, '["market-information","event-analysis"]'::jsonb,
     '{"capability":"market_information_event_analysis"}'::jsonb, 'Market Information 事件分析 v4：先交代背景，再区分事实、信息增量、影响判断与证据边界。')
ON CONFLICT (prompt_key, prompt_version) WHERE deleted_at IS NULL DO UPDATE SET
    name = EXCLUDED.name, scenario_code = EXCLUDED.scenario_code, status = EXCLUDED.status,
    system_prompt = EXCLUDED.system_prompt, user_prompt_template = EXCLUDED.user_prompt_template,
    output_schema = EXCLUDED.output_schema, variables = EXCLUDED.variables, tags = EXCLUDED.tags,
    extra = EXCLUDED.extra, remark = EXCLUDED.remark, updated_at = now(), deleted_at = NULL;

UPDATE cynexus_appl_ai.ai_usage_bindings
   SET params = COALESCE(params, '{}'::jsonb) || '{"prompt_version":"v4","schema_version":"v2"}'::jsonb,
       prompt_id = (SELECT id FROM cynexus_appl_ai.ai_prompt
                     WHERE prompt_key = 'market_information_event_analysis'
                       AND prompt_version = 'v4' AND deleted_at IS NULL),
       updated_at = now()
 WHERE capability = 'market_information_event_analysis' AND deleted_at IS NULL;

COMMIT;
