/*
 *       _   _____            _      _   _          _
 *      | | |  __ \          | |    | \ | |        | |
 *      | | | |__) |   __ _  | | __ |  \| |   ___  | |_
 *  _   | | |  _  /   / _` | | |/ / | . ` |  / _ \ | __|
 * | |__| | | | \ \  | (_| | |   <  | |\  | |  __/ | |_
 *  \____/  |_|  \_\  \__,_| |_|\_\ |_| \_|  \___|  \__|
 *
 * the MIT License (MIT)
 *
 * Copyright (c) 2016-2019 Whirvis T. Wheatley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * the above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.whirvis.jraknet.scheduler;

import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Used by the {@link com.whirvis.jraknet.scheduler.Scheduler Scheduler} to run
 * tasks in the background at specified intervals.
 * <p>
 * Only one instance of this class can exist at a time. When a task is
 * scheduled, the scheduler will automatically check if a scheduler thread is
 * already running. If none exists, one will be created and started
 * automatically. Once the last scheduled task has finished executing, the
 * scheduler thread will shutdown and nullify its own reference in the
 * scheduler. If another task is scheduled after this, the process will repeat.
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.11.0
 * @see com.whirvis.jraknet.scheduler.Scheduler Scheduler
 * @see com.whirvis.jraknet.scheduler.ScheduledTask SchedulerTask
 */
public class SchedulerThread extends Thread {

	private final Logger log;

	/**
	 * Allocates a scheduler thread.
	 */
	protected SchedulerThread() {
		this.log = LogManager.getLogger("jraknet-scheduler");
		this.setName(log.getName());
	}

	@Override
	public void run() {
		log.debug("Started scheduler thread");
		while (Scheduler.TASKS.size() > 0 && Scheduler.thread == this && !this.isInterrupted()) {
			try {
				Thread.sleep(0, 1); // Save CPU usage
			} catch (InterruptedException e) {
				this.interrupt(); // Interrupted during sleep
			}

			// Execute tasks
			Iterator<Long> taskIds = Scheduler.TASKS.keySet().iterator();
			while (taskIds.hasNext()) {
				// Get task and determine if it should be executed
				long taskId = taskIds.next();
				ScheduledTask<?> task = Scheduler.TASKS.get(taskId);
				if (task.isFinished()) {
					log.debug("Scheduler task " + Long.toHexString(taskId).toLowerCase() + " has finished executing");
					taskIds.remove();
					continue; // Task is finished executing
				} else if (!task.shouldExecute()) {
					continue; // Task is not ready to execute
				}

				// Execute task
				Thread schedulerTask = new Thread("scheduler-task-" + Long.toHexString(taskId).toLowerCase()) {
					@Override
					public void run() {
						task.execute();
					}
				};
				schedulerTask.start();
				log.debug("Executed scheduler task thread \"" + schedulerTask.getName() + "\"");
			}
		}

		/*
		 * If there are no tasks left we will destroy this thread by nullifying
		 * the scheduler's reference after the loop has been broken out of. If
		 * there are more tasks that must be run, then a new scheduler thread
		 * will be created automatically.
		 */
		if (Scheduler.thread == this) {
			Scheduler.thread = null;
			log.debug("Terminated scheduler thread");
		}
	}

}
