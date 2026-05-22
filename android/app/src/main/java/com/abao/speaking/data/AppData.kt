package com.abao.speaking.data

import com.abao.speaking.model.Scenario

/** 数据与网页 script.js 中 scenarios / warmupWords / metrics 保持一致。 */
object AppData {
    val scenarios = listOf(
        Scenario(
            id = "public-speaking",
            title = "公开演讲",
            opening = "What is your greatest weakness? Please answer with a problem, an action, and a positive result.",
            zh = "你在面试中被问到最大的弱点。请结合自己的专业和未来工作岗位，选择一个真实且恰当的弱点，用 PAR 模型回答。",
            target = "I used to struggle with public speaking. To improve this, I joined the school's public speaking club. As a result, I became more confident and my last speech was great!",
            next = "Good. Can you give one work-related example of how this improvement will help you in the future?",
            hints = listOf("I used to struggle with...", "To improve this...", "As a result...")
        ),
        Scenario(
            id = "time-management",
            title = "时间管理",
            opening = "What is your greatest weakness? Try to tell a short story about how you overcame it.",
            zh = "请围绕时间管理回答：先说问题，再说改进方法，最后说积极结果。",
            target = "I used to struggle with time management. To improve this, I started to make a to-do list every day. As a result, I can now finish my tasks on time and feel less stressed.",
            next = "Nice answer. How will this habit help you with future work responsibilities?",
            hints = listOf("time management", "make a to-do list", "finish my tasks on time")
        ),
        Scenario(
            id = "team-discussion",
            title = "团队讨论",
            opening = "What is your greatest weakness in teamwork or communication? Use the PAR model to answer.",
            zh = "请围绕团队讨论回答：说出不足、改进行动和积极结果。",
            target = "I used to struggle with team discussions. To improve this, I started using AI talk partners to practice team discussions. As a result, I can share my suggestions in team discussions.",
            next = "Great. What suggestions would you share in a real team discussion?",
            hints = listOf("team discussions", "AI talk partners", "share my suggestions")
        )
    )

    val warmupWords = listOf(
        "weakness" to "弱点，不足",
        "interview" to "面试",
        "identify problem" to "找出问题",
        "overcome" to "克服",
        "positive result" to "积极结果",
        "work-related" to "与工作相关的",
        "skill" to "技能",
        "responsibility" to "职责",
        "position" to "岗位",
        "learn and grow" to "学习与成长",
        "give examples" to "举例",
        "tell a story" to "讲故事",
        "time management" to "时间管理",
        "public speaking" to "公开演讲",
        "team discussion" to "团队讨论",
        "a to-do list" to "任务清单",
        "AI talk partner" to "AI 对话伙伴",
        "stress" to "压力",
        "presentation" to "演讲",
        "confident" to "自信"
    )

    val metrics = listOf(
        "语句词汇搭配", "用词准确度", "句子结构", "发音", "语速", "流利度", "完整度"
    )
}
