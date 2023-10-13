package net.donhofer.fun.threadoff.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.control.ProgressBar;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class UiUpdateTimer extends AnimationTimer {
    private final ProgressBar progressBar;
    private final StatsBox statsBox;
    private final long numTasks;
    private final AtomicInteger successfulTasks;
    private final AtomicLong elapsedTime;

    public UiUpdateTimer(TaskUiElements uiElements, long numTasks, AtomicInteger successfulTasks, AtomicLong elapsedTime) {
        super();
        this.numTasks = numTasks;
        this.progressBar = uiElements.progressBar();
        this.statsBox = uiElements.statsBox();
        this.successfulTasks = successfulTasks;
        this.elapsedTime = elapsedTime;
    }


    @Override
    public void handle(long now) {
        int doneTasks = successfulTasks.get();
        float progress = (float) doneTasks / numTasks;

        statsBox.setSuccessful(doneTasks);
        statsBox.setDurationSeconds(elapsedTime.get() / 1e9);
        // update main pane
        progressBar.setProgress(progress);

        if (doneTasks >= numTasks) stop();
    }
}
