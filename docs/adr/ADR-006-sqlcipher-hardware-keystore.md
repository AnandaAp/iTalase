# ADR-006: SQLCipher 256-bit AES-GCM Key Wrapping & Zero Plaintext Guarantees

## Status
Accepted (2026-10-27)

## Context
iTalase POS is an offline-first financial bookkeeping system for warung merchants. The local database stores proprietary pricing, sales receipts, customer debts, and ledger data. Hardcoded cryptographic keys, passphrase derivation without hardware binding, or plaintext database headers expose warung financial data to extraction on rooted or stolen devices.

## Decision
1. **Hardware-Backed Master Key:** Generate a 256-bit AES cryptographic key protected inside the Android KeyStore (`AndroidKeyStore` provider) using `KeyGenParameterSpec` with AES/GCM/NoPadding.
2. **Encrypted Passphrase Generation:** Derive the SQLCipher passphrase dynamically via hardware KeyStore encryption with Associated Authenticated Data (AAD) bound to the app's package identity (`com.anlian.italase`).
3. **Pure Common SQLite Pragma Isolation:** Isolate SQLCipher execution via `PRAGMA key = '<64-hex-char>'` inside platform-specific drivers without leaking credentials across JVM/Native boundaries.
4. **Automated Plaintext Migration & Verification:** On first initialization, inspect the database header using `isPlainSqliteHeader(header: ByteArray)`. If standard SQLite header `"SQLite format 3\0"` is detected, execute an automated migration pipeline using `sqlcipher_export` and verify `PRAGMA cipher_version` before replacing the active database file.

## Consequences
- **Positive:** Financial records remain 100% encrypted at rest with zero plaintext keys stored in SharedPreferences or source code.
- **Positive:** Complies with enterprise financial security and Play Store data protection standards.
- **Trade-off:** Hardware KeyStore operations introduce a negligible (~15ms) one-time cost during application cold boot.
