package lab.scheduler.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SchedulerConfiguration {
    private boolean isClustered;
    private JobClusteringConfig clusteringConfig;
    private boolean autoAdjustThreadPoolSize = true;
    private int threadPoolSize = 0;
    private boolean shutdownAfterAllJobsDone = true;

}
