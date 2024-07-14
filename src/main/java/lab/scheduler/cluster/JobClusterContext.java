package lab.scheduler.cluster;

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

        HistoryHolder(String jobID) {
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
