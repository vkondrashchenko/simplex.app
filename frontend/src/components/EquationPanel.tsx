import React, { useRef, useState } from 'react'
import { apiParse, ParseResponse } from '../api/client'

interface Props {
  onParsed: (equation: string, info: ParseResponse) => void
}

export default function EquationPanel({ onParsed }: Props) {
  const [equations, setEquations] = useState<string[]>([])
  const [selected, setSelected] = useState<number>(-1)
  const [input, setInput] = useState('')
  const [error, setError] = useState('')
  const fileRef = useRef<HTMLInputElement>(null)

  const handleAdd = async () => {
    const eq = input.trim()
    if (!eq) return
    setError('')
    try {
      const info = await apiParse(eq)
      const next = [...equations, eq]
      setEquations(next)
      const idx = next.length - 1
      setSelected(idx)
      onParsed(eq, info)
    } catch (e: any) {
      setError(e.response?.data?.message ?? e.message ?? 'Parse error')
    }
  }

  const handleRemove = () => {
    if (selected < 0) return
    const next = equations.filter((_, i) => i !== selected)
    setEquations(next)
    setSelected(next.length > 0 ? Math.min(selected, next.length - 1) : -1)
    setInput('')
  }

  const handleSelect = (idx: number) => {
    setSelected(idx)
    setInput(equations[idx])
    apiParse(equations[idx]).then(info => onParsed(equations[idx], info)).catch(() => {})
  }

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = ev => setInput((ev.target?.result as string).trim())
    reader.readAsText(file)
    e.target.value = ''
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div className="section-label">Equation</div>

      <textarea
        rows={3}
        className="mono"
        style={{ width: '100%', resize: 'vertical', fontSize: 11, lineHeight: 1.6 }}
        placeholder="Y = 5.76·x1 + 7.20·x2 + …"
        value={input}
        onChange={e => setInput(e.target.value)}
        onKeyDown={e => { if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) handleAdd() }}
      />

      {error && (
        <div style={{ color: 'var(--error)', fontSize: 11, padding: '4px 6px', background: 'var(--error-dim)', borderRadius: 3 }}>
          {error}
        </div>
      )}

      <div style={{ display: 'flex', gap: 5 }}>
        <button className="btn-primary" style={{ flex: 1 }} onClick={handleAdd}>Add</button>
        <button className="btn-secondary" onClick={() => fileRef.current?.click()}>Load .txt</button>
        <button className="btn-danger" onClick={handleRemove} disabled={selected < 0}>✕</button>
        <input ref={fileRef} type="file" accept=".txt" style={{ display: 'none' }} onChange={handleFile} />
      </div>

      {equations.length > 0 && (
        <select
          size={Math.min(equations.length, 4)}
          style={{ width: '100%', fontFamily: 'var(--mono)', fontSize: 11 }}
          value={selected}
          onChange={e => handleSelect(Number(e.target.value))}
        >
          {equations.map((eq, i) => (
            <option key={i} value={i}>
              {eq.length > 55 ? eq.slice(0, 52) + '…' : eq}
            </option>
          ))}
        </select>
      )}
    </div>
  )
}
