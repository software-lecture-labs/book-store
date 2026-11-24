package com.bookstore.shared.jdbc;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/** The condition behind {@link EnabledIfDocker}; Docker is looked for once per test run. */
public final class DockerAvailableCondition implements ExecutionCondition {

    private static Boolean available;

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return dockerAvailable()
                ? ConditionEvaluationResult.enabled("Docker is available")
                : ConditionEvaluationResult.disabled("Docker is not available; PostgreSQL tests are skipped");
    }

    private static synchronized boolean dockerAvailable() {
        if (available == null) {
            available = DockerClientFactory.instance().isDockerAvailable();
        }
        return available;
    }
}
