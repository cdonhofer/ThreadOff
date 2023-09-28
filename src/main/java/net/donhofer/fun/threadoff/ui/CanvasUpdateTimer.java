package net.donhofer.fun.threadoff.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Shape;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class CanvasUpdateTimer extends AnimationTimer {
    private final int numTasks;
    private final Canvas canvas;
    private final int shapesPerUpdate = 5000;
    private final Queue<Shape> shapes;
    private final AtomicInteger successfulTasks;

    public CanvasUpdateTimer(Canvas canvas, int numTasks, Queue<Shape> shapes, AtomicInteger successfulTasks) {
        super();
        this.numTasks = numTasks;
        this.canvas = canvas;
        this.shapes = shapes;
        this.successfulTasks = successfulTasks;
    }

    @Override
    public void handle(long now) {
        int doneTasks = successfulTasks.get();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        for (int i = 0; i < shapesPerUpdate && !shapes.isEmpty(); i++) {
            Shape s = shapes.poll();

            if (s instanceof ColoredLine line) {
                gc.strokeLine(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());
            } else if (s != null) {
                System.out.println("Warning: only drawing Lines has been implemented so far! " + s.getClass().getName());
            }
        }


        if (doneTasks >= numTasks && shapes.isEmpty()) stop();
    }
}
