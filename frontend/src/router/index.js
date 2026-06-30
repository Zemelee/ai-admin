import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { homePathOf } from './menus'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dispatch',
    children: [
      {
        path: 'dispatch',
        name: 'dispatch',
        component: () => import('@/views/Dispatch.vue'),
        meta: { title: '加载中' }
      },
      // supervisor
      {
        path: 'supervisor/overview',
        name: 'supervisor-overview',
        component: () => import('@/views/supervisor/Overview.vue'),
        meta: { roles: ['supervisor'], title: '全局总览' }
      },
      {
        path: 'supervisor/warnings',
        name: 'supervisor-warnings',
        component: () => import('@/views/supervisor/Warnings.vue'),
        meta: { roles: ['supervisor'], title: '三色预警' }
      },
      {
        path: 'admin/students',
        name: 'admin-students',
        component: () => import('@/views/admin/Students.vue'),
        meta: { roles: ['supervisor'], title: '学生档案' }
      },
      {
        path: 'admin/companies',
        name: 'admin-companies',
        component: () => import('@/views/admin/Companies.vue'),
        meta: { roles: ['supervisor'], title: '企业档案' }
      },
      {
        path: 'admin/teachers',
        name: 'admin-teachers',
        component: () => import('@/views/admin/Teachers.vue'),
        meta: { roles: ['supervisor'], title: '教师档案' }
      },
      {
        path: 'admin/mentors',
        name: 'admin-mentors',
        component: () => import('@/views/admin/Mentors.vue'),
        meta: { roles: ['supervisor'], title: '企业指导档案' }
      },
      // teacher
      {
        path: 'teacher/overview',
        name: 'teacher-overview',
        component: () => import('@/views/teacher/TeacherOverview.vue'),
        meta: { roles: ['teacher'], title: '工作台' }
      },
      {
        path: 'teacher/students',
        name: 'teacher-students',
        component: () => import('@/views/teacher/MyStudents.vue'),
        meta: { roles: ['teacher'], title: '我带的学生' }
      },
      {
        path: 'teacher/weekly-reports',
        name: 'weekly-reports',
        component: () => import('@/views/teacher/WeeklyReports.vue'),
        meta: { roles: ['teacher'], title: '学生周记' }
      },
      {
        path: 'teacher/intern-reports',
        name: 'teacher-intern-reports',
        component: () => import('@/views/teacher/InternReports.vue'),
        meta: { roles: ['teacher'], title: '实习报告评分' }
      },
      {
        path: 'teacher/student-scores',
        name: 'teacher-student-scores',
        component: () => import('@/views/teacher/StudentScores.vue'),
        meta: { roles: ['teacher'], title: '成绩汇总' }
      },
      {
        path: 'teacher/company-evals',
        name: 'teacher-company-evals',
        component: () => import('@/views/teacher/CompanyEvals.vue'),
        meta: { roles: ['teacher'], title: '企业鉴定' }
      },
      // student
      {
        path: 'student/profile',
        name: 'student-profile',
        component: () => import('@/views/student/MyProfile.vue'),
        meta: { roles: ['student'], title: '我的档案' }
      },
      {
        path: 'student/leaves',
        name: 'student-leaves',
        component: () => import('@/views/student/MyLeaves.vue'),
        meta: { roles: ['student'], title: '我的请假' }
      },
      {
        path: 'student/internship-logs',
        name: 'student-internship-logs',
        component: () => import('@/views/student/InternshipLogs.vue'),
        meta: { roles: ['student'], title: '实习日志' }
      },
      {
        path: 'student/weekly-reports',
        name: 'student-weekly-reports',
        component: () => import('@/views/student/WeeklyReports.vue'),
        meta: { roles: ['student'], title: '实习周记' }
      },
      {
        path: 'student/intern-reports',
        name: 'student-intern-reports',
        component: () => import('@/views/student/InternReports.vue'),
        meta: { roles: ['student'], title: '实习报告' }
      },
      {
        path: 'student/my-score',
        name: 'student-my-score',
        component: () => import('@/views/student/MyScore.vue'),
        meta: { roles: ['student'], title: '我的成绩' }
      },
      {
        path: 'student/my-appraisal',
        name: 'student-my-appraisal',
        component: () => import('@/views/student/MyAppraisal.vue'),
        meta: { roles: ['student'], title: '我的实习鉴定' }
      },
      {
        path: 'student/transfers',
        name: 'student-transfers',
        component: () => import('@/views/student/MyTransfers.vue'),
        meta: { roles: ['student'], title: '我的单位变更' }
      },
      {
        path: 'teacher/leaves',
        name: 'teacher-leaves',
        component: () => import('@/views/teacher/LeaveApproval.vue'),
        meta: { roles: ['teacher'], title: '请假审批' }
      },
      {
        path: 'teacher/internship-logs',
        name: 'teacher-internship-logs',
        component: () => import('@/views/teacher/InternshipLogs.vue'),
        meta: { roles: ['teacher'], title: '学生日志' }
      },
      {
        path: 'supervisor/leaves',
        name: 'supervisor-leaves',
        component: () => import('@/views/supervisor/LeaveApproval.vue'),
        meta: { roles: ['supervisor'], title: '请假审批' }
      },
      {
        path: 'leave/:id',
        name: 'leave-detail',
        component: () => import('@/views/leave/LeaveDetail.vue'),
        meta: { roles: ['student', 'teacher', 'supervisor'], title: '请假详情' }
      },
      // mentor
      {
        path: 'mentor/students',
        name: 'mentor-students',
        component: () => import('@/views/mentor/MyStudents.vue'),
        meta: { roles: ['mentor'], title: '我负责的学生' }
      },
      {
        path: 'mentor/internship-logs',
        name: 'mentor-internship-logs',
        component: () => import('@/views/mentor/InternshipLogs.vue'),
        meta: { roles: ['mentor'], title: '日志确认' }
      },
      {
        path: 'mentor/transfers',
        name: 'mentor-transfers',
        component: () => import('@/views/mentor/TransferApproval.vue'),
        meta: { roles: ['mentor'], title: '单位变更审批' }
      },
      {
        path: 'mentor/company-evals',
        name: 'mentor-company-evals',
        component: () => import('@/views/mentor/CompanyEvals.vue'),
        meta: { roles: ['mentor'], title: '实习鉴定' }
      },
      {
        path: 'teacher/transfers',
        name: 'teacher-transfers',
        component: () => import('@/views/teacher/TransferApproval.vue'),
        meta: { roles: ['teacher'], title: '单位变更审批' }
      },
      {
        path: 'supervisor/transfers',
        name: 'supervisor-transfers',
        component: () => import('@/views/supervisor/TransferApproval.vue'),
        meta: { roles: ['supervisor'], title: '单位变更审批' }
      },
      {
        path: 'supervisor/company-evals',
        name: 'supervisor-company-evals',
        component: () => import('@/views/supervisor/CompanyEvals.vue'),
        meta: { roles: ['supervisor'], title: '企业鉴定' }
      },
      {
        path: 'supervisor/student-scores',
        name: 'supervisor-student-scores',
        component: () => import('@/views/supervisor/AllScores.vue'),
        meta: { roles: ['supervisor'], title: '企业鉴定' }
      },
      {
        path: 'admin/announcements',
        name: 'admin-announcements',
        component: () => import('@/views/admin/Announcements.vue'),
        meta: { roles: ['supervisor'], title: '系统公告' }
      },
      {
        path: 'transfer/:id',
        name: 'transfer-detail',
        component: () => import('@/views/transfer/TransferDetail.vue'),
        meta: { roles: ['student', 'mentor', 'teacher', 'supervisor'], title: '变更详情' }
      },
      // 合规知识库 AI 问答（全角色）
      {
        path: 'policy/chat',
        name: 'policy-chat',
        component: () => import('@/views/policy/PolicyChat.vue'),
        meta: { roles: ['student', 'teacher', 'mentor', 'supervisor'], title: '合规知识库' }
      }
    ]
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/Forbidden.vue'),
    meta: { public: true, title: '无权访问' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dispatch'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async (to) => {
  const user = useUserStore()
  const titleSuffix = '实习管理系统'
  document.title = to.meta.title ? `${to.meta.title} - ${titleSuffix}` : titleSuffix

  // 公共页直接放行
  if (to.meta.public) return true

  // 未登录 -> 登录页
  if (!user.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录但未拉到用户信息 -> 拉一次
  if (!user.info) {
    try {
      await user.fetchMe()
    } catch {
      return { path: '/login' }
    }
  }

  // dispatch：按角色跳首页
  if (to.path === '/dispatch') {
    if (user.role) return { path: homePathOf(user.role), replace: true }
    return { path: '/login' }
  }

  // 角色校验
  const allow = to.meta.roles
  if (allow && allow.length > 0 && user.role && !allow.includes(user.role)) {
    console.warn('[Router] 阻止访问: path=' + to.path + ', user.role=' + user.role + ', allow=' + JSON.stringify(allow))
    return { path: '/403' }
  }
  return true
})

export default router
