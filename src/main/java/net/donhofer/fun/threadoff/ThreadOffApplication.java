package net.donhofer.fun.threadoff;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import net.donhofer.fun.threadoff.calc.*;
import net.donhofer.fun.threadoff.ui.CanvasUpdateTimer;
import net.donhofer.fun.threadoff.ui.StatsBox;
import net.donhofer.fun.threadoff.ui.TaskUiElements;
import net.donhofer.fun.threadoff.ui.UiUpdateTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

public class ThreadOffApplication extends Application {
    private Scene scene;
    private Pane rootPane;
    private Button start;
    private Button stop;
    private VBox mainCanvas;
    private BorderPane mainWindow;
    private ScrollPane container;
    private ScrollPane sideBar;
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

    private final int defaultExecutions = 1000;
    private final TextField numCalculationsInput = new TextField();
    private final Label noCalculationDisplay = new Label("n/A");
    UnaryOperator<TextFormatter.Change> integerFilter = change -> {
        String newText = change.getControlNewText();
        if (newText.matches("([1-9][0-9]*)?")) {
            return change;
        }
        return null;
    };

    List<SelectableTask> selectableTasks = List.of(
            new SelectableTask("Blocking sleep", "Simply blocks using Thread.sleep(10)",
                    (var numExec) -> new BlockingTask(completionService, numExec),
                    true),
            new SelectableTask("Calculate primes to 10.000", "Calculates primes up to 10.000",
                    (var numExec) -> new PrimesCalc(completionService, numExec),
                    true),
            new SelectableTask("Koch Flake (small subtasks)", "Calculates a Koch-flake of grade 8, which is then displayed. Every single curve's calculation is a separate task.",
                    (var numExec) -> new KochFlakeTaskSmall(completionService, drawableCanvas, strokeColor),
                    false),
            new SelectableTask("Koch Flake (medium subtasks)", "Calculates a Koch-flake of grade 9, which is then displayed. Every 2 curves' calculation is a separate task.",
                    (var numExec) -> new KochFlakeTaskBig(completionService, drawableCanvas, strokeColor, 2),
                    false),
            new SelectableTask("Koch Flake (larger subtasks)", "Calculates a Koch-flake of grade 9, which is then displayed. Every 4 curves' calculation is a separate task.",
                    (var numExec) -> new KochFlakeTaskBig(completionService, drawableCanvas, strokeColor, 4),
                    false),
            new SelectableTask("Blocking sleep in synchronized method", "Blocks using Thread.sleep(10) in a synchronized method",
                    (var numExec) -> new SyncResourceTask(completionService, numExec),
                    true),
            new SelectableTask("Blocking sleep in reentrant lock", "Blocks using Thread.sleep(10) in a locked code block (with ReentrantLock)",
                    (var numExec) -> new LockResourceTask(completionService, numExec),
                    true),
            new SelectableTask("Blocking sleep in semaphore", "Blocks using Thread.sleep(10) in a semaphore with 100 permits",
                    (var numExec) -> new SyncSemaphoreTask(completionService, numExec),
                    true)
    );

    ObservableList<SelectableTask> options =
            FXCollections.observableArrayList(selectableTasks);
    final ComboBox<SelectableTask> taskSelection = new ComboBox<>(options);
    //TODO switch from strings to some type
    final ComboBox<String> threadTypeSelection = new ComboBox<>(FXCollections.observableArrayList("vThreads", "pThreads"));


    AtomicInteger successfulTasks = new AtomicInteger(0);
    ConcurrentLinkedQueue<Shape> shapes = new ConcurrentLinkedQueue<>();

    @Override
    public void start(Stage stage) {

        // build main component structure
        rootPane = new Pane();
        scene = new Scene(rootPane, 1000, 1000);
        scene.getRoot().getStylesheets().add(getClass().getResource("Style.css").toExternalForm());

        mainWindow = new BorderPane();
        rootPane.getChildren().add(mainWindow);
        mainWindow.prefWidthProperty().bind(rootPane.widthProperty());
        mainWindow.prefHeightProperty().bind(rootPane.heightProperty());

        container = new ScrollPane();
        mainCanvas = new VBox(5);
        container.setContent(mainCanvas);
        container.setFitToWidth(true);
        container.setFitToHeight(true);

        sideBar = new ScrollPane();
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

        numCalculationsInput.setTextFormatter(
                new TextFormatter<>(new IntegerStringConverter(), defaultExecutions, integerFilter));

        // set an upper limit of less than ten million calculations
        final int maxLen = 7;
        numCalculationsInput.textProperty().addListener((final ObservableValue<? extends String> ov, final String oldValue, final String newValue) -> {
            if (numCalculationsInput.getText().length() > maxLen) {
                String s = numCalculationsInput.getText().substring(0, maxLen);
                numCalculationsInput.setText(s);
            }
        });
        noCalculationDisplay.setVisible(false);
        noCalculationDisplay.setManaged(false);

        taskSelection.setValue(selectableTasks.get(0));

        // make calculations input editable only for appropriate tasks
        taskSelection.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                            numCalculationsInput.setEditable(newValue.allowNumExecutions());
                            numCalculationsInput.setVisible(newValue.allowNumExecutions());
                            numCalculationsInput.setManaged(newValue.allowNumExecutions());
                            noCalculationDisplay.setVisible(!newValue.allowNumExecutions());
                            noCalculationDisplay.setManaged(!newValue.allowNumExecutions());
                        }
                );

        threadTypeSelection.setValue(threadTypeSelection.getItems().get(0));

        inputFieldsRow.getChildren().addAll(
                new Label("Task:"),
                taskSelection,
                new Label("# calculations:"),
                noCalculationDisplay,
                numCalculationsInput,
                new Label("Thread type:"),
                threadTypeSelection
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
        final boolean useVThreads = "vThreads".equals(threadTypeSelection.getValue());
        final int numExecutions = Integer.parseInt(numCalculationsInput.getText());
        var selectedTask = taskSelection.getValue();

        // end a possible still running task
        stopCalculations();

        // prepare main canvas
        final TaskUiElements uiElements = prepareUIForTaskRun(selectedTask, useVThreads);


        if (executorService == null || executorService.isShutdown()) {
            executorService = useVThreads ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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