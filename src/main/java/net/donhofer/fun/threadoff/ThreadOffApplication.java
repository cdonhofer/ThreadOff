package net.donhofer.fun.threadoff;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import net.donhofer.fun.threadoff.calc.*;
import net.donhofer.fun.threadoff.data.InitialData;
import net.donhofer.fun.threadoff.data.SelectableTask;
import net.donhofer.fun.threadoff.data.SelectableTask.RepeatableTask;
import net.donhofer.fun.threadoff.data.SelectableTask.SingularTask;
import net.donhofer.fun.threadoff.data.ThreadType;
import net.donhofer.fun.threadoff.ui.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadOffApplication extends Application {

    ExecutorService executorService;
    CompletionService<List<Shape>> completionService;
    private final List<AnimationTimer> runningAnimations = new ArrayList<>();
    long startTime;
    AtomicLong elapsedTime = new AtomicLong(0);
    Future<Void> resultsCollector;

    List<SelectableTask> selectableTasks = List.of(
            new RepeatableTask("Blocking sleep", "Simply blocks using Thread.sleep(10)",
                    (var numExec) -> new BlockingTask(completionService, numExec),
                    () -> 800, // thread pool size
                    100000),
            new RepeatableTask("Calculate primes to 10.000", "Calculates primes up to 10.000",
                    (var numExec) -> new PrimesCalc(completionService, numExec),
                    () -> ThreadOffCalc.getThreadPoolSize() * 10, // thread pool size
                    10000),
            new SingularTask("Koch Flake (small subtasks)", "Calculates a Koch-flake of grade 8, which is then displayed. Every single curve's calculation is a separate task.",
                    (var numExec) -> new KochFlakeTaskSmall(completionService, UIConfig.strokeColor, UIConfig.defaultCanvasWidth, UIConfig.defaultCanvasHeight),
                    ThreadOffCalc::getThreadPoolSize),
            new SingularTask("Koch Flake (medium subtasks)", "Calculates a Koch-flake of grade 9, which is then displayed. Every 2 curves' calculation is a separate task.",
                    (var numExec) -> new KochFlakeTaskBig(completionService, UIConfig.strokeColor, UIConfig.defaultCanvasWidth, UIConfig.defaultCanvasHeight, 2),
                    ThreadOffCalc::getThreadPoolSize),
            new SingularTask("Koch Flake (larger subtasks)", "Calculates a Koch-flake of grade 9, which is then displayed. Every 4 curves' calculation is a separate task.",
                    (var numExec) -> new KochFlakeTaskBig(completionService, UIConfig.strokeColor, UIConfig.defaultCanvasWidth, UIConfig.defaultCanvasHeight, 4),
                    ThreadOffCalc::getThreadPoolSize),
            new SingularTask("Sierpinski Triangle", "Calculates a Sierpinski triangle of grade 10, which is then displayed",
                    (var numExec) -> new SierpinskiTask(completionService, UIConfig.defaultCanvasWidth, UIConfig.defaultCanvasHeight),
                    ThreadOffCalc::getThreadPoolSize),
            new SingularTask("Koch flake filled with Sierpinski triangles", "Calculates a Koch flake filled with Sierpinski triangles, which is then displayed",
                    (var numExec) -> new KochSierpinskiTask(completionService, UIConfig.defaultCanvasWidth, UIConfig.defaultCanvasHeight),
                    ThreadOffCalc::getThreadPoolSize),
            new SingularTask("Sierpinski triangle with Koch extensions", "Calculates a Sierpinski triangle with Koch flake extensions, which is then displayed",
                    (var numExec) -> new SierpinskiKochTask(completionService, UIConfig.defaultCanvasWidth, UIConfig.defaultCanvasHeight),
                    ThreadOffCalc::getThreadPoolSize),
            new RepeatableTask("Blocking sleep in synchronized method", "Blocks using Thread.sleep(10) in a synchronized method",
                    (var numExec) -> new SyncResourceTask(completionService, numExec),
                    () -> ThreadOffCalc.getThreadPoolSize() * 10, // thread pool size
                    100000),
            new RepeatableTask("Blocking sleep in reentrant lock", "Blocks using Thread.sleep(10) in a locked code block (with ReentrantLock)",
                    (var numExec) -> new LockResourceTask(completionService, numExec),
                    () -> ThreadOffCalc.getThreadPoolSize() * 10, // thread pool size
                    100000),
            new RepeatableTask("Blocking sleep in semaphore", "Blocks using Thread.sleep(10) in a semaphore with 100 permits",
                    (var numExec) -> new SyncSemaphoreTask(completionService, numExec),
                    () -> ThreadOffCalc.getThreadPoolSize() * 10, // thread pool size
                    100000)
    );


    AtomicInteger successfulTasks = new AtomicInteger(0);
    ConcurrentLinkedQueue<Shape> shapes = new ConcurrentLinkedQueue<>();

    ThreadOffUI ui;
    Scene scene;
    @Override
    public void start(Stage stage) {
        stage.setTitle("ThreadOff!");
        ui = new ThreadOffUI(
                selectableTasks,
                this::runTask,
                this::stopCalculations,
                getClass().getResource("Style.css").toExternalForm()
        );
        scene = ui.getScene();
        stage.setScene(scene);
        stage.show();
    }



    private void stopCalculations() {
        if (resultsCollector != null) {
            resultsCollector.cancel(true);
        }
        // stop animation timers
        for (AnimationTimer timer : runningAnimations) {
            timer.stop();
        }
        runningAnimations.clear();

        if(executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // todo handle this better
                throw new RuntimeException(e);
            }
        }
        elapsedTime.set(System.nanoTime());
    }

    private void runTask() {
        // get user selection from inputs
        final boolean useVThreads = ThreadType.VIRTUAL == ui.getThreadTypeSelection().getValue();
        final int numExecutions = Integer.parseInt(ui.getNumCalculationsInput().getText());
        var selectedTask = ui.getTaskSelection().getValue();

        // end a possible still running task
        stopCalculations();

        // prepare main canvas
        int poolSize = useVThreads ? 0 : Integer.parseInt(ui.getNumThreadsInput().getText());
        final TaskUiElements uiElements = ui.prepareUIForTaskRun(selectedTask, useVThreads, poolSize);

        if (executorService == null || executorService.isShutdown()) {
            executorService = useVThreads ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newFixedThreadPool(poolSize);
        }
        completionService = new ExecutorCompletionService<>(executorService);

        // init buffer for shapes to draw (used by graphical tasks)
        shapes.clear();

        // update task state data
        successfulTasks.setRelease(0);
        elapsedTime.setRelease(0);
        startTime = System.nanoTime();

        // get data required for running the tasks: initial tasks(s) and number of expected total tasks
        ThreadOffCalc taskHandler = selectedTask.getHandler().apply(numExecutions);

        // pass canvas size to graphical tasks
        if (taskHandler instanceof GraphicalTask gt) {
            gt.setCanvasDimensions(ui.getDrawableCanvas().getWidth(), ui.getDrawableCanvas().getHeight());
        }

        InitialData initialData;

        initialData = taskHandler.getTasks();
        // submit task for collecting the finished tasks' data
        resultsCollector = executorService.submit(() -> collectFinishedTasks(completionService, initialData.expectedTasks()));
        // add initial tasks
        for (Callable<List<Shape>> task : initialData.initialTasks()) {
            completionService.submit(task);
        }
        // draw initial shapes
        for (Shape shape : initialData.initialShapes()) {
            ThreadOffUI.drawShape(shape, ui.getDrawableCanvas().getGraphicsContext2D());
        }

        // start animation timers to update the UI
        AnimationTimer uiUpdater = new UiUpdateTimer(uiElements, initialData.expectedTasks(), successfulTasks, elapsedTime);
        uiUpdater.start();
        AnimationTimer canvasUpdater = new CanvasUpdateTimer(ui.getDrawableCanvas(), initialData.expectedTasks(), shapes, successfulTasks);
        canvasUpdater.start();
        runningAnimations.add(uiUpdater);
        runningAnimations.add(canvasUpdater);
    }


    private Void collectFinishedTasks(CompletionService<List<Shape>> completionService, long numTasks) {

        while(!Thread.currentThread().isInterrupted()) {
            // prefer non-blocking poll for responsiveness(to interruption)
            Future<List<Shape>> completedTask = completionService.poll();
            if (completedTask == null) {
                continue;
            }
            int numDone = successfulTasks.addAndGet(!completedTask.isCancelled() ? 1 : 0);

            try {
                shapes.addAll(completedTask.get());
            } catch (Exception e) {
                // TODO react accordingly
                throw new RuntimeException(e);
            }

            long currTime = System.nanoTime();
            long durationNano = currTime - startTime;
            elapsedTime.set(durationNano);

            // stop thread eventually
            if (numDone >= numTasks) {
                System.out.println("done calculating! rest is UI updates ...");
                break;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() {
        stopCalculations();
    }


}
