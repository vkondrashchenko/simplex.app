import React from 'react'
import { RenderMode } from '../api/client'

export interface RenderSettings {
  mode: RenderMode
  regionMin: number
  regionMax: number
  isoValue: number
  isoFrom: number
  isoTo: number
  isoStep: number
  precision: number
  pixelStep: number
}

interface Props {
  settings: RenderSettings
  onChange: (s: RenderSettings) => void
  onRender: () => void
  loading: boolean
  canRender: boolean
}

function NumField({
  label, value, onChange, step = 0.1, min, max, width = 80,
}: {
  label: string; value: number; onChange: (v: number) => void
  step?: number; min?: number; max?: number; width?: number
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <label>{label}</label>
      <input
        type="number"
        step={step}
        min={min}
        max={max}
        value={value}
        onChange={e => onChange(parseFloat(e.target.value) || 0)}
        style={{ width }}
        className="mono"
      />
    </div>
  )
}

const MODE_LABELS: Record<RenderMode, string> = {
  REGION: 'Region band',
  ISOLINE_SINGLE: 'Single isoline',
  ISOLINE_RANGE: 'Isoline range',
}

export default function RenderPanel({ settings: s, onChange, onRender, loading, canRender }: Props) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div className="section-label">Render settings</div>

      {/* Mode selector */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
        <label>Mode</label>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          {(Object.keys(MODE_LABELS) as RenderMode[]).map(m => (
            <label key={m} style={{
              display: 'flex', alignItems: 'center', gap: 8,
              padding: '5px 8px',
              borderRadius: 4,
              background: s.mode === m ? 'var(--accent-dim)' : 'transparent',
              border: `1px solid ${s.mode === m ? 'var(--accent)' : 'transparent'}`,
              cursor: 'pointer',
              margin: 0,
              color: s.mode === m ? 'var(--text)' : 'var(--text-muted)',
              fontSize: 12,
              transition: 'all 0.12s',
            }}>
              <input
                type="radio" name="mode" value={m}
                checked={s.mode === m}
                onChange={() => onChange({ ...s, mode: m })}
                style={{ display: 'none' }}
              />
              {MODE_LABELS[m]}
            </label>
          ))}
        </div>
      </div>

      {/* Mode-specific fields */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
        {s.mode === 'REGION' && (
          <>
            <NumField label="Min Y" value={s.regionMin} onChange={v => onChange({ ...s, regionMin: v })} />
            <NumField label="Max Y" value={s.regionMax} onChange={v => onChange({ ...s, regionMax: v })} />
          </>
        )}
        {s.mode === 'ISOLINE_SINGLE' && (
          <NumField label="Isoline value" value={s.isoValue} onChange={v => onChange({ ...s, isoValue: v })} />
        )}
        {s.mode === 'ISOLINE_RANGE' && (
          <>
            <NumField label="From" value={s.isoFrom} onChange={v => onChange({ ...s, isoFrom: v })} />
            <NumField label="To"   value={s.isoTo}   onChange={v => onChange({ ...s, isoTo: v })} />
            <NumField label="Step" value={s.isoStep} onChange={v => onChange({ ...s, isoStep: v })} />
          </>
        )}
      </div>

      {/* Precision slider — shown for isoline modes */}
      {s.mode !== 'REGION' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
            <label style={{ marginBottom: 0 }}>Precision</label>
            <span className="mono" style={{ fontSize: 12, color: 'var(--text)', fontWeight: 600 }}>
              {s.precision.toFixed(1)}
            </span>
          </div>
          <input
            type="range"
            min={0} max={3} step={0.1}
            value={s.precision}
            onChange={e => onChange({ ...s, precision: parseFloat(e.target.value) })}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, color: 'var(--text-muted)' }}>
            <span>coarse</span>
            <span>fine</span>
          </div>
        </div>
      )}

      {/* Pixel step slider */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
          <label style={{ marginBottom: 0 }}>Resolution</label>
          <span className="mono" style={{ fontSize: 12, color: 'var(--text)', fontWeight: 600 }}>
            {s.pixelStep === 1 ? 'full' : `1/${s.pixelStep}`}
          </span>
        </div>
        <input
          type="range"
          min={1} max={10} step={1}
          value={s.pixelStep}
          onChange={e => onChange({ ...s, pixelStep: parseInt(e.target.value) })}
        />
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, color: 'var(--text-muted)' }}>
          <span>full resolution</span>
          <span>fast preview</span>
        </div>
      </div>

      <button
        className="btn-primary"
        style={{ padding: '9px 0', fontSize: 13, letterSpacing: '0.05em', width: '100%' }}
        onClick={onRender}
        disabled={!canRender || loading}
      >
        {loading ? 'COMPUTING…' : 'CALCULATE & RENDER'}
      </button>
    </div>
  )
}
