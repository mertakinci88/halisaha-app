#!/usr/bin/env node
// Playwright driver for halisaha-fe. Drives the Vite dev server headlessly
// and screenshots the result. See SKILL.md for usage.
//
// Usage: node driver.mjs [outDir]
//   outDir defaults to ./.claude/skills/run-halisaha-fe/screenshots

import { chromium } from 'playwright';
import path from 'node:path';
import fs from 'node:fs';

const BASE_URL = process.env.HALISAHA_FE_URL || 'http://localhost:5173';
const outDir = process.argv[2] || path.join(import.meta.dirname, 'screenshots');
fs.mkdirSync(outDir, { recursive: true });

const errors = [];

const browser = await chromium.launch();
const page = await browser.newPage();
page.on('console', (msg) => {
  if (msg.type() === 'error') errors.push(msg.text());
});
page.on('pageerror', (err) => errors.push(String(err)));

console.log(`[driver] nav ${BASE_URL}/giris`);
await page.goto(`${BASE_URL}/giris`, { waitUntil: 'networkidle' });
await page.waitForSelector('text=Giriş yap');
await page.screenshot({ path: path.join(outDir, '01-login.png') });
console.log(`[driver] screenshot -> ${path.join(outDir, '01-login.png')}`);

const HS_USER = process.env.HS_USER;
const HS_PASS = process.env.HS_PASS;
if (!HS_USER || !HS_PASS) {
  console.error('[driver] HS_USER ve HS_PASS ortam değişkenleri gerekli (örn. HS_USER=kullanici HS_PASS=sifre node driver.mjs)');
  process.exit(1);
}
console.log('[driver] fill credentials + submit');
await page.fill('input[autocomplete="username"]', HS_USER);
await page.fill('input[autocomplete="current-password"]', HS_PASS);
await page.click('button:has-text("Giriş yap")');
await page.waitForTimeout(1500);
await page.screenshot({ path: path.join(outDir, '02-after-submit.png') });
console.log(`[driver] screenshot -> ${path.join(outDir, '02-after-submit.png')}`);

console.log('[driver] title:', await page.title());
console.log('[driver] url:', page.url());
console.log('[driver] console errors:', JSON.stringify(errors));

await browser.close();

if (errors.length) {
  console.error('[driver] page threw console/page errors (see above) — this is expected if the backend (port 8080) is not running.');
}
