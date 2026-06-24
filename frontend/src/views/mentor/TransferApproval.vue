<template>
  <ApprovalPage
    title="企业指导 — 单位变更审批"
    :pending-api="mentorPendingTransfers"
    :history-api="mentorHistoryTransfers"
    :approve-api="mentorApproveTransfer"
  />
</template>

<script setup>
import ApprovalPage from '@/views/transfer/_ApprovalPage.vue'
import { mentorPendingTransfers, mentorHistoryTransfers, mentorApproveTransfer } from '@/api/transfer'
</script>
