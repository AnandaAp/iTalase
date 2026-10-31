---
name: warung-liquid-glass
description: Architect and implement premium, human-crafted, anti-AI-slop mobile interfaces using the Warung Liquid Glass Design System in Jetpack Compose and Compose Multiplatform (CMP: Android, iOS, Desktop, Web). Covers authentic cultural warmth color tokens (Emerald, Marigold, Paper), glassSurface recipes (specular rim, veil gradients, diffuse shadows), glass alpha tuning matrix, zero-overhead backdrop blur isolation, Apple spring physics, and full adaptive multi-orientation layouts (Portrait vs Landscape split-pane, Navigation Rail vs Floating Tab Bar, and dynamic pinned top app bar docking).
---

# Warung Liquid Glass Design System (Anti AI-Slop)

Architect and construct authentic, human-crafted, GPU-efficient mobile interfaces in Jetpack Compose and Compose Multiplatform (CMP). This skill codifies the complete design system born out of **iTalase POS & Accounting** (`com.anlian.italase`), merging authentic Indonesian cultural warmth with Apple-grade fluid interface physics.

---

## 🎯 When to Use

- The user wants to design, build, polish, or review mobile UI in Jetpack Compose or Compose Multiplatform.
- The user requests an **"anti AI-slop"**, distinctive, or human-crafted aesthetic that avoids generic purple/blue neon gradients and cards-inside-cards.
- Building adaptive mobile layouts that must support both **Portrait** (phones) and **Landscape / Tablet** (POS split-pane, Navigation Rail).
- Tuning glass transparency (`alpha`), specular rim sheens, or elevation across floating bottom tab bars, modal dialogs, pinned top bars, and scrollable cards.
- Implementing glassmorphism without incurring GPU thermal/framerate penalties on mid-range Android devices.

---

## 🏛️ The 5 Golden Rules of Warung Liquid Glass

1. **Zero Live Blur in Scrolling Layers**: Never place `Modifier.blur()` or live refraction inside `LazyColumn`, `LazyRow`, or scrolling containers. Moving blur crushes frame budgets. Form glass in scrollable content from **Translucency ($\alpha \approx 0.88$) + Specular Sheen (1px vertical top gradient) + Dual Brand Veils + Diffuse Tinted Shadow**.
2. **Backdrop Blur Belongs to Static Scaffolds**: Live backdrop blur (20dp) is reserved strictly for the background `Scaffold` container when a modal dialog is open, via the centralized `GlassDialogBackdrop` coordinator.
3. **Paper Over Flat White**: App background uses warm cream `Paper` (`#FAF6EE`), while surfaces use warm `CardWhite` (`#FFFDF7`). This measured $1.051:1$ luminance difference allows glass surfaces to elevate naturally without harsh, amateur 1px border strokes.
4. **Apple Fluid Spring Physics Over Linear Tweens**: Interactive feedback uses `graphicsLayer` press-scale (`0.975f` for cards, `0.94f` for tab items, `0.88f` for action icons) driven by `spring()`. Never use rectangular Material ripple boxes over rounded glass.
5. **Adaptive Multi-Orientation is First-Class**: Always support Landscape and Tablet layouts. Replace the bottom tab bar with a Start-pinned `LiquidGlassNavRail` (72dp) and split screens into 2-column POS split-panes.

---

## 🎨 Design Tokens & Contrast (Single Source of Truth)

All tokens must reference the centralized palette in `Theme.kt`:

```kotlin
// Warung Emerald Palette
val WarungGreen     = Color(0xFF1D6B3C) // primary (WCAG AA compliant with white text >= 5.4:1)
val WarungGreenDark = Color(0xFF0E3D20) // onPrimaryContainer / deep contrast text
val WarungGreenSoft = Color(0xFFDCEFE1) // primaryContainer / active pill surface

// Warm Cultural Accents
val Marigold        = Color(0xFFE8930C) // secondary / warm highlights & actions
val MarigoldSoft    = Color(0xFFFDEBC8) // secondaryContainer / neutral badge surface
val MarigoldInk     = Color(0xFF5C3800) // onSecondaryContainer (text contrast >= 7:1)

// Surfaces & Backgrounds
val Paper           = Color(0xFFFAF6EE) // background / cream warung wrapper
val CardWhite       = Color(0xFFFFFDF7) // surface / card body tint
val SandLow         = Color(0xFFF3EDDE) // surfaceVariant / soft divider
val InkSoft         = Color(0xFF6B6250) // onSurfaceVariant / secondary labels
```

---

## 🎛️ Glass Alpha Tuning Guide & Sweet Spot Matrix

Glass transparency (`alpha`) is mathematically calibrated according to **backdrop condition** (blurred static vs moving unblurred) and **information density**:

### Sweet Spot Matrix

