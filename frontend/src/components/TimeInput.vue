<script setup lang="ts">
import { normalizeTime } from '../utils/date'

const props = defineProps<{ modelValue: string; label: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

function onInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}

function onBlur() {
  const normalized = normalizeTime(props.modelValue)
  if (normalized !== props.modelValue) {
    emit('update:modelValue', normalized)
  }
}
</script>

<template>
  <input
    type="text"
    class="time-input"
    inputmode="numeric"
    maxlength="5"
    placeholder="HH:mm"
    :aria-label="label"
    :value="modelValue"
    @input="onInput"
    @blur="onBlur"
  />
</template>
