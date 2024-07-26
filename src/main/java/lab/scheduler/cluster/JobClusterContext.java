package lab.scheduler.cluster;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JobClusterContext {

    private static JobClusterContext context;
    private Map<String, HistoryHolder> jobHistoryHolders;
    private static Map<JobClusterType, Boolean> clusterInitializedInfo;

    private JobClusterContext() {}

    public static boolean isInitialized(JobClusterType type) {
        if (clusterInitializedInfo == null)
            clusterInitializedInfo = new HashMap<>();
        Boolean initialized = clusterInitializedInfo.get(type);

        return initialized == null ? false : initialized;
    }

    public static void initialize(JobClusterType type) {
        if (isInitialized(type))
            return;

    }

    public static JobClusterContext getInstance() {
        if (context == null) {
            context = new JobClusterContext();
            context.jobHistoryHolders = new ConcurrentHashMap<>();
        }

        return context;
    }



    private class HistoryHolder {
        String jobID;
        boolean inProcess;
        Map<Long, Boolean> jobHistory;

        HistoryHolder(String jobID) {
            this.jobID = jobID;
            jobHistory = new HashMap<>();
        }

        void setInProcess(boolean inProcess) {
            this.inProcess = inProcess;
        }

        void addJobHistory(Date scheduleFiredTime, boolean inProcess) {
            long time = scheduleFiredTime.getTime()/1000;
        }

        public boolean isInProcess() {
            return inProcess;
        }
    }

}