| Component | Light Alpha | Dark Alpha | Backdrop Condition | Design Rationale |
| :--- | :---: | :---: | :--- | :--- |
| **Pop-up Confirm Dialog** (`GlassConfirmDialog`) | **`0.54f`** | **`0.58f`** | Scaffold blurred 20dp + dark scrim | **Crystal Pure Glass**. Uniformly soft backdrop eliminates bleeding. Text must use `onSurface.copy(alpha = 0.86f)` for $\ge 10:1$ WCAG AA contrast. |
| **Form & Input Dialog** (`GlassDialog`) | **`0.88f`** | **`0.86f`** | Scaffold blurred 20dp + form inputs | **Dense Translucency**. Text fields need a stable, opaque base to prevent cursor/text glare. |
| **Scrollable Content Cards** (`GlassCard` / `glassSurface`) | **`0.88f`** | **`0.80f` - `0.82f`** | Moving on `Paper` without live blur | **Crisp Translucency**. $\alpha < 0.80$ without blur causes "dirty glass" bleed. Glass feel comes from top specular rim + diffuse shadow. |
| **Floating Bottom Tab Bar** (`LiquidGlassTabBar`) | **`0.85f`** | **`0.80f`** | Floating over scrolling content | **Smooth Island**. Backed by a 44dp scrim wash ($\alpha = 0.90$) that fades scrolling text before it touches the bar. |
| **Navigation Rail Landscape** (`LiquidGlassNavRail`) | **`0.88f`** | **`0.82f`** | Pinned to start edge (elevation 20dp) | **Solid Anchor**. Shields horizontal transitions and heavy side content. |
| **Pinned Dynamic Top Bar** (`PremiumTopBar`) | **`0.00f`** (Rest)<br>$\to$ **`0.97f`** (Scrolled) | **`0.00f`** (Rest)<br>$\to$ **`0.94f`** (Scrolled) | Rest: zero alpha.<br>Scrolled: heavy headers slide underneath. | **Shielded Dock**. Full docked alpha of `0.97f` guarantees **zero text collision** with revenue numbers sliding underneath, accented by a 1px specular bottom lip. |

### Code Recipes for Tuning Transparency

```kotlin
// 1. Tuning Cards
Modifier.glassSurface(
    shape = RoundedCornerShape(20.dp),
    alpha = 0.82f,      // Custom card alpha (default 0.88f)
    specular = 0.45f,   // Compensate lowered alpha with stronger specular rim
    elevation = 16.dp,
)

// 2. Tuning Dialogs (Common.kt)
Column(
    modifier = Modifier
        .clip(RoundedCornerShape(24.dp))
        .background(scheme.surface.copy(alpha = if (isDark) 0.58f else 0.54f))
)

// 3. Tuning Bottom Tab Bar (AppNavHost.kt)
Box(
    Modifier.glassSurface(
        shape = RoundedCornerShape(26.dp),
        alpha = if (isDark) 0.80f else 0.85f,
        elevation = 26.dp,
        specular = 0.55f, // Enhanced rim for floating elevation
    )
)

// 4. Tuning Dynamic Top Bar Docking (AppNavHost.kt)
val glassBgAlpha = if (isDark) 0.94f * glass else 0.97f * glass
```

### The 3 Glass Compensation Rules
1. **Inverse Alpha-Specular Law**: $\text{Lower Alpha } \implies \text{ Raise Specular Sheen}$. As the panel becomes more transparent, edge definition weakens; boost `specular` (e.g. from `0.34f` to `0.55f`) to keep the glass rim crisp.
2. **Scrim-Translucency Law for Floating Elements**: Never lower floating bar alpha below `0.80f` without an underlying gradient scrim wash to soften scrolling content before it passes beneath.
3. **Solid Input Contrast Law**: Any component hosting active text inputs or QR code scanners must maintain $\alpha \ge 0.85f$ to avoid optical fatigue and typing ambiguity.

---

## 🧩 Core Jetpack Compose Blueprints

### 1. `Modifier.glassSurface` (Zero-Blur GPU Glass)
```kotlin
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.88f,
    elevation: Dp = 22.dp,
    specular: Float = 0.34f,
    veil: Boolean = true,
    onClick: (() -> Unit)? = null,
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.surface.luminance() < 0.5f

    val primaryVeilBrush = if (veil) {
        remember(scheme.primary) {
            Brush.radialGradient(
                colors = listOf(scheme.primary.copy(alpha = 0.06f), Color.Transparent),
                center = Offset.Zero,
                radius = 900f,
            )
        }
    } else null

    val secondaryVeilColors = if (veil) {
        remember(scheme.secondary) {
            listOf(scheme.secondary.copy(alpha = 0.04f), Color.Transparent)
        }
    } else null

    val specularAlpha = if (isDark) specular * 0.4f else specular
    val specularBrush = remember(specularAlpha) {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = specularAlpha),
            0.28f to Color.Transparent,
        )
    }

    val clickModifier = if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        Modifier
            .graphicsLayer {
                val s = if (pressed) 0.975f else 1f
                scaleX = s
                scaleY = s
            }
            .clickable(interaction, indication = null) { onClick() }
    } else {
        Modifier
    }

    return this
        .then(clickModifier)
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = scheme.onSurface.copy(alpha = 0.12f),
            spotColor = scheme.onSurface.copy(alpha = 0.20f),
        )
        .clip(shape)
        .drawBehind {
            drawRect(tint, alpha = alpha)
            if (primaryVeilBrush != null && secondaryVeilColors != null) {
                drawRect(primaryVeilBrush)
                drawRect(
                    Brush.radialGradient(
                        colors = secondaryVeilColors,
                        center = Offset(size.width, size.height),
                        radius = 900f,
                    ),
                )
            }
            drawRect(specularBrush)
        }
}
```

