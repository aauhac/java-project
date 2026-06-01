package com.tradeagent.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface DemoUserRepository extends JpaRepository<DemoUser, Long> {

    Optional<DemoUser> findByUsername(String username);
}
