# Constants Placement Rules

- Repeated literals with shared meaning must be defined once in a `Constants` class and referenced by callers.
- Put truly cross-module values in `cynexus-common`, normally `cynexus-common-core`.
- Put values shared by one bounded service in that service's `cynexus-xxx-common` module.
- Put values used by one layer in that layer's local common package, such as `cynexus-xxx-*/src/main/java/.../<layer>/common/constant`.
- Keep business-specific constants in their owning module even when another module consumes the API.
- Local `private static final` values remain appropriate for implementation details, derived values, regular expressions, SQL fragments, and single-use business rules.
- A constants refactor must preserve DDD dependency direction and must not make domain depend on infra.
