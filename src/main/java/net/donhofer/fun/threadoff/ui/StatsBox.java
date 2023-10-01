package net.donhofer.fun.threadoff.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class StatsBox extends VBox {
    private final KeyValueLabel taskLabel;
    private final KeyValueLabel durationLabel;
    private final KeyValueLabel calculationsLabel;
    private double durationSeconds;
    private int successful;

    public StatsBox(boolean useVThreads, String taskName) {
        // set main container class
        getStyleClass().add("stats-box");

        Label headerLabel = new Label(useVThreads ? "vThreads" : "pThreads");
        headerLabel.getStyleClass().add("stats-box-header");
        headerLabel.getStyleClass().add(useVThreads ? "stats-box-vThreads" : "stats-box-pThreads");
        getChildren().add(headerLabel);
        taskLabel = new KeyValueLabel("Task", taskName);
        durationLabel = new KeyValueLabel("Seconds", String.valueOf(durationSeconds));
        calculationsLabel = new KeyValueLabel("Calculations / tasks", String.valueOf(successful));
        getChildren().add(taskLabel);
        getChildren().add(durationLabel);
        getChildren().add(calculationsLabel);
        if(!useVThreads) {
            getChildren().add(new Label("Active pThreads: " + Thread.activeCount()));
        }
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
        this.durationLabel.setValue(String.valueOf(durationSeconds));
    }

    public void setSuccessful(int successful) {
        this.successful = successful;
        this.calculationsLabel.setValue(String.valueOf(successful));
    }
}
