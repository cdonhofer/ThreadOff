package net.donhofer.fun.threadoff.calc;

import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.ui.ColoredLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;

public final class KochFlakeTaskBig extends KochFlakeTask {
    public KochFlakeTaskBig(CompletionService<List<Shape>> completionService, Canvas canvas, Color strokeColor) {
        super(completionService, canvas, strokeColor, 9);
    }

    @Override
    public Callable<List<Shape>> getCallable(double x1, double y1, double x2, double y2, Color color, int currentLevel) {
        return new BigKochCallable(x1, y1, x2, y2, color, currentLevel);
    }

    @Override
    public int calculateNumTasks() {
        int numLines =  (int) (Math.pow(4, multiplicity+1)-1);
        // every two lines, except the 3 initial ones, are calculated in pairs of two
        return (numLines-3)/2 + 3;
    }

    class BigKochCallable implements Callable<List<Shape>> {
        private final List<LineCoords> lines;
        private final Color color;
        private final int currentLevel;

        BigKochCallable(double x1, double y1, double x2, double y2, Color color, int currentLevel) {
            lines = List.of(new LineCoords(x1, y1, x2, y2));
            this.color = color;
            this.currentLevel = currentLevel;
        }
        BigKochCallable(List<LineCoords> lines, Color color, int currentLevel) {
            this.lines = lines;
            this.color = color;
            this.currentLevel = currentLevel;
        }


        @Override
        public List<Shape> call() {
            if (Thread.currentThread().isInterrupted()) return Collections.emptyList();

            List<Shape> results = Collections.emptyList();

            // do not return intermediate shapes, only the final ones
            if (currentLevel == 0 || Thread.currentThread().isInterrupted()) {
                results = new ArrayList<>();

                for (LineCoords(double x1, double y1, double x2, double y2) : lines) {
                    results.add(new ColoredLine(x1, y1, x2, y2, color));
                }

            } else {
                // final level not yet reached
                // dispatch new tasks, two input lines per task
                for (LineCoords(double x1, double y1, double x2, double y2) : lines) {
                    var newLines = splitLine(x1, y1, x2, y2);
                    completionService.submit(new BigKochCallable(List.of(newLines.get(0), newLines.get(1)), color, currentLevel-1));
                    completionService.submit(new BigKochCallable(List.of(newLines.get(2), newLines.get(3)), color, currentLevel-1));
                }
            }

            return results;
        }
        private List<LineCoords> splitLine(double x1, double y1, double x2, double y2) {

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
            return List.of(
                    new LineCoords(x1, y1, x3, y3),
                    new LineCoords(x3, y3, x4, y4),
                    new LineCoords(x4, y4, x5, y5),
                    new LineCoords(x5, y5, x2, y2)

            );
        }
    }

    private record LineCoords(double x1, double y1, double x2, double y2) {}
}
