import { chromium } from 'playwright'
import { spawn, execSync } from 'child_process'
import { connect } from 'net'

const PORT = 5175   // use a dedicated port so we own the server lifecycle
const BASE = `http://localhost:${PORT}`
const EQ   = 'Y=5.76*x1+7.20*x2+13.36*x3+68.16*x4-3.66*x1*x2+32.13*x1*x3+62.8*x1*x4+42.80*x2*x3+56.86*x2*x4+58.65*x3*x4'

function waitForPort(port, timeout = 30000) {
  return new Promise((resolve, reject) => {
    const deadline = Date.now() + timeout
    const attempt = () => {
      const sock = connect(port, '::1')
      sock.once('connect', () => { sock.destroy(); resolve() })
      sock.once('error', () => {
        sock.destroy()
        if (Date.now() < deadline) setTimeout(attempt, 200)
        else reject(new Error(`port ${port} never opened`))
      })
    }
    attempt()
  })
}

async function run() {
  // ── 0. Kill anything on our test port, start Vite with correct env ─────────
  console.log(`0. Starting Vite on port ${PORT}…`)
  try { execSync(`lsof -ti:${PORT} | xargs kill -9 2>/dev/null`, { stdio: 'ignore' }) } catch {}
  await new Promise(r => setTimeout(r, 300))

  const vite = spawn('npx', ['vite', '--port', String(PORT)], {
    cwd: new URL('.', import.meta.url).pathname,
    env: {
      ...process.env,
      VITE_BACKEND_URL: 'http://localhost:3000',
      // SAM local (Werkzeug) sends Transfer-Encoding + Content-Length together,
      // which Node.js ≥v19 strict HTTP parser rejects. The insecure parser
      // accepts this non-standard response so Vite's proxy can forward it.
      NODE_OPTIONS: '--insecure-http-parser',
    },
    stdio: ['ignore', 'pipe', 'pipe'],
  })
  vite.stdout.on('data', d => process.stdout.write(`[vite] ${d}`))
  vite.stderr.on('data', d => process.stderr.write(`[vite] ${d}`))

  await waitForPort(PORT)
  console.log('   Vite ready.\n')

  let browser
  try {
    browser = await chromium.launch({ headless: true })
    const ctx  = await browser.newContext()
    const page = await ctx.newPage()

    const proxyErrors = []
    page.on('console', msg => {
      if (msg.type() === 'error') proxyErrors.push(msg.text())
    })

    // ── 1. Load ─────────────────────────────────────────────────────────────
    console.log('1. Loading app…')
    await page.goto(BASE, { waitUntil: 'networkidle' })

    // ── 2. Equation ──────────────────────────────────────────────────────────
    console.log('2. Entering equation…')
    await page.locator('textarea').first().fill(EQ)
    await page.locator('button', { hasText: /add/i }).first().click()
    await page.waitForTimeout(1500)
    const freeLabel = await page.locator('text=/\\d+ free/').textContent().catch(() => '?')
    console.log(`   Variables: ${freeLabel}`)

    // ── 3. Fix x1 = 0 ────────────────────────────────────────────────────────
    console.log('3. Fixing x1…')
    await page.locator('input[type="checkbox"]').first().check()
    await page.waitForTimeout(300)

    // ── 4. Isoline single ───────────────────────────────────────────────────
    console.log('4. Selecting isoline single…')
    await page.locator('label', { hasText: /single isoline/i }).click()
    await page.waitForTimeout(300)

    // ── 5. Precision = 0 (coarse → max stroke-width + largest SVG response) ──
    console.log('5. Setting precision slider to 0…')
    await page.locator('input[type="range"]').first().fill('0')

    // ── 6. Render ────────────────────────────────────────────────────────────
    console.log('6. Rendering…')
    const errorsBefore = [...proxyErrors]
    const responseP = page.waitForResponse(
      r => r.url().includes('/api/render'), { timeout: 60000 })
    await page.locator('button', { hasText: /calculate/i }).click()
    const response = await responseP

    const status = response.status()
    console.log(`   /api/render → HTTP ${status}`)

    if (status !== 200) {
      const direct = await fetch('http://localhost:3000/api/render', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          equation: EQ, fixedVars: { 1: 0 }, mode: 'ISOLINE_SINGLE',
          isoValue: 50, isoFrom: 0, isoTo: 0, isoStep: 1,
          regionMin: 0, regionMax: 100, precision: 0, pixelStep: 1,
        }),
      })
      const directBody = await direct.text()
      console.error(`Direct API: HTTP ${direct.status} — ${directBody.slice(0, 400)}`)
      process.exit(1)
    }

    // ── 7. SVG appeared ──────────────────────────────────────────────────────
    console.log('7. Checking SVG…')
    await page.waitForSelector('svg polygon', { timeout: 10000 })
    const pathCount = await page.locator('svg path').count()
    console.log(`   SVG path elements: ${pathCount}`)
    if (pathCount < 1) { console.error('FAIL — no isoline paths'); process.exit(1) }

    // ── 8. No Transfer-Encoding proxy errors ─────────────────────────────────
    const proxyIssues = proxyErrors
      .filter(e => !errorsBefore.includes(e))
      .filter(e => /Transfer-Encoding|Parse Error|proxy error/i.test(e))
    if (proxyIssues.length) {
      console.error('FAIL — proxy error still present:', proxyIssues)
      process.exit(1)
    }
    console.log('   No proxy errors ✓')

    // ── 9. Stats bar ─────────────────────────────────────────────────────────
    const stats = await page.locator('span.mono').filter({ hasText: /Y.*\[/ })
      .first().innerText().catch(() => '')
    if (stats) console.log(`   Stats: ${stats}`)

    console.log('\n✓ All checks passed — proxy fix working, render succeeds')
  } finally {
    await browser?.close()
    vite.kill()
  }
}

run().catch(err => { console.error('FAIL:', err.message); process.exit(1) })
