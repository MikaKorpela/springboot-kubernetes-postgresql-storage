package com.pikecape.kubernetes.common.configuration;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, AfterAllCallback {

  private final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(
      "postgres:latest")
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("postgres");

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    postgreSQLContainer.start();

    TestPropertyValues.of(
        "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgreSQLContainer.getUsername(),
        "spring.datasource.password=" + postgreSQLContainer.getPassword()
    ).applyTo(applicationContext.getEnvironment());
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (postgreSQLContainer == null) {
      return;
    }
    postgreSQLContainer.close();
  }
}
