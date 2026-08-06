# Findings

- User reports Agent tool calls fail with `market.stock_research` and says multiple tools have invalid arguments.
- Agent tool implementations are under `cynexus-ai-infra/.../agent`; schemas are split by bounded context and registered by `AgentToolRegistry`.
- Release stack is currently running version `1.1.281-260726-RELEASE`.
- Database evidence shows the reported failures persisted `{}` for `market.stock_research`, `market.stock_f10`, and `market.stock_information`; the registry then correctly rejected the missing required `operation` field.
- Root cause is in the Responses streaming parser: `response.output_item.added` keyed an accumulator by `call_id`, while `response.function_call_arguments.delta/done` keyed arguments by `item_id`. This created one accumulator with the tool name but no arguments and another with arguments but no tool name, producing `{}`.
