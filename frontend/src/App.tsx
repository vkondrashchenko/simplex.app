import React, { useState } from 'react'
import EquationPanel from './components/EquationPanel'
import VariablePanel, { VarState } from './components/VariablePanel'
import RenderPanel, { RenderSettings } from './components/RenderPanel'
import SimplexCanvas from './components/SimplexCanvas'
import { ParseResponse, apiRender } from './api/client'

const DEFAULT_SETTINGS: RenderSettings = {
  mode: 'ISOLINE_RANGE',
  regionMin: 0,
  regionMax: 100,
  isoValue: 50,
  isoFrom: 25,
  isoTo: 65,
  isoStep: 5,
  precision: 1,
  pixelStep: 1,
}

export default function App() {
  const [equation, setEquation] = useState('')
  const [variables, setVariables] = useState<VarState[]>([])
  const [settings, setSettings] = useState<RenderSettings>(DEFAULT_SETTINGS)
  const [svgContent, setSvgContent] = useState<string | null>(null)
  const [minY, setMinY] = useState<number | null>(null)
  const [maxY, setMaxY] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleParsed = (eq: string, info: ParseResponse) => {
    setEquation(eq)
    setVariables(info.variableNames.map((name, i) => ({
      name, idx: i + 1, fixed: false, value: 0,
    })))
    setSvgContent(null)
    setError(null)
  }

  const freeCount = variables.filter(v => !v.fixed).length
  const canRender = equation.length > 0 && freeCount === 3

  const handleRender = async () => {
    if (!canRender) return
    setLoading(true)
    setError(null)
    try {
      const fixedVars: Record<number, number> = {}
      variables.forEach(v => { if (v.fixed) fixedVars[v.idx] = v.value })
      const result = await apiRender({ equation, fixedVars, ...settings })
      setSvgContent(result.svgContent)
      setMinY(result.min)
      setMaxY(result.max)
    } catch (e: any) {
      setError(e.response?.data?.message ?? e.message ?? 'Render failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', height: '100%', overflow: 'hidden' }}>

      {/* ── Sidebar ───────────────────────────────── */}
      <aside style={{
        width: 310,
        flexShrink: 0,
        height: '100%',
        overflowY: 'auto',
        background: 'var(--surface)',
        borderRight: '1px solid var(--border)',
        display: 'flex',
        flexDirection: 'column',
        gap: 0,
      }}>
        {/* Logo */}
        <div style={{
          padding: '18px 20px 14px',
          borderBottom: '1px solid var(--border)',
          display: 'flex',
          alignItems: 'baseline',
          gap: 8,
        }}>
          <span style={{
            fontFamily: 'var(--mono)',
            fontWeight: 500,
            fontSize: 15,
            letterSpacing: '0.15em',
            color: 'var(--accent)',
          }}>
            SYMPLEX
          </span>
          <span style={{ fontSize: 10, color: 'var(--text-muted)', letterSpacing: '0.06em' }}>
            DIAGRAM BUILDER
          </span>
        </div>

        {/* Panels */}
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)' }}>
          <EquationPanel onParsed={handleParsed} />
        </div>
        <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border)', flex: variables.length ? undefined : 0 }}>
          <VariablePanel variables={variables} onChange={setVariables} />
        </div>
        <div style={{ padding: '16px 20px' }}>
          <RenderPanel
            settings={settings}
            onChange={setSettings}
            onRender={handleRender}
            loading={loading}
            canRender={canRender}
          />
        </div>
      </aside>

      {/* ── Canvas area ───────────────────────────── */}
      <main style={{
        flex: 1,
        height: '100%',
        overflow: 'hidden',
        display: 'flex',
        flexDirection: 'column',
        padding: '20px 24px 16px',
        gap: 12,
        background: 'var(--bg)',
      }}>
        <SimplexCanvas
          svgContent={svgContent}
          min={minY}
          max={maxY}
          error={error}
          loading={loading}
        />
      </main>
    </div>
  )
}
