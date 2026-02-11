# AGENTS.md — Claude Code Agent Teams Configuration

## Agent: feature-agent
### Role: Feature UI + ViewModel development
### Allowed: `feature/*/src/**`, `app/src/**/navigation/**`
### Forbidden: `core/data/**`, `core/network/**`, `core/database/**`, other features
### Rules:
- ONE ViewModel + ONE UiState + ONE UiEvent per screen
- ViewModel receives UseCases via @HiltViewModel constructor injection
- NEVER access Repository/DataSource directly
- Use collectAsStateWithLifecycle() in Composables
- One-time effects via SharedFlow<UiEffect>

## Agent: domain-agent
### Role: Business logic (UseCase) + domain models + repository interfaces
### Allowed: `core/domain/src/**`, `core/model/src/**`
### Forbidden: ALL other modules
### Rules:
- Pure Kotlin only (no android.* imports except @Inject)
- UseCase = class with suspend operator fun invoke() returning Result<T>
- Repository = interface only (no implementation here)
- Domain models = plain data classes in :core:model

## Agent: data-agent
### Role: Repository impl + DataSource + Mapper + DTO/Entity + Hilt modules
### Allowed: `core/data/src/**`, `core/network/src/**`, `core/database/src/**`
### Forbidden: `core/domain/**` (read-only), `feature/**`
### Rules:
- RepositoryImpl implements interface from :core:domain
- DataSource swap = new impl + update Hilt binding (nothing else changes)
- Mappers bridge DTO ↔ Domain ↔ Entity
- All impl classes are `internal`

## Agent: ui-agent
### Role: Design system + shared UI components
### Allowed: `core/ui/src/**`, `core/designsystem/src/**`
### Forbidden: All other modules
### Rules:
- :core:designsystem = theme, colors, typography
- :core:ui = reusable composables that render domain models
- All composables must have @Preview

## Agent: integration-agent
### Role: App wiring, navigation host, build config
### Allowed: `app/src/**`, `build-logic/**`, root build files
### Forbidden: Implementation details of any module

## Agent: test-agent
### Role: Tests across all modules
### Allowed: `*/src/test/**`, `*/src/androidTest/**`, `core/testing/src/**`
### Rules:
- Shared fakes in :core:testing
- Domain: Pure JUnit + FakeRepository
- ViewModel: JUnit + Turbine + fake UseCases
- Repository: JUnit + fake DataSources

## Cross-Agent Data Source Swap Protocol
1. data-agent creates new DataSourceImpl
2. data-agent updates Hilt module binding
3. NO other agents involved — zero cross-boundary impact

## Cross-Agent New Feature Protocol
1. feature-agent defines needed UseCase signature
2. domain-agent creates UseCase + repository interface
3. data-agent implements repository + data sources
4. feature-agent integrates UseCase into ViewModel
5. integration-agent registers navigation in AppNavHost
