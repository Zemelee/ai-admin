<template>
  <div class="image-uploader">
    <!-- 已上传图片缩略图（可放大预览） -->
    <div v-for="(img, idx) in modelValue" :key="img.id" class="thumb">
      <el-image
        :src="img.previewUrl"
        :preview-src-list="previewList"
        :initial-index="idx"
        fit="cover"
        class="thumb-img"
        preview-teleported
      />
      <div class="thumb-name" :title="img.fileName">{{ img.fileName }}</div>
      <el-icon v-if="!readonly" class="thumb-del" @click="onRemove(img, idx)"><Close /></el-icon>
    </div>

    <!-- 上传入口 -->
    <el-upload
      v-if="!readonly && modelValue.length < max"
      class="upload-trigger"
      :show-file-list="false"
      :accept="accept"
      :before-upload="beforeUpload"
      :http-request="customUpload"
      :disabled="uploading"
    >
      <div class="trigger-box" v-loading="uploading">
        <el-icon><Plus /></el-icon>
        <span class="trigger-tip">{{ uploading ? `${progress}%` : '上传图片' }}</span>
      </div>
    </el-upload>

    <div v-if="!readonly" class="uploader-hint">
      支持 jpg/png/gif/webp/bmp，单张 ≤10MB，最多 {{ max }} 张
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Close } from '@element-plus/icons-vue'
import { uploadImage, removeAttachment } from '@/api/upload'

const props = defineProps({
  // [{ id, fileName, previewUrl }]
  modelValue: { type: Array, default: () => [] },
  bizType: { type: String, required: true },
  bizId: { type: Number, default: null },
  max: { type: Number, default: 6 },
  accept: { type: String, default: 'image/*' },
  // 详情场景只读展示，不显示上传/删除
  readonly: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'change'])

const uploading = ref(false)
const progress = ref(0)

const ALLOWED = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']
const MAX_SIZE = 10 * 1024 * 1024

const previewList = computed(() => props.modelValue.map((i) => i.previewUrl))

function beforeUpload(file) {
  if (!ALLOWED.includes(file.type)) {
    ElMessage.error('仅支持 jpg/png/gif/webp/bmp 格式')
    return false
  }
  if (file.size > MAX_SIZE) {
    ElMessage.error('单张图片不能超过 10MB')
    return false
  }
  if (props.modelValue.length >= props.max) {
    ElMessage.warning(`最多上传 ${props.max} 张`)
    return false
  }
  return true
}

async function customUpload({ file }) {
  uploading.value = true
  progress.value = 0
  try {
    const data = await uploadImage(file, props.bizType, props.bizId, (p) => (progress.value = p))
    const next = [...props.modelValue, data]
    emit('update:modelValue', next)
    emit('change', next)
  } catch {
    /* http 拦截器已提示 */
  } finally {
    uploading.value = false
  }
}

async function onRemove(img, idx) {
  try {
    await removeAttachment(img.id)
  } catch {
    return
  }
  const next = props.modelValue.filter((_, i) => i !== idx)
  emit('update:modelValue', next)
  emit('change', next)
  ElMessage.success('已删除')
}
</script>

<style scoped>
.image-uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-start;
}
.thumb {
  position: relative;
  width: 96px;
}
.thumb-img {
  width: 96px;
  height: 96px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  display: block;
}
.thumb-name {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.thumb-del {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 18px;
  height: 18px;
  background: #f56c6c;
  color: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.upload-trigger :deep(.el-upload) { display: block; }
.trigger-box {
  width: 96px;
  height: 96px;
  border: 1px dashed #c0ccda;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8c939d;
  cursor: pointer;
  gap: 6px;
}
.trigger-box:hover { border-color: #409eff; color: #409eff; }
.trigger-tip { font-size: 12px; }
.uploader-hint {
  width: 100%;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}
</style>
