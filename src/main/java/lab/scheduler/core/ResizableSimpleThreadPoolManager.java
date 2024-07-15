package lab.scheduler.core;

import java.util.HashMap;
import java.util.Map;

public class ResizableSimpleThreadPoolManager {
    private Map<String, ResizableSimpleThreadPool> instances = new HashMap<>();
    private static ResizableSimpleThreadPoolManager threadManager;

    private ResizableSimpleThreadPoolManager() {}

    public static ResizableSimpleThreadPoolManager getInstance() {
        if (threadManager == null) {
            threadManager = new ResizableSimpleThreadPoolManager();
            threadManager.instances = new HashMap<>();
        }
        return threadManager;
    }

    public void register(String schedulerId, ResizableSimpleThreadPool pool) {
        instances.put(schedulerId, pool);
    }

    public void remove(String schedulerId) {
        instances.remove(schedulerId);
    }

    public int addWorkerThread(String schedulerId, int createCount) {
        ResizableSimpleThreadPool pool = instances.get(schedulerId);
        if (pool != null) {
            return pool.addWorkerThread(createCount);
        }
        return 0;
    }

    public int removeWorkerThread(String schedulerId, int removeCount) {
        ResizableSimpleThreadPool pool = instances.get(schedulerId);
        if (pool != null) {
            return pool.removeWorkerThread(removeCount);
        }
        return 0;
    }
}
