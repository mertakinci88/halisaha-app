---
name: run-halisaha-fe
description: Build, run, and drive halisaha-fe (the Halı Saha Yönetim Sistemi React/Vite frontend). Use when asked to start halisaha-fe, run the dev server, take a screenshot of its UI, or interact with a running page (login, panel, takvim, etc).
---

This is a React 18 + Vite + Tailwind v4 SPA with no server-side render
step, so "run" means: start the Vite dev server, then drive a headless
Chromium against it via `.claude/skills/run-halisaha-fe/driver.mjs`
(plain Playwright — `chromium-cli` was not available when this skill
was authored, so a small driver script does the same job).

All paths below are relative to the repo root (`halisaha-fe/`).

## Prerequisites

Node 18+ and npm (tested with Node v22.14.0 / npm 10.9.2). No OS
packages were needed beyond what Playwright's own installer pulls in.

## Setup

```bash
npm install
cp .env.example .env        # sets VITE_API_BASE_URL=http://localhost:8080
npx playwright install chromium   # one-time, for the driver below
```

The app is a pure frontend: it talks to `halisaha-be` (Spring Boot) on
`http://localhost:8080`. It **renders and the login page works without
the backend**, but submitting the login form needs the backend up on
8080 to actually reach `/panel` — see Gotchas.

## Run (agent path)

```bash
npm run dev &
for i in $(seq 1 30); do curl -sf http://localhost:5173 >/dev/null && break; sleep 1; done

node .claude/skills/run-halisaha-fe/driver.mjs
```

The driver navigates to `/giris`, screenshots the login form, fills
`admin` / `admin123`, submits, waits, and screenshots the result. It
prints the page title, final URL, and any console/page errors it saw.

Screenshots land in `.claude/skills/run-halisaha-fe/screenshots/`:
`01-login.png`, `02-after-submit.png`.

Stop the dev server when done:

```bash
lsof -ti:5173 -sTCP:LISTEN | xargs -r kill
```

To drive other routes/interactions, copy the pattern in `driver.mjs`
(`page.goto`, `page.fill`, `page.click`, `page.screenshot`) — it's a
plain Playwright script, not a fixed REPL.

## Run (human path)

```bash
npm run dev   # -> http://localhost:5173, Ctrl-C to stop
```

## Test

No test script is defined in `package.json` (`scripts` only has
`dev`/`build`/`preview`).

## Gotchas

- **Login only completes with the backend running.** With port 8080
  unreachable, submitting the login form leaves you on `/giris` with
  an error message (`apiHata`) instead of navigating to `/panel` — the
  driver still succeeds (no thrown error), it just won't reach the
  dashboard. Check `lsof -ti:8080 -sTCP:LISTEN` first if you need the
  post-login screenshot to show real data.
- **Login inputs have no `name`/`id`**, only `type` and `autocomplete`
  (`src/pages/LoginPage.jsx`). Select them with
  `input[autocomplete="username"]` / `input[autocomplete="current-password"]`,
  not `input[name=...]`.
- **`chromium-cli` isn't installed in this environment** — that's why
  this skill ships its own Playwright driver instead of the usual
  heredoc pattern. If `chromium-cli` becomes available later, it can
  replace `driver.mjs` directly (same nav/fill/click/screenshot verbs).
- Vite's first compile of a route can take a couple seconds; the
  driver's `waitForSelector('text=Giriş yap')` handles this — don't
  replace it with a fixed `sleep`.
