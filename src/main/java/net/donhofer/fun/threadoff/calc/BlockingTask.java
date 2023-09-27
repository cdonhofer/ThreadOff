package net.donhofer.fun.threadoff.calc;

import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.stream.IntStream;

public final class BlockingTask extends ThreadOffCalc {

    public BlockingTask(CompletionService<List<Shape>> executorService, int multiplicity) {
        super(executorService, multiplicity);
    }

    @Override
    public InitialData getTasks() {
        List<SleepCallable> initialTasks = new ArrayList<>(multiplicity);
        IntStream.range(0, multiplicity).forEach(it -> initialTasks.add(new SleepCallable()));

        return new InitialData(multiplicity, initialTasks);
    }

    static class SleepCallable implements Callable<List<Shape>> {
        @Override
        public List<Shape> call() throws Exception {
            Thread.sleep(10);
            return Collections.emptyList();
        }
    }
}
