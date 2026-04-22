package com.growthtrace.ai.adapter;

import java.time.Duration;

/** AI 供应商适配层。业务代码不直接使用本接口，统一走 AiService。 */
public interface AiProvider {

    /**
     * 发送 prompt 并返回原始文本响应。调用方需要自己解析 JSON。
     * @param prompt 完整拼接好的 prompt
     * @param timeout 超时时间
     */
    String chat(String prompt, Duration timeout);
}
