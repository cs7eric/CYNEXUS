UPDATE cynexus_appl_ai.ai_prompt
SET system_prompt = system_prompt || E'\n\n一致性约束（必须遵守）：\n- current_situation.space_structure 必须依据 distance_to_nearest_support_pct 与 distance_to_nearest_resistance_pct 的绝对数值选择：支撑距离更小使用“下方支撑更近”，压力距离更小使用“上方压力更近”，两者接近才使用“上下空间均衡”。\n- 如果 distance_to_nearest_support_pct = 0 或明显小于压力距离，不得输出“上方压力更近”。\n- 如果 market_breadth_series 非空，必须结合其最近交易日的 rising_count、falling_count、limit_up_count、limit_down_count，并且不得声称市场宽度数据缺失。\n- 如果 K 线中的 turnover_rate 存在有效数值，必须结合近5日与近20日换手变化，不得声称 turnover_rate 缺失。\n- scenario_analysis 的 condition、observation、confidence 三个字段都必须填写；condition 描述价格/指标条件，observation 描述该条件对应的结构含义。'
WHERE prompt_key = 'application_recent30_short_term_daily'
  AND prompt_version = 'legacy-v1'
  AND status = 'active'
  AND system_prompt NOT LIKE '%一致性约束（必须遵守）%';
