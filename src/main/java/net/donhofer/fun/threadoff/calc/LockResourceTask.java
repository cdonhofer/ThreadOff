package net.donhofer.fun.threadoff.calc;

import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.data.InitialData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public final class LockResourceTask extends ThreadOffCalc {
    private final LockingResource lockingResource;

    public LockResourceTask(CompletionService<List<Shape>> executorService, int multiplicity) {
        super(executorService, multiplicity);
        lockingResource = new LockingResource();
    }

    @Override
    public InitialData getTasks() {
        List<LockCallable> initialTasks = new ArrayList<>(multiplicity);
        IntStream.range(0, multiplicity).forEach(it -> initialTasks.add(new LockCallable()));

        return new InitialData(multiplicity, initialTasks, Collections.emptyList());
    }

    class LockCallable implements Callable<List<Shape>> {
        @Override
        public List<Shape> call() {
            lockingResource.syncMe();
            return Collections.emptyList();
        }
    }

    private final static class LockingResource {
        private static final Lock lock = new ReentrantLock();
        private int resource = 0;
        void syncMe() {
            try {
                lock.lock();
                resource += 1;
            } finally {
                lock.unlock();
            }
        }
    }
}
