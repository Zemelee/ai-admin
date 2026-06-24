<template>
  <ApprovalPage
    title="监管者 — 单位变更审批（终审）"
    :pending-api="supervisorPendingTransfers"
    :history-api="supervisorHistoryTransfers"
    :approve-api="supervisorApproveTransfer"
  />
</template>

<script setup>
import ApprovalPage from '@/views/transfer/_ApprovalPage.vue'
import { supervisorPendingTransfers, supervisorHistoryTransfers, supervisorApproveTransfer } from '@/api/transfer'
</script>
