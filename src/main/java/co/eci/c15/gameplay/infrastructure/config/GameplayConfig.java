package co.eci.c15.gameplay.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;

@Configuration
@EnableJpaRepositories(basePackages = "co.eci.c15.gameplay.infrastructure.persistence")
@EntityScan(basePackages = "co.eci.c15.gameplay.infrastructure.persistence")
public class GameplayConfig {

    @Bean(name = "gameplayScheduler")
    public ThreadPoolTaskScheduler gameplayScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("gameplay-");
        return scheduler;
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
