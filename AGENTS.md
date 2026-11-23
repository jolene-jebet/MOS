# MOS
# AGENTS.md — MOS
## PROJECT CONTEXT

- **MOS** is an Android app that shows road accident risk along routes in Nairobi and ranks routes safest first.
- Transport modes: Walk, Boda Boda, Matatu, Private car.
- Team of 3. The AI risk model is built separately and is NOT ready yet. The app uses fake data until then.
- Screens: Login/Signup, Explore (map + suggested routes), Route Detail, Profile. Bottom nav: Explore | Profile.

## TECH STACK (do not change without asking)

- Kotlin, Jetpack Compose, Material 3. **No XML layouts.**
- MVVM: one ViewModel per screen (Profile may skip it).
- **No Hilt or other dependency injection framework.** Repositories are created in `MainActivity.kt`.
- Navigation Compose.
- Google Maps Compose; Retrofit later, for the AI server.
- Firebase Authentication (email/password) is **planned, not enabled yet**. Its Gradle lines are commented out with `// TODO: enable Firebase`. Until then, use `FakeAuthRepository`.
- Package: `com.example.mos`. Min SDK 26.

## PROJECT STRUCTURE

```
MOS/
├─ AGENTS.md, CLAUDE.md, DESIGN.md, README.md
├─ docs/mockups/                 Figma screenshots
└─ app/src/main/
   ├─ assets/fake_data.json      fake routes, risk, incidents, places
   └─ java/com/example/mos/
      ├─ MainActivity.kt         navigation, bottom bar, creates repositories
      ├─ ui/theme/               Color.kt, Type.kt, Theme.kt (MOSTheme)
      ├─ ui/Components.kt        RiskChip, RiskBar, ModeChip, RouteCard
      ├─ data/Models.kt          Route, Risk, Incident, Stop, Place, User, TransportMode
      ├─ data/RiskRepository.kt  interface + FakeRiskRepository
      ├─ data/RouteRepository.kt interface + FakeRouteRepository
      ├─ data/AuthRepository.kt  interface + FakeAuthRepository
      └─ screens/{auth, explore, routedetail, profile}/
```

- Keep this structure. Don't add new layers, modules or folders without asking.
- Owners: **A** = ui/, auth, profile · **B** = explore, RouteRepository · **C** = data/, routedetail, MainActivity/navigation.
- Only edit files you were asked to touch. Respect the `// OWNER:` comment at the top of each file.

## ARCHITECTURE RULES

- Screens NEVER call the AI, Firebase or the network directly. Always go through a repository.
- Each repository is an interface plus a Fake version. A real version replaces the Fake with a one-line change in `MainActivity.kt`:
    - `FakeRiskRepository` → `RemoteRiskRepository` (AI server via Retrofit)
    - `FakeAuthRepository` → `FirebaseAuthRepository`
    - `FakeRouteRepository` → Google Directions API
- **`RiskRepository` is the only place the AI connects.**
- `fake_data.json` must match the agreed AI response shape: `{ overall, accident }` (0–1). `flooding` and `lighting` are not predicted; they stay in the data only for compatibility.
- The "Safest route" is always shown first, regardless of travel time.

## AI CONTRACT

- Endpoint: `POST /predict` (base URL is `RISK_API_BASE_URL` in `data/RiskApi.kt`, currently the Render server `https://semproject-mlengine-render.onrender.com/`). For a local server on the emulator use `http://10.0.2.2:8000/`.
- Request:
  ```json
  { "coordinates": [[lat, lng], ...], "mode": "MATATU", "hour": 17 }
  ```
  `coordinates` come from the route's stops, `mode` is `TransportMode.name`, `hour` is the current hour (0–23).
- Response (both values 0–1):
  ```json
  { "overall": 0.12, "accident": 0.10 }
  ```
- `flooding` and `lighting` are **not predicted** by the model. They are kept in `Risk`, `RiskPredictResponse` and `fake_data.json` only for compatibility and are not shown in the app.

## SECRETS

- NEVER commit `local.properties` or `app/google-services.json`. Both must be in `.gitignore`.
- The Maps API key is read from `local.properties` (`MAPS_API_KEY`), never hard-coded.

---

# CRITICAL RULES — MUST FOLLOW

## RESPONSES

- Keep responses concise and to the point, unless the user asks otherwise.

## PLANNING MODE

- Always ask clarifying questions before planning.
- Never assume design, tech stack or features. Check this file and DESIGN.md first, then ask.
- Use deep-dive sub-agents for research.
- Use sub-agents to review the different parts of the plan before presenting it to the user.

## CHANGE / EDIT MODE

- Use sub-agents to implement features where possible.
- Identify changes that can run in parallel and give them to separate sub-agents.
- When using sub-agents, act as a coordinator only.
- Use the right model for the task: premium models for complex coding, mid-tier models for simple tasks like documentation.
- After completing features, always run:
    - `./gradlew assembleDebug` (must build)
    - `./gradlew lint`
    - `./gradlew test`
- Fix all build errors before reporting work as done.

## TESTING

- Use the available tools: JUnit for ViewModels and repositories, Compose UI tests for screens.
- Never assume a change works. Always build and test.
- If a change can't be tested (for example, it needs a real device or Firebase keys), say so clearly and ask whether to skip testing.

## GIT

- Small, clear commits (for example, `feat(auth): add login screen`).
- Never push. The user pushes to GitHub.
- Never commit secrets (see SECRETS).

## UI / DESIGN SYSTEM

- Follow DESIGN.md when creating or reviewing any screen or component.
- Use only colours, fonts and spacing from `MOSTheme`. No hard-coded colours in screens.
- Reuse components from `ui/Components.kt` before creating new ones.
- Keep the look consistent across the whole app.
- Design system: @DESIGN.md
- Mockups: `docs/mockups/`
