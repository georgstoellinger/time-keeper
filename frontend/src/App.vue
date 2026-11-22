<script setup lang="ts">
import { ref } from 'vue'
import WeekView from './components/WeekView.vue'
import CategoryManager from './components/CategoryManager.vue'

const tab = ref<'times' | 'categories'>('times')
</script>

<template>
  <div class="app">
    <header class="app-header">
      <h1>Time Keeper</h1>
      <nav class="tabs">
        <button :class="{ active: tab === 'times' }" @click="tab = 'times'">
          Times
        </button>
        <button
          :class="{ active: tab === 'categories' }"
          @click="tab = 'categories'"
        >
          Categories
        </button>
      </nav>
    </header>

    <main class="app-main">
      <WeekView v-if="tab === 'times'" />
      <CategoryManager v-else />
    </main>
  </div>
</template>
