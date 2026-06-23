import { req } from '@/utils/http'

/**
 * 政策问答
 * @param {string} question
 * @param {Array<{role:'user'|'assistant', content:string}>} history
 */
export const askPolicy = (question, history) =>
  req('post', '/policy/ask', { question, history: history || [] })

export const QUICK_QUESTIONS = [
  '实习单位变更需要满足什么条件？',
  '请假超过 1 个月怎么办？',
  '禁入企业类型有哪些？',
  '实习生最低薪酬标准是什么？',
  '夜班是否合规？',
  '总实习时长最少多少？'
]
