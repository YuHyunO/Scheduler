package lab.scheduler.tutorial;

import lab.scheduler.config.ScheduleTemplate;
import lab.scheduler.config.SchedulerConfig;
import lab.scheduler.core.SchedulerManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Step2_StartCronScheduler {

    public static void main(String[] args) throws Exception {
        log.info("Start Tutorial");
        //(1) Create SchedulerManager instance
        SchedulerManager manager = SchedulerManager.getInstance();

        //(2) Create SchedulerConfig
        SchedulerConfig config = new SchedulerConfig();
        config.setAutoAdjustThreadCount(true); // <- The property 'autoAdjustThreadCount=true' enables that thread pool size is automatically sized with schedule Job count. Default value is 'true'.
        config.setThreadCount(7); // <- In this case, the effect of using 'setThreadCount(int count)' is ignored because 'autoAdjustThreadCount' is true as above.
        config.setMaxThreadCount(100); //Set max thread count that can be added to thread pool.

        //(3) Create ScheduleTemplate(s)
        //ScheduleTemplate object contains the information about JobDetail and Trigger
        ScheduleTemplate template1 = new ScheduleTemplate(); //Create new ScheduleTemplate()
        template1.setJobClass(Step1_DefineJobClass.class); //Set the job class which implemented 'org.quartz.Job' interface. If it is null or not set, a default job class is retrieved from ScheduleManager
        template1.setCronExpression("0/3 * * * * ?"); //Set cron expression. It is verified internally by CronExpression.isValidExpression(String expression) method.
        template1.setJobName("MyServiceLogic_1"); //Set unique job name. When null, it is created internally.
        template1.setPriority(10); //Set this job's priority. Higher priority number has higher priority in execution. Default value is -1.
        template1.addJobParam("template name", "template1"); //Set job parameter you want to use in job class
        template1.addJobParam("favorite number", 7); //Set job parameter you want to use in job class

        ScheduleTemplate template2 = new ScheduleTemplate();
        template2.setJobClass("lab.scheduler.tutorial.Step1_DefineJobClass"); //Also possible setting job class with string name
        template2.setCronExpression("0/3 * * * * ?");
        template2.setJobName("MyServiceLogic_2");
        template2.setPriority(9);
        template2.addJobParam("template name", "template2");
        template2.addJobParam("favorite fruit", "grape");

        ScheduleTemplate template3 = new ScheduleTemplate();
        template3.setJobClass(Step1_DefineJobClass.class);
        template3.setCronExpression("0/3 * * * * ?");
        //template3.setjobName("MyServiceLogic_3"); //If you do not assign job name, the ScheduleTemplate generates unique job name internally.
                                                    //It is not recommended to auto generate job name, however.
                                                    //Because a job name means a specific service and used as a key of clustering and job activation control.
        template3.setPriority(8);
        template3.addJobParam("template name", "template3");
        template3.addJobParam("hobby", "playing piano");

        //(4) Add ScheduleTemplates to SchedulerConfig
        config.addScheduleTemplate(template1);
        config.addScheduleTemplate(template2);
        //config.addScheduleTemplate(template3); //Note that I didn't add 'template3' to SchedulerConfig. I am going to add this after Scheduler started

        //(5) Start scheduler
        String schdeulerID = manager.registerScheduler(config); //Register the SchedulerConfig we made to the SchedulerManager.
                                                                         //This method creates new scheduler and returns the unique scheduler ID of it in string.
                                                                         //Scheduler ID must be remembered when you have schedulers more than two, and it used when control a specific scheduler and its jobs.
        manager.startScheduler(schdeulerID); //Start the specific scheduler
        //SchedulerManager.startAllSchedulers(); //Start all schedulers

        Thread.sleep(7000);

        manager.addScheduleJob(schdeulerID, template3, true); //Add new job at scheduler's running. The boolean parameter of this method means that whether to add a new thread to the pool.
        //SchedulerManager.addScheduleJob(template3); //This method can be used when only one scheduler exists in the SchedulerManager

        Thread.sleep(7000);
        manager.removeScheduleJob(schdeulerID, "MyServiceLogic_1", true); //Remove a job at scheduler's running. The boolean parameter of this method means that whether to remove a thread from the pool.

    }

}
