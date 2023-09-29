package net.donhofer.fun.threadoff.calc;

import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.data.InitialData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.stream.IntStream;

public final class SyncResourceTask extends ThreadOffCalc {
    private final SyncResource syncResource;

    public SyncResourceTask(CompletionService<List<Shape>> executorService, int multiplicity) {
        super(executorService, multiplicity);
        syncResource = new SyncResource();
    }

    @Override
    public InitialData getTasks() {
        List<SyncCallable> initialTasks = new ArrayList<>(multiplicity);
        IntStream.range(0, multiplicity).forEach(it -> initialTasks.add(new SyncCallable()));

        return new InitialData(multiplicity, initialTasks);
    }

    class SyncCallable implements Callable<List<Shape>> {
        @Override
        public List<Shape> call() {
            syncResource.syncMe();
            return Collections.emptyList();
        }
    }

    private final static class SyncResource {
        private int resource = 0;
        synchronized void syncMe() {
//            Thread.sleep(10);
            resource += 1;
        }
    }
}
