package com.github.fanshuzaizai.scammony;

import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * @author fanshuzaizai.
 * @date 2019/5/8 17:07
 */
@Import({AopConfig.class})
@AutoConfigureAfter(RedissonAutoConfiguration.class)
@EnableConfigurationProperties(ScammonyProperties.class)
public class ScammonyAutoConfig {
}
