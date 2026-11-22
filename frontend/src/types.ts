export interface Category {
  id: number
  name: string
  color: string | null
}

export interface CategoryPayload {
  name: string
  color?: string | null
}

export interface TimeEntry {
  id: number
  startTime: string
  endTime: string
  durationMinutes: number
  durationHours: number
  categoryId: number
  categoryName: string
  categoryColor: string | null
  notes: string | null
}

export interface Day {
  id: number | null
  date: string
  notes: string | null
  totalMinutes: number
  totalHours: number
  entries: TimeEntry[]
}

export interface Week {
  weekStart: string
  weekEnd: string
  totalMinutes: number
  totalHours: number
  days: Day[]
}

export interface EntryPayload {
  startTime: string
  endTime: string
  categoryId: number
  notes?: string | null
}
