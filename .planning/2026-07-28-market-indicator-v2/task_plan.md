# Market Indicator V2

## Goal

重构 Market Indicator，使用户能看懂全市场状态、发现行业/关联项联动、点击行业下钻成分股，并在真实 Release 页面完成验证。

## Phases

- [completed] 1. 梳理现有页面、API、数据模型与可用行业关系数据
- [completed] 2. 设计并实现关联关系与行业成分股接口
- [completed] 3. 重构前端为行业下钻与关系研究工作流
- [completed] 4. 测试、版本同步、构建并验证 Release

## Decisions

- 关联程度先使用可解释的规则评分：行业共同性、涨跌方向、变化幅度和同步采集时间；不伪造因果关系，界面使用“联动线索/关联度”措辞。
- 行业点击采用页面内下钻面板，不跳转、不丢失当前全市场视角。
- 成分股列表使用当前快照数据，并明确显示快照时间与数据可用性。

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| Repo-local impeccable context script missing | 1 | Used installed user-level script at `C:\Users\Cs7er\.claude\skills\impeccable\scripts\context.mjs` |
