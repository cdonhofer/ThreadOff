package net.donhofer.fun.threadoff.calc;

import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.data.InitialData;

import java.util.List;
import java.util.concurrent.CompletionService;

public sealed abstract class ThreadOffCalc permits BlockingTask, KochFlakeTask, LockResourceTask, PrimesCalc, SierpinskiTask, SyncResourceTask, SyncSemaphoreTask {
    protected final CompletionService<List<Shape>> completionService;
    protected final int multiplicity;

    public ThreadOffCalc(CompletionService<List<Shape>> executorService, int multiplicity) {
        this.completionService = executorService;
        this.multiplicity = multiplicity;
    }

    /**
     * build the initial tasks and calculate the expected number of total tasks, including subsequent
     * ones; then return that data, so the calling thread can decide when to start the process by itself
     * and possibly take precautions according to the data it receives
     * @return the initial data, tasks and the number of expected total tasks
     */
    public abstract InitialData getTasks();

    /**
     * suggested size for the platform thread pool when running this task
     * @return the suggested size
     */
    public static int getThreadPoolSize() {
        // assuming zero calculation time for the default + 10 ms waiting time
        int wait = 10;
        int duration = 1;
        return Runtime.getRuntime().availableProcessors() * (1 + (wait / duration));
    }
}
