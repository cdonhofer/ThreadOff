package net.donhofer.fun.threadoff.calc;

import javafx.scene.shape.Shape;

import java.util.List;
import java.util.concurrent.CompletionService;

public sealed abstract class ThreadOffCalc permits BlockingTask, KochFlakeTask, PrimesCalc {
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
}