### 2. `GlassCard`
```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.88f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.glassSurface(shape = shape, tint = tint, alpha = alpha, onClick = onClick),
        content = content,
    )
}
```

### 3. Centralized Modal Dialog Backdrop Blur Pattern
```kotlin
// 1. Singleton Coordinator
internal object GlassDialogBackdrop {
    var open: Boolean by mutableStateOf(false)
}

// 2. Announce inside any Dialog
@Composable
internal fun AnnounceGlassDialogOpen() {
    DisposableEffect(Unit) {
        GlassDialogBackdrop.open = true
        onDispose { GlassDialogBackdrop.open = false }
    }
}

// 3. Connect to root Scaffold in AppNavHost
Scaffold(
    modifier = Modifier.blur(if (GlassDialogBackdrop.open) 20.dp else 0.dp),
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0.dp),
)
```

---

## 📱 Adaptive Multi-Orientation Architecture

Always use `BoxWithConstraints` to detect layout mode:
```kotlin
val isLandscape = (maxWidth > maxHeight && maxWidth >= 540.dp) || maxWidth >= 600.dp
```

### 1. Navigation Topology
- **Portrait**: Floating island bottom tab bar (`LiquidGlassTabBar`, 64dp height, 26dp rounded shape) over a 44dp background scrim wash (`Brush.verticalGradient`).
- **Landscape / Tablet**: Vertical navigation rail (`LiquidGlassNavRail`, 72dp width, 24dp rounded shape) pinned to start window insets, preventing vertical viewport truncation.

### 2. Dynamic Pinned Top App Bar (`PremiumTopBar`)
- **Portrait**: Edge-to-edge transparent top bar. When scrolled, it frosts with 1px specular rim, docked title scale (`titleScale` 1.0 $\to$ 0.96), and docked background alpha (`0.97f`).
- **Landscape**: Automatically adapts into a docked compact island with rounded bottom corners (`bottomStart = 24.dp, bottomEnd = 24.dp`), dynamic shadow elevation, collapsed & faded-out eyebrow, and title transitioning to `titleMedium`.

### 3. POS Split-Pane Paradigm
In landscape, divide screens into 2 parallel columns:
- **Cashier**: Product catalog grid on the left (weight 1.2f), sticky cart summary & payment actions on the right (weight 0.8f).
- **Dashboard**: Revenue hero & KPIs on the left (weight 1.05f), top items & low stock alerts on the right (weight 0.95f).
- **Settings & QRIS**: Controls & previews on the left (weight 1.15f), guidelines & educational notes on the right (weight 0.85f).

---

## 🍎 Apple Fluid Physics & Micro-Interactions

Interactive feedback uses `graphicsLayer` press-scale driven by `spring()`:
- **Glass Cards**: `scale = 0.975f` when pressed.
- **Tab / Nav Rail Items**: `scale = 0.94f` when pressed.
- **Round Action Buttons / Back Arrow**: `scale = 0.88f` when pressed with `Spring.DampingRatioMediumBouncy`.

---

## 📋 Pre-Flight Audit Checklist

Before releasing any new Compose screen:
- [ ] No hardcoded border strokes (`BorderStroke(1.dp, ...)`) on cards.
- [ ] No live `Modifier.blur()` inside LazyColumn or scrollable Column.
- [ ] Alpha values follow the **Glass Alpha Tuning Matrix**: Dialog $\approx 0.54$, Cards $\approx 0.88$, Tab Bar $\approx 0.85$, Docked Top Bar $\approx 0.97$.
- [ ] Verified in both Portrait and Landscape orientations.
- [ ] Dialogs are wrapped with `AnnounceGlassDialogOpen()` and use `imePadding()`.
- [ ] Press interactions use Apple-style spring scaling instead of rectangular ripple boxes.
- [ ] Full-page scrollable routes are registered in `Routes.FULL_PAGE_SCROLL_ROUTES` so top bars frost correctly.
- [ ] All text passes WCAG AA contrast against its respective translucent background.
