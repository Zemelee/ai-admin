<template>
  <ScoreListPage title="全部学生实习成绩" :fetcher="allScores" />
</template>

<script setup>
import ScoreListPage from '@/views/score/ScoreListPage.vue'
import { allScores } from '@/api/score'
</script>
