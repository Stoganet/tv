# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

Native Android TV client for the stoganet ecosystem. **Kotlin + Compose-for-TV**, single Gradle
module (`:app`), manual DI via a `ServiceLocator`, MVI-lite (single `UiState` + `onIntent`) per
screen. Talks exclusively to `api-proxy` (`https://api.stoganet.com`) — never directly to
Jellyfin. Sibling repos: `api-proxy` (Go backend), `infra` (compose stack), `edge` (Caddy),
`stogad` (Rust host daemon).

## Key wiring

**Two OkHttp clients:** `rawOkHttp` (no auth, used by `AuthHandler` for token refresh) and
`authedOkHttp` (adds `Authorization: Bearer` via `AuthHandler` as OkHttp `Interceptor`; handles
401 → refresh → retry via `AuthHandler` as OkHttp `Authenticator`). Using `authedOkHttp` inside
the refresh path would cause an infinite 401 loop — that's why the refresh always goes through
`rawOkHttp`.

**Two NavHosts:** `AuthNavHost` (no tokens → Quick Connect screen today) and `AppNavHost`
(authenticated → Home placeholder today). `MainActivity` reads `TokenStore` to decide which to
show.

**ServiceLocator graph:** `TokenStore` → `AuthHandler` → `authedOkHttp` → (screens use authed
Retrofit). `rawOkHttp` is used by `AuthHandler` only. New repositories should be added to
`ServiceLocator` and wired there.

**OpenAPI client:** Generated from `openapi/openapi.yaml` (kept in sync with `api-proxy`'s spec)
via `./gradlew :app:openApiGenerate`. Output lands in `app/build/generated/openapi/`. When
`api-proxy` adds new endpoints, copy the updated spec here and regenerate before implementing
the repository method.

## Architecture invariants

These cross-file constraints matter when editing — violating any is a 🔴 Important in review:

- **`AuthInterceptor` uses a separate `refreshClient`** — calling the authed client from inside
  the interceptor creates an infinite 401 loop.
- **Token refresh is serialised by `refreshMutex`** — N parallel 401s must trigger exactly one
  refresh; the other N-1 see the new token and retry.
- **All token access goes through `TokenStore`** (Proto DataStore + Tink). No raw
  `SharedPreferences`, no direct file I/O, no globals.
- **`UiState` is immutable** and updated only via `_state.update { it.copy(...) }`.
- **`UiState` data classes are annotated `@Immutable`** — tells the Compose compiler the type is
  stable, enables recomposition skipping.
- **Collections inside `UiState` are `ImmutableList` / `ImmutableMap`** from
  `kotlinx.collections.immutable`.
- **`ViewModel → Repository → generated ApiClient`** layering is one-way.
- **Composables never receive a `ViewModel` as a parameter.** The NavHost `composable {}` block
  owns ViewModel creation and state collection; it passes `state: UiState` and
  `onIntent: (Intent) -> Unit` to the screen composable. This keeps screens pure, previewable,
  and testable without Android framework dependencies.
- **Every screen composable has `@Preview` functions** for each meaningful UI state.
- **All interactive TV elements have an explicit `contentDescription`** via
  `Modifier.semantics { contentDescription = "..." }`. On TV, TalkBack reads `contentDescription`
  from the focusable element — child `Text` implicit labels are unreliable on D-pad focus.
- **Strings live in `res/values/strings.xml`.** No hardcoded user-visible English in Compose code.
- **No telemetry / crash reporting / analytics.**
- **CI actions are SHA-pinned**, never tag-pinned.

## Common operations

```bash
./gradlew :app:installDebug
./gradlew :app:testDebugUnitTest
./gradlew detekt
./gradlew detekt --auto-correct
./gradlew :app:openApiGenerate
./gradlew :app:assembleRelease
```

## Editing notes

- **Generated OpenAPI client (`app/build/generated/openapi/`) is never edited by hand.**
- **Android TV manifest is load-bearing.** Do not remove `LEANBACK_LAUNCHER`, the leanback
  `uses-feature`, the banner reference, or flip `allowBackup` to true.
- **ProGuard rules in `app/proguard-rules.pro`** are required for release builds.

## Stoganet conventions

Skills in `.claude/skills/` (if present) enforce the org-wide workflow — invoke them via the
Skill tool:

- **committing** — Conventional Commits; match the style used in `api-proxy`.
- **creating-pull-requests** — PR title becomes the squash-commit.
- **creating-issues** — Shared body template across all Stoganet repos.
- **handoff** — Use before context compaction when there's uncommitted work worth preserving.
