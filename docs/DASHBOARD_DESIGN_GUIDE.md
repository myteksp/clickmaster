# Clicker — Campaign Dashboard UX Design Guide

A practical, implementation-ready design guide for the Clicker frontend (React 19 + Vite + TypeScript + Tailwind 4, STOMP WebSockets). Recommendations are grounded in the existing data model (`Campaign`, `GeoDistribution`, `DeviceProfile`, `Scenario`/`ScenarioStep`, `SimulationStats`, `VisitEvent`) and the current pages (`DashboardPage`, `CampaignsPage`, `CampaignFormPage`, `CampaignDetailPage`).

Guiding principles are sourced from Nielsen Norman Group (NN/g) research on [progressive disclosure](https://www.nngroup.com/articles/progressive-disclosure/) and [wizards](https://www.nngroup.com/articles/wizards/), plus established patterns from Google Ads, Meta Ads Manager, LinkedIn Campaign Manager, Datadog, Vercel, and Stripe.

---

## 0. The Three Principles That Govern Everything

1. **Progressive disclosure.** Show the few options that matter to 80% of users up front; defer everything else behind a clear, well-labeled affordance (NN/g: *"the very fact that something appears on the initial display tells users that it's important"*).
2. **Smart defaults.** A new campaign should be runnable the moment it has a name + a site. Every other field already has a sensible default (`HTTP_ONLY`, `CONSTANT`, 100 visits/h, 60 min, US 100%). Defaults are the most powerful UX feature you have — they make "Advanced settings" safe to ignore.
3. **List = scan and act; Detail = understand and monitor.** Never mix those jobs. The campaigns list optimizes for triage (status, quick actions); the detail page optimizes for live observation and config review.

Everything below is an application of these three rules.

---

## 1. Campaign Creation: Wizard vs. Single-Page (and the Hybrid Winner)

### 1.1 The decision, decided

NN/g's research is decisive here. Key findings:

- **Wizards** reduce overwhelm and errors, but impose higher interaction cost (more clicks) and are poor for *frequent, expert* use.
- **Single-page forms** are better when users need to compare/adjust interdependent fields (the "what-if" stage) — NN/g's hotel-booking example: one screen is great for experimenting with room/date combinations but bad for lumping in unrelated payment fields.
- The best answer is **rarely 1-screen-vs-N-screens; it's a 2- or 3-screen compromise**.

**Recommendation for Clicker: a single-page form with preset templates + progressive disclosure — NOT a multi-step wizard.**

Rationale specific to Clicker:
- Campaign creation is a **frequent, expert task** (users create many campaigns, tune them, clone them). This is the exact case NN/g says wizards hurt.
- Clicker's fields are **interdependent and "what-if" in nature**: changing `simulationLevel` changes whether scenarios show; `trafficPattern` changes the meaning of `visitsPerHour`; geo weights must sum/feel right together. Users adjust these *together*. A wizard that locks geo behind "step 4" forces back-and-forth.
- The form is already short enough for one page once advanced options are collapsed.
- A wizard is appropriate **only** the very first time (onboarding) — solve that with a template, not a forced linear flow (see §3.3).

### 1.2 Recommended structure: single page, ordered top-to-bottom by importance

Replace the current 5 equal "cards" (`CampaignFormPage.tsx`) with a single scrollable column ordered by decision weight. Everything below "Scenarios" is collapsed by default.

```
┌─────────────────────────────────────────────────┐
│  [Template pills: Quick Start | Balanced | Heavy]│  ← §3.3 presets
├─────────────────────────────────────────────────┤
│  1. Essentials                                   │  always open
│     • Name                                       │
│     • Target Site           (inline "Create site")│
│     • Simulation Level      (3 radio cards)      │
├─────────────────────────────────────────────────┤
│  2. Traffic                                      │  always open
│     • Visits/hour · Duration   (side by side)     │
│     • Traffic Pattern         (4 radio cards)    │
│     • [tiny inline preview of the curve]         │  ← §4.3
├─────────────────────────────────────────────────┤
│  3. Scenarios            (only if level ≠ HTTP)   │  contextual
├─────────────────────────────────────────────────┤
│  ▸ Advanced Settings        (collapsed)          │  §3 progressive disclosure
│     • Geographic distribution                     │
│     • Device / browser targeting                  │
│     • User-agent rotation                         │
│     • Proxy provider                              │
│     • Schedule (cron)                             │
├─────────────────────────────────────────────────┤
│  Sticky footer:  [Cancel]      [Create Campaign] │
│  + live "estimated load" summary line            │  ← §1.4
└─────────────────────────────────────────────────┘
```

### 1.3 If you later want a wizard (power users / first-run only)

Use it **as an optional mode**, and follow NN/g's rules:
- Show a persistent **stepper/list of steps on the left** with the current step highlighted and completed steps marked (Veeam pattern). Never let users lose their place.
- **Enforce sequential order**; don't let users jump to step 5 first.
- Label buttons with the **next step's name**, not generic "Next".
- **Save state and allow resume** — Clicker already has `DRAFT` status; persist the in-progress campaign server-side and let users return.
- Make each step **self-sufficient** (no needing info from another page).
- Never run it in a blocking modal that hides the rest of the app.

### 1.4 The "estimated load" summary (low-effort, high-value)

A sticky line above the submit button that recomputes live:

> `≈ 100 visits/h × 1.0h = ~100 visits · 1 proxy · est. 1000 pageviews`

This turns abstract numbers (`visitsPerHour`, `durationMinutes`) into a mental model of cost/impact before submit. It's the single highest-ROI addition to the form.

---

## 2. Campaign List / Table View

The current `CampaignsPage.tsx` is a stack of rows with name + a trailing "Delete". This undersells the list's real job: **fast triage and bulk control of many campaigns**. Model it on Google Ads / Meta Ads Manager tables.

### 2.1 What belongs in a list row (vs. detail)

| In the LIST (scan + act) | In the DETAIL (understand + monitor) |
|---|---|
| Name, Site | Full config (all fields, editable) |
| Status badge + live mini-metric | Live stats grid + charts |
| Simulation level (icon) | Visit feed / activity stream |
| Visits/h, Duration | Run history |
| Last run time / progress | Geo distribution breakdown |
| Inline Start/Pause/Stop | Logs, errors, per-visit inspection |

### 2.2 Recommended table columns

```
[☐] | Status | Name + Site | Level | Pattern | Visits/h | Progress / Last run | Actions (⋯)
```

- **Checkbox column** enables bulk ops (§6.5).
- **Status** as a colored dot + label (not just text) — glanceable from across the screen.
- **Name + Site** collapsed into one cell (primary + secondary line) to save width.
- **Level** as an icon with tooltip (globe = HTTP, compass = Browser nav, cursor = Full browser).
- **Progress / Last run**: if `RUNNING`, show a thin progress bar (elapsed/`durationMinutes`); otherwise relative time ("2h ago").
- **Actions menu (⋯)**: Start / Pause / Resume / Stop / Duplicate / Edit / Delete. Never put "Delete" as a bare trailing text button (current code) — it's a destructive action and should be in a menu with confirmation.

### 2.3 Interaction patterns that reduce friction

- **Row click → detail.** But reserve an explicit actions area (right side) so clicking actions doesn't navigate.
- **Keyboard navigation**: ↑/↓ to move between rows, Space to toggle selection, Enter to open.
- **Sorting** on Status, Last run, Visits/h, Created. Default: Status (RUNNING first).
- **Filtering**: by status (multi-select chips), by site, by "running only" toggle. A single "Running" toggle chip at the top covers the most common need.
- **Search** by name — instant, client-side for <500 rows.
- **Sticky header + sticky action bar** that appears when rows are selected.
- **Empty state** with a call to action and a "Quick Start" template (current code just says "No campaigns yet" — upgrade it).

### 2.4 Real-time in the list

When a campaign is `RUNNING`, the row should show a **live pulse** on the status dot and a moving progress bar — updated via the existing STOMP `/topic/stats/{runId}` channel rather than the current 5s `setInterval` polling. Polling a list every 5s (current `DashboardPage`) causes layout thrash and wastes requests; prefer push for anything that moves.

---

## 3. Progressive Disclosure Patterns (Rich Config Without Overwhelm)

NN/g's two requirements, applied directly:
1. **Get the split right** — primary surface holds only what's frequently needed.
2. **Make progression obvious** — a visible, well-labeled affordance with strong information scent.

### 3.1 "Advanced Settings" collapsible

- One `<details>`-style section at the bottom of the form, **collapsed by default**.
- Label it **"Advanced settings"** with a count chip: `Advanced (4)`.
- When expanded, animate height (Tailwind: `transition-all` + `grid-rows` trick or `max-h`).
- Persist the user's preference (`localStorage`) — if a power user expands it once, keep it expanded.
- **Never hide a field that's required or that most users must touch.** Geo currently sits as a primary card; for most users US-100% is fine, so it's a perfect candidate to move *into* Advanced (keep the current default).

### 3.2 Smart defaults (the 80% case)

Field | Default | Why
---|---|---
`simulationLevel` | `HTTP_ONLY` | Cheapest, most reliable, covers the common "just inflate visits" goal
`trafficPattern` | `CONSTANT` | Simplest to reason about
`visitsPerHour` | `100` | Safe, non-alarming load
`durationMinutes` | `60` | Short enough to verify, long enough to matter
`geoDistribution` | `US 100%` | Matches typical proxy availability via Asoks
`deviceProfile` | `[]` → engine picks from `OrganicProfile` | Empty = "auto", the most realistic option
`userAgentConfig.rotation` | `RANDOM` | Most natural
`proxyConfig.provider` | `ASOCKS` | Only configured provider
`scenarios` | none until level ≠ HTTP | Contextual, not always needed

Mark defaults visibly. On the `simulationLevel` radio cards, add a subtle **"Recommended"** tag to `HTTP_ONLY`. This single word dramatically reduces decision paralysis.

### 3.3 Preset templates (the highest-leverage pattern)

A row of pills at the top of the form. Clicking one fills the whole form with a coherent bundle — far better than a wizard for "I just want it to work".

| Template | simulationLevel | pattern | visits/h | duration | geo | device |
|---|---|---|---|---|---|---|
| **Quick Start** | HTTP_ONLY | CONSTANT | 50 | 30m | US 100% | auto |
| **Balanced** | BROWSER_NAVIGATION | REALISTIC_WAVE | 200 | 120m | US 60 / GB 25 / DE 15 | auto |
| **Heavy / Stress** | FULL_BROWSER | RAMP_UP | 500 | 240m | US 50 / DE 30 / BR 20 | desktop+mobile mix |
| **Custom** | (current values) | — | — | — | — | — |

Behavior:
- Selecting a template **overlays** values with a brief toast ("Applied 'Balanced' preset") and a one-time **Undo** action (critical — users fear destructive overwrites).
- After applying, the user is free to tweak any field; the active pill becomes "Custom".
- Templates double as **onboarding** — they teach what good configurations look like, replacing a forced first-run wizard.
- Store templates as a static array in the frontend (`src/constants/templates.ts`); later allow user-saved templates if desired.

This is the exact pattern used by Google Ads ("Conversions: Smart / Manual") and Vercel deployment presets.

### 3.4 Inline help and tooltips

- **Field-level help**: a small `?` icon next to labels for jargon fields (`trafficPattern`, `deviceProfile`, `scheduleCron`). On hover/focus, show a short definition + an example. NN/g: help should appear *next to* the field, not cover it.
- **Definition tooltips** for `simulationLevel` cards: the current labels already do this well ("HTTP Only — Simple page loads with proxy & user-agent rotation"). Keep that pattern; replicate it on every option card.
- **Status tooltips**: hovering a `PAUSED` badge explains "Visits paused; can resume without losing run."
- **Inline error help** (not tooltips): when validation fails, the message sits under the field, persistent, in `red-400` (see §4.5). Tooltips must never be the only carrier of an error.
- Avoid tooltip overuse: if a field needs a paragraph to explain, the field name/label is the real problem — fix that first.

---

## 4. Form UX for the Complex Fields

### 4.1 Geographic distribution selector

Current implementation (`CampaignFormPage.tsx:224-249`): a country-code text input + a 0–100 slider per row. Three problems: (a) raw 2-letter codes are error-prone, (b) weights don't visibly sum to anything, (c) no sense of "which countries".

**Recommended: segmented weight bars, not free sliders.**
- A **searchable country picker** (`<Combobox>`) returning `{countryCode, countryName}` — replaces the raw text field. Show the flag emoji + name.
- A **linked-segment bar**: one horizontal bar split into colored segments, each segment's width = that country's weight. Dragging a divider shifts weight between neighbors so the total always = 100%. (Same pattern as Stripe's revenue-split UI and Meta's geo budget allocation.)
- A small **legend** below: `🇺🇸 US 60% · 🇬🇧 GB 25% · 🇩🇪 DE 15% · Total 100%` with a green check when balanced, a warning when weights don't reflect likely proxy availability.

**When to use a map instead:** only on the *detail/monitoring* view, to show *actual* visit geo distribution from live data (§5.4). For *input*, the map is a vanity component that adds friction — NN/g's research repeatedly shows maps look impressive in demos but slow down real data entry. Keep input as a compact bar/table; reserve the map for output.

**City-level targeting** (`geoDistribution.city` is in the model): expose only when a country is selected, as an optional "Specify city…" expandable under that country's row. Don't show it by default.

### 4.2 Device / browser targeting (`deviceProfile`)

The model is `{device, os, browser, weight}[]`. Recommended UX:
- A **matrix** rather than free rows: rows = device classes (Desktop / Mobile / Tablet), columns = browser, cells = a weight or on/off.
- Provide **preset mixes**: "Desktop only", "Mobile-heavy (70/30)", "Balanced (realistic)", "Custom". Same pill pattern as campaign templates.
- When `deviceProfile` is empty, show **"Auto (realistic distribution)"** as the active state with a one-line explanation that the engine draws from `OrganicProfile`'s 11 profiles. Empty = auto is the cleanest mental model.
- Show a live **donut/stacked bar** preview of the resulting mix next to the matrix.

### 4.3 Traffic pattern selection

The four patterns (`CONSTANT`, `RAMP_UP`, `PULSE`, `REALISTIC_WAVE`) are perfect for **visual option cards**: each card shows a tiny inline SVG sparkline of the shape (flat line / rising line / spikes / wavy). A picture is worth the verbose label. Current radio cards (`CampaignFormPage.tsx:191-213`) already use the card pattern — just add the sparkline.

Bonus: a larger **live preview chart** below the selected card, showing visits/hour across the campaign duration (e.g., "RAMP_UP over 120 min" → a rising line from 50 to 200). This makes `durationMinutes` and `visitsPerHour` jointly meaningful and directly feeds the "estimated load" summary (§1.4).

### 4.4 Duration and scheduling inputs

- **Duration**: replace the bare number input with a **segmented control + slider combo**: `30m | 1h | 2h | 4h | 8h | Custom`. The slider snaps to these steps; the number remains editable for precision. This is the Slack/Stripe duration pattern and is much faster than typing.
- **Schedule (`scheduleCron`)**: hide behind a **"Schedule instead"** toggle. When on, show a human-readable cron builder (every N hours/days at HH:00) — **never expose raw cron syntax as the primary input**. Render a live preview line: *"Runs daily at 09:00 and 21:00, starting tomorrow."* If you must allow raw cron, put it under an "Advanced cron expression" expandable with a "Test expression" button.

### 4.5 Validation and error handling

Current code uses native `required` + `alert('Failed to create campaign')` (`CampaignFormPage.tsx:88`). Both are anti-patterns:
- `alert()`/`confirm()` block the thread, can't be styled, and lose context. Replace with inline errors and a toast/confirmation modal.
- Errors should be: **inline, persistent under the field, in `red-400`**, appearing on blur (not on every keystroke), with a clear fix ("Visits/hour must be between 1 and 10,000").

Validation rules to enforce (client-side, mirrored from the backend DTO validation):
| Field | Rule | Message |
|---|---|---|
| name | non-empty, ≤100 chars | "Give your campaign a name." |
| siteId | required (and site exists) | "Pick a target site." + offer "Create site" link |
| visitsPerHour | 1–10,000 | "1–10,000 visits per hour." |
| durationMinutes | 1–1440 | "1–1440 minutes (max 24h)." |
| geoDistribution | weights sum sensible (warn if 0 or wildly off) | non-blocking warning |
| scenarios weights | ≥1 each when present | "Each scenario needs a weight ≥1." |
| entryUrl | valid path starting with `/` when provided | "Enter a path like /landing." |

Cross-field validation:
- If `simulationLevel = HTTP_ONLY` and scenarios are attached → warning "HTTP-only mode ignores scenarios." (Currently the code just hides the scenarios block — good, keep that.)
- If duration × visits/hour would exceed a proxy/concurrency ceiling → warning with the estimated proxy count.

On submit failure, surface the **backend's error message** (the `GlobalExceptionHandler` already returns structured errors) in an inline error banner at the top of the form, with field-level mapping where possible. Never swallow the error into a generic alert.

---

## 5. Real-Time Dashboard & Monitoring (the Detail Page)

This is where Clicker should invest most. The current detail page (`CampaignDetailPage.tsx`) has a 6-up stat grid + a monospace visit log + a run list. It's functional but static-feeling. The goal: make a running campaign feel **alive and trustworthy**.

### 5.1 Data transport (you already have the right tools)

The backend pushes two STOMP topics per run:
- `/topic/visits/{runId}` → individual `VisitEvent`s
- `/topic/stats/{runId}` → aggregated `SimulationStats`

**Keep WebSockets as the single source of truth for live data.** Remove the 3s `setInterval(load)` polling on the detail page (`CampaignDetailPage.tsx:39`) — it races the socket and causes flicker. Use REST only for the *initial* snapshot, then let the socket drive updates. For the *list* page, a single slow poll (15–30s) or a user-wide `/topic/campaigns` summary event is fine.

UX details for streaming data:
- **Throttle/batch DOM updates**: visits can arrive faster than 60fps. Buffer events and flush in a `requestAnimationFrame` loop (or every ~250ms). The current `.slice(0, 50)` cap is good; add a virtualized list if you go beyond ~200 rows.
- **Animate value changes**: when `totalVisits` increments, do a brief count-up animation or a flash. A number that snaps without feedback feels broken; a number that visibly ticks feels alive.
- **Graceful degradation**: if the socket disconnects, show a subtle "Reconnecting…" banner and fall back to a 5s poll — never silently freeze.

### 5.2 The live stat grid (upgrade the current 6-up)

Current grid is good in structure. Improvements:
- **Status first, large and colored**, with a pulsing dot when `RUNNING`.
- Make the **two metrics users care about most visually dominant**: `successfulVisits` (green) and `failedVisits` (red). Demote `activeProxies` and `elapsedTime` to secondary.
- Add a **success rate** derived metric: `successfulVisits / totalVisits` as a percentage with a colored ring/gauge — the single most glanceable health metric.
- Add a **visits/sec sparkline** (last 60s) next to the `visitsPerSecond` number — a lone number doesn't convey trend; a 1-inch sparkline does.
- Show **elapsed / estimated remaining** as a progress bar, not two strings: `████████░░░░ 47m / 120m · ~73m left`.

### 5.3 Live charts — which types for which metric

Pick one charting library and use it everywhere. Recommendations for this stack:

| Need | Library | Why |
|---|---|---|
| Ultra-fast live time-series (visits/sec, success/fail over time) | **uPlot** | Sub-millisecond render; built for streaming; tiny bundle. The industry standard for live dashboards (Grafana-adjacent). |
| Rich charts + geo map + gauges in one lib | **Apache ECharts** | Handles time-series, world map choropleth, donuts, gauges. Heavy but one dependency. |
| Simplest React DX | **Recharts** | Easy, but slow for high-frequency updates — avoid for the live visit stream. |

**Recommendation:** **ECharts** as the primary lib (geo map + charts unified), with **uPlot** for the high-frequency visits/sec stream if ECharts can't keep up. Start with ECharts alone and only add uPlot if you see frame drops at >50 visits/sec.

Chart-type mapping:

| Metric | Chart | Notes |
|---|---|---|
| Visits over time (volume) | **Area chart** (stacked success/fail) | Time on X, count on Y, streaming window last 5–15 min. |
| Success vs failure rate | **Donut** + center % | Updates live; pair with the trend area chart. |
| Response time | **Line** with p50/p95 bands | Detects target-site slowdowns. |
| Visits/sec (instant) | **Sparkline** in the stat card | Last 60s only. |
| Geo distribution | **World choropleth** | §5.4 |
| Status code breakdown | **Horizontal bar** | 200/301/404/500 counts. |
| Per-path popularity | **Horizontal bar** (top 10 paths) | Which URLs got traffic. |

Always **left-pad the Y axis** and use a **sliding time window** so the chart doesn't rescale jarringly on every update. Provide a small timeframe switcher: `1m | 5m | 15m | Full run`.

### 5.4 Geographic visualization

- **World choropleth** (ECharts `map` series with a world GeoJSON): countries colored by visit count, legend on the side, click a country to drill into its city breakdown.
- Keep it **on the detail page only**, fed by aggregating `VisitEvent.proxyAddress` geo (or a backend-provided per-country rollup). This is an *output* viz, never an input (see §4.1).
- Pair with a compact **top-5 countries table** for accessibility/precision — charts alone fail colorblind users and exact-value readers.
- Add a **device/OS breakdown donut** beside it for a complete "who is visiting" picture.

### 5.5 Visit feed / activity stream

Current monospace log (`CampaignDetailPage.tsx:151-168`) is a fine starting point — terminal-style logs feel appropriately "real-time". Upgrade it:
- **Color-code by outcome**: green ✓ for success, red ✗ for failure, amber for slow (`responseTimeMs` > threshold). Already partially done.
- **Virtualize** the list for performance if it grows.
- **Filter chips** above the feed: `All | Success | Failed | Slow`. The failed filter is the one users actually want — failures need debugging.
- **Pause-on-hover**: when the user hovers/scrolls the feed, stop prepending new rows so they can read one. Resume on mouse leave. This is the Datadog/Splunk pattern and prevents the "log keeps jumping while I read" rage.
- **Click a visit → expand** to show full detail (path, proxy, UA, status code, timing). Don't cram everything into one line.
- Add a **"Export" / "Copy as JSON"** for support/debugging.

### 5.6 Status indicators & progress

- **Status dot** with semantic color + optional pulse for `RUNNING` (Tailwind: `animate-pulse`).
- **Progress bar** for the run: `elapsedTime / durationMinutes`, with the bar turning amber in the last 10% and green at completion.
- **Health ring** (success rate) as the headline indicator.
- **Empty running state**: if `RUNNING` but no visits yet (warmup), show a skeleton + "Starting workers…" rather than zeros, which look broken.

### 5.7 Success/failure over time

The stacked area chart (success area + failure area) over a sliding window is the right primitive. Add a **failure-annotation** layer: when the failure rate spikes above a threshold, mark that time region with a subtle red background band and a tooltip ("12 failures in 30s — possible proxy exhaustion / target 5xx"). This turns a chart into a diagnostic tool.

---

## 6. Design Patterns: Evaluation & Verdicts

### 6.1 Wizard/stepper vs single-page form
**Verdict: single-page + presets + progressive disclosure.** Wizard only as optional first-run or never. Reasoning in §1.1.

### 6.2 Inline editing vs modal dialogs
- **Bulk/lifecycle actions** (Start/Stop/Pause on the list): **inline** — a row action menu, no modal. Modals for read/act tasks add clicks.
- **Quick edits** (rename, adjust visits/hour): **inline edit** (click-to-edit on the field) on the detail page. Fast, no context loss.
- **Destructive actions** (Delete campaign): **modal confirmation** with the campaign name typed-to-confirm for the final safety. Replace the current `confirm()` (`CampaignsPage.tsx:23`).
- **Complex edits** (full config change): the dedicated edit page/form, not a modal — config is too rich for a modal.
- **Create-site / create-scenario from within the campaign form**: **modal** (small, focused), because the user is mid-task and shouldn't lose their place.
- **Rule of thumb (NN/g):** modals interrupt; use them only when the sub-task blocks the primary task and must be completed/dismissed.

### 6.3 Drag-and-drop for scenario building
The `Scenario` → ordered `ScenarioStep` model (actionType, selector, delays, probability) is a natural fit for a **vertical drag-and-drop list** (like a playlist or Notion blocks).
- Use `@dnd-kit` (modern, accessible, React 19-friendly; avoids the older `react-beautiful-dnd` which is unmaintained).
- Each step is a card: drag handle on the left, action type as a colored chip (`LOAD`/`CLICK`/`SCROLL`/…), inline fields for selector/value, and a probability slider.
- Reorder updates `orderIndex` live.
- Provide an **"Add step"** with an action-type picker (icon menu), and **step templates** ("Add a typical scroll", "Add a 2s wait").
- For *campaign→scenario* weighting (the `CampaignScenarioDto` with `weight`), use the **linked-segment bar** from §4.1 rather than free number inputs — drag dividers to rebalance weights.

Drag-and-drop is **worth it here** because step order is semantically meaningful and users think about it spatially. Don't add DnD where order is irrelevant.

### 6.4 Template / preset system
Covered in §3.3. Also apply at the **scenario** level (scenario templates: "Read an article", "Browse homepage", "Multi-page wander") and the **site** level (none needed — sites are trivial). Templates are the single most impactful onboarding + power-user feature combined.

### 6.5 Bulk operations
Currently absent. Add when a user has >5 campaigns (common case).
- **Checkbox selection** in the list (column 1), with **select-all** in the header.
- A **sticky bulk action bar** appears at the bottom (or top) when ≥1 row is selected: `N selected · [Start] [Pause] [Stop] [Duplicate] [Delete]`.
- **Confirm destructive bulk actions** in a modal listing the affected campaign names.
- **Optimistic UI**: reflect the action immediately, roll back on error with a toast.
- Bulk ops require backend endpoints (`POST /campaigns/bulk/{action}` with id list) — coordinate with the backend.

---

## 7. Layout & Information Architecture

### 7.1 App shell

The current `Layout.tsx` (sidebar nav) is the right base. Recommended primary nav, ordered by frequency:
```
Dashboard      → live overview, "what's happening now"
Campaigns      → the working list (primary surface)
Scenarios      → reusable building blocks
Sites          → targets (rarely touched)
Settings       → proxy/account
```

### 7.2 Dashboard (overview) page

The current `DashboardPage.tsx` shows 3 stat cards + a recent list. This is the "command center". Make it answer two questions instantly: **(1) Is anything broken right now? (2) What's running?**

```
┌──────────────────────────────────────────────────────────┐
│  Today: 1,204 visits · 98.2% success · 3 running         │  headline strip
├──────────────┬──────────────┬──────────────┬─────────────┤
│ Running (3)  │ Active proxies│ Success rate │ Failures 24h│  KPI cards
│  ▸ pulse     │  47           │  98.2% ↑     │  21 (1.8%)  │
├──────────────────────────────────────────────────────────┤
│  Visits (last 15 min)  [area chart, all running combined]│  heartbeat
├─────────────────────────────────┬────────────────────────┤
│  Running campaigns (live rows)  │  Recent failures        │
│  · Camp A  ▓▓▓▓░░  62 v/s 99%  │  · 5xx from /api  ×4    │
│  · Camp B  ▓▓▓▓▓▓ 120 v/s 97%  │  · proxy timeout ×2     │
└─────────────────────────────────┴────────────────────────┘
```

- One combined heartbeat chart across all running campaigns is more valuable than per-campaign charts here.
- A **"Recent failures"** panel turns the dashboard from decorative into actionable.
- Each running row links to its detail page.

### 7.3 Detail page IA

```
[Header: name, site, status, primary actions (Start/Pause/Stop/...)]
[KPI strip: status | visits/h | success% | visits/s sparkline | progress bar]
[Tab: Live  |  Configuration  |  Runs  |  Logs]
   ├─ Live: stacked area + donut + geo map + visit feed
   ├─ Configuration: the same form as creation, read/edit
   ├─ Runs: run history table (expandable per run)
   └─ Logs: error log / per-visit inspection
```

Tabs separate **monitoring** (Live) from **management** (Configuration) — don't mix them on one long scroll.

### 7.4 Responsive / density
- This is a power tool; optimize for **desktop (≥1280px)**. A dense, information-rich layout is correct here (users are experts, at desks).
- Provide a **density toggle** (Comfortable / Compact) for the table — power users want Compact.
- Mobile: a read-only monitoring view (Live tab + status) is worth supporting; full config editing can be desktop-only with a clear note.

---

## 8. Color, Typography & Component Conventions (Tailwind 4)

The existing dark theme (`gray-900` cards, `gray-800` inputs, `emerald` accent) is solid and on-trend. Codify it so the dashboard is consistent:

| Token | Meaning | Tailwind |
|---|---|---|
| Success / RUNNING | `emerald-400/500`, pulse dot | `text-emerald-400`, `bg-emerald-500` |
| Warning / PAUSED | `amber-400/500` | `text-amber-400` |
| Danger / FAILED / Stop | `red-400/500` | `text-red-400` |
| Info / COMPLETED | `blue-400` | `text-blue-400` |
| Neutral / DRAFT | `gray-400/500` | `text-gray-400` |
| Surface | card | `bg-gray-900 border-gray-800` |
| Input | form field | `bg-gray-800 border-gray-700 focus:border-emerald-500` |

Status badges already follow this in `CampaignsPage.tsx:28` — promote that map to a shared `<StatusBadge status=... />` component used everywhere (it's currently duplicated inline in 3 places).

**Recommended shared components to extract** (the codebase has repetition that will multiply as the dashboard grows):
- `StatusBadge` (status → color, used in list, detail, dashboard)
- `StatCard` / `KpiCard` (already a `StatCard` in `CampaignDetailPage.tsx:203` — extract)
- `SectionCard` (the repeating `bg-gray-900 border rounded-lg p-5` wrapper)
- `OptionCard` (radio-as-card, used for level + pattern — extract)
- `SegmentedBar` (linked-weight bar for geo/scenario weights, §4.1)
- `Toggle` / `Stepper` primitives
- `EmptyState` (icon + heading + CTA)
- `ConfirmDialog` (replaces all `confirm()` calls)
- `Toast` system (replaces all `alert()` calls)

Type scale: keep it tight. `text-2xl font-bold` for page titles, `text-lg font-semibold` for section heads, `text-sm` for body, `text-xs text-gray-500` for meta — all already in use. Use **tabular-nums** (`tabular-nums` / `font-variant-numeric`) on every live-updating number so digits don't shift width as they tick.

---

## 9. Implementation Priority (do these first, highest ROI)

1. **Replace `alert()`/`confirm()`** with a toast + confirm-modal. (Cheap, huge perceived-quality lift.)
2. **Extract shared components** (`StatusBadge`, `StatCard`, `SectionCard`, `OptionCard`). Stops divergence.
3. **Add the "estimated load" summary line** + traffic-pattern sparklines on the form. Tiny code, big clarity.
4. **Add preset template pills** (Quick Start / Balanced / Heavy / Custom). Best onboarding move.
5. **Move geo/device/UA/proxy/schedule into a collapsed "Advanced" section.** Cleans the form for 80% of users.
6. **Add the success-rate donut + a visits/sec sparkline + a progress bar** to the detail page. Makes runs feel alive.
7. **Drop the 3s poll on the detail page; rely on STOMP** (with reconnect banner).
8. **Upgrade the campaigns list**: checkbox + actions menu + status dot + progress bar + filters.
9. **Add ECharts** for the live area chart + world map on the detail/dashboard pages.
10. **Bulk operations** (Start/Pause/Stop on multiple campaigns).

Steps 1–7 are frontend-only and low-risk; 8–10 may need small backend support.

---

## 10. References

- Nielsen Norman Group — [Progressive Disclosure](https://www.nngroup.com/articles/progressive-disclosure/) (Jakob Nielsen)
- Nielsen Norman Group — [Wizards: Definition and Design Recommendations](https://www.nngroup.com/articles/wizards/) (Raluca Budiu)
- Nielsen Norman Group — [Top 10 Application-Design Mistakes](https://www.nngroup.com/articles/top-10-application-design-mistakes/)
- Nielsen Norman Group — [Indicators, Validations, and Notifications](https://www.nngroup.com/articles/indicators-validations-notifications/)
- Nielsen Norman Group — [Modal & Nonmodal Dialogs](https://www.nngroup.com/articles/modal-nonmodal-dialog/)
- Product references: Google Ads (campaign table, status taxonomy), Meta Ads Manager (geo budget allocation, segmented bars), LinkedIn Campaign Manager (objective-first creation), Datadog (live log streams, pause-on-hover), Vercel (deployment presets), Grafana (uPlot streaming), Stripe (revenue-split linked bars).
