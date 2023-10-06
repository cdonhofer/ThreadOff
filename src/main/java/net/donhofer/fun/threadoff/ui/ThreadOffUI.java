package net.donhofer.fun.threadoff.ui;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import net.donhofer.fun.threadoff.data.SelectableTask;
import net.donhofer.fun.threadoff.data.ThreadType;

import java.util.List;

public class ThreadOffUI {
    private Button start;
    private Button stop;
    private final VBox mainCanvas;
    private final VBox statsSideBar;
    private final List<SelectableTask> selectableTasks;
    private final Runnable runTasks, stopCalculations;

    private Canvas drawableCanvas = new Canvas(UIConfig.defaultCanvasWidth, UIConfig.defaultCanvasHeight);
    private final IntegerField numCalculationsInput = new IntegerField(UIConfig.defaultExecutions, UIConfig.minExecutions, UIConfig.maxExecutions);
    private final IntegerField numThreadsInput = new IntegerField(Runtime.getRuntime().availableProcessors(), UIConfig.minThreads, UIConfig.maxThreads);
    private final ToggleLabel noCalculationLabel = new ToggleLabel("n/A");
    private final ToggleLabel numThreadsLabel = new ToggleLabel("Pool size:");
    final TaskComboBox taskSelection;
    final ComboBox<ThreadType> threadTypeSelection = new ComboBox<>(FXCollections.observableArrayList(ThreadType.VIRTUAL, ThreadType.PLATFORM));

    private final Scene scene;
    public ThreadOffUI(List<SelectableTask> selectableTasks, Runnable runTasks, Runnable stopCalculations, String styleSheet) {
        Pane rootPane = new Pane();
        scene = new Scene(rootPane, UIConfig.defaultAppWidth, UIConfig.defaultAppHeight);

        this.selectableTasks = selectableTasks;
        this.runTasks = runTasks;
        this.stopCalculations = stopCalculations;
        taskSelection = new TaskComboBox(selectableTasks);
        rootPane.getStylesheets().add(styleSheet);

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
        start.setOnAction(startClickEvent -> runTasks.run());
        stop.setOnAction(endClickEvent -> stopCalculations.run());

        noCalculationLabel.hide();

        taskSelection.setValue(task);
        if (task instanceof SelectableTask.RepeatableTask t) {
            numCalculationsInput.setText("" + t.defaultExecutions());
        }
        numThreadsLabel.hide();
        numThreadsInput.hide();
        numThreadsInput.setText("" + task.getThreadPoolSize().get());

        // make calculations input editable only for appropriate tasks
        taskSelection.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                            final boolean isRepeatable = newValue instanceof SelectableTask.RepeatableTask;
                            numCalculationsInput.toggle(isRepeatable);
                            noCalculationLabel.toggle(!isRepeatable);
                            if (newValue instanceof SelectableTask.RepeatableTask t) {
                                numCalculationsInput.setText(t.defaultExecutions() + "");
                            }
                            numThreadsInput.setText("" + newValue.getThreadPoolSize().get());
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

    public TaskUiElements prepareUIForTaskRun(SelectableTask task, boolean useVThreads, int poolSize) {
        mainCanvas.getChildren().clear();
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(0F);
        progressBar.getStyleClass().add("progress-bar");
        StatsBox statsBox = new StatsBox(useVThreads, task.name(), poolSize);

        VBox detailsBox = new VBox();
        detailsBox.getStyleClass().add("details-box");
        final Label detailsHeader = new Label("Task: " + task.name());
        detailsHeader.getStyleClass().add("details-box-header");
        detailsBox.getChildren().add(detailsHeader);
        Text description = new Text("Description: " + task.description());
        description.getStyleClass().add("details-box-content");
        detailsBox.getChildren().add(new TextFlow(description));
        mainCanvas.getChildren().add(detailsBox);
        mainCanvas.getChildren().add(progressBar);

        // canvas for visual tasks to draw on
        drawableCanvas = new Canvas(mainCanvas.getWidth() * 0.8, mainCanvas.getHeight() * 0.8);
        drawableCanvas.getGraphicsContext2D().setStroke(UIConfig.strokeColor);
        drawableCanvas.getGraphicsContext2D().setFill(UIConfig.backgroundColor);

        mainCanvas.getChildren().add(drawableCanvas);

        //add new section on top of stats canvas
        final VBox newStatsBox = new VBox();
        newStatsBox.getStyleClass().add("sidebar-box");
        statsSideBar.getChildren().add(0, newStatsBox);

        VBox statsContainer = (VBox) statsSideBar.getChildren().get(0);
        statsContainer.getChildren().add(statsBox);

        return new TaskUiElements(progressBar, statsBox);
    }

    public Canvas getDrawableCanvas() {
        return drawableCanvas;
    }

    public IntegerField getNumCalculationsInput() {
        return numCalculationsInput;
    }

    public IntegerField getNumThreadsInput() {
        return numThreadsInput;
    }

    public TaskComboBox getTaskSelection() {
        return taskSelection;
    }

    public ComboBox<ThreadType> getThreadTypeSelection() {
        return threadTypeSelection;
    }

    public Scene getScene() {
        return scene;
    }
}
