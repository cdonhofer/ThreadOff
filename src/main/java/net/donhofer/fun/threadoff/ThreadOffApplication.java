package net.donhofer.fun.threadoff;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import net.donhofer.fun.threadoff.calc.*;
import net.donhofer.fun.threadoff.data.InitialData;
import net.donhofer.fun.threadoff.data.SelectableTask;
import net.donhofer.fun.threadoff.data.ThreadType;
import net.donhofer.fun.threadoff.ui.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadOffApplication extends Application {
    private Button start;
    private Button stop;
    private VBox mainCanvas;
    private VBox statsSideBar;
    ExecutorService executorService;
    CompletionService<List<Shape>> completionService;
    private final List<AnimationTimer> runningAnimations = new ArrayList<>();
    long startTime;
    AtomicLong elapsedTime = new AtomicLong(0);
    Future<Void> resultsCollector;

    private final double canvasStartingWidth = 400, canvasStartingHeight = 400;
    Canvas drawableCanvas = new Canvas(canvasStartingWidth, canvasStartingHeight);
    Color strokeColor = Color.BLACK;
    Color backgroundColor = Color.valueOf("#f4f4f4");
    private final IntegerField numCalculationsInput = new IntegerField(UIConfig.defaultExecutions, UIConfig.minExecutions, UIConfig.maxExecutions);
    private final IntegerField numThreadsInput = new IntegerField(Runtime.getRuntime().availableProcessors(), UIConfig.minThreads, UIConfig.maxThreads);
    private final Label noCalculationLabel = new Label("n/A");
    private final ToggleLabel numThreadsLabel = new ToggleLabel("Pool size:");

    List<SelectableTask> selectableTasks = List.of(
            new SelectableTask("Blocking sleep", "Simply blocks using Thread.sleep(10)",
                    (var numExec) -> new BlockingTask(completionService, numExec),
                    true, 10000),
            new SelectableTask("Calculate primes to 10.000", "Calculates primes up to 10.000",
                    (var numExec) -> new PrimesCalc(completionService, numExec),
                    true, 10000),
            new SelectableTask("Koch Flake (small subtasks)", "Calculates a Koch-flake of grade 8, which is then displayed. Every single curve's calculation is a separate task.",
                    (var numExec) -> new KochFlakeTaskSmall(completionService, drawableCanvas, strokeColor),
                    false, 0),
            new SelectableTask("Koch Flake (medium subtasks)", "Calculates a Koch-flake of grade 9, which is then displayed. Every 2 curves' calculation is a separate task.",
                    (var numExec) -> new KochFlakeTaskBig(completionService, drawableCanvas, strokeColor, 2),
                    false, 0),
            new SelectableTask("Koch Flake (larger subtasks)", "Calculates a Koch-flake of grade 9, which is then displayed. Every 4 curves' calculation is a separate task.",
                    (var numExec) -> new KochFlakeTaskBig(completionService, drawableCanvas, strokeColor, 4),
                    false, 0),
            new SelectableTask("Blocking sleep in synchronized method", "Blocks using Thread.sleep(10) in a synchronized method",
                    (var numExec) -> new SyncResourceTask(completionService, numExec),
                    true, 100000),
            new SelectableTask("Blocking sleep in reentrant lock", "Blocks using Thread.sleep(10) in a locked code block (with ReentrantLock)",
                    (var numExec) -> new LockResourceTask(completionService, numExec),
                    true, 100000),
            new SelectableTask("Blocking sleep in semaphore", "Blocks using Thread.sleep(10) in a semaphore with 100 permits",
                    (var numExec) -> new SyncSemaphoreTask(completionService, numExec),
                    true, 100000)
    );

    ObservableList<SelectableTask> taskOptions =
            FXCollections.observableArrayList(selectableTasks);
    final ComboBox<SelectableTask> taskSelection = new ComboBox<>(taskOptions);
    final ComboBox<ThreadType> threadTypeSelection = new ComboBox<>(FXCollections.observableArrayList(ThreadType.VIRTUAL, ThreadType.PLATFORM));

    AtomicInteger successfulTasks = new AtomicInteger(0);
    ConcurrentLinkedQueue<Shape> shapes = new ConcurrentLinkedQueue<>();

    @Override
    public void start(Stage stage) {

        // build main component structure
        Pane rootPane = new Pane();
        Scene scene = new Scene(rootPane, UIConfig.defaultAppWidth, UIConfig.defaultAppHeight);
        scene.getRoot().getStylesheets().add(getClass().getResource("Style.css").toExternalForm());

        BorderPane mainWindow = new BorderPane();
        rootPane.getChildren().add(mainWindow);
        mainWindow.prefWidthProperty().bind(rootPane.widthProperty());
        mainWindow.prefHeightProperty().bind(rootPane.heightProperty());

        ScrollPane container = new ScrollPane();
        mainCanvas = new VBox(5);
        container.setContent(mainCanvas);
        container.setFitToWidth(true);
        container.setFitToHeight(true);

        ScrollPane sideBar = new ScrollPane();
        sideBar.setFitToWidth(true);
        statsSideBar = new VBox();
        sideBar.setContent(statsSideBar);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(container, sideBar);
        splitPane.setDividerPositions(0.7); // 70% to container, 30% to statsSideBar

        // build bottom box / menu with inputs
        final VBox bottomPanel = buildBottomPanel();

        // build title bar
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");
        Label titleLabel = new Label("ThreadOff: vThreads vs pThreads!");
        titleLabel.getStyleClass().add("title-label");
        titleBar.getChildren().add(titleLabel);

        // assign css classes and ids
        mainCanvas.setId("mainArea");
        statsSideBar.setId("sidebar");
        statsSideBar.getStyleClass().add("padding");
        statsSideBar.prefWidthProperty().bind(sideBar.widthProperty());
        container.setId("canvasArea");
        container.getStyleClass().add("padding");
        bottomPanel.getStyleClass().add("horizontal-bar");
        start.getStyleClass().add("start-button");
        stop.getStyleClass().add("stop-button");
        sideBar.setId("sidebar-container");
        sideBar.getStyleClass().add("padding");

        // assign positions in main structure
        mainWindow.setTop(titleBar);
        mainWindow.setCenter(splitPane); // Set SplitPane to center
        mainWindow.setBottom(bottomPanel);

        stage.setScene(scene);
        stage.show();
    }

    private VBox buildBottomPanel() {
        // define a default tasks, used to set the correct starting states for the widgets
        SelectableTask task = selectableTasks.get(0);

        VBox bottomPanel = new VBox();
        HBox inputFieldsRow = new HBox();
        HBox buttonRow = new HBox();
        inputFieldsRow.getStyleClass().add("input-panel");
        buttonRow.getStyleClass().add("input-panel");
        bottomPanel.getChildren().addAll(inputFieldsRow, buttonRow);

        // start button
        Region startIcon = new Region();
        startIcon.getStyleClass().add("icon");
        start = new Button("Start", startIcon);
        start.getStyleClass().add("start-button");

        // stop button
        Region stopIcon = new Region();
        stopIcon.getStyleClass().add("icon");
        stop = new Button("Stop", stopIcon);
        stop.getStyleClass().add("stop-button");

        // assign actions to the buttons
        start.setOnAction(startClickEvent -> runTask());
        stop.setOnAction(endClickEvent -> stopCalculations());

        noCalculationLabel.setVisible(false);
        noCalculationLabel.setManaged(false);

        taskSelection.setValue(task);
        numThreadsInput.hide();

        // make calculations input editable only for appropriate tasks
        taskSelection.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                            numCalculationsInput.toggle(newValue.allowNumExecutions());
                            noCalculationLabel.setVisible(!newValue.allowNumExecutions());
                            noCalculationLabel.setManaged(!newValue.allowNumExecutions());
                            if (newValue.allowNumExecutions()) numCalculationsInput.setText(newValue.defaultExecutions() +"");

                            // todo get calculated default num thread from selection
                        }
                );


        threadTypeSelection.setValue(threadTypeSelection.getItems().get(0));
        threadTypeSelection.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    numThreadsLabel.toggle(ThreadType.PLATFORM == newValue);
                    numThreadsInput.toggle(ThreadType.PLATFORM == newValue);
                });

        inputFieldsRow.getChildren().addAll(
                new Label("Task:"),
                taskSelection,
                new Label("# calculations:"),
                noCalculationLabel,
                numCalculationsInput,
                new Label("Thread type:"),
                threadTypeSelection,
                numThreadsLabel,
                numThreadsInput
        );
        buttonRow.getChildren().addAll(start, stop);
        return bottomPanel;
    }

    private void stopCalculations() {
        System.out.println("in stopCalculations!-------------------------");
        if (resultsCollector != null) {
            System.out.println("cancelling stats task");
            resultsCollector.cancel(true);
        }
        // stop animation timers
        for (AnimationTimer timer : runningAnimations) {
            timer.stop();
        }
        runningAnimations.clear();

        if(executorService != null && !executorService.isShutdown()) {
            System.out.println("shutting down service");
            executorService.shutdownNow();
            try {
                System.out.println("awaiting termination");
                executorService.awaitTermination(5, TimeUnit.SECONDS);
                System.out.println("post await ...");
            } catch (InterruptedException e) {
                // todo handle this better
                throw new RuntimeException(e);
            }
        }
        elapsedTime.set(System.nanoTime());
    }

    private void runTask() {
        // get user selection from inputs
        final boolean useVThreads = ThreadType.VIRTUAL == threadTypeSelection.getValue();
        final int numExecutions = Integer.parseInt(numCalculationsInput.getText());
        var selectedTask = taskSelection.getValue();

        // end a possible still running task
        stopCalculations();

        // prepare main canvas
        final TaskUiElements uiElements = prepareUIForTaskRun(selectedTask, useVThreads);


        if (executorService == null || executorService.isShutdown()) {
            executorService = useVThreads ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newFixedThreadPool(Integer.parseInt(numThreadsInput.getText()));
        }
        completionService = new ExecutorCompletionService<>(executorService);

        // init buffer for shapes to draw (used by graphical tasks)
        shapes.clear();

        // update task state data
        successfulTasks.setRelease(0);
        elapsedTime.setRelease(0);
        startTime = System.nanoTime();

        // get data required for running the tasks: initial tasks(s) and number of expected total tasks
        ThreadOffCalc taskRunnable = selectedTask.supplier().apply(numExecutions);
        InitialData initialData;

        initialData = taskRunnable.getTasks();
        // submit task for collecting the finished tasks' data
        resultsCollector = executorService.submit(() -> collectFinishedTasks(completionService, initialData.expectedTasks()));
        // add initial tasks
        for (Callable<List<Shape>> task : initialData.initialTasks()) {
            completionService.submit(task);
        }

        // start animation timers to update the UI
        AnimationTimer uiUpdater = new UiUpdateTimer(uiElements, initialData.expectedTasks(), successfulTasks, elapsedTime);
        uiUpdater.start();
        AnimationTimer canvasUpdater = new CanvasUpdateTimer(drawableCanvas, initialData.expectedTasks(), shapes, successfulTasks);
        canvasUpdater.start();
        runningAnimations.add(uiUpdater);
        runningAnimations.add(canvasUpdater);
    }

    private TaskUiElements prepareUIForTaskRun(SelectableTask task, boolean useVThreads) {
        mainCanvas.getChildren().clear();
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(0F);
        progressBar.getStyleClass().add("progress-bar");

        mainCanvas.getChildren().add(new Label("Task: " + task.name()));
        mainCanvas.getChildren().add(new Label("Description: " + task.description()));
        mainCanvas.getChildren().add(progressBar);

        // canvas for visual tasks to draw on
        drawableCanvas = new Canvas(mainCanvas.getWidth() * 0.8, mainCanvas.getHeight() * 0.8);
        drawableCanvas.getGraphicsContext2D().setStroke(strokeColor);
        drawableCanvas.getGraphicsContext2D().setFill(backgroundColor);
        mainCanvas.getChildren().add(drawableCanvas);

        //add new section on top of stats canvas
        final VBox newStatsBox = new VBox();
        newStatsBox.getStyleClass().add("sidebar-box");
        statsSideBar.getChildren().add(0, newStatsBox);

        VBox statsContainer = (VBox) statsSideBar.getChildren().get(0);
        StatsBox statsBox = new StatsBox(useVThreads, task.name());
        Platform.runLater(() -> statsContainer.getChildren().add(statsBox));

        return new TaskUiElements(progressBar, statsBox);
    }

    private Void collectFinishedTasks(CompletionService<List<Shape>> completionService, int numTasks) {

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