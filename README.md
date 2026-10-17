# iTalase

Offline-first POS (point of sale) for small shops / warung. Built with Kotlin Multiplatform, so
Android and iOS share the same business logic, data layer and Compose UI. No network needed for
daily use — everything lives on the device.

## Features

| Screen          | What it does                                                                                                                                                                     |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Beranda**     | Today's omzet, profit, transaction count, expenses and net income (bersih), plus low-stock items and best sellers.                                                               |
| **Kasir**       | Search and category filter, cart, checkout as cash / transfer / bon (credit). Stock is decremented inside a single database transaction.                                         |
| **Stok**        | Add, edit and delete products: name, barcode, category, buy price, sell price, stock, minimum stock, unit. Out-of-stock items sort to the bottom; each group stays alphabetical. |
| **Riwayat**     | Sales list with a category filter and a receipt detail view. Customer and note can be edited on an existing receipt.                                                             |
| **Bon**         | Customer debts, recorded manually or created automatically from an unpaid sale. Payments and progress; status is unpaid or paid.                                                 |
| **Pengeluaran** | Daily operational costs, included in the net-income calculation.                                                                                                                 |
| **Lainnya**     | Entry points to Riwayat and Pengeluaran, plus app info.                                                                                                                          |

Stock is only moved by sales; restocking is done by editing a product.

## Design system

The glass language with Material3 Expressive structure lives in
`shared/src/commonMain/.../ui/components/Common.kt`.

- **One surface recipe.** `Modifier.glassSurface(shape, tint, alpha, elevation,
  specular, veil, onClick)` draws shadow → press-scale → clip → tint → brand veil → specular sheen
  in a single `drawBehind` pass. Every card, row, field, chip, button, panel and dialog goes through
  it.
- **No `BorderStroke` anywhere.** A hairline border is the cheapest tell of a stock Material card;
  glass edges come from the sheen and the tinted shadow.
- **Blur only where nothing behind it moves.** Static overlays (the floating bottom bar, the
  confirm-dialog backdrop) may use `Modifier.blur`. Cards inside a scrolling list may not — they
  re-rasterise every frame and blow the frame budget on mid-range phones, so their glass is
  compositing-only.
- **One dialog recipe.** `GlassConfirmDialog` owns the panel, the entrance animation and the two
  buttons; `ExitConfirmDialog` and the stock-delete confirmation are thin text-only wrappers. Extend
  it instead of writing a second dialog.
- **Motion.** `rememberAppear()` + `Enter(visible, index)` / `Modifier.enter(...)`
  give every screen the same staggered fade-and-rise. Use the `Modifier` form on anything carrying
  `weight(1f)` or `align` — wrapping those in a `Box` moves the parent data and breaks the layout.
- **Type.** Noto Sans, four static weights (Regular / Medium / SemiBold / Bold)
  in `composeResources/font/`. `ITalaseTheme` maps all 30 Material3 text styles *and* provides
  `LocalTextStyle`; without that second step plain `Text(...)`
  calls silently keep the system font.
- **Icons.** Material Symbols downloaded as Android vector drawables in
  `composeResources/drawable/` (~1 KB each) and exposed through the `AppIcon`
  enum. Nothing depends on `material-icons-extended`.

## Tech stack

| Layer      | Library                                                                             | Version                 |
|------------|-------------------------------------------------------------------------------------|-------------------------|
| Language   | Kotlin                                                                              | 2.4.21                  |
| UI         | Compose Multiplatform / Material3                                                   | 1.12.2 / 1.12.0-alpha03 |
| Navigation | Navigation Compose (multiplatform)                                                  | 2.9.2                   |
| DI         | Koin + Koin annotations (KSP)                                                       | 4.2.2                   |
| Database   | Room (KMP) + bundled SQLite driver                                                  | 2.8.5 / 2.7.1           |
| Async      | Kotlin Coroutines, Flow                                                             | 1.11.0                  |
| Build      | AGP 9.4.1, KSP 2.3.12, Gradle version catalog                                       |                         |
| Targets    | Android `minSdk 26` / `targetSdk 37`; iOS arm64 + simulator, deployment target 16.0 |                         |

App version `1.0.0 (100000)` — `app-version` / `app-versioncode` in
`gradle/libs.versions.toml`.

