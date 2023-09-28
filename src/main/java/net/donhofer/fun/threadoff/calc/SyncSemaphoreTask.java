package net.donhofer.fun.threadoff.calc;

import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

public final class SyncSemaphoreTask extends ThreadOffCalc {
    private final SyncSemaphore syncSemaphore;

    public SyncSemaphoreTask(CompletionService<List<Shape>> executorService, int multiplicity) {
        super(executorService, multiplicity);
        syncSemaphore = new SyncSemaphore();
    }

    @Override
    public InitialData getTasks() {
        List<SyncCallable> initialTasks = new ArrayList<>(multiplicity);
        IntStream.range(0, multiplicity).forEach(it -> initialTasks.add(new SyncCallable()));

        return new InitialData(multiplicity, initialTasks);
    }

    class SyncCallable implements Callable<List<Shape>> {
        @Override
        public List<Shape> call() throws Exception {
            syncSemaphore.syncMe();
            return Collections.emptyList();
        }
    }

    private final static class SyncSemaphore {
        private final Semaphore semaphore = new Semaphore(100);
        void syncMe() throws InterruptedException {
            try {
                semaphore.acquire();
                Thread.sleep(10);
            } finally {
                semaphore.release();
            }
        }
    }

}
