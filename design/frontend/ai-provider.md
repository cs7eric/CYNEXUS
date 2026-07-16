# AI Provider Settings

The AI Provider settings page is the source of truth for runtime model routing.
Provider records include input and output token prices per one million tokens and
the pricing currency. These values are used as the cost fallback when Langfuse
does not return a provider-calculated total cost. Prices are entered as decimal
amounts in the provider currency; blank values are stored as zero. API keys are
never returned to the UI in clear text.

Capability bindings continue to select the provider and model used by each
business operation. Changing a binding is persisted automatically, while
provider identity, endpoint, credentials, and pricing are saved from the edit
dialog.
