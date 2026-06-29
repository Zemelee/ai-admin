<template>
  <EvalListPage title="学生实习鉴定" :fetcher="teacherEvals" />
</template>

<script setup>
import EvalListPage from '@/views/companyeval/EvalListPage.vue'
import { teacherEvals } from '@/api/companyEval'
</script>
