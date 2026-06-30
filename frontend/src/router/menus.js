/**
 * 按角色 code 返回菜单。
 * MVP：每个角色 1~2 项，后续阶段往里追加（请假、日志、周记、审批…）。
 */
export const MENUS_BY_ROLE = {
  supervisor: [
    { path: '/supervisor/overview', title: '全局总览', icon: 'DataAnalysis' },
    { path: '/supervisor/warnings', title: '三色预警', icon: 'Warning' },
    { path: '/admin/students', title: '学生档案', icon: 'User' },
    { path: '/admin/teachers', title: '教师档案', icon: 'UserFilled' },
    { path: '/admin/mentors', title: '企业指导', icon: 'Avatar' },
    { path: '/admin/companies', title: '企业档案', icon: 'OfficeBuilding' },
    { path: '/supervisor/leaves', title: '请假审批', icon: 'DocumentChecked' },
    { path: '/supervisor/transfers', title: '单位变更审批', icon: 'Files' },
    { path: '/supervisor/company-evals', title: '企业鉴定', icon: 'DocumentChecked' },
    { path: '/supervisor/student-scores', title: '成绩汇总', icon: 'DataAnalysis' },
    { path: '/admin/announcements', title: '系统公告', icon: 'Message' },
    { path: '/policy/chat', title: '合规知识库', icon: 'ChatDotRound' }
  ],
  teacher: [
    { path: '/teacher/overview', title: '工作台', icon: 'Monitor' },
    { path: '/teacher/students', title: '我带的学生', icon: 'User' },
    { path: '/teacher/leaves', title: '请假审批', icon: 'DocumentChecked' },
    { path: '/teacher/internship-logs', title: '学生日志', icon: 'EditPen' },
    { path: '/teacher/weekly-reports', title: '周记评分', icon: 'Memo' },
    { path: '/teacher/intern-reports', title: '实习报告评分', icon: 'Document' },
    { path: '/teacher/transfers', title: '单位变更审批', icon: 'Files' },
    { path: '/teacher/company-evals', title: '企业鉴定', icon: 'DocumentChecked' },
    { path: '/teacher/student-scores', title: '成绩汇总', icon: 'DataAnalysis' },
    { path: '/policy/chat', title: '合规知识库', icon: 'ChatDotRound' }
  ],
  student: [
    { path: '/student/profile', title: '我的档案', icon: 'UserFilled' },
    { path: '/student/leaves', title: '我的请假', icon: 'Document' },
    { path: '/student/internship-logs', title: '实习日志', icon: 'EditPen' },
    { path: '/student/weekly-reports', title: '实习周记', icon: 'Memo' },
    { path: '/student/intern-reports', title: '实习报告', icon: 'Files' },
    { path: '/student/my-appraisal', title: '我的鉴定', icon: 'DocumentChecked' },
    { path: '/student/my-score', title: '我的成绩', icon: 'DataAnalysis' },
    { path: '/student/transfers', title: '单位变更', icon: 'Files' },
    { path: '/policy/chat', title: '合规知识库', icon: 'ChatDotRound' }
  ],
  mentor: [
    { path: '/mentor/students', title: '我负责的学生', icon: 'User' },
    { path: '/mentor/internship-logs', title: '日志确认', icon: 'EditPen' },
    { path: '/mentor/transfers', title: '单位变更审批', icon: 'Files' },
    { path: '/mentor/company-evals', title: '实习鉴定', icon: 'DocumentChecked' },
    { path: '/policy/chat', title: '合规知识库', icon: 'ChatDotRound' }
  ]
}

export function homePathOf(role) {
  return MENUS_BY_ROLE[role][0].path
}
