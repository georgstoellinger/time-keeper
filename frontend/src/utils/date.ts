export function toIso(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function parseIso(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function addDays(iso: string, days: number): string {
  const date = parseIso(iso)
  date.setDate(date.getDate() + days)
  return toIso(date)
}

export function isToday(iso: string): boolean {
  return iso === toIso(new Date())
}

export function formatDay(iso: string): string {
  return parseIso(iso).toLocaleDateString(undefined, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  })
}

export function formatWeekRange(startIso: string, endIso: string): string {
  const start = parseIso(startIso)
  const end = parseIso(endIso)
  const startLabel = start.toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
  })
  const endLabel = end.toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
  return `${startLabel} – ${endLabel}`
}

export function trimTime(time: string): string {
  return time.length >= 5 ? time.slice(0, 5) : time
}

export function durationPreview(start: string, end: string): number | null {
  if (!start || !end) return null
  const [sh, sm] = start.split(':').map(Number)
  const [eh, em] = end.split(':').map(Number)
  const minutes = eh * 60 + em - (sh * 60 + sm)
  return minutes > 0 ? minutes : null
}
