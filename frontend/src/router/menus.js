/**
 * 按角色 code 返回菜单。
 * MVP：每个角色 1~2 项，后续阶段往里追加（请假、日志、周记、审批…）。
 */
export const MENUS_BY_ROLE = {
  supervisor: [
    { path: '/supervisor/overview', title: '全局总览', icon: 'DataAnalysis' },
    { path: '/supervisor/leaves', title: '请假审批', icon: 'DocumentChecked' }
  ],
  teacher: [
    { path: '/teacher/students', title: '我带的学生', icon: 'User' },
    { path: '/teacher/leaves', title: '请假审批', icon: 'DocumentChecked' }
  ],
  student: [
    { path: '/student/profile', title: '我的档案', icon: 'UserFilled' },
    { path: '/student/leaves', title: '我的请假', icon: 'Document' }
  ],
  mentor: [
    { path: '/mentor/students', title: '我负责的学生', icon: 'User' }
  ]
}

export function homePathOf(role) {
  return MENUS_BY_ROLE[role][0].path
}
