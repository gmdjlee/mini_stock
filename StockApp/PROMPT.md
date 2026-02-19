Read TASK.md and PROGRESS.md.

Mission: Generate a comprehensive migration specification document for the feature located at Stock Analysis > Financial Information. The spec must be detailed enough for another development team to independently replicate this feature in a different project without access to this codebase.

Agent Team (2 members): Spawn 2 teammates.
1. Analyzer (Sonnet): Use feature-extractor subagent to analyze all code, dependencies, data flows for the Financial Information feature. Catalog every file, class, API call, data model.
2. Spec-Writer (Sonnet): Use spec-writer subagent to produce the migration specification document. Translate analysis into developer-ready documentation.

Use Subagents:
- feature-extractor (haiku): Fast codebase scanning and dependency mapping.
- spec-writer (sonnet): Detailed document generation.

Document Requirements:
The final MIGRATION_SPEC.md must include ALL of these sections:
1. Feature Overview: purpose, user flows, screenshots description
2. Architecture Diagram: mermaid diagram showing all layers and data flow
3. File Manifest: every file needed with its role
4. Data Models: all entities, DTOs, database tables with field definitions
5. API Contracts: every network call with URL, method, params, response schema
6. Business Logic: step-by-step algorithm for each calculation or transformation
7. Dependencies: external libraries with versions, shared modules needed
8. DI Configuration: Hilt modules, bindings, scopes
9. UI Components: screens, views, navigation with layout descriptions
10. Resource List: strings, drawables, dimensions, styles used
11. Migration Checklist: ordered steps to implement this feature from scratch
12. Edge Cases and Error Handling: documented failure modes and recovery

Workflow:
1. Lead reads TASK.md, assigns next task.
2. Analyzer scans codebase, logs structured findings to PROGRESS.md.
3. Spec-Writer converts findings into spec sections.
4. Cross-review: Analyzer verifies spec accuracy against actual code.
5. Lead marks task done after verification.

Completion (ALL must be met):
- Every task in TASK.md is checked done.
- MIGRATION_SPEC.md generated with all 12 required sections.
- FILE_MANIFEST.md listing every file with role and dependencies.
- PROGRESS.md contains LOOP_COMPLETE.

Output COMPLETE when ALL verified.