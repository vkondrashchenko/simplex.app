import React from 'react'

export interface VarState {
  name: string
  idx: number    // 1-based
  fixed: boolean
  value: number
}

interface Props {
  variables: VarState[]
  onChange: (vars: VarState[]) => void
}

export default function VariablePanel({ variables, onChange }: Props) {
  if (variables.length === 0) return null

  const update = (idx: number, patch: Partial<VarState>) =>
    onChange(variables.map((v, i) => i === idx ? { ...v, ...patch } : v))

  const freeCount = variables.filter(v => !v.fixed).length
  const valid = freeCount === 3

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div className="section-label" style={{ marginBottom: 0 }}>Variables</div>
        <span style={{
          fontSize: 10,
          fontFamily: 'var(--mono)',
          color: valid ? 'var(--accent)' : 'var(--error)',
          background: valid ? 'var(--accent-dim)' : 'var(--error-dim)',
          border: `1px solid ${valid ? 'rgba(34,211,238,0.2)' : 'rgba(248,113,113,0.2)'}`,
          borderRadius: 3,
          padding: '2px 6px',
          letterSpacing: '0.04em',
        }}>
          {freeCount} free
        </span>
      </div>

      {!valid && (
        <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>
          Fix {variables.length - 3} variable{variables.length - 3 !== 1 ? 's' : ''} to leave exactly 3 free for the simplex axes.
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
        {/* Header row */}
        <div style={{ display: 'grid', gridTemplateColumns: '36px 1fr 1fr', gap: 8, padding: '2px 0' }}>
          <span style={{ fontSize: 10, color: 'var(--text-dim)' }}></span>
          <span style={{ fontSize: 10, color: 'var(--text-dim)', letterSpacing: '0.06em' }}>FIX</span>
          <span style={{ fontSize: 10, color: 'var(--text-dim)', letterSpacing: '0.06em' }}>VALUE</span>
        </div>

        {variables.map((v, i) => (
          <div key={v.idx} style={{
            display: 'grid',
            gridTemplateColumns: '36px 1fr 1fr',
            gap: 8,
            alignItems: 'center',
            padding: '4px 6px',
            borderRadius: 4,
            background: v.fixed ? 'var(--surface-3)' : 'transparent',
            border: `1px solid ${v.fixed ? 'var(--border-mid)' : 'transparent'}`,
            transition: 'all 0.12s',
          }}>
            <span className="mono" style={{ fontSize: 12, color: v.fixed ? 'var(--text)' : 'var(--text-muted)' }}>
              {v.name}
            </span>
            <label style={{ margin: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <input
                type="checkbox"
                checked={v.fixed}
                onChange={e => update(i, { fixed: e.target.checked })}
              />
            </label>
            <input
              type="number"
              step="0.01"
              min="0"
              max="1"
              disabled={!v.fixed}
              value={v.value}
              onChange={e => update(i, { value: parseFloat(e.target.value) || 0 })}
              className="mono"
              style={{ width: '100%', opacity: v.fixed ? 1 : 0.3 }}
            />
          </div>
        ))}
      </div>
    </div>
  )
}
