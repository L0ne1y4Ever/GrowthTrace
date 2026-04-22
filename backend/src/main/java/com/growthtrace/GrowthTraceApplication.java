package com.growthtrace;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * MapperScan 说明：
 *   @MapperScan.basePackages 不支持 Ant 通配（"**" 会被当作字面包名），所以既不能写成
 *   "com.growthtrace.**.mapper"，也不应逐个模块列举（新增模块易漏）。
 *   这里统一从 com.growthtrace 根包递归扫描，用 annotationClass = Mapper.class
 *   过滤出带 @Mapper 的接口，等效于"扫整棵树里所有 @Mapper 接口"。
 */
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.growthtrace")
@MapperScan(basePackages = "com.growthtrace", annotationClass = Mapper.class)
public class GrowthTraceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrowthTraceApplication.class, args);
    }
}
