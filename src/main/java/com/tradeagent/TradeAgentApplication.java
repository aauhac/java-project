package com.tradeagent;

import com.tradeagent.config.AlpacaProperties;
import com.tradeagent.config.VllmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        AlpacaProperties.class,
        VllmProperties.class
})
public class TradeAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeAgentApplication.class, args);
    }
}
