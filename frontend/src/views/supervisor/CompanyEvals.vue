<template>
  <EvalListPage title="全部实习鉴定" :fetcher="supervisorEvals" />
</template>

<script setup>
import EvalListPage from '@/views/companyeval/EvalListPage.vue'
import { supervisorEvals } from '@/api/companyEval'
</script>
