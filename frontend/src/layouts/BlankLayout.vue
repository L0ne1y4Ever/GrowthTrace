<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-50 px-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <div class="text-2xl font-semibold text-slate-800">GrowthTrace</div>
        <div class="text-sm text-slate-500 mt-1">计算机专业学生成长跟踪与阶段诊断平台</div>
      </div>
      <RouterView />
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>
