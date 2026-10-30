# ADR-010: Static Merchant QRIS Architecture, Room Schema v7 & Multiplatform Image Optimization Pipeline

## Status
Accepted (2026-10-30)

## Context
In Indonesia's retail and MSME (*Warung*) ecosystem, digital transactions via **QRIS (Quick Response Code Indonesian Standard)** account for an increasingly dominant share of daily sales.
Prior to this architectural enhancement (AUD-107):
1. **Cashier Payment Limitations:** iTalase only supported Cash (`TUNAI`) and Store Debt (`BON`), lacking any native digital QR payment mechanism.
2. **Memory & Image Handling Constraints:** Storing raw, multi-megapixel merchant camera photos or gallery screenshots of QR codes directly in SQLite can cause massive database bloat and trigger Android `OutOfMemoryError` (OOM) during bitmap decoding on lower-end devices.
3. **Multiplatform & Offline-First Guarantees:** The QRIS solution must operate completely offline without external cloud dependencies, maintain backward-compatible Room migrations, and provide responsive, ergonomic display in both Portrait and Landscape POS cashier modes.

## Decision
1. **Room Schema v7 Bump & `qris_config` Table (`MIGRATION_6_7`):**
   - Bump Room database version from `6` to `7` in `ITalaseDatabase.kt`.
   - Create single-row configuration table `qris_config` (`Tables.QRIS_CONFIG`) with `id = 1`:
     ```sql
     CREATE TABLE IF NOT EXISTS qris_config (
         id INTEGER NOT NULL,
         isEnabled INTEGER NOT NULL,
         imageBytes BLOB,
         mimeType TEXT NOT NULL,
         promptDismissCount INTEGER NOT NULL,
         updatedAt INTEGER NOT NULL,
         PRIMARY KEY(id)
     );
     INSERT OR IGNORE INTO qris_config (id, isEnabled, imageBytes, mimeType, promptDismissCount, updatedAt)
     VALUES (1, 0, NULL, 'image/jpeg', 0, 0);
     ```
   - Provide `QrisConfigDao` with reactive `observeConfig(): Flow<QrisConfigEntity?>` and update methods.

2. **Zero-OOM Multiplatform Image Optimization Pipeline (`ImageOptimizer`):**
   - Provide platform-agnostic `ImageOptimizer` interface in `commonMain` with high-performance `ImageOptimizer.android.kt` implementation.
   - Utilize Android `BitmapFactory.Options` with power-of-2 `inSampleSize` calculations to decode raw streams without loading full-resolution bitmaps into memory.
   - Constrain bounding box to $\le 1024 \times 1024$ pixels and compress to JPEG format at 82% quality, ensuring memory consumption $< 150 \text{ KB}$ per stored QRIS code.

3. **Multiplatform Image Picker Contract (`rememberImagePickerLauncher`):**
   - Encapsulate native Jetpack Activity Result contracts (`PickVisualMedia` for Android Photo Picker and `TakePicture` via `FileProvider`).
   - Stub iOS counterpart cleanly with hardware quarantine markers in accordance with Windows host development constraints.

4. **Adaptive Cashier Ergonomics & Warung Liquid Glass UI:**
   - Implement `QrisPromptBanner` on `CashierScreen` with persistent dismissal tracking ($\le 3$ dismissals) to guide onboarding.
   - In `PayDialog`, introduce tabbed/toggle selection for Cash, Debt, and QRIS.
   - Adapt QRIS preview layout with orientation-aware constraints: max height 140dp in Landscape and 220dp in Portrait, wrapped in scrollable containers to guarantee zero layout overflow across phone, foldable, and tablet form factors.

5. **Receipt Formatting & Financial Accounting Consistency:**
   - Enhance `ReceiptFormatter.kt` to format payment method headers (`METODE PEMBAYARAN: QRIS`) for both 58mm/80mm thermal ESC/POS text and WhatsApp sharing strings.

## Consequences
- **Positive:** Enables Indonesian warung merchants to accept instant digital payments via their own static QRIS code without merchant aggregator fees.
- **Positive:** Bounded memory footprint ($<150\text{ KB}$) eliminates Android OOM risks during checkout.
- **Positive:** Guaranteed zero data loss across existing merchants through verified sequential `MIGRATION_6_7`.
- **Positive:** Full responsive layout support for phone and landscape cashier setups.
- **Trade-off:** Static QRIS relies on cashier visual verification of customer payment on customer mobile screen rather than dynamic webhook reconciliation (optimal for offline warung operation).
