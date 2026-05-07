package com.tradeagent;

import com.tradeagent.chart.ChartProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ChartProperties.class)
public class TradeAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeAgentApplication.class, args);
    }

}
