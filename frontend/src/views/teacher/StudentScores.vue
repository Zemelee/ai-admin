<template>
  <ScoreListPage title="分管学生实习成绩" :fetcher="teacherScores" />
</template>

<script setup>
import ScoreListPage from '@/views/score/ScoreListPage.vue'
import { teacherScores } from '@/api/score'
</script>
