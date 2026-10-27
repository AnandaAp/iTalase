# iTalase

Offline-first POS (point of sale) and inventory manager for small shops / warung. Built with Kotlin
Multiplatform, so Android and iOS share the same business logic, data layer, encrypted storage, and
Compose UI. No network needed for daily use — everything lives securely on the device.

> For product philosophy, in-depth architectural design, and the upcoming Google Play Store release
> roadmap, see [IDEA.md](IDEA.md).

## Features

| Screen          | What it does                                                                                                                                                                     |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Beranda**     | Today's omzet, profit, transaction count, expenses and net income (bersih), plus low-stock items and best sellers. In landscape orientation, automatically transforms into a dual-pane layout with financial KPIs on the left and inventory alerts on the right. |
| **Kasir**       | Fast search and category filter, cart, checkout as cash / transfer / bon (credit). In landscape orientation, adapts to a professional POS Master-Detail Split Pane: product catalog on the left and dedicated interactive cart pane on the right. |
| **Stok**        | Add, edit and delete products: name, barcode, category, buy price, sell price, stock, minimum stock, unit. Out-of-stock items sort to the bottom; each group stays alphabetical. |
| **Riwayat**     | Sales list with category filter and receipt detail view. Customer name, note, payment method, and tendered cash can be edited on an existing receipt. Export sales reports to PDF with custom date ranges and secure native share sheet. |
| **Bon**         | Customer debts, recorded manually or created automatically from an unpaid sale. Payments and progress; status is unpaid or paid.                                                 |
| **Pengeluaran** | Daily operational costs, included in the net-income calculation.                                                                                                                 |
| **Lainnya**     | Entry points to Riwayat and Pengeluaran, SQLCipher database security & keystore status, offline access indicator, and app release information.                                 |

Stock is only moved by sales; restocking is done by editing a product.

## Design System & Responsive Ergonomics

The glass language with Material3 Expressive structure lives in
`shared/src/commonMain/.../ui/components/Common.kt` and `shared/src/commonMain/.../ui/navigation/`.

- **One surface recipe.** `Modifier.glassSurface(shape, tint, alpha, elevation,
  specular, veil, onClick)` draws shadow → press-scale → clip → tint → brand veil → specular sheen
  in a single `drawBehind` pass. Every card, row, field, chip, button, panel, and dialog goes through it.
- **No `BorderStroke` anywhere.** A hairline border is the cheapest tell of a stock Material card;
  glass edges come from the sheen and the tinted shadow.
- **Device Orientation & Landscape POS Support.** Layouts adapt seamlessly between Portrait and Landscape orientations:
  - *Kasir POS Split Pane*: The catalog occupies 55% of the left screen while the cart panel stays pinned on the right with real-time item steppers, subtotal, and Pay/Cancel buttons.
  - *Beranda Dual Pane*: Financial KPIs (Omzet, Bersih, Pengeluaran, Piutang) sit on the left; Top Selling & Low Stock alerts sit on the right.
  - *Dock Tab Bar & Compact Header*: On landscape, the navigation bar centers as a floating Apple-style dock (`widthIn(max = 560.dp)`) with reduced 48dp height, and the top bar compacts to 52dp to maximize vertical screen real estate.
  - *Scrollable GlassDialog*: All modal dialogs (Payment, Stock form, Expense, Debt) wrap input fields in vertical scroll containers (`Modifier.weight(1f, fill = false).verticalScroll(...)`) with pinned action buttons, completely preventing keyboard clipping on horizontal screens.
- **Dynamic Liquid Glass Top App Bar & Nested Scroll.** Top bars dynamically adapt to scroll
  fractions (`scrollBehavior.state.collapsedFraction`), increasing translucent elevation and specular
  sheen as content scrolls beneath. Outer layout columns bind through `Modifier.nestedScrollConnection(scrollBehavior, listState)`.
- **Isolated Directional Number Ticker.** `TopBarEyebrow` decomposes eyebrow text via regex
  `(\D*)(\d+)(.*)`. When a count changes (e.g. cart item count, product total, debt tally), **only** the
  numeric digits animate through a spring-driven vertical ticker (sliding up when increasing, down when
  decreasing). Prefix and suffix text remain stationary.
- **Liquid Glass Floating Action Buttons.** `PrimaryFab` uses the same unified `glassSurface` recipe
  with a spring press-scale interaction and high-contrast icon tinting aligned with edge-to-edge system bars.
- **Blur only where nothing behind it moves.** Static overlays (the floating bottom bar, the
  confirm-dialog backdrop) may use `Modifier.blur`. Cards inside a scrolling list do not re-rasterise to maintain 60/120 fps.

## iTalase Agentic Dashboard

The project includes an interactive, zero-config real-time monitoring and telemetry dashboard located in:
`iTalase Agentic Dashboard/`

