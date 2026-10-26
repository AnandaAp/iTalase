# iTalase

Offline-first POS (point of sale) and inventory manager for small shops / warung. Built with Kotlin
Multiplatform, so Android and iOS share the same business logic, data layer, encrypted storage, and
Compose UI. No network needed for daily use — everything lives securely on the device.

> For product philosophy, in-depth architectural design, and the upcoming Google Play Store release
> roadmap, see [IDEA.md](IDEA.md).

## Features

| Screen          | What it does                                                                                                                                                                     |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Beranda**     | Today's omzet, profit, transaction count, expenses and net income (bersih), plus low-stock items and best sellers. Reaktif top bar eyebrow shows daily transaction metrics.    |
| **Kasir**       | Search and category filter, cart, checkout as cash / transfer / bon (credit). Dynamic top bar counter ticker tracks active cart items or total catalog. Single-transaction stock decrement. |
| **Stok**        | Add, edit and delete products: name, barcode, category, buy price, sell price, stock, minimum stock, unit. Out-of-stock items sort to the bottom; each group stays alphabetical. |
| **Riwayat**     | Sales list with category filter and receipt detail view. Customer and note can be edited on an existing receipt. Export sales reports to PDF with custom date ranges and native share sheet. |
| **Bon**         | Customer debts, recorded manually or created automatically from an unpaid sale. Payments and progress; status is unpaid or paid.                                                 |
| **Pengeluaran** | Daily operational costs, included in the net-income calculation.                                                                                                                 |
| **Lainnya**     | Entry points to Riwayat and Pengeluaran, SQLCipher database security & keystore status, offline access indicator, and app release information.                                 |

Stock is only moved by sales; restocking is done by editing a product.

## Design system

The glass language with Material3 Expressive structure lives in
`shared/src/commonMain/.../ui/components/Common.kt` and `shared/src/commonMain/.../ui/navigation/`.

- **One surface recipe.** `Modifier.glassSurface(shape, tint, alpha, elevation,
  specular, veil, onClick)` draws shadow → press-scale → clip → tint → brand veil → specular sheen
  in a single `drawBehind` pass. Every card, row, field, chip, button, panel, and dialog goes through
  it.
- **No `BorderStroke` anywhere.** A hairline border is the cheapest tell of a stock Material card;
  glass edges come from the sheen and the tinted shadow.
- **Dynamic Liquid Glass Top App Bar & Nested Scroll.** Top bars dynamically adapt to scroll
  fractions (`scrollBehavior.state.collapsedFraction`), increasing translucent elevation and specular
  sheen as content scrolls beneath. Outer layout columns bind through `Modifier.nestedScrollConnection(scrollBehavior, listState)`,
  guaranteeing that short screens fitting comfortably on display never collapse the header unnecessarily.
- **Isolated Directional Number Ticker.** `TopBarEyebrow` decomposes eyebrow text via regex
  `(\D*)(\d+)(.*)`. When a count changes (e.g. cart item count, product total, debt tally), **only** the
  numeric digits animate through a spring-driven vertical ticker (sliding up when increasing, down when
  decreasing). Prefix and suffix text remain stationary, preventing font jitter, text re-measurement, or
  layout jumps.
- **Liquid Glass Floating Action Buttons.** `PrimaryFab` uses the same unified `glassSurface` recipe
  with a spring press-scale interaction and high-contrast icon tinting aligned with edge-to-edge system bars.
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

| Layer        | Library                                                                             | Version                 |
|--------------|-------------------------------------------------------------------------------------|-------------------------|
| Language     | Kotlin                                                                              | 2.4.21                  |
| UI           | Compose Multiplatform / Material3                                                   | 1.12.2 / 1.12.0-alpha03 |
| Navigation   | Navigation Compose (multiplatform)                                                  | 2.9.2                   |
| DI           | Koin + Koin annotations (KSP)                                                       | 4.2.2                   |
| Database     | Room (KMP) + SQLCipher (Android & iOS)                                              | 2.8.5 / 4.19.1 (4.6.0)  |
| Reporting    | Pure Kotlin PDF Engine (zero external dependencies)                                 | In-house                |
| Async        | Kotlin Coroutines, Flow                                                             | 1.11.0                  |
| Build & R8   | AGP 9.4.1, KSP 2.3.12, R8 / ProGuard bytecode shrinking & minification              |                         |
| Release      | Signed Android App Bundle (AAB), dynamic Keystore via `local.properties`            |                         |
| Tooling      | Android CLI Jetpack Compose `@Preview` render engine                                |                         |
| Targets      | Android `minSdk 26` / `targetSdk 37`; iOS arm64 + simulator, deployment target 16.0 |                         |

App version `1.0.0 (100000)` — `app-version` / `app-versioncode` in
`gradle/libs.versions.toml`.

## Project structure

