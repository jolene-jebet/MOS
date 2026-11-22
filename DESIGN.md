# DESIGN.md — MOS

The design system for MOS, taken from the Figma mockups in `docs/mockups/`.
`ui/theme/` (MOSTheme) is the code version of this file. Keep the two in sync.

> ⚠️ Hex values were estimated from screenshots. Confirm the exact values in Figma (Inspect panel) and update both this file and `Color.kt`.

---

## 1. Design principles

- **Safety first:** the safest route is always shown first, even if it's slower.
- **Familiar:** the layout feels like Google Maps (light, clean, rounded, map-centred).
- **Risk at a glance:** green / amber / red is used consistently everywhere.
- **Nairobi-specific:** transport modes are Walk, Boda Boda, Matatu, Private.

---

## 2. Colours

### Brand
| Name | Hex | Use |
|---|---|---|
| `Primary` | `#14532D` | Deep green: buttons, selected chip, links, active nav, app title |
| `PrimaryDark` | `#0B3B20` | Profile header gradient end, pressed states |
| `PrimaryContainer` | `#E4F0E8` | Selected route card background, icon tiles |
| `OnPrimary` | `#FFFFFF` | Text and icons on Primary |

### Risk (most important: never use for anything else)
| Name | Hex | Container (chip bg) | Use |
|---|---|---|---|
| `Safe` | `#2DB864` | `#D7F5E2` | Low risk, "Safest", "GPS Safe", safe stops |
| `Caution` | `#F9AB00` | `#FEF3D6` | Moderate risk, "Moderate Crowd", caution stops |
| `Risk` | `#D93025` | `#FCE8E6` | High risk, incidents, "High theft risk", Sign out |
| `Info` | `#1A73E8` | `#E8F0FE` | "Notice" tags, flooding bar, "Destination" chip |

**Brand green vs Safe green:** the brand colour is a dark forest green; Safe is a brighter, lighter green and always comes with a label or shield icon ("Safest", "Safe"). Never use `Primary` to mean "safe".

**Thresholds:** Low < 15% · Moderate 15–30% · High > 30% *(adjust once the AI model is ready)*.

### Neutrals
| Name | Hex | Use |
|---|---|---|
| `Background` | `#F5F7FB` | Screen background |
| `Surface` | `#FFFFFF` | Cards, sheets, bottom nav |
| `SurfaceVariant` | `#EEF1F6` | Text field fill, inactive chips, toggle off |
| `Outline` | `#E1E5EB` | Card borders, dividers |
| `TextPrimary` | `#1F1F1F` | Headings, main text |
| `TextSecondary` | `#5F6368` | Subtitles, captions, timestamps |

---

## 3. Typography

Font: **Roboto** (Material default). Optional: Google Sans / Inter for headings.

| Style | Size / Weight | Use |
|---|---|---|
| `displaySmall` | 32sp / Bold | Big ETA ("24 min") on Route Detail |
| `headlineSmall` | 24sp / Bold | Screen headings ("Create your account", "John Kamau", app title) |
| `titleLarge` | 20sp / SemiBold | Top bar titles ("Route Detail", "Profile") |
| `titleMedium` | 16sp / SemiBold | Card titles, route time ("24 min"), list item titles |
| `bodyMedium` | 14sp / Regular | Body text, field values |
| `bodySmall` | 12sp / Regular | Subtitles, timestamps, distances |
| `labelMedium` | 12sp / Medium | Chips, badges, nav labels |
| `labelSmall` | 11sp / SemiBold, UPPERCASE, +0.5 letter-spacing | Section headers ("SAFETY BREAKDOWN", "SAVED LOCATIONS") |

---

## 4. Spacing, shape, elevation

- **Spacing scale:** 4 · 8 · 12 · 16 · 24 · 32 dp. Screen padding = **16dp**; gap between cards = **12dp**.
- **Corner radius:**
    - Chips, buttons, text fields: **full pill** (50%)
    - Cards: **16dp**
    - Bottom sheet top corners: **24dp**
    - Icon tiles: **12dp**
- **Elevation:** cards are flat with a 1dp `Outline` border or a very soft shadow (1–2dp). The bottom sheet and map popups use 4dp.
- **Icons:** Material Symbols, Rounded, 20–24dp.

---

## 5. Components (`ui/Components.kt`)

### RiskChip
Pill, `labelMedium`, coloured by risk level.
- Variants: `Safest` (Safe container + shield icon), `12% risk`, `Caution`, `Destination` (Info), `Slow` (Caution), `Notice` (Info).
- Text and icon use the strong colour; the background uses the matching container colour.

### RiskBar
- **Segmented:** one 8dp-tall rounded bar split into Safe / Caution / Risk percentages, with a legend underneath ("● Safe 78% ● Caution 14% ● Risk 8%").
- **Single:** a thin 4dp bar for each risk factor (accident, flooding, lighting), filled to its % in the factor's colour.

### ModeChip
Pill with icon + label: Walk · Boda Boda · Matatu · Private.
- Selected: `Primary` fill, white text.
- Unselected: `Surface` with `Outline` border, `TextPrimary`.
- Shown in a horizontally scrollable row.

