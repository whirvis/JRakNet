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
 * Copyright (c) 2016-2019 Trent Summerlin
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

import java.util.UUID;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.whirvis.jraknet.map.concurrent.ConcurrentLongMap;

/**
 * Used to execute tasks in the background without taking up the main thread.
 * 
 * @author Trent Summerlin
 * @since JRakNet v2.11.0
 * @see #scheduleSync(Object, Consumer, long, boolean)
 * @see #scheduleAsync(Object, Consumer, long, boolean)
 */
public class Scheduler extends Thread {

	private static final Logger LOG = LogManager.getLogger("jraknet-scheduler");
	protected static final ConcurrentLongMap<ScheduledTask<?>> TASKS = new ConcurrentLongMap<ScheduledTask<?>>();
	protected static SchedulerThread thread = null;

	private Scheduler() {
		// Static class
	}

	/**
	 * Schedules a task. All tasks will be executed on their own thread, so as
	 * to prevent other tasks from being prevented from running.
	 * 
	 * @param sync
	 *            <code>true</code> if each task execution must finish before
	 *            the next execution can begin, <code>false</code> otherwise.
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param wait
	 *            how long to wait before initially executing the task in
	 *            milliseconds, and how long to wait before executing the task
	 *            again if it executes more than once. To have the task be
	 *            executed immediately, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INSTANTANEOUS
	 *            INSTANTANEOUS}.
	 * @param count
	 *            the amount of times the task should be executed. To have the
	 *            task be executed indefinitely, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INFINITE
	 *            INFINITE}.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @throws NullPointerException
	 *             if the command is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the execution count is less than zero and not equal to
	 *             {@value com.whirvis.jraknet.scheduler.ScheduledTask#INFINITE}.
	 * @return the task ID.
	 */
	private static synchronized <T> long schedule(boolean sync, T obj, Consumer<T> operation, long wait, int count)
			throws NullPointerException, IllegalArgumentException {
		long taskId = UUID.randomUUID().getMostSignificantBits();
		TASKS.put(taskId, new ScheduledTask<T>(sync, obj, operation, wait, count));
		if (thread == null) {
			thread = new SchedulerThread();
			thread.start();
		}
		LOG.debug("Scheduled " + (sync ? "synchronous" : "asynchronous") + " task with ID " + Long.toHexString(taskId));
		return taskId;
	}

	/**
	 * Schedules a task. All tasks will be executed on their own thread, so as
	 * to prevent other tasks from being prevented from running. If the task
	 * executes multiple times however, the next execution will not begin until
	 * the one before it is finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param wait
	 *            how long to wait before initially executing the task, and how
	 *            long to wait before executing the task again if it executes
	 *            more than once. To have the task be executed immediately, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INSTANTANEOUS
	 *            INSTANTANEOUS}.
	 * @param count
	 *            the amount of times the task should be executed. To have the
	 *            task be executed indefinitely, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INFINITE
	 *            INFINITE}.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the execution count is less than zero and not equal to
	 *             {@value com.whirvis.jraknet.scheduler.ScheduledTask#INFINITE}.
	 * @see #scheduleAsync(Object, Consumer, long, int)
	 */
	public static <T> long scheduleSync(T obj, Consumer<T> operation, long wait, int count)
			throws NullPointerException, IllegalArgumentException {
		return schedule(true, obj, operation, wait, count);
	}

	/**
	 * Schedules a task. All tasks will be executed on their own thread, so as
	 * to prevent other tasks from being prevented from running. If the task
	 * executes multiple times however, the next execution will not begin until
	 * the one before it is finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param wait
	 *            how long to wait before initially executing the task, and how
	 *            long to wait before executing the task again if it executes
	 *            more than once. To have the task be executed immediately, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INSTANTANEOUS
	 *            INSTANTANEOUS}.
	 * @param repeat
	 *            <code>true</code> if the task should indefinitely repeat,
	 *            <code>false</code> otherwise.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @see #schduleAsync(Object, Consumer, long, boolean)
	 */
	public static <T> long scheduleSync(T obj, Consumer<T> operation, long wait, boolean repeat)
			throws NullPointerException {
		return scheduleSync(obj, operation, wait, repeat ? ScheduledTask.INFINITE : 1);
	}

