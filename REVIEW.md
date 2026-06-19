# Review instructions

## What Important means here

Reserve 🔴 Important for findings that would break behavior, leak data, or create a security
vulnerability: auth bypasses, token leakage in logs, missing TLS validation, unhandled
coroutine failures that silently swallow errors, unbounded resource allocation (e.g. an
infinite polling loop without cancellation), and PII or credentials in user-visible UI or logs.
Style, naming, and refactoring suggestions are 🟡 Nit at most.

## Always check (always Important if violated)

- Token reads/writes go through `TokenStore` — no raw DataStore file access, no SharedPreferences,
  no globals.
- `refreshTokens` in `HttpClientFactory` calls `markAsRefreshTokenRequest()` — without it a 401
  on the refresh triggers another refresh → infinite loop.
- No raw `Authorization` header construction outside `HttpClientFactory` / the Ktor `Auth` plugin.
- `StateFlow<UiState>` values are immutable; updates go through `_state.update { it.copy(...) }`,
  never direct reassignment.
- `UiState` data classes are annotated `@Immutable`.
- Collection fields inside `UiState` use `ImmutableList` / `ImmutableMap` from
  `kotlinx.collections.immutable`, never stdlib `List` / `Map`.
- Every focusable Compose element has a `contentDescription` (or explicit `null` for decorative).
- No hardcoded user-visible strings in Compose code — every string lives in `res/values/strings.xml`.
- No `runBlocking` on the main thread — ViewModel `initializer {}` blocks, `@Composable` functions,
  and any non-coroutine call site are main-thread. Exception: lambdas the library documents as
  running off-main (e.g. `DataSource.Factory` in Media3).
- Composables never receive a `ViewModel` as a parameter — the NavHost composable block owns VM
  creation; screens receive `state` and `onIntent` only.

## Always check

- New API call sites go through a `Repository`, never directly from a `ViewModel`.
- New `ViewModel`s expose a single `UiState` + `onIntent(intent)` entry point.
- Any new ViewModel has a corresponding `*ViewModelTest` covering at least the happy path and one
  error path.
- `CompletableDeferred` in tests is always `.complete()`'d or `.cancel()`'d before the test body
  ends — a deferred left unresolved parks a coroutine past the `runTest` block.

## Do not report

- Formatting, import ordering, or lint issues — detekt handles these in CI.
- Anything inside `app/build/generated/openapi/` — generated, never edited by hand.
- The `runBlocking { ... }` inside the `DataSource.Factory` lambda in `PlayerViewModel` — ExoPlayer
  calls `createDataSource()` on its loader thread, not main; intentional.
- Missing tests for screens / Composables — UI tests are explicitly out of scope for MVP.

## Cap the nits

Report at most five 🟡 Nits per review. If you found more, say "plus N similar items" in the
summary. If all findings are nits, lead the summary with "No blocking issues."