```
iTalase/
├── androidApp/            # Android entry point (MainActivity, manifest, launcher assets, release signing)
├── iosApp/                # iOS entry point (SwiftUI wrapper + Xcode project)
├── IDEA.md                # Product philosophy, architecture manifesto, and Play Store roadmap
└── shared/src/
    ├── commonMain/
    │   ├── composeResources/     # font/, drawable/ (Material Symbols), values/strings.xml
    │   └── kotlin/com/anlian/italase/shared/
    │       ├── data/             # Room entities, DAOs, Tables, ShopRepository, ShopDefaults, encryption
    │       ├── report/           # pure Kotlin PDF writer & sales report exporter
    │       ├── ui/components/    # glass primitives, AppIcon, DateRangePicker, LoadMoreFooter
    │       ├── ui/navigation/    # Routes, AppNavHost, BackHandling, TopBarInfo, NestedScroll
    │       ├── ui/preview/       # Compose @Preview definitions for Beranda & Kasir
    │       ├── ui/screens/       # one file per screen (Dashboard, Cashier, Product, Debt, Expense, More)
    │       ├── ui/state/         # ViewModels + UiMessage
    │       ├── ui/theme/         # ITalaseTheme, colour tokens, WarungBackground
    │       └── util/             # ProductFilter, Paging, Format, Time
    ├── androidMain/              # database driver (SQLCipher), Keystore key provider, DI, theme, report share
    ├── iosMain/                  # database driver (SQLCipher), Keychain key provider, DI, UIActivityViewController share
    └── commonTest/               # pure-function, report PDF, and DB encryption tests
```

Exported Room schemas live in `shared/schemas/` and are consumed by Room's migration validation.
Bump the database version and add a `Migration` in
`ITalaseDatabase.kt` whenever an entity changes; keep the schema files in sync.

## Data flow, security, and performance

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
- **Encrypted at rest.** The database is encrypted via SQLCipher (`sqlcipher-android` on Android,
  CocoaPods `SQLCipher` on iOS). Master keys are securely generated and kept in hardware-backed
  storage (`AndroidKeyStore` / iOS Keychain). Existing plain-text databases are seamlessly migrated
  to encrypted format on first launch without data loss.
- **Indexed queries.** Database schema version 4 adds indices on foreign keys and filter columns
  (`dateMillis`, `saleId`, `category`, `debtId`, `status`) so paging and filtering remain instantaneous
  even with thousands of records.
- **Zero-dependency PDF export.** Sales history reports are formatted and rendered directly via a
  pure Kotlin PDF generation pipeline (`com.anlian.italase.shared.report`), generating compliant PDF/1.4
  documents with JPEG store logo embedding and platform-native share integration.
- Rendering is always `LazyColumn` — only visible rows compose.

## Building and running

Requirements: Android Studio (Koala or newer), JDK 21, Xcode 16+ for iOS.

### Debug build
```bash
./gradlew :androidApp:assembleDebug     # Android debug APK
./gradlew :shared:allTests              # shared tests (50 tests)
```

iOS: open `iosApp/` in Xcode and press Run.

### Compose `@Preview` rendering
To render Jetpack Compose `@Preview` composables defined in `:shared` without launching an emulator:
```bash
android preview render --all gradle://.?androidApp@debug
```

### Release build & Google Play Store packaging
To generate release APKs and Google Play Store App Bundles (AAB), configure your keystore credentials in `local.properties`:

```properties
RELEASE_STORE_FILE=C\:\\path\\to\\your\\keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

1. **Build signed release APK**:
   ```bash
   ./gradlew :androidApp:assembleRelease
   ```
2. **Build signed release AAB (for Google Play Console)**:
   ```bash
   ./gradlew :androidApp:bundleRelease
   ```
   - Signed AAB location: `androidApp/build/outputs/bundle/release/androidApp-release.aab`
   - ProGuard/R8 deobfuscation map: `androidApp/build/outputs/mapping/release/mapping.txt`

If the build reports a missing launcher drawable or a stale resource while the file clearly exists
on disk, the resource merge or the Gradle build cache is stale:

```bash
./gradlew :androidApp:assembleDebug --no-build-cache --rerun-tasks
```

## Tests

```bash
./gradlew :shared:allTests      # 50 tests
```

Coverage is on pure functions and platform-agnostic business logic (`ProductList`, paging arithmetic,
route tables, currency/date formatting, PDF document generation, and SQLite header/key derivation
in `SalesReportPdfTest` and `DbEncryptionTest`).
`ProductFilterTest` keeps the previous filtering pipeline side by side with the new one and asserts both produce identical
ordering — that equivalence is what makes the optimisation safe to trust.

## Known limitations

Real, currently true, and deliberate for now:

- **A saved sale cannot be voided or cancelled from the UI.** There is no
  `deleteSale` path; correct a receipt by editing its customer and note.
- **No discount field.** `total` is the sum of the cart lines.
- **No camera barcode scanning.** A product has a barcode field and search matches it, but nothing
  reads a barcode from the camera yet.
- **Dashboard summary is per-day only.** Beranda dashboard focuses on today's snapshot (`ShopRepository.daySummary`);
  arbitrary multi-day/monthly period reporting is handled via Riwayat's PDF export.
- **No crash reporting.** The Crashlytics plugin was applied with no SDK dependency and no
  `google-services.json`, so it was removed instead of left inert. Re-add it against a real Firebase
  project if crash reports are wanted.
- **`local.properties`** (SDK path, signing keys) is git-ignored and never committed.

## Third-party assets

Noto Sans is bundled under the SIL Open Font License — see
`licenses/NotoSans-OFL.txt`. Icons come from Google Material Symbols (Apache 2.0).
