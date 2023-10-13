package net.donhofer.fun.threadoff.calc;

import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.ui.ColoredLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;

public final class KochFlakeTaskBig extends KochFlakeTask {
    private final int linesPerTask;

    public KochFlakeTaskBig(CompletionService<List<Shape>> completionService, Color strokeColor, double canvasWidth, double canvasHeight, int linesPerTask) {
        super(completionService, strokeColor, 9, canvasHeight, canvasWidth);
        this.linesPerTask = linesPerTask;
    }

    @Override
    public Callable<List<Shape>> getCallable(double x1, double y1, double x2, double y2, Color color, int currentLevel) {
        return new BigKochCallable(x1, y1, x2, y2, color, currentLevel);
    }

    @Override
    public int calculateNumTasks() {
        int numLines =  (int) (Math.pow(4, multiplicity+1)-1);
        // every two lines, except the 3 initial ones, are calculated in groups of linesPerTask
        return (numLines-3)/linesPerTask + 3;
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

                for (LineCoords coords : lines) {
                    results.add(new ColoredLine(coords.x1(), coords.y1(), coords.x2(), coords.y2(), color));
                }

            } else {
                // final level not yet reached
                // dispatch new tasks, two input lines per task
                for (LineCoords coords : lines) {
                    var newLines = splitLine(coords.x1(), coords.y1(), coords.x2(), coords.y2());
                    if(linesPerTask == 2) {
                        completionService.submit(new BigKochCallable(List.of(newLines.get(0), newLines.get(1)), color, currentLevel-1));
                        completionService.submit(new BigKochCallable(List.of(newLines.get(2), newLines.get(3)), color, currentLevel-1));
                    } else {
                        completionService.submit(new BigKochCallable(newLines, color, currentLevel-1));
                    }
                }
            }

            return results;
        }
        private List<LineCoords> splitLine(double x1, double y1, double x2, double y2) {
            TriangleCoords triangle = CommonCalculations.splitLineKoch(x1, y1, x2, y2);

            // todo: this could be optimized: only draw over the segment that'll be removed,
            //  then just return the two new lines
            return List.of(
                    new LineCoords(x1, y1, triangle.x1(), triangle.y1()),
                    new LineCoords(triangle.x1(), triangle.y1(), triangle.x2(), triangle.y2()),
                    new LineCoords(triangle.x2(), triangle.y2(), triangle.x3(), triangle.y3()),
                    new LineCoords(triangle.x3(), triangle.y3(), x2, y2)

            );
        }
    }

    private record LineCoords(double x1, double y1, double x2, double y2) {}
}
