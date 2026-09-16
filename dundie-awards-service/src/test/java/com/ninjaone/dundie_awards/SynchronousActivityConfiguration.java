package com.ninjaone.dundie_awards;

import com.ninjaone.dundie_awards.config.AsyncConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Runs the activity listener inline on the committing thread so assertions made right after a
 * write are deterministic. The listener still fires after commit, so this keeps the transactional
 * semantics under test and only removes the scheduling race.
 * {@link ActivityAsyncIntegrationTest} covers the real executor.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SynchronousActivityConfiguration {

    @Bean(AsyncConfig.ACTIVITY_EXECUTOR)
    TaskExecutor activityExecutor() {
        return new SyncTaskExecutor();
    }
}
