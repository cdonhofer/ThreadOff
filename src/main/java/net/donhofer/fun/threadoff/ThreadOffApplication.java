package net.donhofer.fun.threadoff;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import net.donhofer.fun.threadoff.calc.*;
import net.donhofer.fun.threadoff.ui.ColoredLine;
import net.donhofer.fun.threadoff.ui.StatsBox;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
    long startTime;
    long endTime;
    Future<Void> statsTask;
    Future<Void> canvasUpdateTask;

    private final double canvasStartingWidth = 400, canvasStartingHeight = 400;
    Canvas drawableCanvas = new Canvas(canvasStartingWidth, canvasStartingHeight);
    Color strokeColor = Color.BLACK;
    Color backgroundColor = Color.valueOf("#f4f4f4");

    private final int defaultExecutions = 1000;
    private final TextField numCalculationsInput = new TextField();
    UnaryOperator<TextFormatter.Change> integerFilter = change -> {
        String newText = change.getControlNewText();
        if (newText.matches("([1-9][0-9]*)?")) {
            return change;
        }
        return null;
    };

    // todo find non-raw solution
    List<SelectableTask> selectableTasks = List.of(
            new SelectableTask("Blocking sleep", "Simply blocks using Thread.sleep(10)", (var numExec) -> new BlockingTask(completionService, numExec)),
            new SelectableTask("Calculate primes to 10.000", "Calculates primes up to 10.000", (var numExec) -> new PrimesCalc(completionService, numExec)),
            new SelectableTask("Koch", "Calculates and displays a Koch-flake", (var numExec) -> new KochFlakeTask(completionService, numExec, drawableCanvas, strokeColor, backgroundColor))
    );
    ObservableList<SelectableTask> options =
            FXCollections.observableArrayList(selectableTasks);
    final ComboBox<SelectableTask> taskSelection = new ComboBox<>(options);
    //TODO switch from strings to some type
    final ComboBox<String> threadTypeSelection = new ComboBox<>(FXCollections.observableArrayList("vThreads", "pThreads"));


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
        start.setOnAction(startClickEvent -> {
            runTask();
        });
        stop.setOnAction(endClickEvent -> {
            stopCalculations();
        });

        numCalculationsInput.setTextFormatter(
                new TextFormatter<>(new IntegerStringConverter(), defaultExecutions, integerFilter));
        taskSelection.setValue(selectableTasks.get(0));
        threadTypeSelection.setValue(threadTypeSelection.getItems().get(0));

        inputFieldsRow.getChildren().addAll(
                new Label("Task:"),
                taskSelection,
                new Label("# calculations:"),
                numCalculationsInput,
                new Label("Thread type:"),
                threadTypeSelection
        );
        buttonRow.getChildren().addAll(start, stop);
        return bottomPanel;
    }

    private void stopCalculations() {
        System.out.println("in stopCalculations!-------------------------");
        if (statsTask != null) {
            System.out.println("cancelling stats task");
            statsTask.cancel(true);
        }
        if (canvasUpdateTask != null) {
            System.out.println("cancelling canvasUpdateTask");
            canvasUpdateTask.cancel(true);
        }
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
        endTime = System.nanoTime();
    }

    private void runTask() {
        // get user selection from inputs
        final boolean useVThreads = "vThreads".equals(threadTypeSelection.getValue());
        final int numExecutions = Integer.parseInt(numCalculationsInput.getText());
        var selectedTask = taskSelection.getValue();

        // end a possible still running task
        stopCalculations();

        // prepare main canvas
        final ProgressBar progressBar = prepareUIForTaskRun(selectedTask);

        if (executorService == null || executorService.isShutdown()) {
            executorService = useVThreads ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
        completionService = new ExecutorCompletionService<>(executorService);

        // init buffer for shapes to draw (used by graphical tasks)
        shapes.clear();

        startTime = System.nanoTime();

        // get data required for running the tasks: initial tasks(s) and number of expected total tasks
        ThreadOffCalc taskRunnable = selectedTask.supplier().apply(numExecutions);
        InitialData initialData;

        initialData = taskRunnable.getTasks();
        // submit task for updating the stats and canvas
        statsTask = executorService.submit(() -> showStats(completionService, progressBar, initialData.expectedTasks(), selectedTask, useVThreads));
        // canvas update task
        canvasUpdateTask = executorService.submit(() -> updateCanvas());


        // add initial tasks
        for (var task : initialData.initialTasks()) {
            completionService.submit(task);
        }
    }

    private ProgressBar prepareUIForTaskRun(SelectableTask task) {
        mainCanvas.getChildren().clear();
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(0F);
        progressBar.getStyleClass().add("progress-bar");

        mainCanvas.getChildren().add(new Label("Task: " + task.name()));
        mainCanvas.getChildren().add(new Label("Description: " + task.description()));
        mainCanvas.getChildren().add(progressBar);

        // canvas for visual tasks to draw on
        // todo determine available space
        drawableCanvas = new Canvas(mainCanvas.getWidth() * 0.8, mainCanvas.getHeight() * 0.8);
        drawableCanvas.getGraphicsContext2D().setStroke(strokeColor);
        drawableCanvas.getGraphicsContext2D().setFill(backgroundColor);
//        drawableCanvas.widthProperty().bind(mainCanvas.widthProperty());
//        drawableCanvas.heightProperty().bind(mainCanvas.heightProperty());
        mainCanvas.getChildren().add(drawableCanvas);

        //add new section on top of stats canvas
        final VBox newStatsBox = new VBox();
        newStatsBox.getStyleClass().add("sidebar-box");
        statsSideBar.getChildren().add(0, newStatsBox);
        return progressBar;
    }


    private Void showStats(CompletionService<List<Shape>> completionService, ProgressBar progressBar, int numTasks, SelectableTask task, boolean useVThreads) {
        AtomicInteger successful = new AtomicInteger(0);

        VBox statsContainer = (VBox) statsSideBar.getChildren().get(0);
        StatsBox statsBox = new StatsBox(useVThreads, task.name());
        long lastUpdate = System.nanoTime();
        Platform.runLater(() -> statsContainer.getChildren().add(statsBox));

        // create a buffer canvas for large numbers of updates
        Canvas bufferCanvas = new Canvas(drawableCanvas.getWidth(), drawableCanvas.getHeight());
        bufferCanvas.getStyleClass().addAll(drawableCanvas.getStyleClass());

        while(!Thread.currentThread().isInterrupted()) {
            // prefer non-blocking poll for responsiveness(to interruption)
            Future<List<Shape>> completedTask = completionService.poll();
            if (completedTask == null) {
                continue;
            }
            int doneTasks = successful.addAndGet(!completedTask.isCancelled() ? 1 : 0);
            System.out.println("Completed: " + doneTasks);

            // calculate progress
            float progress = (float) doneTasks / numTasks;
            try {
                shapes.addAll(completedTask.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // limit updates to every ten ms for smooth updates
            long currentTime = System.nanoTime();
            long elapsedMs = (long) ((currentTime-lastUpdate) / 1e6);
            if (elapsedMs < 50 && doneTasks < numTasks)
                continue;
            lastUpdate = currentTime;

            long currTime = System.nanoTime();
            long durationNano = currTime - startTime;
            double durationSeconds = durationNano / 1e9;

            Platform.runLater(() -> {
                // update stats sidebar
                statsBox.setSuccessful(doneTasks);
                statsBox.setDurationSeconds(durationSeconds);
                // update main pane
                progressBar.setProgress(progress);
            });

        }
        return null;
    }


    private Void updateCanvas() {

        final int updateBound = 200;

        long lastUpdate = System.nanoTime();

        while(!Thread.currentThread().isInterrupted()) {
            // draw in batches of updateBound lines and after at least updateBound ms
            long currentTime = System.nanoTime();
            long elapsedMs = (long) ((currentTime-lastUpdate) / 1e6);
            if (elapsedMs < updateBound || shapes.size() < updateBound)
                continue;
            Platform.runLater(() -> {
                GraphicsContext gc = drawableCanvas.getGraphicsContext2D();
                for (int i = 0; i < updateBound; i++) {
                    Shape s = shapes.poll();
                    if (s instanceof ColoredLine line) {
                        gc.strokeLine(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());
                    }
                }
            });
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