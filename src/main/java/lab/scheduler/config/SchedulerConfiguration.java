package lab.scheduler.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.List;

@Slf4j
@Data
public class SchedulerConfiguration {
    private boolean isClustered;
    private JobClusteringConfig clusteringConfig;
    private boolean autoSizingThreadPoolCount = true;
    private int threadPoolSize = 0;
    private boolean shutdownAfterAllJobsDone = true;

}
