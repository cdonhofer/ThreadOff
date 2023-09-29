package net.donhofer.fun.threadoff.data;

import net.donhofer.fun.threadoff.calc.ThreadOffCalc;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * represents a selectable task
 * @param name display name
 * @param description description for UI
 * @param getHandler get an instance of the handler class to use for task creation
 * @param getThreadPoolSize get suggested pool size for platform threads
 * @param allowNumExecutions does the task support choosing the number of executions
 * @param defaultExecutions default number of executions
 */
public record SelectableTask(String name, String description, Function<Integer, ThreadOffCalc> getHandler, Supplier<Integer> getThreadPoolSize, boolean allowNumExecutions, int defaultExecutions) {
    @Override
    public String toString() {
        return name;
    }
}
