<template>
  <ApprovalPage
    title="教师 — 请假审批"
    :pending-api="teacherPending"
    :history-api="teacherHistory"
    :approve-api="teacherApprove"
  />
</template>

<script setup>
import ApprovalPage from '@/views/leave/_ApprovalPage.vue'
import { teacherPending, teacherHistory, teacherApprove } from '@/api/leave'
</script>
