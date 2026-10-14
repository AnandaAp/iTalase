# iTalase

Simple offline POS (point of sale) for small shops / warung. Built with Kotlin Multiplatform so Android and iOS share the same code and UI.

## What can it do?

- **Dashboard (Home)** — today's omzet, profit, transaction count, expense, and net income (bersih). Plus low-stock alerts and recent sales.
- **Cashier (Kasir)** — search products, build a cart, checkout with cash / other payment methods. Stock is reduced automatically in one database transaction.
- **Stock (Stok)** — add, edit, delete products. Track buy price, sell price, stock, and minimum stock.
- **History (Riwayat)** — list of past sales with detail view. Void cancelled transactions.
- **Debt / Bon** — record customer debts from unpaid sales, track payments and status (unpaid / partial / paid).
- **Expense (Biaya)** — record daily operational costs, included in net income calculation.
- **More** — extra settings and app info.

All data is stored locally on the device. No internet required for daily use.

## Tech stack

| Layer | Library |
|---|---|
| UI | Compose Multiplatform, Material3, Navigation Compose |
| DI | Koin (+ Koin annotations / KSP) |
| Database | Room (KMP) + SQLite bundled, offline-first |
| Async | Kotlin Coroutines, Flow |
| Targets | Android (minSdk 26), iOS (SwiftUI entry + shared UI) |

## Project structure

```
iTalase/
├── androidApp/        # Android entry point (MainActivity, manifest, resources)
├── iosApp/            # iOS entry point (SwiftUI + Xcode project)
└── shared/            # Shared Kotlin code
    └── src/
        ├── commonMain/    # App UI, navigation, repository, Room entities/DAOs
        ├── androidMain/   # Android database driver, DI, theme, time
        └── iosMain/       # iOS database driver, DI, theme, time
```

Key shared files:

- `shared/.../App.kt` — root composable
- `shared/.../ui/navigation/` — `Routes` (`home`, `kasir`, `stok`, `riwayat`, `bon`, `biaya`, `more`) + `AppNavHost`
- `shared/.../ui/screens/` — one file per screen
- `shared/.../data/repository/ShopRepository.kt` — cart, checkout, products, sales, debts, expenses
- `shared/.../data/local/` — Room database, tables, DAOs, schemas in `shared/schemas/`

## Run it

Requirements: Android Studio (Koala or newer), Xcode 15+ for iOS, JDK 17+.

```bash
# Android (debug APK)
./gradlew :androidApp:assembleDebug

# Shared tests
./gradlew :shared:allTests
```

iOS: open `iosApp/` in Xcode and press Run.

## App version

Current version: `1.1.2 (10012)` — see `gradle/libs.versions.toml` (`app-version`, `app-versioncode`).

## Notes

- `local.properties` (SDK path, signing keys) is git-ignored and never committed.
- Room schemas are versioned under `shared/schemas/` — keep them when changing entities.
- `androidApp/google-services.json` is a public client config and is committed intentionally.
