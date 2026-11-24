package com.bookstore.shared.jdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Runs a test class only when a Docker daemon is reachable, and skips it otherwise, so
 * {@code ./mvnw verify} passes without Docker (ADR-0008). Put it on the PostgreSQL variants of the
 * contract tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(DockerAvailableCondition.class)
public @interface EnabledIfDocker {
}
