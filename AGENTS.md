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
- The submodule tag and the downloaded `libv2ray.aar` must come from the same release; never mix
  revisions. Keep the AAR out of git (the whole `app/libs/` directory is ignored).
- Flavors are `playstore` and `fdroid` (`BuildConfig.DISTRIBUTION`). Default to the Play Store
  debug variant; run both variants when the change branches on flavor, `DISTRIBUTION`, or
  flavor-specific resources/dependencies.

## Versioning and upstream sync

- This fork tracks `2dust/v2rayNG`. Port upstream changes one-for-one (one upstream commit = one
  local commit) and adapt them to this tree. Do not drop our own divergences — Material 3
  expressive setup with the pre-release override, the Monet/material-kolor stack, the combined
  connection bar, cookie-shaped badges and the quick-action menu — unless the task says so.
- `versionName` mirrors the upstream release being synced (numeric `X.Y.Z`); `versionCode` is our
  own monotonically increasing sequence and may run ahead of upstream.
- AGP is pinned to the version the maintainer's Android Studio supports. Do not bump it from an
  upstream sync without checking Studio compatibility; keep the pin and say why in the report.

## Subscription identity

- The default subscription `User-Agent` is `v2rayMD/<versionName>/Android`
  (`util/AppUserAgent.kt`). A per-subscription `userAgent` override wins; do not hardcode another
  client name or append extra segments — panels match the three-part UA prefix.
- When HWID sending is enabled, subscription requests send `X-HWID` plus `x-device-os`,
  `x-ver-os` and `x-device-model`. Device values come from `Build` and are sanitized before they
  reach a header; the headers are gated by the same setting as `X-HWID`.

## Profile pickers

- `ui/compose/ServerSelectListDialog.kt` is the shared picker for profile remarks (routing
  outbound, proxy-chain member, policy-group fallback, subscription pre/next profile). It
  searches `remarks` only; keep its list height bounded inside the dialog.
- `EConfigType.CUSTOM` profiles are whole Xray configs and are intentionally excluded from those
  pickers. `CUSTOM_OUTBOUND` profiles are single outbounds and stay selectable.

## Workflow: коммиты

- Не коммить и не пушь каждый шаг; изменения копятся локально.
- Коммит и пуш — только когда пользователь скажет; тогда разложить изменения по отдельным коммитам (один коммит = одно изменение, хоть на несколько файлов), если не указано иначе.
- Стежить только явные пути; никогда `git add .`/`-A`/`commit -a`.

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
- This checkout lives on a Drvfs mount (`/mnt/d`): a WSL Gradle run can fail with
  `java.io.IOException: Input/output error`. In that environment run builds and tests in Android
  Studio and mark every unrun check `Not run` with the reason.

## Keeping this guide current

- When a change alters build/versioning rules, native artifacts, subscription identity, profile
  pickers, or fork-specific divergences, update this file in the same batch and commit it
  separately as a docs change.

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

## Чеклист перед сдачей задачи

1. **Внешние источники:** Всегда читай предоставленные URL и документацию перед генерацией кода.
   