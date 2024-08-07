package lab.scheduler.config;

import lab.scheduler.core.SchedulerManager;
import lab.scheduler.cluster.JobClusterOption;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.utils.Key;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class ScheduleTemplate {
    private String jobName;
    private int priority = -1;
    private String cronExpression;
    private Date startTime;
    private Date endTime;
    private int repeatCount = -1;
    private int repeatInterval = -1;
    private DateBuilder.IntervalUnit intervalUnit;
    private TimeOfDay startTimeOfDay;
    private TimeOfDay endTimeOfDay;
    private String description;
    private String triggerGroupName = "Trigger-group";
    private String jobGroupName = "Job-group";
    private Map<String, Object> jobParams;
    private TriggerType triggerType;
    private Class<? extends Job> jobClass;
    private boolean removeThreadWhenNextJobNotExist = true;
    private JobClusterOption jobClusterOption = JobClusterOption.FOLLOW_SCHEDULER_DEFAULT;

    public Trigger getTrigger() throws IllegalStateException {
        if (triggerType == null) {
            throw new IllegalStateException("Trigger type not set");
        }
        return getTrigger(triggerType);
    }

    public Trigger getTrigger(TriggerType triggerType) {
        if (jobName == null || jobName.isEmpty()) {
            jobName = Key.createUniqueName(jobGroupName);
        }
        switch (triggerType) {
            case CRON_TRIGGER -> {
                return TriggerBuilder.newTrigger()
                        .withIdentity("trg-" + jobName, triggerGroupName)
                        .withPriority(priority)
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                        .forJob(jobName, jobGroupName)
                        .build();
            }
            case SIMPLE_TRIGGER -> {
                TriggerBuilder trgBuilder = TriggerBuilder.newTrigger()
                        .withIdentity("trg-" + jobName, triggerGroupName)
                        .withPriority(priority)
                        .forJob(jobName, jobGroupName);
                if (startTime == null) {
                    startTime = new Date();
                }
                if (endTime == null) {
                    endTime = toFormattedDate("9999-12-31 23:59:59");
                }
                trgBuilder.startAt(startTime);
                trgBuilder.endAt(endTime);

                SimpleScheduleBuilder simSchd = SimpleScheduleBuilder.simpleSchedule();
                if (repeatCount <= -1) {
                    simSchd.repeatForever();
                } else {
                    simSchd.withRepeatCount(repeatCount);
                }
                if (repeatInterval > 0) {
                    switch (intervalUnit) {
                        case HOUR -> simSchd.withIntervalInHours(repeatInterval);
                        case MINUTE -> simSchd.withIntervalInMinutes(repeatInterval);
                        case SECOND -> simSchd.withIntervalInSeconds(repeatInterval);
                        case DAY -> simSchd.withIntervalInMilliseconds(((long)repeatInterval) * 60 * 60 * 24 * 1000);
                        default -> throw new IllegalStateException("Unrecognized intervalUnit: " + intervalUnit);
                    }
                }
                return trgBuilder.withSchedule(simSchd).build();
            }
            case CALENDAR_INTERVAL_TRIGGER -> {
                TriggerBuilder trgBuilder = TriggerBuilder.newTrigger()
                        .withIdentity("trg-" + jobName, triggerGroupName)
                        .withPriority(priority)
                        .withSchedule(CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
                                .withInterval(repeatInterval, intervalUnit))
                        .forJob(jobName, jobGroupName) ;
                if (startTime == null) {
                    startTime = new Date();
                }
                if (endTime == null) {
                    endTime = toFormattedDate("9999-12-31 23:59:59");
                }
                trgBuilder.startAt(startTime);
                trgBuilder.endAt(endTime);

                return trgBuilder.build();
            }
            case DAILY_TIME_INTERVAL_TRIGGER -> {
                TriggerBuilder trgBuilder = TriggerBuilder.newTrigger()
                        .withIdentity("trg-" + jobName, triggerGroupName)
                        .withPriority(priority)
                        .withSchedule(DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()
                                .withInterval(repeatInterval, intervalUnit)
                                .startingDailyAt(startTimeOfDay)
                                .endingDailyAt(endTimeOfDay))
                        .forJob(jobName, jobGroupName);

                if (startTime == null) {
                    startTime = new Date();
                }
                if (endTime == null) {
                    endTime = toFormattedDate("9999-12-31 23:59:59");
                }
                trgBuilder.startAt(startTime);
                trgBuilder.endAt(endTime);
            }
        }
        throw new IllegalArgumentException("Unsupported trigger type: " + triggerType);
    }

    public JobDetail getJob() {
        if (jobName == null || jobName.isEmpty()) {
            jobName = Key.createUniqueName(jobGroupName);
        }
        if (jobClass == null) {
            Class<? extends Job> clazz = SchedulerManager.getInstance().getDefaultJobClass();
            if (clazz == null) {
                throw new IllegalStateException("Job class not set. You must specify at ScheduleTemplate or a default job class at ScheduleManager.");
            }
            jobClass = clazz;
        }
        JobBuilder builder = JobBuilder.newJob(jobClass)
                .withIdentity(jobName, jobGroupName);
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("clusterOption", jobClusterOption);
        if (jobParams != null) {
            for (String key : jobParams.keySet()) {
                dataMap.put(key, jobParams.get(key));
            }
            builder.setJobData(dataMap);
        }
        if (description != null && !description.isEmpty()) {
            builder.withDescription(description);
        }
        return builder.build();
    }

    public void setJobName(String jobName) {
        if (jobName != null && jobName.isEmpty()) {
            jobName = null;
        }
        this.jobName = jobName;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setCronExpression(String cronExpression) {
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression);
        }
        this.cronExpression = cronExpression;
        triggerType = TriggerType.CRON_TRIGGER;
    }

    public void setSimpleTrigger(Date startTime, Date endTime, int repeatCount, int repeatInterval) {
        setStartTime(startTime);
        setEndTime(endTime);
        setRepeatCount(repeatCount);
        setRepeatInterval(repeatInterval);
        triggerType = TriggerType.SIMPLE_TRIGGER;
    }

    public void setCalendarIntervalTrigger(Date startTime, Date endTime, DateBuilder.IntervalUnit intervalUnit, int repeatInterval) {
        setStartTime(startTime);
        setEndTime(endTime);
        setIntervalUnit(intervalUnit);
        setRepeatInterval(repeatInterval);
        triggerType = TriggerType.CALENDAR_INTERVAL_TRIGGER;
    }

    public void setDailyTimeIntervalTrigger(Date startTime, Date endTime, TimeOfDay startTimeOfDay, TimeOfDay endTimeOfDay, DateBuilder.IntervalUnit intervalUnit, int repeatInterval) {
        setStartTime(startTime);
        setEndTime(endTime);
        setStartTimeOfDay(startTimeOfDay);
        setEndTimeOfDay(endTimeOfDay);
        setIntervalUnit(intervalUnit);
        setRepeatInterval(repeatInterval);
        triggerType = TriggerType.DAILY_TIME_INTERVAL_TRIGGER;
    }

    private Date toFormattedDate(String dateExpression) throws IllegalArgumentException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {
            dateExpression = dateExpression.trim();
            if (dateExpression.equalsIgnoreCase("NOW")) {
                return new Date();
            }
            if (dateExpression.length() == "yyyy-MM-dd HH:mm:ss".length()) {
                dateExpression = dateExpression + ".000";
            }
            return format.parse(dateExpression.trim());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Could not parse date: " + dateExpression + ", date format must be yyyy-MM-dd HH:mm:ss");
        }
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = toFormattedDate(startTime);
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = toFormattedDate(endTime);
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public void setRepeatInterval(int repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public void setIntervalUnit(DateBuilder.IntervalUnit intervalUnit) {
        this.intervalUnit = intervalUnit;
    }

    public void setIntervalUnit(String intervalUnit) throws IllegalArgumentException {
        this.intervalUnit = DateBuilder.IntervalUnit.valueOf(intervalUnit.toUpperCase());
    }

    private Date toFormattedTime(String timeExpression) throws IllegalArgumentException {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        try {
            return format.parse(timeExpression.trim());
        } catch (ParseException e) {
            throw new IllegalArgumentException("Could not parse time: " + timeExpression + ", time format must be HH:mm:ss");
        }
    }

    public void setStartTimeOfDay(TimeOfDay startTimeOfDay) {
        this.startTimeOfDay = startTimeOfDay;
    }

    public void setStartTimeOfDay(String startTimeOfDay) {
        this.startTimeOfDay = TimeOfDay.hourAndMinuteAndSecondFromDate(toFormattedTime(startTimeOfDay));
    }

    public void setEndTimeOfDay(TimeOfDay endTimeOfDay) {
        this.endTimeOfDay = endTimeOfDay;
    }

    public void setEndTimeOfDay(String endTimeOfDay) {
        this.endTimeOfDay = TimeOfDay.hourAndMinuteAndSecondFromDate(toFormattedTime(endTimeOfDay));
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setJobParams(Map<String, Object> jobParams) {
        this.jobParams = jobParams;
    }

    public void addJobParam(String key, Object value) {
        if (jobParams == null) {
            jobParams = new HashMap<>();
        }
        jobParams.put(key, value);
    }

    public void setTriggerGroupName(String triggerGroupName) {
        if (triggerGroupName == null) {
            throw new IllegalArgumentException("Trigger group name cannot be null");
        }
        triggerGroupName = triggerGroupName.trim();
        if (triggerGroupName.isEmpty()) {
            throw new IllegalArgumentException("Trigger group name cannot be empty");
        }
        this.triggerGroupName = triggerGroupName;
    }

    public void setJobGroupName(String jobGroupName) {
        if (jobGroupName == null) {
            throw new IllegalArgumentException("Job group name cannot be null");
        }
        jobGroupName = jobGroupName.trim();
        if (jobGroupName.isEmpty()) {
            throw new IllegalArgumentException("Job group name cannot be empty");
        }
        this.jobGroupName = jobGroupName;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public void setTriggerType(String triggerType) {
        triggerType = triggerType.toUpperCase().trim();
        switch (triggerType) {
            case "CRON_TRIGGER" : case "CRON" : this.triggerType = TriggerType.CRON_TRIGGER; break;
            case "SIMPLE_TRIGGER" : case "SIMPLE" : this.triggerType = TriggerType.SIMPLE_TRIGGER; break;
            case "CALENDAR_INTERVAL_TRIGGER" : case "CALENDAR_TRIGGER" : case "CALENDAR" : this.triggerType = TriggerType.CALENDAR_INTERVAL_TRIGGER; break;
            case "DAILY_TIME_INTERVAL_TRIGGER" : case "DAILY_TIME_INTERVAL" : case "DAILY_TIME" : case "DAILY" : this.triggerType = TriggerType.DAILY_TIME_INTERVAL_TRIGGER; break;
            default: throw new IllegalArgumentException("There is no supported trigger type: " + triggerType);
        }
    }

    public void setJobClass(String jobClass) {
        Class clazz = null;
        try {
            clazz = Class.forName(jobClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find class: " + jobClass + ", ClassNotFoundException");
        }
        if (!Job.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("'" + jobClass + "' is not type of Job class");
        }
        this.jobClass = clazz;
    }

    public void setJobClass(Class<? extends Job> jobClass) {
        this.jobClass = jobClass;
    }

    public void setJobClusterOption(JobClusterOption jobClusterOption) {
        this.jobClusterOption = jobClusterOption;
    }

    public void setJobClusterOption(boolean jobHAOption) {
        if (jobHAOption) {
            this.jobClusterOption = JobClusterOption.FOLLOW_SCHEDULER_DEFAULT;
        } else {
            this.jobClusterOption = JobClusterOption.INACTIVE;
        }
    }


}
