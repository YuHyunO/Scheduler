package lab.scheduler.cluster;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JobClusterContext {

    private static JobClusterContext context;
    private Map<String, JobClusterInfo> jobClusterInfos;

    private JobClusterContext() {}


    public static JobClusterContext getInstance() {
        if (context == null) {
            context = new JobClusterContext();
            context.jobClusterInfos = new ConcurrentHashMap<>();
        }
        return context;
    }


    private class JobClusterInfo {
        String jobID;
        boolean inProcess;

        JobClusterInfo(String jobID) {
            this.jobID = jobID;
        }

        void setInProcess(boolean inProcess) {
            this.inProcess = inProcess;
        }

        public boolean isInProcess() {
            return inProcess;
        }
    }

}
