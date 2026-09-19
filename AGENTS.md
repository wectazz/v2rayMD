# v2rayMD agent guide

Android client for Xray/v2fly cores. Kotlin, Gradle Kotlin DSL, Compose + Material 3,
coroutines, AndroidX lifecycle. Android project lives under `V2rayMD/`; run Gradle from there
(`./gradlew` on POSIX, `gradlew.bat` on Windows). Versions: read SDK levels from
`V2rayMD/app/build.gradle.kts`, dependencies from `V2rayMD/gradle/libs.versions.toml`,
CI toolchain (SDK 37, NDK, Java) from `.github/workflows/build.yml` — never copy numbers here.

## Scoped guides (mandatory)

- Before editing `V2rayMD/app/src/main/java/com/v2ray/md/service/`,
  `V2rayMD/app/src/test/java/com/v2ray/md/service/`, `core/CoreServiceManager.kt`,
  `core/LauncherManager.kt`, `root/`, `contracts/ServiceControl.kt`,
  `contracts/Tun2SocksControl.kt`, `handler/NotificationManager.kt`,
  `helper/MessageHelper.kt`, or `helper/NotificationHelper.kt`,
  read `V2rayMD/app/src/main/java/com/v2ray/md/service/AGENTS.md`.
- Before editing `V2rayMD/app/src/main/java/com/v2ray/md/ui/` or
  `V2rayMD/app/src/test/java/com/v2ray/md/ui/`,
  read `V2rayMD/app/src/main/java/com/v2ray/md/ui/AGENTS.md`.
- Where root and scoped guides conflict, the scoped guide wins; all other root rules stay active.

## Ownership

- `core/` owns native config and lifecycle, `handler/` data and app operations, `service/`
  Android services, `ui/` Compose screens and ViewModels. Extend the owning module instead of
  adding a new manager/repository/holder for the same data or lifetime.
- `AngApplication` initializes MMKV, locales, WorkManager, defaults, theme. Persist app data
  through `MmkvManager`/`SettingsManager`; do not add `SharedPreferences` for data they own.
- Shared lifecycle/config code must keep VPN, proxy-only, and root-mode branches. Change one
  mode only when the task names it; gate the change and preserve the others.

## Build: submodules and generated native artifacts

- `AndroidLibXrayLite` and `hev-socks5-tunnel` are git submodules. Clean checkout needs
  `git submodule update --init --recursive`.
- `V2rayMD/app/libs/` is generated, not source: `libv2ray.aar` is downloaded from the
  `AndroidLibXrayLite` release matching that submodule's tag, and HEV binaries are built by
  `compile-hevtun.sh` (needs `NDK_HOME`; produces `libhev-socks5-tunnel.so` for
  `TProxyService` plus the `libhevsockstun.so` root-mode executable). Reproduce
  `.github/workflows/build.yml` before trusting a clean-checkout build. Never edit or commit
  `app/libs/` output unless the task upgrades a native dependency — then name the source
  revision and verify every ABI in `splits.abi` (or `ABI_FILTERS` when set).
- Flavors are `playstore` and `fdroid` (`BuildConfig.DISTRIBUTION`). Default to the Play Store
  debug variant; run both variants when the change branches on flavor, `DISTRIBUTION`, or
  flavor-specific resources/dependencies.

## Commands and validation

- Docs-only: `git diff --check` (no Gradle task needed).
- Focused JVM test from `V2rayMD/`:
  `./gradlew :app:testPlaystoreDebugUnitTest --tests "com.v2ray.md.handler.OrphanProfileCleanerTest"`
  (replace `Playstore` with `Fdroid` for F-Droid-only changes).
- Kotlin/Java change: run the test class for each changed behavior, then
  `:app:testPlaystoreDebugUnitTest` and `:app:compilePlaystoreDebugKotlin`.
- Resources, manifest, Gradle, dependencies, native packaging: `:app:assemblePlaystoreDebug`.
- Service lifecycle, framework callback, native, permission, accessibility, focus, or state
  restoration: also check on an emulator or device — a JVM test or APK does not verify runtime.
- Add/extend JUnit tests under `V2rayMD/app/src/test/java/` for changed deterministic logic.
  Report any required check you could not run as `Not run` with command and reason; never
  claim an unrun behavior is verified.

## Cross-cutting rules

- Keep the diff inside task scope; no unrelated refactors, renames, or formatting-only changes.
  There is no enforced Kotlin formatter — preserve local indentation and import order.
- Identify servers/groups by GUID and group ID in persistence, async work, and UI state. Never
  use a list index or adapter/paging position as identity.
- Run disk, network, package-manager, native, bitmap, and CPU-heavy work off the main thread in
  a named lifecycle/ViewModel scope; cancel when the owner ends or newer work supersedes it.
- Put visible and accessibility text in Android resources. Update every locale in
  `androidResources.localeFilters` for each changed key, preserving placeholders, plurals, and
  formatting tags.
- Never rename/delete a persisted, serialized, routing, or import/export field without a
  backward-compatible migration plus a regression test reading the prior released format.
  Multi-record writes must complete together or roll back partial writes on failure.
- Log recoverable failures via `LogUtil` with operation, mode/component, stable non-secret ID,
  and exception. Never log credentials, full proxy URLs, private keys, or exported configs.
- Comment each platform workaround with the Android/vendor boundary, the failure it prevents,
  and the exact removal condition.
- Guard APIs above `minSdk` by runtime SDK check in the same function; never crash or report
  false success on lower versions. Do not introduce new deprecated or experimental API call
  sites outside task scope; scope any suppression/opt-in to the direct declaration.

--

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

Tradeoff: These guidelines bias toward caution over speed. For trivial tasks, use judgment.

1. Think Before Coding
Don't assume. Don't hide confusion. Surface tradeoffs.

Before implementing:

State your assumptions explicitly. If uncertain, ask.
If multiple interpretations exist, present them - don't pick silently.
If a simpler approach exists, say so. Push back when warranted.
If something is unclear, stop. Name what's confusing. Ask.
2. Simplicity First
Minimum code that solves the problem. Nothing speculative.

No features beyond what was asked.
No abstractions for single-use code.
No "flexibility" or "configurability" that wasn't requested.
No error handling for impossible scenarios.
If you write 200 lines and it could be 50, rewrite it.
Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

3. Surgical Changes
Touch only what you must. Clean up only your own mess.

When editing existing code:

Don't "improve" adjacent code, comments, or formatting.
Don't refactor things that aren't broken.
Match existing style, even if you'd do it differently.
If you notice unrelated dead code, mention it - don't delete it.
When your changes create orphans:

Remove imports/variables/functions that YOUR changes made unused.
Don't remove pre-existing dead code unless asked.
The test: Every changed line should trace directly to the user's request.

4. Goal-Driven Execution
Define success criteria. Loop until verified.

Transform tasks into verifiable goals:

"Add validation" → "Write tests for invalid inputs, then make them pass"
"Fix the bug" → "Write a test that reproduces it, then make it pass"
"Refactor X" → "Ensure tests pass before and after"
For multi-step tasks, state a brief plan:

1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

These guidelines are working if: fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.