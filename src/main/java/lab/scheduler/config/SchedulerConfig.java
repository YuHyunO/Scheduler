package lab.scheduler.config;

import lab.scheduler.core.ResizableSimpleThreadPool;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Slf4j
@Data
public class SchedulerConfig {
    private boolean isClustered;
    private JobClusterConfig clusterConfig;
    private boolean autoAdjustThreadCount = true;
    private int threadCount = 1;
    private int maxThreadCount = 100;
    private String threadPoolName;
    private String threadPoolClass;
    private boolean shutdownAfterAllJobsDone = true;
    private Properties properties = new Properties();
    private Map<String, ScheduleTemplate> scheduleTemplates = new HashMap<>();

    public Properties getProperties() {
        if (autoAdjustThreadCount) {
            setThreadCount(scheduleTemplates.size());
        }
        return properties;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        properties.setProperty("org.quartz.threadPool.threadCount", String.valueOf(threadCount));
    }

    public void setMaxThreadCount(int maxThreadCount) {
        this.maxThreadCount = maxThreadCount;
        properties.setProperty("org.quartz.threadPool.maxThreadCount", String.valueOf(maxThreadCount));
        properties.setProperty("org.quartz.threadPool.class", ResizableSimpleThreadPool.class.getName());
        /*if (!properties.containsKey("org.quartz.threadPool.class")) {
        }*/
    }

    public void setThreadPoolName(String threadPoolName) {
        this.threadPoolName = threadPoolName;
        properties.setProperty("org.quartz.scheduler.threadName", threadPoolName);
    }

    @Deprecated
    /**
     * Deprecated this method for compatibility with ResizableSimpleThreadPool
     * */
    public void setThreadPoolClass(String threadPoolClass) {
        this.threadPoolClass = threadPoolClass;
        //properties.setProperty("org.quartz.threadPool.class", threadPoolClass);
        properties.setProperty("org.quartz.threadPool.class", ResizableSimpleThreadPool.class.getName());
    }

    public void setProperties(Properties properties) {
        if (this.properties.isEmpty()) {
            this.properties = properties;
        } else {
            for (String key : properties.stringPropertyNames()) {
                this.properties.put(key, properties.getProperty(key));
            }
        }
    }

    public void setPropertiesFileLocation(String propertiesFileLocation) throws IOException {
        Properties tempProps = new Properties();
        tempProps.load(new FileReader(propertiesFileLocation));
        for (String key : tempProps.stringPropertyNames()) {
            properties.put(key, tempProps.getProperty(key));
        }
    }

    public void setScheduleTemplates(Map<String, ScheduleTemplate> scheduleTemplates) {
        if (scheduleTemplates ==  null) {
            throw new IllegalArgumentException("scheduleTemplates cannot be null");
        }
        this.scheduleTemplates = scheduleTemplates;
    }

    public void addScheduleTemplate(ScheduleTemplate scheduleTemplate) {
        this.scheduleTemplates.put(scheduleTemplate.getJobName(), scheduleTemplate);
    }

    public ScheduleTemplate getScheduleTemplate(String jobName) {
        return scheduleTemplates.get(jobName);
    }
}
