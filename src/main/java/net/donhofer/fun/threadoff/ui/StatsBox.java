package net.donhofer.fun.threadoff.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StatsBox extends VBox {
    private final KeyValueLabel taskLabel;
    private final KeyValueLabel durationLabel;
    private final KeyValueLabel calculationsLabel;
    private final KeyValueLabel activeThreadsLabel;
    private double durationSeconds;
    private int successful;

    public StatsBox(boolean useVThreads, String taskName, int poolSize) {
        // set main container class
        getStyleClass().add("stats-box");

        Label headerLabel = new Label(useVThreads ? "vThreads" : "pThreads");
        headerLabel.getStyleClass().add("stats-box-header");
        headerLabel.getStyleClass().add(useVThreads ? "stats-box-vThreads" : "stats-box-pThreads");
        getChildren().add(headerLabel);
        taskLabel = new KeyValueLabel("Task", taskName);
        durationLabel = new KeyValueLabel("Seconds", String.valueOf(durationSeconds));
        calculationsLabel = new KeyValueLabel("Calculations / tasks", String.valueOf(successful));
        activeThreadsLabel = new KeyValueLabel("Active pThreads", "" + Thread.activeCount());
        getChildren().add(taskLabel);
        getChildren().add(durationLabel);
        getChildren().add(calculationsLabel);
        getChildren().add(activeThreadsLabel);
        if(!useVThreads) {
            getChildren().add(new Label("Pool size: " + poolSize));
        }
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
        this.durationLabel.setValue(String.valueOf(durationSeconds));

        // update num threads whenever time is updated
        this.activeThreadsLabel.setValue("" + Thread.activeCount());
    }

    public void setSuccessful(int successful) {
        this.successful = successful;
        this.calculationsLabel.setValue(String.valueOf(successful));
    }
}
