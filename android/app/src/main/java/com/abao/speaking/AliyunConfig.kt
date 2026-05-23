package com.abao.speaking

object AliyunConfig {
    /** 百炼（DashScope）API Key，格式 sk-...，见 https://help.aliyun.com/zh/isi/developer-reference/quick-start */
    const val DASHSCOPE_API_KEY = "sk-a8ddf367847541378cc1a25e12352457"

    /** 旧版「智能语音交互」NLS（可选，与百炼二选一即可） */
    const val NLS_APP_KEY = "nOBt4SZhSXTB7ynx"
    const val ACCESS_KEY_ID = "填写新的临时AccessKeyId"
    const val ACCESS_KEY_SECRET = "填写新的临时AccessKeySecret"
    const val NLS_TOKEN = "填写NLS临时Token"
    const val NLS_ASR_URL = "https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/asr"

    fun isDashScopeConfigured(): Boolean =
        DASHSCOPE_API_KEY.startsWith("sk-") && DASHSCOPE_API_KEY.length > 10

    fun isNlsConfigured(): Boolean =
        NLS_APP_KEY.isNotBlank() && !NLS_TOKEN.startsWith("填写")

    /** 已配置云端识别（百炼或 NLS） */
    fun isCloudAsrConfigured(): Boolean = isDashScopeConfigured() || isNlsConfigured()
}
