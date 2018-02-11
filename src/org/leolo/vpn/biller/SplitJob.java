package org.leolo.vpn.biller;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SplitJob implements Job{
	Logger logger = LoggerFactory.getLogger(SplitJob.class);
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		logger.info("Job started");
		UsageCache.getInstance().split();
	}
}
