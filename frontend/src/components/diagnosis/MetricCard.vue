<template>
  <div class="border border-slate-200 rounded-md p-3">
    <div class="text-xs text-slate-500">{{ label }}</div>
    <div class="text-xl font-semibold text-slate-800 mt-1">{{ value }}</div>
    <div v-if="hint" class="text-xs text-slate-400 mt-0.5">{{ hint }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  hint?: string
}>()
</script>
