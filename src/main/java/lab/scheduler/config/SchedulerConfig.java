package lab.scheduler.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
@Data
public class SchedulerConfig {
    private boolean isClustered;
    private JobClusteringConfig clusteringConfig;
    private boolean autoAdjustThreadCount = true;
    private int threadCount = 1;
    private String threadPoolName;
    private String threadPoolClass;
    private boolean shutdownAfterAllJobsDone = true;
    private Properties properties = new Properties();
    private List<ScheduleTemplate> scheduleTemplates = new ArrayList<>();

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

    public void setThreadPoolName(String threadPoolName) {
        this.threadPoolName = threadPoolName;
        properties.setProperty("org.quartz.scheduler.threadName", threadPoolName);
    }

    public void setThreadPoolClass(String threadPoolClass) {
        this.threadPoolClass = threadPoolClass;
        properties.setProperty("org.quartz.threadPool.class", threadPoolClass);
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

    public void setScheduleTemplates(List<ScheduleTemplate> scheduleTemplates) {
        if (scheduleTemplates ==  null) {
            throw new IllegalArgumentException("scheduleTemplates cannot be null");
        }
        this.scheduleTemplates = scheduleTemplates;
    }

    public void addScheduleTemplate(ScheduleTemplate scheduleTemplate) {
        this.scheduleTemplates.add(scheduleTemplate);
    }
}
