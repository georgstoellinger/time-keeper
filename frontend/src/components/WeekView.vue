<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../api/client'
import { extractError } from '../errors'
import type { Category, Week } from '../types'
import { addDays, formatWeekRange, toIso } from '../utils/date'
import DayCard from './DayCard.vue'

const anchor = ref(toIso(new Date()))
const week = ref<Week | null>(null)
const categories = ref<Category[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    const [loadedWeek, loadedCategories] = await Promise.all([
      api.getWeek(anchor.value),
      api.getCategories(),
    ])
    week.value = loadedWeek
    categories.value = loadedCategories
    anchor.value = loadedWeek.weekStart
  } catch (e) {
    error.value = extractError(e)
  } finally {
    loading.value = false
  }
}

function shift(days: number) {
  anchor.value = addDays(anchor.value, days)
  void load()
}

function thisWeek() {
  anchor.value = toIso(new Date())
  void load()
}

const rangeLabel = computed(() =>
  week.value ? formatWeekRange(week.value.weekStart, week.value.weekEnd) : '',
)

onMounted(load)
</script>

<template>
  <section class="week-view">
    <div class="week-toolbar">
      <div class="week-nav">
        <button @click="shift(-7)">Prev</button>
        <button @click="thisWeek">This week</button>
        <button @click="shift(7)">Next</button>
      </div>

      <div class="week-summary" v-if="week">
        <span class="week-range">{{ rangeLabel }}</span>
        <span class="week-total">
          {{ week.totalHours.toFixed(2) }} h
          <span class="muted">/ week</span>
        </span>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="loading && !week" class="muted">Loading…</div>

    <div v-if="week" class="days">
      <DayCard
        v-for="day in week.days"
        :key="day.date"
        :day="day"
        :categories="categories"
        @changed="load"
      />
    </div>
  </section>
</template>
