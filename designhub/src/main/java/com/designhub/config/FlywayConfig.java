package com.designhub.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        return flyway;
    }

    @Bean
    public static BeanFactoryPostProcessor entityManagerDependsOnFlywayPostProcessor() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                var bd = beanFactory.getBeanDefinition("entityManagerFactory");
                String[] currentDependsOn = bd.getDependsOn();
                List<String> dependsOnList = currentDependsOn != null ? new ArrayList<>(Arrays.asList(currentDependsOn)) : new ArrayList<>();
                if (!dependsOnList.contains("flyway")) {
                    dependsOnList.add("flyway");
                    bd.setDependsOn(dependsOnList.toArray(new String[0]));
                }
            }
        };
    }
}
