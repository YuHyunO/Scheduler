package lab.scheduler.core;

import lab.scheduler.config.ScheduleTemplate;
import lab.scheduler.config.SchedulerConfig;
import lab.scheduler.listener.NextFireTimeCheckTriggerListener;
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

    public String registerScheduler(SchedulerConfig config) {
        try {
            return registerScheduler(null, config);
        } catch (Exception e) {
            log.error("", e);
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public String registerScheduler(String schedulerId, SchedulerConfig config) throws SchedulerException {
        if (schedulerId == null || schedulerId.isEmpty()) {
            schedulerId = Key.createUniqueName("SCHEDULER");
        }
        if (schedulerRegistry.containsKey(schedulerId)) {
            throw new SchedulerException("Scheduler with ID " + schedulerId + " already exists");
        }
        if (config == null) {
            throw new IllegalArgumentException("SchedulerConfig is null");
        }
        Properties props = config.getProperties();
        props.put("org.quartz.scheduler.instanceId", schedulerId);
        SchedulerFactory factory = new StdSchedulerFactory(props);
        Map<String, ScheduleTemplate> templates = config.getScheduleTemplates();
        if (templates == null || templates.isEmpty()) {
            throw new IllegalArgumentException("ScheduleTemplates are null or empty");
        }
        Scheduler scheduler = factory.getScheduler();
        for (String jobName : templates.keySet()) {
            ScheduleTemplate template = templates.get(jobName);
            JobDetail jobDetail = template.getJob();
            JobDataMap dataMap = jobDetail.getJobDataMap();
            if (dataMap != null) {
                dataMap.put("schedulerId", schedulerId);
            }
            Trigger trigger = template.getTrigger();
            scheduler.scheduleJob(jobDetail, trigger);
            jobKeyRegistry.put(jobName, jobDetail.getKey());
            triggerKeyRegistry.put(jobName, trigger.getKey());
        }
        addTriggerListener(scheduler, new NextFireTimeCheckTriggerListener());

        schedulerRegistry.put(schedulerId, scheduler);
        configRegistry.put(schedulerId, config);
        return schedulerId;
    }

    public Scheduler getScheduler(String schedulerId) {
        return schedulerRegistry.get(schedulerId);
    }

    public SchedulerConfig getSchedulerConfig(String schedulerId) {
        return configRegistry.get(schedulerId);
    }

    public List<Scheduler> getAllSchedulers() {
        return new ArrayList<>(schedulerRegistry.values());
    }

    public List<SchedulerConfig> getAllSchedulerConfigs() {
        return new ArrayList<>(configRegistry.values());
    }

    public void addJobListener(Scheduler scheduler, JobListener jobListener) throws SchedulerException {
        scheduler.getListenerManager().addJobListener(jobListener);
    }

    public void addTriggerListener(Scheduler scheduler, TriggerListener triggerListener) throws SchedulerException {
        scheduler.getListenerManager().addTriggerListener(triggerListener);
    }

    public void startScheduler(String schedulerId) throws SchedulerException {
        Scheduler scheduler = getScheduler(schedulerId);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerId + " not found");
        }
        if (!scheduler.isStarted()) {
            scheduler.start();
        }
    }

    public Map<String, Exception> startAllSchedulers() throws SchedulerException {
        Map<String, Exception> resultMap = new LinkedHashMap<>();
        for (String schedulerId : schedulerRegistry.keySet()) {
            try {
                startScheduler(schedulerId);
                resultMap.put(schedulerId, null);
            } catch (Exception e) {
                resultMap.put(schedulerId, e);
            }
        }
        return resultMap;
    }

    public void stopScheduler(String schedulerId) throws SchedulerException {
        Scheduler scheduler = schedulerRegistry.get(schedulerId);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerId + " does not exist");
        }
        SchedulerConfig config = configRegistry.get(schedulerId);
        if (scheduler.isStarted()) {
            scheduler.shutdown(config.isShutdownAfterAllJobsDone());
        }
    }

    public Map<String, Exception> stopAllSchedulers() {
        Map<String, Exception> resultMap = new LinkedHashMap<>();
        for (String schedulerId : schedulerRegistry.keySet()) {
            try {
                stopScheduler(schedulerId);
                resultMap.put(schedulerId, null);
            } catch (Exception e) {
                resultMap.put(schedulerId, e);
            }
        }
        return resultMap;
    }

    public void removeScheduler(String schedulerId) {
        Scheduler scheduler = schedulerRegistry.get(schedulerId);
        try {
            stopScheduler(schedulerId);
        } catch (Exception e) {}
        schedulerRegistry.remove(schedulerId);
        configRegistry.remove(schedulerId);
    }

    public void removeAllSchedulers() {
        List<String> schedulerIDs = new ArrayList<>(schedulerRegistry.keySet());
        for (String schedulerId : schedulerIDs) {
            removeScheduler(schedulerId);
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
            addThread(schedulerRegistry.keySet().toArray()[0].toString(), 1);
            scheduler.scheduleJob(template.getJob(), template.getTrigger());
        } catch (SchedulerException e) {

        }
    }

    public void addScheduleJob(String schedulerId, ScheduleTemplate template, boolean addThread) throws SchedulerException {
        Scheduler scheduler = getScheduler(schedulerId);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerId + " not found");
        }
        if (template == null) {
            throw new IllegalArgumentException("ScheduleTemplate is null");
        }
        if (addThread) {
            addThread(schedulerId, 1);
        }
        scheduler.scheduleJob(template.getJob(), template.getTrigger());
        log.info("Added the job '{}' to the scheduler '{}'", template.getJobName(), schedulerId);
    }

    public boolean removeScheduleJob(String schedulerId, String jobID, boolean removeThread) throws SchedulerException {
        Scheduler scheduler = getScheduler(schedulerId);
        if (scheduler == null) {
            throw new SchedulerException("Scheduler with ID " + schedulerId + " not found");
        }
        JobKey key = jobKeyRegistry.get(jobID);
        if (key != null) {
            boolean result = scheduler.deleteJob(key);
            if (!result) {
                log.warn("Couldn't remove the job '{}'", jobID);
                return false;
            }
            log.info("Removed the job '{}' from the scheduler '{}'", jobID, schedulerId);
            if (removeThread) {
                removeThread(schedulerId, 1);
            }
        } else {
            log.warn("Couldn't remove the job '{}'. job key is null", jobID);
        }
        return false;
    }

    public boolean removeScheduleJob(String schedulerId, String jobID) throws SchedulerException {
        Scheduler scheduler = getScheduler(schedulerId);
        boolean removeThread = configRegistry.get(schedulerId).getScheduleTemplate(jobID).isRemoveThreadWhenNextJobNotExist();
        return removeScheduleJob(schedulerId, jobID, removeThread);
    }

    private int addThread(String schedulerId, int addCount) {
        return ResizableSimpleThreadPoolManager.getInstance().addWorkerThread(schedulerId, addCount);
    }

    private int removeThread(String schedulerId, int removeCount) {
        return ResizableSimpleThreadPoolManager.getInstance().removeWorkerThread(schedulerId, removeCount);
    }
}
