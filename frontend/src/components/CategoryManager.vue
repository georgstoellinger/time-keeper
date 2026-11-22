<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api/client'
import { extractError } from '../errors'
import type { Category } from '../types'

const categories = ref<Category[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const newName = ref('')
const newColor = ref('#4f46e5')

const editingId = ref<number | null>(null)
const editName = ref('')
const editColor = ref('#4f46e5')

async function load() {
  loading.value = true
  error.value = null
  try {
    categories.value = await api.getCategories()
  } catch (e) {
    error.value = extractError(e)
  } finally {
    loading.value = false
  }
}

async function create() {
  const name = newName.value.trim()
  if (!name) return
  error.value = null
  try {
    await api.createCategory({ name, color: newColor.value })
    newName.value = ''
    await load()
  } catch (e) {
    error.value = extractError(e)
  }
}

function startEdit(category: Category) {
  editingId.value = category.id
  editName.value = category.name
  editColor.value = category.color ?? '#4f46e5'
  error.value = null
}

function cancelEdit() {
  editingId.value = null
  error.value = null
}

async function saveEdit(category: Category) {
  const name = editName.value.trim()
  if (!name) return
  error.value = null
  try {
    await api.updateCategory(category.id, { name, color: editColor.value })
    editingId.value = null
    await load()
  } catch (e) {
    error.value = extractError(e)
  }
}

async function remove(category: Category) {
  if (!confirm(`Delete category "${category.name}"?`)) return
  error.value = null
  try {
    await api.deleteCategory(category.id)
    await load()
  } catch (e) {
    error.value = extractError(e)
  }
}

onMounted(load)
</script>

<template>
  <section class="category-manager">
    <h2>Categories</h2>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading" class="muted">Loading…</p>

    <div class="category-list">
      <div v-for="category in categories" :key="category.id" class="category-row">
        <template v-if="editingId === category.id">
          <input v-model="editName" type="text" class="name-input" />
          <input v-model="editColor" type="color" />
          <button class="primary" @click="saveEdit(category)">Save</button>
          <button class="ghost" @click="cancelEdit">Cancel</button>
        </template>
        <template v-else>
          <span
            class="cat-badge"
            :style="{ backgroundColor: category.color ?? '#64748b' }"
          >
            {{ category.name }}
          </span>
          <span class="spacer"></span>
          <button class="ghost" @click="startEdit(category)">Edit</button>
          <button class="ghost danger" @click="remove(category)">Delete</button>
        </template>
      </div>
    </div>

    <div class="category-new">
      <input
        v-model="newName"
        type="text"
        class="name-input"
        placeholder="New category name"
        @keyup.enter="create"
      />
      <input v-model="newColor" type="color" />
      <button class="primary" @click="create">Add category</button>
    </div>
  </section>
</template>
