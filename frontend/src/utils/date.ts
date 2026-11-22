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

export function normalizeTime(raw: string): string {
  const value = raw.trim()
  if (!value) return ''

  const withColon = value.replace(/[.,]/g, ':')
  if (withColon.includes(':')) {
    const [hoursPart, minutesPart] = withColon.split(':')
    if (!hoursPart || minutesPart == null || minutesPart === '') return ''
    if (hoursPart.length > 2 || minutesPart.length > 2) return ''
    const hours = hoursPart.padStart(2, '0')
    const minutes = minutesPart.padStart(2, '0')
    if (!/^\d{2}$/.test(hours) || !/^\d{2}$/.test(minutes)) return ''
    if (Number(hours) > 23 || Number(minutes) > 59) return ''
    return `${hours}:${minutes}`
  }

  const digits = withColon.replace(/\D/g, '')
  if (!digits) return ''
  if (digits.length <= 2) {
    const hours = Number(digits)
    if (hours > 23) return ''
    return `${String(hours).padStart(2, '0')}:00`
  }
  if (digits.length <= 4) {
    const hours = Number(digits.slice(0, digits.length - 2))
    const minutes = Number(digits.slice(-2))
    if (hours > 23 || minutes > 59) return ''
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
  }
  return ''
}

export function durationPreview(start: string, end: string): number | null {
  if (!start || !end) return null
  const [sh, sm] = start.split(':').map(Number)
  const [eh, em] = end.split(':').map(Number)
  const minutes = eh * 60 + em - (sh * 60 + sm)
  return minutes > 0 ? minutes : null
}
