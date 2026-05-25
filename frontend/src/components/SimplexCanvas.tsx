import React from 'react'

interface Props {
  svgContent: string | null
  min: number | null
  max: number | null
  error: string | null
  loading?: boolean
}

export default function SimplexCanvas({ svgContent, min, max, error, loading }: Props) {
  const handleSave = () => {
    if (!svgContent) return
    const blob = new Blob([svgContent], { type: 'image/svg+xml' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'symplex.svg'
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', gap: 10 }}>

      {/* Error banner */}
      {error && (
        <div style={{
          background: 'var(--error-dim)',
          border: '1px solid rgba(248,113,113,0.3)',
          borderRadius: 6,
          padding: '8px 12px',
          color: 'var(--error)',
          fontSize: 12,
          flexShrink: 0,
        }}>
          {error}
        </div>
      )}

      {/* SVG area — fills remaining space */}
      <div style={{
        flex: 1,
        minHeight: 0,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        position: 'relative',
      }}>
        {loading && (
          <div style={{
            position: 'absolute', inset: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            background: 'rgba(237, 237, 234, 0.82)',
            backdropFilter: 'blur(2px)',
            borderRadius: 8,
            zIndex: 2,
          }}>
            <span style={{ color: 'var(--text-muted)', fontSize: 12, letterSpacing: '0.08em', fontWeight: 600 }}>
              COMPUTING…
            </span>
          </div>
        )}

        {svgContent ? (
          <div
            style={{
              width: '100%',
              height: '100%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 12,
              boxShadow: '0 4px 24px rgba(0,0,0,0.07)',
              overflow: 'hidden',
            }}
            dangerouslySetInnerHTML={{ __html: svgContent }}
          />
        ) : (
          !error && (
            <div style={{
              width: '100%',
              aspectRatio: '1 / 1',
              maxWidth: '100%',
              maxHeight: '100%',
              background: 'var(--surface)',
              border: '1px dashed var(--border-mid)',
              borderRadius: 12,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 10,
              color: 'var(--text-dim)',
            }}>
              <svg width="32" height="32" viewBox="0 0 32 32" fill="none" stroke="currentColor" strokeWidth="1.2">
                <polygon points="16,4 1,28 31,28" />
              </svg>
              <span style={{ fontSize: 11, letterSpacing: '0.08em', fontWeight: 600 }}>DIAGRAM WILL APPEAR HERE</span>
            </div>
          )
        )}
      </div>

      {/* Stats bar */}
      {svgContent && min !== null && max !== null && (
        <div style={{
          flexShrink: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '6px 10px',
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 5,
        }}>
          <span className="mono" style={{ fontSize: 11, color: 'var(--text-muted)' }}>
            Y&nbsp;∈&nbsp;[<span style={{ color: 'var(--text)' }}>{min.toFixed(4)}</span>,&nbsp;<span style={{ color: 'var(--text)' }}>{max.toFixed(4)}</span>]
          </span>
          <button className="btn-secondary" style={{ padding: '3px 10px', fontSize: 11 }} onClick={handleSave}>
            Save SVG
          </button>
        </div>
      )}
    </div>
  )
}
