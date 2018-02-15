package org.leolo.vpn.biller;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.DateBuilder.*;
import static org.quartz.JobBuilder.*;

public class OpenVPNBiller {
	private static Logger logger = LoggerFactory.getLogger(OpenVPNBiller.class);

	public static void main(String... args) {
		Properties prop = new Properties();
		try {
			prop.load(new FileReader("setting.conf"));
		} catch (FileNotFoundException e) {
			logger.error(e.getLocalizedMessage(), e);
			System.exit(1);
		} catch (IOException e) {
			logger.error(e.getLocalizedMessage(), e);
			System.exit(1);
		}
		ExecutorService threadPool = new ThreadPoolExecutor(1, 10, 180L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>());
		SharedResource.getInstance().prop = prop;
		SharedResource.getInstance().threadPool = threadPool;
		new MainThread().start();
		DBManager.getInstance();
		JobDetail job = newJob(CleanupJob.class).withIdentity("staleJob", "mainGroup").build();
		Trigger trigger = newTrigger().withIdentity("staleTrigger", "mainGroup")
				.withSchedule(cronSchedule("0 * * * * ?"))
				.forJob("staleJob", "mainGroup").build();
		JobDetail job2 = newJob(SplitJob.class).withIdentity("splitJob", "mainGroup").build();
		Trigger trigger2 = newTrigger().withIdentity("splitTrigger", "mainGroup")
				.withSchedule(cronSchedule("0 0 * * * ?"))
				.forJob("splitJob", "mainGroup").build();
		JobDetail job3 = newJob(ExportJob.class).withIdentity("exportJob", "mainGroup").build();
		Trigger trigger3 = newTrigger().withIdentity("exportTrigger", "mainGroup")
				.withSchedule(cronSchedule("0 15 */3 * * ?"))
				.forJob("exportJob", "mainGroup").build();
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();

		Scheduler sched;
		try {
			sched = schedFact.getScheduler();
			sched.start();
			sched.scheduleJob(job, trigger);
			sched.scheduleJob(job2, trigger2);
			sched.scheduleJob(job3, trigger3);
			
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
}
