import axios from 'axios'
import type {
  Category,
  CategoryPayload,
  Day,
  EntryPayload,
  Week,
} from '../types'

const http = axios.create({ baseURL: '/api' })

export const api = {
  getWeek: (date: string) =>
    http.get<Week>('/week', { params: { date } }).then((r) => r.data),

  getCategories: () =>
    http.get<Category[]>('/categories').then((r) => r.data),

  createCategory: (payload: CategoryPayload) =>
    http.post<Category>('/categories', payload).then((r) => r.data),

  updateCategory: (id: number, payload: CategoryPayload) =>
    http.put<Category>(`/categories/${id}`, payload).then((r) => r.data),

  deleteCategory: (id: number) =>
    http.delete(`/categories/${id}`).then(() => undefined),

  updateDay: (date: string, notes: string | null) =>
    http.put<Day>(`/days/${date}`, { notes }).then((r) => r.data),

  addEntry: (date: string, payload: EntryPayload) =>
    http.post<Day>(`/days/${date}/entries`, payload).then((r) => r.data),

  updateEntry: (date: string, entryId: number, payload: EntryPayload) =>
    http
      .put<Day>(`/days/${date}/entries/${entryId}`, payload)
      .then((r) => r.data),

  deleteEntry: (date: string, entryId: number) =>
    http.delete<Day>(`/days/${date}/entries/${entryId}`).then((r) => r.data),
}
