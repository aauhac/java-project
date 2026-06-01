package com.tradeagent.demo;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Disabled: using seed-based DataInitializer instead
// @Component
public class DemoUserFactory {

    public static final Long DEMO_USER_ID = 1L;
    public static final String DEMO_USERNAME = "demo_user";

    private final DemoUserRepository demoUserRepository;

    public DemoUserFactory(DemoUserRepository demoUserRepository) {
        this.demoUserRepository = demoUserRepository;
    }

    @Transactional
    public DemoUser ensureDemoUser() {
        return demoUserRepository.findById(DEMO_USER_ID)
                .orElseGet(() -> demoUserRepository.save(new DemoUser(DEMO_USER_ID, DEMO_USERNAME)));
    }
}
