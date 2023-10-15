---
layout: page
title: Adding Tasks
permalink: /try-out-your-own-tasks
order: 2
---
## Check out the project

You can get it here: 
[https://github.com/cdonhofer/ThreadOff](https://github.com/cdonhofer/ThreadOff).

It's a maven project and requires [JDK 21](https://jdk.java.net/21/) or later.

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

		// the first param. is the number of total tasks (callables)
		// including tasks added by other tasks
		// in this example, tasks don't add others to the executor
		// so it's the same as multiplicity
        return new InitialData(multiplicity, initialTasks);
    }

	// example for a very simple blocking task
	// if you want your task to draw something on the canvas
	// just make it return a Shape (javafx.scene.shape.Shape)
	// e.g. a Line, Ellipse or a Text
    static class SleepCallable implements Callable<List<Shape>> {
        @Override
        public List<Shape> call() throws Exception {
            Thread.sleep(10);
            return Collections.emptyList();
        }
    }
}
```
In case you want to add a task returning Shapes to be drawn on the Canvas, make sure your task handler implements the GraphicalTask interface.

## Add it to the UI's ComboBox
```java
// in ThreadOffApplication, add it to selectableTasks:

 List<SelectableTask> selectableTasks = List.of(
            new RepeatableTask("Blocking sleep", "Simply blocks using Thread.sleep(10)",
                    (var numExec) -> new YourTaskHandler(completionService, numExec),
                    () -> 800, // thread pool size
                    100000),
            //.....
            );
                    

```

You can add it as either *RepeatableTask* or *SingularTask*. For a repeatable task, the UI offers an input field for the number of calculations. This number is then passed to your task handler (in this case *YourTaskHandler*) and should represent the number of callables returned.

For an example of a *RepeatableTask*, see *BlockingTask.java*. For a *SingularTask*, see *KochFlakeTask.java*.

Alternatively, you can just modify one of the existing implementations to get going faster! 