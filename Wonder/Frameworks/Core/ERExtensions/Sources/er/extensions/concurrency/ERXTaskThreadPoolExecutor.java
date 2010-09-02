package er.extensions.concurrency;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import er.extensions.eof.ERXEC;

/**
 * This is a custom {@link ThreadPoolExecutor} subclass whose purpose in life is 
 * (1) to ensure that we initialize {@link ERXTaskThread} status before task execution and reset status after execution,
 * (2) use ERXFutureTask subclass of {@link FutureTask} so we have a reference to the wrapped task.
 * (3) tell ERXEC to unlock all editing contexts in the background thread at the end of task execution.
 * 
 * This is accomplished by overriding the protected hook methods {@link ThreadPoolExecutor#beforeExecute(Thread t, Runnable r)}
 * and {@link ThreadPoolExecutor#afterExecute(Runnable r, Throwable t)}, and also the submit methods.
 * 
 * @author kieran
 *
 */
public class ERXTaskThreadPoolExecutor extends ThreadPoolExecutor {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(ERXTaskThreadPoolExecutor.class);

	public ERXTaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
					BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public ERXTaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
					BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	}

	public ERXTaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
					BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	public ERXTaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
					BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}
	
    @Override
	public Future<?> submit(Runnable task) {
        if (task == null) throw new NullPointerException();
        ERXFutureTask<Object> ftask = new ERXFutureTask<Object>(task, null);
        execute(ftask);
        return ftask;
    }

    @Override
	public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        ERXFutureTask<T> ftask = new ERXFutureTask<T>(task, result);
        execute(ftask);
        return ftask;
    }

    @Override
	public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        ERXFutureTask<T> ftask = new ERXFutureTask<T>(task);
        execute(ftask);
        return ftask;
    }

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		// Store reference to the task
		if (t instanceof ERXTaskThread) {
			((ERXTaskThread)t).setTask(r);
			((ERXTaskThread)t).startStopWatch();
			if (log.isDebugEnabled()) {
				log.debug("About to execute " + (r == null ? "null" : r) + " in thread " + t);
			}
		}

		if (r instanceof ERXExecutionStateTransition) {
			((ERXExecutionStateTransition)r).beforeExecute();
		} //~ if (r instanceof ERXExecutionStateTransition)

		super.beforeExecute(t, r);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);

		// Clear reference to the task
		if (Thread.currentThread() instanceof ERXTaskThread) {
			ERXTaskThread thread = (ERXTaskThread)Thread.currentThread();
			thread.setTask(null);
			thread.stopStopWatch();
			String elapsedTime = thread.elapsedTime();
			if (log.isDebugEnabled())
				log.debug("Finished executing " + (r == null ? "null" : r) + " after " + elapsedTime);
		}

		if (r instanceof ERXExecutionStateTransition) {
			((ERXExecutionStateTransition)r).afterExecute();
		} //~ if (r instanceof ERXExecutionStateTransition)
		
		// Safety net to unlock any locked EC's at the end of this task's operation in this thread
		ERXEC.unlockAllContextsForCurrentThread();
	}

}