- **Live Telemetry**: Real-time tracking of release APK size (-13.3%), single DEX bytecode reduction (-39%), JaCoCo test coverage (76.18%), and hardware SQLCipher encryption.
- **Autonomous Permission Logs**: Transparent audit trail for all agentic actions executed in Turbo Mode (`run_command`, `write_file`, `edit_file`).
- **Interactive Auto-Moving Kanban**: Cards dynamically animate across columns (*In Progress* → *Done*) using Apple-style spring physics with layout isolation (`content-visibility: auto`).
- **How to Open**: Double-click `iTalase Agentic Dashboard/index.html` to open in any web browser. Works out-of-the-box over `file:///` without requiring node servers or build steps.

## Tech Stack

| Layer        | Library                                                                             | Version                 |
|--------------|-------------------------------------------------------------------------------------|-------------------------|
| Language     | Kotlin                                                                              | 2.4.21                  |
| UI           | Compose Multiplatform / Material3                                                   | 1.12.2 / 1.12.0-alpha03 |
| Navigation   | Navigation Compose (multiplatform)                                                  | 2.9.2                   |
| DI           | Koin + Koin annotations (KSP)                                                       | 4.2.2                   |
| Database     | Room (KMP) + SQLCipher (Android & iOS)                                              | 2.8.5 / 4.19.1 (4.6.0)  |
| Reporting    | Pure Kotlin PDF Engine (zero external dependencies, path traversal secured)         | In-house                |
| Async        | Kotlin Coroutines, Flow                                                             | 1.11.0                  |
| Build & R8   | AGP 9.4.1, KSP 2.3.12, R8 / ProGuard bytecode shrinking & minification              |                         |
| Release      | Signed Android App Bundle (AAB), dynamic Keystore via `local.properties`            |                         |
| Tooling      | Android CLI Jetpack Compose `@Preview` render engine                                |                         |
| Targets      | Android `minSdk 26` / `targetSdk 37`; iOS arm64 + simulator, deployment target 16.0 |                         |

App version `1.0.0 (100000)` — `app-version` / `app-versioncode` in `gradle/libs.versions.toml`.

## Project Structure

```
iTalase/
├── androidApp/                     # Android entry point (MainActivity, manifest, launcher assets, release signing)
├── iosApp/                         # iOS entry point (SwiftUI wrapper + Xcode project)
├── iTalase Agentic Dashboard/      # Interactive real-time AI engineering telemetry dashboard & Kanban
│   ├── index.html                 # Apple Design web dashboard
│   ├── audit_state.js             # Live reactive telemetry state store
│   └── README.md                  # Dashboard documentation
├── IDEA.md                         # Product philosophy, architecture manifesto, and Play Store roadmap
└── shared/src/
    ├── commonMain/
    │   ├── composeResources/      # font/, drawable/ (Material Symbols), values/strings.xml
    │   └── kotlin/com/anlian/italase/shared/
    │       ├── data/              # Room entities, DAOs, Tables, ShopRepository, ShopDefaults, encryption
    │       ├── report/            # Pure Kotlin PDF writer & sales report exporter
    │       ├── ui/components/     # Glass primitives, AppIcon, DateRangePicker, LoadMoreFooter
    │       ├── ui/navigation/     # Routes, AppNavHost, BackHandling, TopBarInfo, NestedScroll
    │       ├── ui/preview/        # Compose @Preview definitions for Portrait & Landscape
    │       ├── ui/screens/        # Dashboard, Cashier, Product, Debt, Expense, More, SaleDetail
    │       ├── ui/state/          # ViewModels + UiMessage
    │       ├── ui/theme/          # ITalaseTheme, colour tokens, WarungBackground
    │       └── util/              # ProductFilter, Paging, Format, Time
    ├── androidMain/               # Database driver (SQLCipher), Keystore key provider, DI, theme, report share
    ├── iosMain/                   # Database driver (SQLCipher), Keychain key provider, DI, UIActivityViewController share
    ├── commonTest/                # Unit tests (77 tests, JaCoCo >= 75% coverage)
    └── androidTest/               # On-device connected instrumentation tests on physical hardware
```

Exported Room schemas live in `shared/schemas/` and are consumed by Room's migration validation.

## Data Flow, Security, and Performance

- **One query, one list.** `ShopRepository` caches `observeProducts()` in a single `StateFlow`, so Beranda, Kasir and Stok share one Room query and one copy of the list.
- **Derive by what actually changes.** `ProductList` sorts and lowercases search keys once per table change; keystrokes run `contains` over an already-sorted list with 120 ms debounce.
- **Paged lists.** Riwayat and Pengeluaran request 20 rows and grow the SQL `LIMIT` by 20 when reaching the end (`util/Paging.kt`).
- **Encrypted at rest.** SQLCipher 256-bit AES-GCM encryption with hardware-backed key storage (`AndroidKeyStore` / iOS Keychain). Plaintext databases migrate seamlessly on first launch.
- **Security-hardened PDF Export.** Laporan penjualan di-render langsung oleh generator PDF murni Kotlin. Nama file disanitasi dari path traversal (`../`), cache sementara lama (>24 jam) dibersihkan otomatis, dan URI dibagikan dengan flag `Intent.FLAG_GRANT_READ_URI_PERMISSION` + `ClipData`.
- **Indexed queries.** Database schema version 4 indexes foreign keys and search filter columns (`dateMillis`, `saleId`, `category`, `debtId`, `status`).