### RouteCard
- Left: mode icon in a rounded tile.
- Middle: **time** (`titleMedium`), distance + "Via ___ Road" (`bodySmall`), then tags (e.g. "12% Accident Risk", "High CCTV Coverage").
- Right: badge ("SAFEST ROUTE" in Safe green, or "Moderate Crowd" in Caution).
- Safest card: `PrimaryContainer` background + 4dp `Primary` bar on the left edge. It is always the first card.

### Other shared pieces
| Component | Description |
|---|---|
| **LocationCard** | White card: "Current Location" (green dot) and "Destination" (red pin), connected by a dotted line. Swap button (Primary circle) on the right. |
| **RiskFactorRow** | Icon tile + label + % on the right + single RiskBar underneath. |
| **IncidentCard** | Coloured icon tile + title (1 line, ellipsis) + "place · 28m ago" + RiskChip tag. |
| **StopsTimeline** | Vertical line with coloured dots (Safe / Caution / Destination pin). Each stop: name, time + offset ("08:38 (+8m)"), status chip. |
| **SectionHeader** | `labelSmall` uppercase grey text, optional right-side action ("Live audit ⓘ", "2 Reported" in red). |
| **SettingsRow** | Icon tile + title + optional subtitle + chevron or toggle switch. |
| **StatTile** | Big number (`headlineSmall`) + small label ("ROUTES TAKEN"). Colours: Primary, Safe, Risk. |
| **PrimaryButton** | Full-width pill, `Primary` fill, white text, optional leading icon, 52dp tall. |
| **DangerButton** | Full-width pill, `Risk` container fill, `Risk` text ("Sign out"). |
| **TextField** | Pill, `SurfaceVariant` fill, no border, leading icon, label above the field (`bodySmall`). Password fields have an eye toggle. |
| **MapAlertPopup** | White card on the map: red warning icon, bold title, red risk text, "Reported 4m ago by 18 commuters". |
| **MapLegend** | "● Safe ● Caution ● Risk" in the bottom-left corner of the map. |
| **BottomNavBar** | 2 items: **Explore** (compass) · **Profile** (person). Active = `Primary`. |

---

## 6. Screens

**MOS logo:** a `Primary` circle with "MOS" in bold white (`OnPrimary`). Used on Welcome, Login and Signup.

### Welcome *(first screen when signed out)*
- MOS logo centred on the screen, tagline "Move smart. Arrive safe." underneath (`titleMedium`, `TextSecondary`).
- At the bottom: "Log in" PrimaryButton and "Sign up" outlined pill button (Primary border and text).

### Login *(no mockup: mirror Signup)*
MOS logo, "Welcome back", email + password fields, "Log in" PrimaryButton, "Continue with Google" button (Google "G" logo), "Don't have an account? **Sign up**".

### Signup
- Small top bar: "MOS / SIGNUP".
- Centred MOS logo.
- "Create your account" + subtitle "Join thousands navigating Nairobi safely".
- White card with fields: Full name, Email, Phone (+254 country picker), Create password, Confirm password.
- "Create account" PrimaryButton, then "Continue with Google" button (Google "G" logo).
- "Already have an account? **Log in**".
- Footer: lock icon + "End-to-end encrypted safety geolocation".

### Explore (Map Navigation)
1. Top bar: shield icon, "Map Navigation", profile avatar button.
2. App title "MOS" (Primary, bold) + "● LIVE RADAR" chip.
3. LocationCard (with "GPS Safe" chip and area label "Nairobi NW").
4. ModeChip row.
5. Map: coloured route lines (green/amber/red), destination pin, MapAlertPopup, MapLegend, locate-me and layers buttons on the right.
6. Bottom sheet "Suggested Routes" + "● Live Safety Updates" chip, followed by RouteCards (**safest first**).
7. BottomNavBar.

### Route Detail
1. Top bar: back arrow, "Route Detail", profile button.
2. Map preview with chips ("Via Ngong Road", "Fastest route").
3. Summary row: Current location · distance · destination.
4. ETA card: "24 min" + "On schedule" (green), distance, "Safest" chip + "12% risk" chip.
5. **Safety Breakdown:** overall risk score ("Low — 12%"), segmented RiskBar, then RiskFactorRows for Accident, Flooding and Lighting (night safety).
6. **Active Incidents Nearby:** IncidentCards.
7. **Stops Along Route:** StopsTimeline ("4 checkpoints").
8. BottomNavBar.

### Profile
1. Top bar: back, "Profile", menu (⋮).
2. Header: deep green gradient banner (Primary → PrimaryDark), avatar circle with initials ("JK") + verified badge, name, email, "Edit profile" outlined button.
3. Stats row: Routes Taken (Primary) · Reports Filed (green) · Safety Points (red).
4. Badge card: "Nairobi Route Guardian", "Trusted community contributor".
5. **Saved Locations:** Home, Work, "+ Add a location".
6. **Transport Preferences:** Default mode (Matatu), Avoid routes with (High theft risk, in red).
7. **Notifications:** Safety alerts (toggle on), Incident updates (toggle off).
8. **Account:** Change password, Privacy settings, Terms and privacy, Rate this app.
9. DangerButton "Sign out".
10. BottomNavBar.

---

## 7. Rules

- Use **only** MOSTheme colours, typography and shapes. No hard-coded hex values in screens.
- Risk colours mean risk only. Never use them for decoration.
- Reuse components from `ui/Components.kt` before creating new ones.
- Light mode only for now (dark mode later).
- Minimum touch target: 48dp. All icons need a `contentDescription`.