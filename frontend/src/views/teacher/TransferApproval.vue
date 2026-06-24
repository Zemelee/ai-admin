<template>
  <ApprovalPage
    title="教师 — 单位变更审批"
    :pending-api="teacherPendingTransfers"
    :history-api="teacherHistoryTransfers"
    :approve-api="teacherApproveTransfer"
  />
</template>

<script setup>
import ApprovalPage from '@/views/transfer/_ApprovalPage.vue'
import { teacherPendingTransfers, teacherHistoryTransfers, teacherApproveTransfer } from '@/api/transfer'
</script>
