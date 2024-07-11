package lab.scheduler.core;

import lab.scheduler.config.ScheduleTemplate;
import lab.scheduler.config.SchedulerConfig;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.utils.Key;

import java.util.*;

@Slf4j
public class SchedulerManager {
    private Map<String, Scheduler> schedulerRegistry;
    private Map<String, SchedulerConfig> configRegistry;
    private Map<String, TriggerKey> triggerKeyRegistry;
    private Map<String, JobKey> jobKeyRegistry;
    private Class<? extends Job> defaultedJobClass;

    private static SchedulerManager manager;

    private SchedulerManager() {}

    public static SchedulerManager getInstance() {
        if (manager == null) {
            manager = new SchedulerManager();
            manager.schedulerRegistry = new HashMap<>();
            manager.configRegistry = new HashMap<>();
            manager.triggerKeyRegistry = new HashMap<>();
            manager.jobKeyRegistry = new HashMap<>();
        }
        return manager;
    }

    public void setDefaultJobClass(Class<? extends Job> jobClass) {
        if (!Job.class.isAssignableFrom(jobClass)) {
            throw new IllegalArgumentException("Job class must be an instance of " + Job.class.getName());
        }
        defaultedJobClass = jobClass;
    }

    public void setDefaultJobClass(String jobClassName) {
        Class jobClass = null;
        try {
            jobClass = Class.forName(jobClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find class: " + jobClass + ", ClassNotFoundException");
        }
        setDefaultJobClass(jobClass);
    }

    public Class<? extends Job> getDefaultJobClass() {
        return defaultedJobClass;
    }

    public String registerScheduler(SchedulerConfig config) throws SchedulerException {
        return registerScheduler(null, config);
    }

    public String registerScheduler(String schedulerID, SchedulerConfig config) throws SchedulerException {
        if (schedulerID == null || schedulerID.isEmpty()) {
            schedulerID = Key.createUniqueName("SCHEDULER");
        }
        if (schedulerRegistry.containsKey(schedulerID)) {
            throw new SchedulerException("Scheduler with ID " + schedulerID + " already exists");
        }
        if (config == null) {
            throw new IllegalArgumentException("SchedulerConfig is null");
        }
        Properties props = config.getProperties();
        SchedulerFactory factory = new StdSchedulerFactory(props);
        List<ScheduleTemplate> templates = config.getScheduleTemplates();
        if (templates == null || templates.isEmpty()) {
            throw new IllegalArgumentException("ScheduleTemplates are null or empty");
        }
        Scheduler scheduler = factory.getScheduler();
        for (ScheduleTemplate template : templates) {
            JobDetail jobDetail = template.getJob();
            Trigger trigger = template.getTrigger();
            scheduler.scheduleJob(jobDetail, trigger);
            String jobName = jobDetail.getKey().getName();
            jobKeyRegistry.put(jobName, jobDetail.getKey());
            triggerKeyRegistry.put(jobName, trigger.getKey());
        }
        schedulerRegistry.put(schedulerID, scheduler);
        configRegistry.put(schedulerID, config);
        return schedulerID;
    }

    public Scheduler getScheduler(String schedulerID) {
        return schedulerRegistry.get(schedulerID);
    }

    public SchedulerConfig getSchedulerConfig(String schedulerID) {
        return configRegistry.get(schedulerID);
    }

    public List<Scheduler> getAllSchedulers() {
        return new ArrayList<>(schedulerRegistry.values());
    }

    public List<SchedulerConfig> getAllSchedulerConfigs() {
        return new ArrayList<>(configRegistry.values());
    }

    public void startScheduler(String schedulerID) throws SchedulerException {
        Scheduler scheduler = getScheduler(schedulerID);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerID + " not found");
        }
        if (!scheduler.isStarted()) {
            scheduler.start();
        }
    }

    public Map<String, Exception> startAllSchedulers() throws SchedulerException {
        Map<String, Exception> resultMap = new LinkedHashMap<>();
        for (String schedulerID : schedulerRegistry.keySet()) {
            try {
                startScheduler(schedulerID);
                resultMap.put(schedulerID, null);
            } catch (Exception e) {
                resultMap.put(schedulerID, e);
            }
        }
        return resultMap;
    }

    public void stopScheduler(String schedulerID) throws SchedulerException {
        Scheduler scheduler = schedulerRegistry.get(schedulerID);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerID + " does not exist");
        }
        SchedulerConfig config = configRegistry.get(schedulerID);
        if (scheduler.isStarted()) {
            scheduler.shutdown(config.isShutdownAfterAllJobsDone());
        }
    }

    public Map<String, Exception> stopAllSchedulers() {
        Map<String, Exception> resultMap = new LinkedHashMap<>();
        for (String schedulerID : schedulerRegistry.keySet()) {
            try {
                stopScheduler(schedulerID);
                resultMap.put(schedulerID, null);
            } catch (Exception e) {
                resultMap.put(schedulerID, e);
            }
        }
        return resultMap;
    }

    public void removeScheduler(String schedulerID) {
        Scheduler scheduler = schedulerRegistry.get(schedulerID);
        try {
            stopScheduler(schedulerID);
        } catch (Exception e) {}
        schedulerRegistry.remove(schedulerID);
        configRegistry.remove(schedulerID);
    }

    public void removeAllSchedulers() {
        List<String> schedulerIDs = new ArrayList<>(schedulerRegistry.keySet());
        for (String schedulerID : schedulerIDs) {
            removeScheduler(schedulerID);
        }
    }

    public void addScheduleJob(ScheduleTemplate template) throws SchedulerException {
        try {
            if (schedulerRegistry.size() != 1) {
                throw new SchedulerException("addScheduleJob method is only supported when there is only one scheduler in registry");
            }
            if (template == null) {
                throw new IllegalArgumentException("ScheduleTemplate is null");
            }
            Scheduler scheduler = new ArrayList<>(schedulerRegistry.values()).getFirst();
            addThread(1);
            scheduler.scheduleJob(template.getJob(), template.getTrigger());
        } catch (SchedulerException e) {

        }
    }

    public void addScheduleJob(String schedulerID, ScheduleTemplate template, boolean addThread) throws SchedulerException {
        Scheduler scheduler = getScheduler(schedulerID);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerID + " not found");
        }
        if (template == null) {
            throw new IllegalArgumentException("ScheduleTemplate is null");
        }
        if (addThread) {
            addThread(1);
        }
        scheduler.scheduleJob(template.getJob(), template.getTrigger());
        log.info("Added the job '{}' to the scheduler '{}'", template.getJobName(), schedulerID);
    }

    public boolean removeScheduleJob(String schedulerID, String jobID, boolean removeThread) throws SchedulerException {
        Scheduler scheduler = getScheduler(schedulerID);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerID + " not found");
        }
        JobKey key = jobKeyRegistry.get(jobID);
        if (key != null) {
            boolean result = scheduler.deleteJob(key);
            if (!result) {
                log.warn("Couldn't remove the job '{}'", jobID);
                return false;
            }
            log.info("Removed the job '{}' from the scheduler '{}'", jobID, schedulerID);
            if (removeThread) {
                removeThread(1);
            }
        } else {
            log.warn("Couldn't remove the job '{}'. job key is null", jobID);
        }
        return false;
    }

    private int addThread(int addCount) {
        ResizableSimpleThreadPool tp = ResizableSimpleThreadPool.getInstance();
        if (tp != null) {
            return tp.addWorkerThread(addCount);
        }
        return 0;
    }

    private int removeThread(int removeCount) {
        ResizableSimpleThreadPool tp = ResizableSimpleThreadPool.getInstance();
        if (tp != null) {
            return tp.removeWorkerThread(removeCount);
        }
        return 0;
    }
}
