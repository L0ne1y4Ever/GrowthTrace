<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-50 px-4">
    <div class="text-center">
      <div class="text-5xl font-semibold text-slate-300">404</div>
      <div class="mt-2 text-slate-600">页面不存在</div>
      <RouterLink to="/dashboard" class="inline-block mt-6 text-sm text-brand-600 hover:underline">
        返回成长总览
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
