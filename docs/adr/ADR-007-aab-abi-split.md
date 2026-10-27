# ADR-007: Google Play App Bundle (AAB) ABI Split & Multi-Architecture Optimization

## Status
Accepted (2026-10-27)

## Context
iTalase integrates native C/C++ binaries (`libsqlcipher.so`) for database encryption across multiple CPU architectures (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`). In universal APK builds, bundling all architectures bloats the download size to ~14.4 MB (7.6 MB native libraries alone). For rural Indonesian merchants on limited 4G bandwidth and budget Android phones, minimizing app download size and device storage footprint is crucial.

## Decision
1. **Google Play App Bundle (AAB) Architecture:** Target Google Play App Bundle (`:androidApp:bundleRelease`) as the primary distribution artifact.
2. **Explicit ABI, Density, and Language Splits:** Configure Gradle `android.bundle` splits in `androidApp/build.gradle.kts`:
   ```kotlin
   bundle {
       abi {
           enableSplit = true
       }
       density {
           enableSplit = true
       }
       language {
           enableSplit = false // Retain Indonesian localization
       }
   }
   ```
3. **Architecture-Specific Stripping:** Google Play delivers single-architecture slices tailored to the user's specific handset (e.g. `arm64-v8a` for modern Samsung Galaxy, Xiaomi, and Vivo phones).

## Consequences
- **Positive:** Over-the-air download size drops from 14.37 MB to ~4.8 MB (~66% reduction for end users).
- **Positive:** Single Dex bytecode footprint preserved at 7.46 MB without triggering MultiDex runtime loading overhead.
- **Trade-off:** Sideloading via raw `adb install` requires either universal development APKs or `bundletool` for split APK extraction.
