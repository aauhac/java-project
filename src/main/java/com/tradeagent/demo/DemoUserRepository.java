package com.tradeagent.demo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DemoUserRepository extends JpaRepository<DemoUser, Long> {

    Optional<DemoUser> findByUsername(String username);
}
