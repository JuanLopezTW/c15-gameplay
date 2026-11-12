package co.eci.c15.gameplay.infrastructure.config;

import co.eci.c15.gameplay.application.HeroVillainChallengeService;
import co.eci.c15.gameplay.application.MatchResultService;
import co.eci.c15.gameplay.infrastructure.web.MapFreezeInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

@Configuration
public class ChallengeConfig implements WebMvcConfigurer {

    private final HeroVillainChallengeService challenges;
    private final MatchResultService results;
    private final ObjectMapper json;

    public ChallengeConfig(@Lazy HeroVillainChallengeService challenges, @Lazy MatchResultService results,
                           ObjectMapper json) {
        this.challenges = challenges;
        this.results = results;
        this.json = json;
    }

    @Bean(name = "challengeRandom")
    public RandomGenerator challengeRandom() {
        return new SecureRandom();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MapFreezeInterceptor(challenges, results, json))
                .addPathPatterns(MapFreezeInterceptor.FROZEN_PATHS);
    }
}
