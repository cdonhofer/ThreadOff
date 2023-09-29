package net.donhofer.fun.threadoff.data;

import net.donhofer.fun.threadoff.calc.ThreadOffCalc;

import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface SelectableTask {
    String name();
    String description();
    Function<Integer, ThreadOffCalc> getHandler();
    Supplier<Integer> getThreadPoolSize();

    /**
     * represents a repeatable task
     * @param name display name
     * @param description description for UI
     * @param getHandler get an instance of the handler class to use for task creation
     * @param getThreadPoolSize get suggested pool size for platform threads
     * @param defaultExecutions default number of executions
     */
    record RepeatableTask(String name, String description, Function<Integer, ThreadOffCalc> getHandler, Supplier<Integer> getThreadPoolSize, int defaultExecutions) implements SelectableTask {}
    /**
     * represents a non-repeatable task
     * @param name display name
     * @param description description for UI
     * @param getHandler get an instance of the handler class to use for task creation
     * @param getThreadPoolSize get suggested pool size for platform threads
     */
    record SingularTask(String name, String description, Function<Integer, ThreadOffCalc> getHandler, Supplier<Integer> getThreadPoolSize) implements SelectableTask {}
}
