package lab.scheduler.cluster;

public enum JobClusterStrategy {
    FREE_HEAP_MEMORY,
    FIRST_STARTED,
    FAILOVER,
    ROUND_ROBIN
}
