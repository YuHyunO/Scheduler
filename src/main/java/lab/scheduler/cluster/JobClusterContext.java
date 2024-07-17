package lab.scheduler.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JobClusterContext {

    private static JobClusterContext context;
    private Map<String, HistoryHolder> jobHistoryHolders;

    private JobClusterContext() {}


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

        HistoryHolder(String jobID, long scheduledFireTime, boolean inProcess) {
            this.jobID = jobID;
            if (jobHistory == null) {
                jobHistory = new HashMap<>();
            }
            jobHistory.put(scheduledFireTime, inProcess);
        }

        void setInProcess(boolean inProcess) {
            this.inProcess = inProcess;
        }

        public boolean isInProcess() {
            return inProcess;
        }
    }

}
