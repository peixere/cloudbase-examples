package cn.gotom;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.gotom.commons.Note;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ThreadExecutor {

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			log.debug(e.getMessage());
		}
	}

	private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

	public static String executorService() {
		return executorService.toString();
	}

	public static void execute(Runnable object) {
		try {
			executorService.execute(object);
		} catch (RejectedExecutionException ex) {
			log.error(ex.getMessage());
		} catch (Throwable ex) {
			log.error("", ex);
		}
	}

	public static void shutdown() {
		executorService.shutdown();
	}

	@Note("定时任务执行器")
	@Bean
	public ScheduledExecutorService scheduledExecutor() {
		return executorService;
	}
}
