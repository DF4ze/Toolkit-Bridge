# Shared Global Context Boundary

`workspace/global-context` stores stable markdown knowledge intended to be reloaded by agents.

Examples:
- durable user profile
- stable project conventions
- reusable global rules

`workspace/shared` remains reserved for current work artifacts and exchanges.

Examples:
- reports
- exports
- scripts
- drafts

The distinction is materialized in code by `WorkspaceLayout`, `WorkspaceArea`, and the dedicated `SharedGlobalContextProvider`, which only reads from the global context root.