	/**
	 * Schedules a task to be executed once. All tasks will be executed on their
	 * own thread, so as to prevent other tasks from being prevented from
	 * running. If the task executes multiple times however, the next execution
	 * will not begin until the one before it is finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param wait
	 *            how long to wait before initially executing the task, and how
	 *            long to wait before executing the task again if it executes
	 *            more than once. To have the task be executed immediately, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INSTANTANEOUS
	 *            INSTANTANEOUS}.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @see #scheduleAsync(Object, Consumer, long)
	 */
	public static <T> long scheduleSync(T obj, Consumer<T> operation, long wait) throws NullPointerException {
		return scheduleSync(obj, operation, wait, 1);
	}

	/**
	 * Schedules a task to be executed instantaneously. All tasks will be
	 * executed on their own thread, so as to prevent other tasks from being
	 * prevented from running. If the task executes multiple times however, the
	 * next execution will not begin until the one before it is finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param count
	 *            the amount of times the task should be executed. To have the
	 *            task be executed indefinitely, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INFINITE
	 *            INFINITE}.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the execution count is less than zero and not equal to
	 *             {@value com.whirvis.jraknet.scheduler.ScheduledTask#INFINITE}.
	 * @see #scheduleAsync(Object, Consumer, int)
	 */
	public static <T> long scheduleSync(T obj, Consumer<T> operation, int count)
			throws NullPointerException, IllegalArgumentException {
		return scheduleSync(obj, operation, ScheduledTask.INSTANTANEOUS, count);
	}

	/**
	 * Schedules a task to be executed instantaneously. All tasks will be
	 * executed on their own thread, so as to prevent other tasks from being
	 * prevented from running. If the task executes multiple times however, the
	 * next execution will not begin until the one before it is finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param wait
	 *            how long to wait before initially executing the task, and how
	 *            long to wait before executing the task again if it executes
	 *            more than once. To have the task be executed immediately, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INSTANTANEOUS
	 *            INSTANTANEOUS}.
	 * @param repeat
	 *            <code>true</code> if the task should indefinitely repeat,
	 *            <code>false</code> otherwise.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @see #scheduleAsync(Object, Consumer, boolean)
	 */
	public static <T> long scheduleSync(T obj, Consumer<T> operation, boolean repeat) throws NullPointerException {
		return scheduleSync(obj, operation, ScheduledTask.INSTANTANEOUS, repeat ? ScheduledTask.INFINITE : 1);
	}

	/**
	 * Schedules a task to be executed once instantaneously. All tasks will be
	 * executed on their own thread, so as to prevent other tasks from being
	 * prevented from running. If the task executes multiple times however, the
	 * next execution will not begin until the one before it is finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @see #scheduleAsync(Object, Consumer)
	 */
	public static <T> long scheduleSync(T obj, Consumer<T> operation) throws NullPointerException {
		return scheduleSync(obj, operation, ScheduledTask.INSTANTANEOUS, 1);
	}

	/**
	 * Schedules a task. All tasks will be executed on their own thread, so as
	 * to prevent other tasks from being prevented from running. If the task
	 * executes multiple times however, the next execution will begin even if
	 * the one before it is not finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param wait
	 *            how long to wait before initially executing the task, and how
	 *            long to wait before executing the task again if it executes
	 *            more than once. To have the task be executed immediately, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INSTANTANEOUS
	 *            INSTANTANEOUS}.
	 * @param count
	 *            the amount of times the task should be executed. To have the
	 *            task be executed indefinitely, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INFINITE
	 *            INFINITE}.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the execution count is less than zero and not equal to
	 *             {@value com.whirvis.jraknet.scheduler.ScheduledTask#INFINITE}.
	 * @see #scheduleSync(Object, Consumer, long, int)
	 */
	public static <T> long scheduleAsync(T obj, Consumer<T> operation, long wait, int count)
			throws NullPointerException, IllegalArgumentException {
		return schedule(false, obj, operation, wait, count);
	}

