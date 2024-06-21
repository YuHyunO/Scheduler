package lab.scheduler;

import lab.scheduler.config.ScheduleJobInfo;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;

import java.net.InetAddress;

@Slf4j
public class SchedulerApplication {

    public static void main(String[] args) throws Exception {
        ScheduleJobInfo info = new ScheduleJobInfo();
    }

}
