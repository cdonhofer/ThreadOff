package net.donhofer.fun.threadoff.calc;

import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.data.InitialData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;

public abstract sealed class KochFlakeTask extends ThreadOffCalc permits KochFlakeTaskBig, KochFlakeTaskSmall {
    private Canvas canvas;
    private Color strokeColor;

    public KochFlakeTask(CompletionService<List<Shape>> completionService) {
        // fixed multiplicity for this task
        super(completionService, 8);
    }

    protected KochFlakeTask(CompletionService<List<Shape>> completionService, int multiplicity) {
        super(completionService, multiplicity);
    }


    public KochFlakeTask(CompletionService<List<Shape>> completionService,
                         Canvas canvas, Color strokeColor) {
        this(completionService);
        this.canvas = canvas;
        this.strokeColor = strokeColor;
    }

    protected KochFlakeTask(CompletionService<List<Shape>> completionService,
                              Canvas canvas, Color strokeColor, int multiplicity) {
        this(completionService, multiplicity);
        this.canvas = canvas;
        this.strokeColor = strokeColor;
    }

    @Override
    public InitialData getTasks() {

        // calculations for initial lines
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();

        // Calculate margin and side length to center the snowflake in the canvas
        double margin = Math.min(canvasWidth, canvasHeight) * 0.1;
        double sideLength = Math.min(canvasWidth, canvasHeight) - 2 * margin;

        // height of the triangle
        double height = Math.sqrt(3) * sideLength / 2;

        // Calculate the center point of the canvas
        double cx = canvasWidth / 2;
        double cy = canvasHeight / 2;

        // Calculate the three vertices of the triangle
        double x1 = cx, y1 = cy - 2 * height / 3;  // Top vertex
        double x2 = cx - sideLength / 2, y2 = cy + height / 3; // Bottom-left vertex
        double x3 = cx + sideLength / 2, y3 = cy + height / 3; // Bottom-right vertex

        final int expectedTasks = calculateNumTasks();

        List<Callable<List<Shape>>> initialTasks = new ArrayList<>(3);
        initialTasks.add(getCallable(
                x1, y1,
                x2, y2,
                strokeColor, multiplicity
        ));
        initialTasks.add(getCallable(
                x2, y2,
                x3, y3,
                strokeColor, multiplicity
        ));
        initialTasks.add(getCallable(
                x3, y3,
                x1, y1,
                strokeColor, multiplicity
        ));


        return new InitialData(expectedTasks, initialTasks);
    }

    public abstract int calculateNumTasks();

    public abstract Callable<List<Shape>> getCallable(double x1, double y1, double x2, double y2, Color color, int currentLevel);
}
