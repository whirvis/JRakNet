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

import java.util.function.Consumer;

/**
 * Represents a task that is executed by the {@link SchedulerThread} after being
 * scheduled by the {@link Scheduler}.
 * 
 * @author Whirvis T. Wheatley
 * @since JRakNet v2.11.0
 * @param <T>
 *            the type of the input to the operation.
 */
public final class ScheduledTask<T> {

	/**
	 * The task will execute as soon as possible.
	 */
	public static final long INSTANTANEOUS = -1L;

	/**
	 * The task will repeat indefinitely.
	 */
	public static final int INFINITE = -1;

	private final boolean sync;
	private final T obj;
	private final Consumer<T> operation;
	private final long wait;
	private int count;
	private long lastExecute;
	private boolean executing;

	/**
	 * Creates scheduled task.
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
	 *            executed immediately, use {@link #INSTANTANEOUS}.
	 * @param count
	 *            the amount of times the task should be executed. To have the
	 *            task be executed indefinitely, use {@link #INFINITE}.
	 * @throws NullPointerException
	 *             if the operation is <code>null</code>.
	 * @throws IllegalArgumentException
	 *             if the execution <code>count</code> is less than zero and not
	 *             equal to {@value #INFINITE}.
	 */
	protected ScheduledTask(boolean sync, T obj, Consumer<T> operation, long wait, int count)
			throws NullPointerException, IllegalArgumentException {
		if (operation == null) {
			throw new NullPointerException("Consumer operation cannot be null");
		} else if (count <= 0 && count != INFINITE) {
			throw new IllegalArgumentException(
					"Execution count must be greater than zero or equal to negative one (for infinite executions)");
		}
		this.sync = sync;
		this.obj = obj;
		this.operation = operation;
		this.wait = wait;
		this.count = count;
		this.lastExecute = System.currentTimeMillis();
	}

	/**
	 * Returns whether or not the task is finished. This does <i>not</i> mean
	 * that the task has finished executing, but rather that the task has
	 * finished every execution it has.
	 * 
	 * @return <code>true</code> if the task has been finished,
	 *         <code>false</code> otherwise.
	 */
	protected boolean isFinished() {
		return count <= 0 && count != INFINITE;
	}

	/**
	 * Returns whether or not the task should be executed.
	 * 
	 * @return <code>true</code> if the task should be executed,
	 *         <code>false</code> otherwise.
	 * @see #isFinished()
	 * @see #execute()
	 */
	protected boolean shouldExecute() {
		if (this.isFinished()) {
			return false; // Task executions have been depleted
		} else if (sync == true && executing == true) {
			return false; // The task is already being executed
		}
		return System.currentTimeMillis() - lastExecute > wait;
	}

	/**
	 * Executes the task.
	 * 
	 * @throws IllegalStateException
	 *             if the state cannot be executed according to
	 *             {@link #shouldExecute()}.
	 */
	protected void execute() throws IllegalStateException {
		if (!this.shouldExecute()) {
			throw new IllegalStateException("Task cannot be executed");
		}

		// Update execution state
		this.lastExecute = System.currentTimeMillis();
		if (count != INFINITE) {
			this.count--;
		}

		// Execute command
		this.executing = true;
		operation.accept(obj);
		this.executing = false;
	}

}
