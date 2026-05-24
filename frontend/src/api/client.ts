import axios from 'axios'

const BASE = import.meta.env.VITE_API_URL ?? '/api'

export type RenderMode = 'REGION' | 'ISOLINE_SINGLE' | 'ISOLINE_RANGE'

export interface ParseResponse {
  varCount: number
  monomialCount: number
  variableNames: string[]
}

export interface RenderRequest {
  equation: string
  fixedVars: Record<number, number>
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

export interface RenderResponse {
  svgContent: string
  min: number
  max: number
}

export const apiParse = (equation: string) =>
  axios.post<ParseResponse>(`${BASE}/parse`, { equation }).then(r => r.data)

export const apiRender = (req: RenderRequest) =>
  axios.post<RenderResponse>(`${BASE}/render`, req).then(r => r.data)
