package com.tradeagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // 스케줄 작업은 각 Service에서 @Scheduled로 선언
}
