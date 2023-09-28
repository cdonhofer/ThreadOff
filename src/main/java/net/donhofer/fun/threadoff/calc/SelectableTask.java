package net.donhofer.fun.threadoff.calc;

import java.util.function.Function;

public record SelectableTask(String name, String description, Function<Integer, ThreadOffCalc> supplier, boolean allowNumExecutions) {
    @Override
    public String toString() {
        return name;
    }
}
