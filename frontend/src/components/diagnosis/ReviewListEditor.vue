<template>
  <div class="space-y-2">
    <div class="flex items-center justify-between">
      <label class="text-sm text-slate-600">{{ label }}</label>
      <button type="button" class="text-xs text-brand-600 hover:underline" @click="$emit('add')">
        + 添加
      </button>
    </div>

    <div
      v-for="(_, i) in items"
      :key="`r-${i}`"
      class="flex items-center gap-2"
    >
      <input
        :value="items[i]"
        type="text"
        :maxlength="max"
        class="flex-1 px-3 py-1.5 border border-slate-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
        :placeholder="placeholder"
        @input="onInput(i, ($event.target as HTMLInputElement).value)"
      />
      <button
        type="button"
        class="text-xs text-slate-400 hover:text-red-600"
        @click="$emit('remove', i)"
      >
        删除
      </button>
    </div>
    <div v-if="items.length === 0" class="text-xs text-slate-400">（空）</div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{
  label: string
  items: string[]
  placeholder?: string
  max?: number
}>()

const emit = defineEmits<{
  add: []
  remove: [index: number]
  update: [index: number, value: string]
}>()

function onInput(i: number, v: string) {
  // 直接变更父组件传入的数组引用；Vue 3 reactive 数组项 mutate 会触发更新。
  props.items[i] = v
  emit('update', i, v)
}
</script>
