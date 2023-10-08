package net.donhofer.fun.threadoff.calc;

import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.data.InitialData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.stream.IntStream;

public final class PrimesCalc extends ThreadOffCalc {

    public PrimesCalc(CompletionService<List<Shape>> executorService, int multiplicity) {
        super(executorService, multiplicity);
    }

    public static void primeNumbersTill(int n) {

        for (int i = 0; i <= n; i++) {
            // be responsive to interruption
            if (Thread.currentThread().isInterrupted())
                break;
            // do nothing with the result, just calculate
            isPrime(i);
        }
    }

    private static boolean isPrime(int number) {
        return IntStream.rangeClosed(2, (int) (Math.sqrt(number)))
                .allMatch(n -> number % n != 0);
    }

    @Override
    public InitialData getTasks() {
        List<PrimeCallable> initialTasks = new ArrayList<>(multiplicity);
        IntStream.range(0, multiplicity).forEach(it -> initialTasks.add(new PrimeCallable()));

        return new InitialData(multiplicity, initialTasks, Collections.emptyList());
    }

    static class PrimeCallable implements Callable<List<Shape>> {

        @Override
        public List<Shape> call() {
            primeNumbersTill(10_000);
            return Collections.emptyList();
        }
    }
}
