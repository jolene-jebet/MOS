# MOS

MOS is an Android app that shows road accident risk along routes in Nairobi and
ranks routes safest first. Transport modes: Walk, Boda Boda, Matatu, Private car.

This repo is currently a **skeleton**: every file exists with placeholder code
and TODOs. Real features are still to be built.

## Setup

1. Open the project in Android Studio and let Gradle sync.
2. Add your Google Maps key to `local.properties` (this file is not committed):

   ```
   MAPS_API_KEY=your_key_here
   ```

   The app builds without a key, but maps will not load.
3. Firebase setup is coming later. The Firebase lines in the Gradle files are
   commented out and marked `TODO: enable Firebase`.
4. Run the app, or build from a terminal with `./gradlew assembleDebug`.

## Tech

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Maps Compose,
Retrofit + Gson, coroutines. No Hilt and no XML layouts: repositories are
created by hand in `MainActivity.kt`.

## Folder structure

```
AGENTS.md / CLAUDE.md / DESIGN.md
docs/mockups/                       Figma screenshots
app/src/main/
  AndroidManifest.xml
  assets/fake_data.json             fake routes, incidents, places
  java/com/example/mos/
    MainActivity.kt                 repositories, NavHost, bottom bar
    ui/
      theme/                        Color.kt, Type.kt, Theme.kt (MOSTheme)
      Components.kt                 RiskChip, RiskBar, ModeChip, RouteCard
    data/
      Models.kt                     Route, Risk, Incident, Stop, Place, User, TransportMode
      RiskRepository.kt
      RouteRepository.kt
      AuthRepository.kt
    screens/
      auth/                         AuthScreens.kt (Login, Signup), AuthViewModel.kt
      explore/                      ExploreScreen.kt, ExploreViewModel.kt
      routedetail/                  RouteDetailScreen.kt, RouteDetailViewModel.kt
      profile/                      ProfileScreen.kt
```

Navigation routes: `login`, `signup`, `explore`, `routedetail/{routeId}`, `profile`.

### fake_data.json

Top-level keys: `routes`, `incidents`, `places`. Each route has a `risk` object:
`{ overall, accident }` (0–1). `flooding` and `lighting` are not predicted; they stay in the data only for compatibility.

## Who owns what

Every Kotlin file starts with an `// OWNER:` comment.

| Owner | Files |
|-------|-------|
| A | `ui/` (theme, Components), `screens/auth/`, `screens/profile/`, `data/AuthRepository.kt` |
| B | `screens/explore/`, `data/RouteRepository.kt` |
| C | `data/` (Models, RiskRepository), `screens/routedetail/`, `MainActivity.kt` / navigation |
