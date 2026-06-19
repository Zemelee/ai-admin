<template>
  <ApprovalPage
    title="系主任 — 请假审批（>30 天）"
    :pending-api="supervisorPending"
    :history-api="supervisorHistory"
    :approve-api="supervisorApprove"
  />
</template>

<script setup>
import ApprovalPage from '@/views/leave/_ApprovalPage.vue'
import { supervisorPending, supervisorHistory, supervisorApprove } from '@/api/leave'
</script>
