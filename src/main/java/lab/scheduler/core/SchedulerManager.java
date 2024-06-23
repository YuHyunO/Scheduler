package lab.scheduler.core;

import lab.scheduler.config.ScheduleTemplate;
import lab.scheduler.config.SchedulerConfig;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.utils.Key;

import java.util.*;

public class SchedulerManager {
    private static final Map<String, Scheduler> SCHEDULER_REGISTRY = new HashMap<>();
    private static final Map<String, SchedulerConfig> SCHEDULER_CONFIG_REGISTRY = new HashMap<>();
    private static final Map<String, TriggerKey> TRIGGER_KEY_REGISTRY = new HashMap<>();
    private static Class<? extends Job> defaultedJobClass;

    public static void setDefaultJobClass(Class<? extends Job> jobClass) {
        if (!Job.class.isAssignableFrom(jobClass)) {
            throw new IllegalArgumentException("Job class must be an instance of " + Job.class.getName());
        }
        defaultedJobClass = jobClass;
    }

    public static void setDefaultJobClass(String jobClassName) {
        Class jobClass = null;
        try {
            jobClass = Class.forName(jobClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find class: " + jobClass + ", ClassNotFoundException");
        }
        if (!Job.class.isAssignableFrom(jobClass)) {
            throw new IllegalArgumentException("Job class must be an instance of " + Job.class.getName());
        }
        defaultedJobClass = jobClass;
    }

    public static Class<? extends Job> getDefaultJobClass() {
        return defaultedJobClass;
    }

    public static String registerScheduler(SchedulerConfig config) throws SchedulerException {
        return registerScheduler(null, config);
    }

    public static String registerScheduler(String schedulerID, SchedulerConfig config) throws SchedulerException {
        if (schedulerID == null || schedulerID.isEmpty()) {
            schedulerID = Key.createUniqueName("SCHEDULER");
        }
        if (SCHEDULER_REGISTRY.containsKey(schedulerID)) {
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
            TRIGGER_KEY_REGISTRY.put(jobDetail.getKey().getName(), trigger.getKey());
        }
        SCHEDULER_REGISTRY.put(schedulerID, scheduler);
        SCHEDULER_CONFIG_REGISTRY.put(schedulerID, config);
        return schedulerID;
    }

    public static Scheduler getScheduler(String schedulerID) {
        return SCHEDULER_REGISTRY.get(schedulerID);
    }

    public static SchedulerConfig getSchedulerConfig(String schedulerID) {
        return SCHEDULER_CONFIG_REGISTRY.get(schedulerID);
    }

    public static List<Scheduler> getAllSchedulers() {
        return new ArrayList<>(SCHEDULER_REGISTRY.values());
    }

    public static List<SchedulerConfig> getAllSchedulerConfigs() {
        return new ArrayList<>(SCHEDULER_CONFIG_REGISTRY.values());
    }

    public static void startScheduler(String schedulerID) throws SchedulerException {
        Scheduler scheduler = getScheduler(schedulerID);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerID + " not found");
        }
        if (!scheduler.isStarted()) {
            scheduler.start();
        }
    }

    public static Map<String, Exception> startAllSchedulers() throws SchedulerException {
        Map<String, Exception> resultMap = new LinkedHashMap<>();
        for (String schedulerID : SCHEDULER_REGISTRY.keySet()) {
            try {
                startScheduler(schedulerID);
                resultMap.put(schedulerID, null);
            } catch (Exception e) {
                resultMap.put(schedulerID, e);
            }
        }
        return resultMap;
    }

    public static void stopScheduler(String schedulerID) throws SchedulerException {
        Scheduler scheduler = SCHEDULER_REGISTRY.get(schedulerID);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerID + " does not exist");
        }
        SchedulerConfig config = SCHEDULER_CONFIG_REGISTRY.get(schedulerID);
        if (scheduler.isStarted()) {
            scheduler.shutdown(config.isShutdownAfterAllJobsDone());
        }
    }

    public static Map<String, Exception> stopAllSchedulers() {
        Map<String, Exception> resultMap = new LinkedHashMap<>();
        for (String schedulerID : SCHEDULER_REGISTRY.keySet()) {
            try {
                stopScheduler(schedulerID);
                resultMap.put(schedulerID, null);
            } catch (Exception e) {
                resultMap.put(schedulerID, e);
            }
        }
        return resultMap;
    }

    public static void removeScheduler(String schedulerID) {
        Scheduler scheduler = SCHEDULER_REGISTRY.get(schedulerID);
        try {
            stopScheduler(schedulerID);
        } catch (Exception e) {}
        SCHEDULER_REGISTRY.remove(schedulerID);
        SCHEDULER_CONFIG_REGISTRY.remove(schedulerID);
    }

    public static void removeAllSchedulers() {
        List<String> schedulerIDs = new ArrayList<>(SCHEDULER_REGISTRY.keySet());
        for (String schedulerID : schedulerIDs) {
            removeScheduler(schedulerID);
        }
    }

    public static void addScheduleJob(ScheduleTemplate template) throws SchedulerException {
        if (SCHEDULER_REGISTRY.size() != 1) {
            throw new SchedulerException("addScheduleJob method is only supported when there is only one scheduler in registry");
        }
        if (template == null) {
            throw new IllegalArgumentException("ScheduleTemplate is null");
        }
        Scheduler scheduler = new ArrayList<>(SCHEDULER_REGISTRY.values()).getFirst();
        scheduler.scheduleJob(template.getJob(), template.getTrigger());
    }

    public static void addScheduleJob(String schedulerID, ScheduleTemplate template) throws SchedulerException {
        Scheduler scheduler = getScheduler(schedulerID);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerID + " not found");
        }
        if (template == null) {
            throw new IllegalArgumentException("ScheduleTemplate is null");
        }
        scheduler.scheduleJob(template.getJob(), template.getTrigger());
    }

    public static void removeScheduleJob(String schedulerID, String jobID) throws SchedulerException {
        Scheduler scheduler = getScheduler(schedulerID);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerID + " not found");
        }

    }
}
