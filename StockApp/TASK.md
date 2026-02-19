# TASK.md — Financial Info Feature Migration Spec

## Phase 1: Feature Boundary Analysis (iterations 1-3)
- [x] S-001 Identify feature entry point: find the Financial Information screen/fragment in navigation
- [x] S-002 Trace Presentation layer: ViewModels, Fragments, adapters, UI components for this feature
- [x] S-003 Trace Domain layer: UseCases, Repository interfaces, Entity/model classes
- [x] S-004 Trace Data layer: Repository impls, API services, DTOs, response mappers, local DB

## Phase 2: Dependency and Data Analysis (iterations 4-6)
- [x] S-005 Map external dependencies: libraries used (Retrofit, Room, charting libs, etc.) with versions
- [x] S-006 Map shared dependencies: core modules, utilities, base classes shared with other features
- [x] S-007 Document API contracts: every network endpoint (URL, method, headers, request/response schema)
- [x] S-008 Document data models: entities, DTOs, DB tables with all field definitions and types

## Phase 3: Business Logic Documentation (iterations 7-8)
- [x] S-009 Document calculation logic: financial ratios, indicators, data transformations step-by-step
- [x] S-010 Document UI logic: list/detail flows, filtering, sorting, chart rendering, error states

## Phase 4: Spec Assembly (iterations 9-10)
- [x] S-011 Assemble MIGRATION_SPEC.md with all 12 sections, architecture diagram (mermaid)
- [x] S-012 Create FILE_MANIFEST.md: complete file list with layer, role, dependencies
- [x] S-013 Cross-verify: Analyzer confirms spec matches actual implementation
