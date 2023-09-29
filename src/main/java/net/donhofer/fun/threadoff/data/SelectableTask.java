package net.donhofer.fun.threadoff.data;

import net.donhofer.fun.threadoff.calc.ThreadOffCalc;

import java.util.function.Function;

public record SelectableTask(String name, String description, Function<Integer, ThreadOffCalc> supplier, boolean allowNumExecutions, int defaultExecutions) {
    @Override
    public String toString() {
        return name;
    }
}