## Project structure

```
iTalase/
├── androidApp/            # Android entry point (MainActivity, manifest, launcher assets)
├── iosApp/                # iOS entry point (SwiftUI wrapper + Xcode project)
└── shared/src/
    ├── commonMain/
    │   ├── composeResources/     # font/, drawable/ (Material Symbols), values/strings.xml
    │   └── kotlin/com/anlian/italase/shared/
    │       ├── data/             # Room entities, DAOs, Tables, ShopRepository, ShopDefaults
    │       ├── ui/components/    # glass primitives, AppIcon, LoadMoreFooter
    │       ├── ui/navigation/    # Routes, AppNavHost, BackHandling, TopBarInfo
    │       ├── ui/screens/       # one file per screen
    │       ├── ui/state/         # ViewModels + UiMessage
    │       ├── ui/theme/         # ITalaseTheme, colour tokens, WarungBackground
    │       └── util/             # ProductFilter, Paging, Format
    ├── androidMain/              # database driver, DI, theme, time, previews
    ├── iosMain/                  # database driver, DI, time, MainViewController
    └── commonTest/               # pure-function tests
```

Exported Room schemas live in `shared/schemas/` and are consumed by Room's migration validation.
Bump the database version and add a `Migration` in
`ITalaseDatabase.kt` whenever an entity changes; keep the schema files in sync.

## Data flow and performance

- **One query, one list.** `ShopRepository` caches `observeProducts()` in a single
  `StateFlow`, so Beranda, Kasir and Stok share one Room query and one copy of the list instead of
  running the same `SELECT` three times per stock change.
- **Derive by what actually changes.** `ProductList` sorts and lowercases the search keys once per
  table change; a keystroke only runs `contains` over an already-sorted list. The search field is
  debounced (120 ms), so typing "susu"
  is one filter, not four.
- **Paged lists.** Riwayat and Pengeluaran request 20 rows and grow the SQL
  `LIMIT` by 20 when the list reaches its end (`util/Paging.kt`). Growing `LIMIT`
  instead of `OFFSET` keeps the query reactive, so a new sale still lands on page one with no extra
  code.
- Rendering is always `LazyColumn` — only visible rows compose.

## Tests

```bash
./gradlew :shared:allTests      # 32 tests
```

Coverage is on pure functions only (`ProductList`, paging arithmetic, route tables, currency/date
formatting). There are no Compose UI or Room instrumentation tests yet. `ProductFilterTest` keeps
the previous filtering pipeline side by side with the new one and asserts both produce identical
ordering — that equivalence is what makes the optimisation safe to trust.

## Run it

Requirements: Android Studio (Koala or newer), JDK 21, Xcode 16+ for iOS.

```bash
./gradlew :androidApp:assembleDebug     # Android debug APK
./gradlew :shared:allTests              # shared tests
```

iOS: open `iosApp/` in Xcode and press Run.

If the build reports a missing launcher drawable or a stale resource while the file clearly exists
on disk, the resource merge or the Gradle build cache is stale:

```bash
./gradlew :androidApp:assembleDebug --no-build-cache --rerun-tasks
```

## Known limitations

Real, currently true, and deliberate for now:

- **A saved sale cannot be voided or cancelled from the UI.** There is no
  `deleteSale` path; correct a receipt by editing its customer and note.
- **No discount field.** `total` is the sum of the cart lines.
- **No camera barcode scanning.** A product has a barcode field and search matches it, but nothing
  reads a barcode from the camera.
- **Reports are per-day only** (`ShopRepository.daySummary`); there is no monthly report.
- **The SQLite file is not encrypted.** SQLCipher is declared as an iOS pod, but the Android builder
  passes an empty passphrase and uses the bundled driver, so the database is plaintext on disk.
  Encrypting it means implementing a custom
  `SQLiteDriver`.
- **No crash reporting.** The Crashlytics plugin was applied with no SDK dependency and no
  `google-services.json`, so it was removed instead of left inert. Re-add it against a real Firebase
  project if crash reports are wanted.
- **`local.properties`** (SDK path, signing keys) is git-ignored and never committed.

## Third-party assets

Noto Sans is bundled under the SIL Open Font License — see
`licenses/NotoSans-OFL.txt`. Icons come from Google Material Symbols (Apache 2.0).
