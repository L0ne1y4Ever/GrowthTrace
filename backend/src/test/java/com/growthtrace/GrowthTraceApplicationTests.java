package com.growthtrace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 上下文加载烟雾测试。用 H2 或内存 DB 启动时需额外配置；当前仅校验编译与 Bean 依赖图合法。
 * 若 MySQL 不可用，可以给 test profile 单独配置 datasource。
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:growthtrace;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "growthtrace.security.jwt.secret=growthtrace-test-secret-please-override-in-ci-env-32bytes",
        "growthtrace.ai.base-url=https://api.example.com/v1",
        "growthtrace.ai.api-key=test",
        "growthtrace.ai.model=test-model"
})
class GrowthTraceApplicationTests {

    @Test
    void contextLoads() {
        // 仅验证 Spring 容器能否装配成功。
    }
}
