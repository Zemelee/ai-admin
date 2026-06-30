<template>
  <el-container class="admin-layout">
    <!-- 桌面端侧边栏 -->
    <el-aside v-if="!isMobile" :width="collapse ? '64px' : '220px'" class="aside">
      <div class="logo" :class="{ collapsed: collapse }">
        <span class="dot" />
        <span v-show="!collapse" class="title">实习管理</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="collapse"
        background-color="#001428"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="iconOf(m.icon)" /></el-icon>
          <template #title>{{ m.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 移动端抽屉菜单 -->
    <el-drawer
      v-if="isMobile"
      v-model="drawerVisible"
      :with-header="false"
      size="200px"
      direction="ltr"
      class="mobile-drawer"
    >
      <div class="drawer-menu">
        <div class="drawer-logo">
          <span class="dot" />
          <span class="title">实习管理</span>
        </div>
        <el-menu
          :default-active="route.path"
          background-color="#001428"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
          @select="drawerVisible = false"
        >
          <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
            <el-icon><component :is="iconOf(m.icon)" /></el-icon>
            <template #title>{{ m.title }}</template>
          </el-menu-item>
        </el-menu>
      </div>
    </el-drawer>

    <el-container>
      <el-header class="header">
        <div class="left">
          <!-- 移动端：汉堡菜单按钮；桌面端：折叠按钮 -->
          <el-icon v-if="isMobile" class="trigger" @click="drawerVisible = true">
            <Fold />
          </el-icon>
          <el-icon v-else class="trigger" @click="collapse = !collapse">
            <Fold v-if="!collapse" />
            <Expand v-else />
          </el-icon>
          <span class="crumb">{{ currentTitle }}</span>
        </div>
        <div class="right">
          <el-tag :type="roleTagType" effect="plain" class="role-tag">
            {{ user.info?.roleLabel }}
          </el-tag>
          <el-dropdown @command="onCommand">
            <span class="user">
              {{ user.info?.name || user.info?.username }}
              <el-icon><CaretBottom /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <AnnouncementBar />
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { MENUS_BY_ROLE } from '@/router/menus'
import {
  Fold,
  Expand,
  CaretBottom,
  DataAnalysis,
  User,
  UserFilled,
  Document,
  DocumentChecked,
  EditPen,
  OfficeBuilding,
  Avatar,
  ChatDotRound,
  Memo,
  Files,
  Warning,
  Message
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import AnnouncementBar from '@/components/AnnouncementBar.vue'

const ICONS = {
  DataAnalysis, User, UserFilled, Document, DocumentChecked,
  EditPen, OfficeBuilding, Avatar, ChatDotRound, Memo, Files, Warning, Message
}

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const collapse = ref(false)
const isMobile = ref(false)
const drawerVisible = ref(false)

const menus = computed(() => (user.role ? MENUS_BY_ROLE[user.role] ?? [] : []))
const currentTitle = computed(() => route.meta.title || '')

const roleTagType = computed(() => {
  switch (user.role) {
    case 'supervisor': return 'danger'
    case 'teacher': return 'warning'
    case 'mentor': return 'success'
    default: return 'primary'
  }
})

function iconOf(name) {
  return name ? ICONS[name] || Document : Document
}

function checkScreen() {
  isMobile.value = window.innerWidth < 768
}

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
    await user.logout()
    router.replace('/login')
  }
}

onMounted(() => {
  checkScreen()
  window.addEventListener('resize', checkScreen)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScreen)
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}
.aside {
  background: #001428;
  transition: width 0.2s;
  overflow-x: hidden;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  padding: 0 18px;
  border-bottom: 1px solid #002b50;
}
.logo .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #409eff;
  box-shadow: 0 0 6px #409eff;
  flex-shrink: 0;
}
.logo.collapsed { justify-content: center; padding: 0; }

.header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}
.header .left,
.header .right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.trigger {
  font-size: 20px;
  cursor: pointer;
}
.crumb {
  font-size: 15px;
  color: #303133;
}
.user {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.role-tag { margin-right: 4px; }
.main {
  background: #f5f7fa;
  padding: 16px;
}

/* 移动端抽屉样式 */
.mobile-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #001428;
}
.drawer-menu { display: flex; flex-direction: column; height: 100%; }
.drawer-logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  padding: 0 18px;
  border-bottom: 1px solid #002b50;
}
.drawer-menu :deep(.el-menu) { border-right: none; }

/* 小屏适配 */
@media (max-width: 768px) {
  .role-tag { display: none; }
  .crumb { font-size: 14px; }
  .header { padding: 0 12px; }
}
</style>