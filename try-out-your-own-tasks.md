---
layout: page
title: Try out your own tasks
permalink: /try-out-your-own-tasks
order: 2
---
## Check out the project

....


## Implement a task handler
```java
public final class YourTaskHandler extends ThreadOffCalc {

    public YourTaskHandler(CompletionService<List<Shape>> executorService, int multiplicity) {
	    // multiplicity is the user input for "# calculations"
	    // feel free to ignore it or hardcode it, if it's irrelevant for 
	    // your task
	    // executorService is an ExecutorCompletionService with an executor
	    // as requested by the UI user
        super(executorService, multiplicity);
    }

    @Override
    public InitialData getTasks() {
	    // override this method to provide the initial set of tasks
	    // this can be a list of all tasks (callables), or just a few
	    // which will spawn other (see the Koch Flake tasks for an example of that)
        List<SleepCallable> initialTasks = new ArrayList<>(multiplicity);
        IntStream.range(0, multiplicity).forEach(it -> initialTasks.add(new SleepCallable()));

        return new InitialData(multiplicity, initialTasks);
    }

	// example for a very simple blocking task
    static class SleepCallable implements Callable<List<Shape>> {
        @Override
        public List<Shape> call() throws Exception {
            Thread.sleep(10);
            return Collections.emptyList();
        }
    }
}
```