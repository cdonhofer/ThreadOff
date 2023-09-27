package net.donhofer.fun.threadoff.calc;

import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import net.donhofer.fun.threadoff.ui.ColoredLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;

public final class KochFlakeTask extends ThreadOffCalc {
    private Canvas canvas;
    private Color strokeColor, bgColor;

    public KochFlakeTask(CompletionService<List<Shape>> completionService, int multiplicity) {
        // todo multiplicity
        super(completionService, 9);
    }


    public KochFlakeTask(CompletionService<List<Shape>> completionService, int multiplicity,
                         Canvas canvas, Color strokeColor, Color bgColor) {
        this(completionService, multiplicity);
        this.canvas = canvas;
        this.strokeColor = strokeColor;
        this.bgColor = bgColor;
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

        final int expectedTasks = (int) (Math.pow(4, multiplicity+1)-1);

        List<KochCallable> initialTasks = new ArrayList<>(3);
        initialTasks.add(new KochCallable(
                x1, y1,
                x2, y2,
                strokeColor, multiplicity
        ));
        initialTasks.add(new KochCallable(
                x2, y2,
                x3, y3,
                strokeColor, multiplicity
        ));
        initialTasks.add(new KochCallable(
                x3, y3,
                x1, y1,
                strokeColor, multiplicity
        ));


        return new InitialData(expectedTasks, initialTasks);
    }


    class KochCallable implements Callable<List<Shape>> {
        private final double x1, y1, x2, y2;
        private final Color color;
        private final int currentLevel;

        KochCallable(double x1, double y1, double x2, double y2, Color color, int currentLevel) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
            this.currentLevel = currentLevel;
        }


        @Override
        public List<Shape> call() {
            if (Thread.currentThread().isInterrupted()) return Collections.emptyList();
            return splitLine();
        }
        protected List<Shape> splitLine() {

            double deltaX = x2 - x1;
            double deltaY = y2 - y1;

            double x3 = x1 + deltaX / 3;
            double y3 = y1 + deltaY / 3;

            double x4 = (0.5 * (x1+ x2) + Math.sqrt(3) * (y1- y2)/6);
            double y4 = (0.5 * (y1+ y2) + Math.sqrt(3) * (x2 -x1)/6);

            double x5 = x1 + 2 * deltaX / 3;
            double y5 = y1 + 2 * deltaY / 3;

            // todo: this could be optimized: only draw over the segment that'll be removed,
            //  then just return the two new lines

//            Path removal = new Path();
//            removal.getElements().add(new MoveTo(x1, y1));
//            removal.getElements().add(new LineTo(x2, y2));
//            removal.getElements().add(new ClosePath());
//            removal.setStroke(bgColor);


            Path newLines = new Path();
            newLines.setStroke(strokeColor);
            newLines.getElements().add(new MoveTo(x1, y1));
            newLines.getElements().add(new LineTo(x3, y3));
            newLines.getElements().add(new LineTo(x4, y4));
            newLines.getElements().add(new LineTo(x5, y5));
            newLines.getElements().add(new LineTo(x2, y2));
            newLines.getElements().add(new ClosePath());

            List<Shape> shapes = new ArrayList<>(5);
//            shapes.add(newLines);


            // add subsequent tasks
            if (currentLevel > 0 && !Thread.currentThread().isInterrupted()) {
                completionService.submit(new KochCallable(
                        x1, y1, x3, y3, color, currentLevel - 1
                ));
                completionService.submit(new KochCallable(
                        x3, y3, x4, y4, color, currentLevel - 1
                ));
                completionService.submit(new KochCallable(
                        x4, y4, x5, y5, color, currentLevel - 1
                ));
                completionService.submit(new KochCallable(
                        x5, y5, x2, y2, color, currentLevel - 1
                ));
            } else {
                // add lines only when final stage has been reached
                var paths = List.of(
//                        new ColoredLine(x1, y1, x2, y2, bgColor), // remove previous line
                        new ColoredLine(x1, y1, x3, y3, strokeColor),
                        new ColoredLine(x3, y3, x4, y4, strokeColor),
                        new ColoredLine(x4, y4, x5, y5, strokeColor),
                        new ColoredLine(x5, y5, x2, y2, strokeColor)

                );
                shapes.addAll(paths);
            }

            return shapes;
        }
    }
}
