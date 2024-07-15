package lab.scheduler.core;

import org.quartz.SchedulerConfigException;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResizableSimpleThreadPool implements ThreadPool {

    private int count = 1;
    private int maxThreadCount = 300;
    private int prio = Thread.NORM_PRIORITY;
    private boolean isShutdown = false;
    private boolean handoffPending = false;
    private boolean inheritLoader = false;
    private boolean inheritGroup = true;
    private ThreadGroup threadGroup;
    private final Object nextRunnableLock = new Object();
    private List<WorkerThread> workers;
    private LinkedList<WorkerThread> availWorkers = new LinkedList<WorkerThread>();
    private LinkedList<WorkerThread> busyWorkers = new LinkedList<WorkerThread>();
    private String threadNamePrefix;
    private String schedulerInstanceName;
    private String schedulerInstanceId;
    private int lastIdNum = 0;
    private boolean minSizeReached = false;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public ResizableSimpleThreadPool() {}

    public ResizableSimpleThreadPool(int threadCount, int threadPriority) {
        setThreadCount(threadCount);
        setThreadPriority(threadPriority);
    }

    public Logger getLog() {
        return log;
    }

    public int getPoolSize() {
        return getThreadCount();
    }

    public void setThreadCount(int count) {
        this.count = count;
        if (this.count > maxThreadCount) {
            maxThreadCount = this.count;
        }
    }

    public int getThreadCount() {
        return count;
    }

    public void setThreadPriority(int prio) {
        this.prio = prio;
    }

    public int getThreadPriority() {
        return prio;
    }

    public void setThreadNamePrefix(String prfx) {
        this.threadNamePrefix = prfx;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public boolean isThreadsInheritContextClassLoaderOfInitializingThread() {
        return inheritLoader;
    }

    public void setThreadsInheritContextClassLoaderOfInitializingThread(
            boolean inheritLoader) {
        this.inheritLoader = inheritLoader;
    }

    public boolean isThreadsInheritGroupOfInitializingThread() {
        return inheritGroup;
    }

    public void setThreadsInheritGroupOfInitializingThread(
            boolean inheritGroup) {
        this.inheritGroup = inheritGroup;
    }

    public void setInstanceId(String schedulerInstanceId) {
        this.schedulerInstanceId = schedulerInstanceId;
    }

    public void setInstanceName(String schedulerInstanceName) {
        this.schedulerInstanceName = schedulerInstanceName;
    }

    public void setMaxThreadCount(int maxThreadCount) {
        if (maxThreadCount < 1) {
            throw new IllegalArgumentException("maxThreadCount must be greater than 1");
        }
        this.maxThreadCount = maxThreadCount;
    }

    public int getMaxThreadCount() {
        return maxThreadCount;
    }

    public void initialize() throws SchedulerConfigException {

        if(workers != null && workers.size() > 0) // already initialized...
            return;

        if (count <= 0) {
            throw new SchedulerConfigException(
                    "Thread count must be > 0");
        }

        if (prio <= 0 || prio > 9) {
            throw new SchedulerConfigException(
                    "Thread priority must be > 0 and <= 9");
        }

        if(isThreadsInheritGroupOfInitializingThread()) {
            threadGroup = Thread.currentThread().getThreadGroup();
        } else {
            // follow the threadGroup tree to the root thread group.
            threadGroup = Thread.currentThread().getThreadGroup();
            ThreadGroup parent = threadGroup;
            while ( !parent.getName().equals("main") ) {
                threadGroup = parent;
                parent = threadGroup.getParent();
            }
            threadGroup = new ThreadGroup(parent, schedulerInstanceName + "-ResizableSimpleThreadPool");
        }

        if (isThreadsInheritContextClassLoaderOfInitializingThread()) {
            getLog().info(
                    "Job execution threads will use class loader of thread: "
                            + Thread.currentThread().getName());
        }

        // create the worker threads and start them
        Iterator<WorkerThread> workerThreads = createWorkerThreads(count).iterator();
        while(workerThreads.hasNext()) {
            WorkerThread wt = workerThreads.next();
            wt.start();
            availWorkers.add(wt);
        }
        ResizableSimpleThreadPoolManager.getInstance().register(schedulerInstanceId, this);
    }

    protected List<WorkerThread> createWorkerThreads(int createCount) {
        if (workers == null) {
            workers = new LinkedList<WorkerThread>();
        }
        List<WorkerThread> newWorkerThreads = new LinkedList<>();
        for (int i = 1; i<= createCount; ++i) {
            String threadPrefix = getThreadNamePrefix();
            if (threadPrefix == null) {
                threadPrefix = schedulerInstanceName + "_Worker";
            }
            WorkerThread wt = new WorkerThread(this, threadGroup,
                    threadPrefix + "-" + (++lastIdNum),
                    getThreadPriority());
            if (isThreadsInheritContextClassLoaderOfInitializingThread()) {
                wt.setContextClassLoader(Thread.currentThread()
                        .getContextClassLoader());
            }
            newWorkerThreads.add(wt);
        }
        workers.addAll(newWorkerThreads);

        return newWorkerThreads;
    }

    int addWorkerThread(int createCount) {
        if(workers == null) {
            getLog().info("The thread pool " + ResizableSimpleThreadPool.class.getName() + " is not initialized");
            return 0;
        }

        if (minSizeReached) { //This flag means that all jobs of this pool were removed and the current pool size is 1.
            createCount = createCount - 1; //Prevent the increase of unnecessary thread.
            if (createCount >= 0) {
                minSizeReached = false; //Change flag to false only when the add size is greater or equal than 0.
            }
            if (createCount == 0) { //The createCount '0' at this 'if block' means that it originally '1'. The pool already has 1 thread in it, so no necessity to add.
                return 0;
            }
        }

        if (createCount <= 0) {
            return 0;
        }

        if ((workers.size() + createCount) > maxThreadCount) {
            getLog().warn("Max thread count reached. Current threads: " + workers.size() + ", Count to add: " + createCount + ", Max thread count: " + maxThreadCount);
            return 0;
        }

        Iterator<WorkerThread> addedWorkerThreads = createWorkerThreads(createCount).iterator();
        while(addedWorkerThreads.hasNext()) {
            WorkerThread wt = addedWorkerThreads.next();
            wt.start();
            availWorkers.add(wt);
        }
        getLog().info("Added " + createCount + " workers to the pool");
        return createCount;
    }

    int removeWorkerThread(int removeCount) {
        int currentSize = workers.size();
        if (removeCount >= currentSize) {
            removeCount = currentSize - 1; //Thread pool must have 1 thread at least.
        }

        if (currentSize == 1) { //Thread pool must have 1 thread at least.
            minSizeReached = true; //This flag means that all jobs of this pool were removed.
            return 0;
        }

        if (removeCount <= 0) {
            return 0;
        }

        int removedCount = 0;
        synchronized (nextRunnableLock) {
            getLog().info("Removing " + removeCount + " workers from the pool");
            if (workers == null)
                return 0;

            Iterator<WorkerThread> availThreads = availWorkers.iterator();
            while(availThreads.hasNext()) {
                if (removedCount == removeCount) {
                    getLog().info("Removed " + removeCount + " workers from the pool");
                    nextRunnableLock.notifyAll();
                    break;
                }
                WorkerThread wt = availThreads.next();
                wt.shutdown();
                availWorkers.remove(wt);
                ++removedCount;
            }
        }
        return removedCount;
    }


    public void shutdown() {
        shutdown(true);
    }

    public void shutdown(boolean waitForJobsToComplete) {

        synchronized (nextRunnableLock) {
            getLog().debug("Shutting down threadpool...");

            isShutdown = true;

            if(workers == null) // case where the pool wasn't even initialize()ed
                return;

            // signal each worker thread to shut down
            Iterator<WorkerThread> workerThreads = workers.iterator();
            while(workerThreads.hasNext()) {
                WorkerThread wt = workerThreads.next();
                wt.shutdown();
                availWorkers.remove(wt);
            }

            // Give waiting (wait(1000)) worker threads a chance to shut down.
            // Active worker threads will shut down after finishing their
            // current job.
            nextRunnableLock.notifyAll();

            if (waitForJobsToComplete == true) {

                boolean interrupted = false;
                try {
                    // wait for hand-off in runInThread to complete...
                    while(handoffPending) {
                        try {
                            nextRunnableLock.wait(100);
                        } catch(InterruptedException e) {
                            interrupted = true;
                        }
                    }

                    // Wait until all worker threads are shut down
                    while (busyWorkers.size() > 0) {
                        WorkerThread wt = (WorkerThread) busyWorkers.getFirst();
                        try {
                            getLog().debug(
                                    "Waiting for thread " + wt.getName()
                                            + " to shut down");

                            // note: with waiting infinite time the
                            // application may appear to 'hang'.
                            nextRunnableLock.wait(2000);
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    }

                    workerThreads = workers.iterator();
                    while(workerThreads.hasNext()) {
                        WorkerThread wt = (WorkerThread) workerThreads.next();
                        try {
                            wt.join();
                            workerThreads.remove();
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    }
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
                getLog().debug("No executing jobs remaining, all threads stopped.");
            }
            getLog().debug("Shutdown of threadpool complete.");
        }
        ResizableSimpleThreadPoolManager.getInstance().remove(schedulerInstanceName);
    }

    /**
     * <p>
     * Run the given <code>Runnable</code> object in the next available
     * <code>Thread</code>. If while waiting the thread pool is asked to
     * shut down, the Runnable is executed immediately within a new additional
     * thread.
     * </p>
     *
     * @param runnable
     *          the <code>Runnable</code> to be added.
     */
    public boolean runInThread(Runnable runnable) {
        if (runnable == null) {
            return false;
        }

        synchronized (nextRunnableLock) {

            handoffPending = true;

            // Wait until a worker thread is available
            while ((availWorkers.size() < 1) && !isShutdown) {
                try {
                    nextRunnableLock.wait(500);
                } catch (InterruptedException ignore) {
                }
            }

            if (!isShutdown) {
                WorkerThread wt = (WorkerThread)availWorkers.removeFirst();
                busyWorkers.add(wt);
                wt.run(runnable);
            } else {
                // If the thread pool is going down, execute the Runnable
                // within a new additional worker thread (no thread from the pool).
                WorkerThread wt = new WorkerThread(this, threadGroup,
                        "WorkerThread-LastJob", prio, runnable);
                busyWorkers.add(wt);
                workers.add(wt);
                wt.start();
            }
            nextRunnableLock.notifyAll();
            handoffPending = false;
        }

        return true;
    }

    public int blockForAvailableThreads() {
        synchronized(nextRunnableLock) {

            while((availWorkers.size() < 1 || handoffPending) && !isShutdown) {
                try {
                    nextRunnableLock.wait(500);
                } catch (InterruptedException ignore) {
                }
            }

            return availWorkers.size();
        }
    }

    protected void makeAvailable(WorkerThread wt) {
        synchronized(nextRunnableLock) {
            if(!isShutdown) {
                availWorkers.add(wt);
            }
            busyWorkers.remove(wt);
            nextRunnableLock.notifyAll();
        }
    }

    protected void clearFromBusyWorkersList(WorkerThread wt) {
        synchronized(nextRunnableLock) {
            busyWorkers.remove(wt);
            nextRunnableLock.notifyAll();
        }
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *
     * WorkerThread Class.
     *
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * A Worker loops, waiting to execute tasks.
     * </p>
     */
    class WorkerThread extends Thread {

        private final Object lock = new Object();

        // A flag that signals the WorkerThread to terminate.
        private AtomicBoolean run = new AtomicBoolean(true);

        private ResizableSimpleThreadPool tp;

        private Runnable runnable = null;

        private boolean runOnce = false;

        /**
         * <p>
         * Create a worker thread and start it. Waiting for the next Runnable,
         * executing it, and waiting for the next Runnable, until the shutdown
         * flag is set.
         * </p>
         */
        WorkerThread(ResizableSimpleThreadPool tp, ThreadGroup threadGroup, String name,
                     int prio) {

            this(tp, threadGroup, name, prio, null);
        }

        /**
         * <p>
         * Create a worker thread, start it, execute the runnable and terminate
         * the thread (one time execution).
         * </p>
         */
        WorkerThread(ResizableSimpleThreadPool tp, ThreadGroup threadGroup, String name,
                     int prio, Runnable runnable) {

            super(threadGroup, name);
            this.tp = tp;
            this.runnable = runnable;
            if(runnable != null)
                runOnce = true;
            setPriority(prio);
        }

        /**
         * <p>
         * Signal the thread that it should terminate.
         * </p>
         */
        void shutdown() {
            run.set(false);
        }

        public void run(Runnable newRunnable) {
            synchronized(lock) {
                if(runnable != null) {
                    throw new IllegalStateException("Already running a Runnable!");
                }

                runnable = newRunnable;
                lock.notifyAll();
            }
        }

        @Override
        public void run() {
            boolean ran = false;

            while (run.get()) {
                try {
                    synchronized(lock) {
                        while (runnable == null && run.get()) {
                            lock.wait(500);
                        }

                        if (runnable != null) {
                            ran = true;
                            runnable.run();
                        }
                    }
                } catch (InterruptedException unblock) {
                    // do nothing (loop will terminate if shutdown() was called
                    try {
                        getLog().error("Worker thread was interrupt()'ed.", unblock);
                    } catch(Exception e) {
                        // ignore to help with a tomcat glitch
                    }
                } catch (Throwable exceptionInRunnable) {
                    try {
                        getLog().error("Error while executing the Runnable: ",
                                exceptionInRunnable);
                    } catch(Exception e) {
                        // ignore to help with a tomcat glitch
                    }
                } finally {
                    synchronized(lock) {
                        runnable = null;
                    }
                    // repair the thread in case the runnable mucked it up...
                    if(getPriority() != tp.getThreadPriority()) {
                        setPriority(tp.getThreadPriority());
                    }

                    if (runOnce) {
                        run.set(false);
                        clearFromBusyWorkersList(this);
                    } else if(ran) {
                        ran = false;
                        makeAvailable(this);
                    }

                }
            }

            //if (log.isDebugEnabled())
            try {
                getLog().debug("WorkerThread is shut down.");
            } catch(Exception e) {
                // ignore to help with a tomcat glitch
            }
        }
    }
}