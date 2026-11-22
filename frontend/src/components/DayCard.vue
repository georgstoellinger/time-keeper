<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { api } from '../api/client'
import { extractError } from '../errors'
import type { Category, Day, TimeEntry } from '../types'
import {
  durationPreview,
  formatDay,
  isToday,
  normalizeTime,
  trimTime,
} from '../utils/date'
import TimeInput from './TimeInput.vue'

const props = defineProps<{ day: Day; categories: Category[] }>()
const emit = defineEmits<{ (e: 'changed'): void }>()

const form = reactive<{
  start: string
  end: string
  categoryId: number | null
  notes: string
}>({
  start: '',
  end: '',
  categoryId: null,
  notes: '',
})

const editingId = ref<number | null>(null)
const saving = ref(false)
const error = ref<string | null>(null)
const dayNotes = ref('')

watch(
  () => props.day.notes,
  (notes) => {
    dayNotes.value = notes ?? ''
  },
  { immediate: true },
)

watch(
  () => props.categories,
  (categories) => {
    if (form.categoryId == null && categories.length > 0) {
      form.categoryId = categories[0].id
    }
  },
  { immediate: true, deep: true },
)

const previewMinutes = computed(() =>
  durationPreview(normalizeTime(form.start), normalizeTime(form.end)),
)

function resetForm() {
  editingId.value = null
  form.start = ''
  form.end = ''
  form.notes = ''
  error.value = null
}

function startEdit(entry: TimeEntry) {
  editingId.value = entry.id
  form.start = trimTime(entry.startTime)
  form.end = trimTime(entry.endTime)
  form.categoryId = entry.categoryId
  form.notes = entry.notes ?? ''
  error.value = null
}

async function submit() {
  const startTime = normalizeTime(form.start)
  const endTime = normalizeTime(form.end)

  if (!startTime || !endTime || form.categoryId == null) {
    error.value = 'Please enter valid times (HH:mm) and a category.'
    return
  }

  form.start = startTime
  form.end = endTime

  saving.value = true
  error.value = null
  try {
    const payload = {
      startTime,
      endTime,
      categoryId: form.categoryId,
      notes: form.notes.trim() ? form.notes.trim() : null,
    }
    if (editingId.value != null) {
      await api.updateEntry(props.day.date, editingId.value, payload)
    } else {
      await api.addEntry(props.day.date, payload)
    }
    resetForm()
    emit('changed')
  } catch (e) {
    error.value = extractError(e)
  } finally {
    saving.value = false
  }
}

async function remove(entry: TimeEntry) {
  const label = `${trimTime(entry.startTime)}–${trimTime(entry.endTime)}`
  if (!confirm(`Delete entry ${label}?`)) return
  error.value = null
  try {
    await api.deleteEntry(props.day.date, entry.id)
    if (editingId.value === entry.id) resetForm()
    emit('changed')
  } catch (e) {
    error.value = extractError(e)
  }
}

async function saveNotes() {
  const value = dayNotes.value.trim()
  const current = props.day.notes ?? ''
  if (value === current) return
  try {
    await api.updateDay(props.day.date, value ? value : null)
    emit('changed')
  } catch (e) {
    error.value = extractError(e)
  }
}
</script>

<template>
  <article class="day-card" :class="{ today: isToday(day.date) }">
    <header class="day-head">
      <span class="day-label">{{ formatDay(day.date) }}</span>
      <span class="day-total">{{ day.totalHours.toFixed(2) }} h</span>
    </header>

    <div class="entries">
      <div v-if="day.entries.length === 0" class="muted empty">
        No entries yet
      </div>

      <div v-for="entry in day.entries" :key="entry.id" class="entry-row">
        <span class="entry-time">
          {{ trimTime(entry.startTime) }}–{{ trimTime(entry.endTime) }}
        </span>
        <span class="entry-duration">{{ entry.durationHours.toFixed(2) }} h</span>
        <span
          class="cat-badge"
          :style="{ backgroundColor: entry.categoryColor ?? '#64748b' }"
        >
          {{ entry.categoryName }}
        </span>
        <span class="entry-notes">{{ entry.notes }}</span>
        <span class="entry-actions">
          <button class="ghost" @click="startEdit(entry)">Edit</button>
          <button class="ghost danger" @click="remove(entry)">Delete</button>
        </span>
      </div>
    </div>

    <div class="entry-form">
      <TimeInput v-model="form.start" label="Start time" />
      <TimeInput v-model="form.end" label="End time" />
      <select v-model="form.categoryId" aria-label="Category">
        <option v-for="c in categories" :key="c.id" :value="c.id">
          {{ c.name }}
        </option>
      </select>
      <input
        v-model="form.notes"
        type="text"
        class="notes-input"
        placeholder="Notes / ticket"
        aria-label="Notes"
      />
      <span v-if="previewMinutes != null" class="preview">
        {{ (previewMinutes / 60).toFixed(2) }} h
      </span>
      <button class="primary" :disabled="saving" @click="submit">
        {{ editingId == null ? 'Add' : 'Save' }}
      </button>
      <button v-if="editingId != null" class="ghost" @click="resetForm">
        Cancel
      </button>
    </div>

    <textarea
      v-model="dayNotes"
      class="day-notes"
      rows="2"
      placeholder="Day notes…"
      @blur="saveNotes"
    ></textarea>

    <p v-if="error" class="error">{{ error }}</p>
  </article>
</template>
