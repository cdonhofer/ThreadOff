package net.donhofer.fun.threadoff.calc;

import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.data.InitialData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;

public abstract sealed class KochFlakeTask extends ThreadOffCalc implements GraphicalTask permits KochFlakeTaskBig, KochFlakeTaskSmall {
    private Color strokeColor;
    double canvasWidth, canvasHeight;

    public KochFlakeTask(CompletionService<List<Shape>> completionService, double canvasWidth, double canvasHeight) {
        // fixed multiplicity for this task
        super(completionService, 8);
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
    }

    protected KochFlakeTask(CompletionService<List<Shape>> completionService, int multiplicity, double canvasWidth, double canvasHeight) {
        super(completionService, multiplicity);
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
    }


    public KochFlakeTask(CompletionService<List<Shape>> completionService,
                         Color strokeColor, double canvasHeight, double canvasWidth) {
        this(completionService, canvasWidth, canvasHeight);
        this.strokeColor = strokeColor;
    }

    protected KochFlakeTask(CompletionService<List<Shape>> completionService, Color strokeColor, int multiplicity, double canvasHeight, double canvasWidth) {
        this(completionService, multiplicity, canvasWidth, canvasHeight);
        this.strokeColor = strokeColor;
    }

    @Override
    public void setCanvasDimensions(double canvasWidth, double canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
    }

    @Override
    public InitialData getTasks() {

        // calculations for initial lines
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


        return new InitialData(expectedTasks, initialTasks, Collections.emptyList());
    }

    public abstract int calculateNumTasks();

    public abstract Callable<List<Shape>> getCallable(double x1, double y1, double x2, double y2, Color color, int currentLevel);
}