	/**
	 * Schedules a task. All tasks will be executed on their own thread, so as
	 * to prevent other tasks from being prevented from running. If the task
	 * executes multiple times however, the next execution will begin even if
	 * the one before it is not finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param wait
	 *            how long to wait before initially executing the task, and how
	 *            long to wait before executing the task again if it executes
	 *            more than once. To have the task be executed immediately, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INSTANTANEOUS
	 *            INSTANTANEOUS}.
	 * @param repeat
	 *            <code>true</code> if the task should indefinitely repeat,
	 *            <code>false</code> otherwise.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @see #scheduleSync(Object, Consumer, long, boolean)
	 */
	public static <T> long scheduleAsync(T obj, Consumer<T> operation, long wait, boolean repeat)
			throws NullPointerException {
		return scheduleAsync(obj, operation, wait, repeat ? ScheduledTask.INFINITE : 1);
	}

	/**
	 * Schedules a task to be executed once. All tasks will be executed on their
	 * own thread, so as to prevent other tasks from being prevented from
	 * running. If the task executes multiple times however, the next execution
	 * will begin even if the one before it is not finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param wait
	 *            how long to wait before initially executing the task, and how
	 *            long to wait before executing the task again if it executes
	 *            more than once.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @see #scheduleSync(Object, Consumer, long)
	 */
	public static <T> long scheduleAsync(T obj, Consumer<T> operation, long wait) throws NullPointerException {
		return scheduleAsync(obj, operation, wait, 1);
	}

	/**
	 * Schedules a task to be executed instantaneously. All tasks will be
	 * executed on their own thread, so as to prevent other tasks from being
	 * prevented from running. If the task executes multiple times however, the
	 * next execution will begin even if the one before it is not finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param count
	 *            the amount of times the task should be executed. To have the
	 *            task be executed indefinitely, use
	 *            {@link com.whirvis.jraknet.scheduler.ScheduledTask#INFINITE
	 *            INFINITE}.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the execution count is less than zero and not equal to
	 *             {@value com.whirvis.jraknet.scheduler.ScheduledTask#INFINITE}.
	 * @see #scheduleSync(Object, Consumer, int)
	 */
	public static <T> long scheduleAsync(T obj, Consumer<T> operation, int count)
			throws NullPointerException, IllegalArgumentException {
		return scheduleAsync(obj, operation, ScheduledTask.INSTANTANEOUS, count);
	}

	/**
	 * Schedules a task to be executed instantaneously. All tasks will be
	 * executed on their own thread, so as to prevent other tasks from being
	 * prevented from running. If the task executes multiple times however, the
	 * next execution will begin even if the one before it is not finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param wait
	 *            how long to wait before initially executing the task, and how
	 *            long to wait before executing the task again if it executes
	 *            more than once.
	 * @param repeat
	 *            <code>true</code> if the task should indefinitely repeat,
	 *            <code>false</code> otherwise.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @see #scheuleSync(Object, Consumer, boolean)
	 */
	public static <T> long scheduleAsync(T obj, Consumer<T> operation, boolean repeat) throws NullPointerException {
		return scheduleAsync(obj, operation, ScheduledTask.INSTANTANEOUS, repeat ? ScheduledTask.INFINITE : 1);
	}

	/**
	 * Schedules a task to be executed once instantaneously. All tasks will be
	 * executed on their own thread, so as to prevent other tasks from being
	 * prevented from running. If the task executes multiple times however, the
	 * next execution will begin even if the one before it is not finished.
	 * 
	 * @param obj
	 *            the object to execute the operation on.
	 * @param operation
	 *            the consumer operation to execute.
	 * @param <T>
	 *            the type of the input to the operation.
	 * @return the task ID.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @see #scheduleSync(Object, Consumer)
	 */
	public static <T> long scheduleAsync(T obj, Consumer<T> operation) throws NullPointerException {
		return scheduleAsync(obj, operation, ScheduledTask.INSTANTANEOUS, 1);
	}

	/**
	 * Cancels a task. This allows for infinitely repeating tasks to be ended or
	 * to prevent a task from ever executing in the first place after it has
	 * been scheduled.
	 * 
	 * @param taskId
	 *            the ID of the task to cancel.
	 * @return <code>true</code> if the task was successfully cancelled,
	 *         <code>false</code> otherwise.
	 */
	public static boolean cancelTask(long taskId) {
		if (TASKS.remove(taskId) != null) {
			LOG.debug("Cancelled task with ID " + Long.toHexString(taskId));
			return true;
		}
		return false;
	}

}