## Building, Running, and Testing

Requirements: Android Studio (Koala or newer), JDK 21, Xcode 16+ for iOS.

### Unit Tests & Code Coverage
```bash
./gradlew :shared:testAndroidHostTest                     # Run all 77 unit tests
./gradlew :shared:jacocoTestCoverageVerification          # Verify >= 75% instruction coverage (current: 76.18%)
```

### Connected On-Device Instrumentation Tests
Run instrumentation tests directly on physical hardware (e.g. Samsung Galaxy A54 5G):
```bash
./gradlew :androidApp:connectedDebugAndroidTest
```
Verifies activity lifecycle, screen orientation rotation cycles (Portrait ↔ Landscape), and SQLCipher binary encryption on device.

### Compose `@Preview` Rendering
Render Jetpack Compose `@Preview` composables without launching an emulator:
```bash
android preview render --all gradle://.?androidApp@debug
```

### Release Build & Google Play Store Packaging
Configure keystore credentials in `local.properties`:
```properties
RELEASE_STORE_FILE=C\:\path\to\your\keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

1. **Build signed release APK**:
   ```bash
   ./gradlew :androidApp:assembleRelease
   ```
   Output: `androidApp/build/outputs/apk/release/androidApp-release.apk` (11.89 MB, single DEX)

2. **Build signed release AAB (for Google Play Console)**:
   ```bash
   ./gradlew :androidApp:bundleRelease
   ```
   Output: `androidApp/build/outputs/bundle/release/androidApp-release.aab`

## iTalase Agentic Dashboard (React.js Edition) & Antigravity Skill

Proyek iTalase dilengkapi dengan **React 18 Agentic Telemetry Dashboard** yang mengawasi progres kerja AI agent, log izin eksekusi tool, metrik JaCoCo, dan status perangkat keras secara real-time.

### 1. Antigravity Skill: `italase-agentic-dashboard`
Skill ini terpasang secara global (`~/.gemini/config/skills/italase-agentic-dashboard/`) dan pada workspace (`.agents/skills/italase-agentic-dashboard/`), sehingga **selalu muncul pada menu Customizations Antigravity**.
- **Protokol Otomatis**: Setiap AI agent di sesi baru otomatis membaca aturan di [`AGENTS.md`](file:///c:/Users/anand/StudioProjects/iTalase/AGENTS.md) dan [`GEMINI.md`](file:///c:/Users/anand/StudioProjects/iTalase/GEMINI.md) untuk menyinkronkan status Kanban saat memulai, mengeksekusi, atau memverifikasi tugas.

### 2. Menjalankan React Dashboard
- **Opsi Cepat (Zero-Build Browser)**:
  Cukup klik ganda [`iTalase Agentic Dashboard/index.html`](file:///c:/Users/anand/StudioProjects/iTalase/iTalase%20Agentic%20Dashboard/index.html) atau jalankan:
  ```powershell
  start "iTalase Agentic Dashboard/index.html"
  ```
  Dashboard React 18 langsung berjalan secara offline/lokal dengan dukungan *Drag & Drop*, filter kategori, modal detail tugas, dan *polling* otomatis setiap 2.5 detik.
- **Opsi Vite Dev Server**:
  ```cmd
  cd "iTalase Agentic Dashboard"
  npm install
  npm run dev
  ```

### 3. CLI State Controller untuk AI Agent
AI agent dan script otomatis dapat mengecek atau memperbarui papan Kanban dengan perintah:
```powershell
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --status
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --move CARD-01 --to inProgress
node "iTalase Agentic Dashboard/scripts/update_dashboard.js" --log-perm run_command "Run Tests" "./gradlew test"
```

## Known Limitations

- **A saved sale cannot be voided or cancelled from the UI.** Correct a receipt by editing its customer, note, payment method, or received cash.
- **No discount field.** `total` is the sum of the cart lines.
- **No camera barcode scanning.** Product has a barcode field and search matches it; camera scanner planned for future releases.
- **Dashboard summary is per-day only.** Riwayat PDF export handles arbitrary multi-day/monthly period reporting.
- **`local.properties`** is git-ignored and never committed.

## Third-Party Assets

Noto Sans and Outfit are bundled under the SIL Open Font License. Icons come from Google Material Symbols (Apache 2.0).
