package lab.scheduler.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.List;

@Slf4j
@Data
public class SchedulerConfiguration {
    private boolean haEnabled;
    private int haConnectTimeoutMs = 2000;
    private int haReadTimeoutMs = 2000;
    private int haServerOpenPort = 7070;
    private List<InetAddress> haServerAddresses;
    private boolean threadPoolAutoSizing = true;
    private int threadPoolSize = 0;
    private boolean shutdownAfterAllJobsDone = true;

}
