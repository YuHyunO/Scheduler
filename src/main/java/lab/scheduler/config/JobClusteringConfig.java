package lab.scheduler.config;

import lab.scheduler.enums.JobClusterStrategy;
import lab.scheduler.enums.JobClusterType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Getter
public class JobClusteringConfig {

    private JobClusterType clusterType = JobClusterType.TCP_COMMUNICATION;
    private JobClusterStrategy clusterStrategy = JobClusterStrategy.FREE_HEAP_MEMORY;
    private int haConnectTimeoutMs = 2000;
    private int haReadTimeoutMs = 2000;
    private int haServerOpenPort = 7070;
    private List<InetAddress> haServerAddresses;
    private Properties quartzClusteringProperties;
    private Path jobStorePath;


    public void setClusterType(JobClusterType clusterType) {
        this.clusterType = clusterType;
    }

    public void setClusterType(String clusterType) {
        clusterType = clusterType.toUpperCase().trim();
        switch (clusterType) {
            case "TCP_COMMUNICATION" : case "TCP" : this.clusterType = JobClusterType.TCP_COMMUNICATION; break;
            case "DB_JOBSTORE" : case "DB" : this.clusterType = JobClusterType.DB_JOBSTORE; break;
            case "FILE_JOBSTORE" : case "FILE" : this.clusterType = JobClusterType.FILE_JOBSTORE; break;
            default: throw new IllegalArgumentException("Unknown cluster type: " + clusterType);
        }
    }

    public void setClusterStrategy(String clusterStrategy) {
        clusterStrategy = clusterStrategy.toUpperCase().trim();
        switch (clusterStrategy) {
            case "FREE_HEAP_MEMORY" : case "FREE_MEMORY" : this.clusterStrategy = JobClusterStrategy.FREE_HEAP_MEMORY; break;
            case "FIRST_STARTED" : this.clusterStrategy = JobClusterStrategy.FIRST_STARTED; break;
            case "FAILOVER" : this.clusterStrategy = JobClusterStrategy.FAILOVER; break;
            case "ROUND_ROBIN" : this.clusterStrategy = JobClusterStrategy.ROUND_ROBIN; break;
            case "RANDOM" : this.clusterStrategy = JobClusterStrategy.RANDOM; break;
            default: throw new IllegalArgumentException("Unknown cluster strategy: " + clusterStrategy);
        }
    }

    public void setCLusterStrategy(String clusterStrategy) {}

    public void setHaConnectTimeoutMs(int haConnectTimeoutMs) {
        if (haConnectTimeoutMs > 3000) {
            throw new IllegalArgumentException("HA connect timeout must be less than 3000ms");
        }
        if (haConnectTimeoutMs < 500) {
            throw new IllegalArgumentException("HA connect timeout must be less than 500ms");
        }
        this.haConnectTimeoutMs = haConnectTimeoutMs;
    }

    public void setHaReadTimeoutMs(int haReadTimeoutMs) {
        if (haReadTimeoutMs > 3000) {
            throw new IllegalArgumentException("HA read timeout must be less than 3000ms");
        }
        if (haReadTimeoutMs < 500) {
            throw new IllegalArgumentException("HA read timeout must be less than 500ms");
        }
    }

    public void setHaServerOpenPort(int haServerOpenPort) throws IOException {
        new ServerSocket(haServerOpenPort).close();
        this.haServerOpenPort = haServerOpenPort;
    }

    public void setHaServerAddresses(List<InetAddress> haServerAddresses) {
        this.haServerAddresses = haServerAddresses;
    }

    public void setHaServerAddresses(String... haServerAddresses) throws UnknownHostException {
        this.haServerAddresses = new ArrayList<>();
        for (String addr : haServerAddresses) {
            this.haServerAddresses.add(InetAddress.getByName(addr));
        }
    }

    public void setQuartzClusteringProperties(Properties quartzClusteringProperties) {
        this.quartzClusteringProperties = quartzClusteringProperties;
    }

    public void setQuartzClusteringPropertiesFileLocation(String quartzClusteringPropertiesFileLocation) throws IOException {
        Properties props = new Properties();
        Properties jobStoreProps = new Properties();
        props.load(new FileReader(quartzClusteringPropertiesFileLocation));
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("org.quartz.jobStore.") || key.startsWith("org.quartz.dataSource.")) {
                jobStoreProps.put(key, props.get(key));
            }
        }
        this.quartzClusteringProperties = props;
    }

    public void setJobStorePath(Path jobStorePath) {
        this.jobStorePath = jobStorePath;
    }

    public void setJobStorePath(String jobStorePath) {
        Paths.get(jobStorePath);
    }

}
