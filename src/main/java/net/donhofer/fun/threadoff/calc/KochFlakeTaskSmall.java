package net.donhofer.fun.threadoff.calc;

import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.ui.ColoredLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;

public final class KochFlakeTaskSmall extends KochFlakeTask {
    public KochFlakeTaskSmall(CompletionService<List<Shape>> completionService, Color strokeColor, double canvasWidth, double canvasHeight) {
        super(completionService, strokeColor, canvasHeight, canvasWidth);
    }

    @Override
    public int calculateNumTasks() {
        return (int) (Math.pow(4, multiplicity+1)-1);
    }

    @Override
    public Callable<List<Shape>> getCallable(double x1, double y1, double x2, double y2, Color color, int currentLevel) {
        return new SmallKochCallable(x1, y1, x2, y2, color, currentLevel);
    }

    class SmallKochCallable implements Callable<List<Shape>> {
        private final double x1, y1, x2, y2;
        private final Color color;
        private final int currentLevel;

        SmallKochCallable(double x1, double y1, double x2, double y2, Color color, int currentLevel) {
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
            TriangleCoords triangle = CommonCalculations.splitLineKoch(x1, y1, x2, y2);

            // todo: this could be optimized: only draw over the segment that'll be removed,
            //  then just return the two new lines
            List<Shape> shapes = new ArrayList<>(5);
            // add subsequent tasks
            if (currentLevel > 0 && !Thread.currentThread().isInterrupted()) {
                completionService.submit(new SmallKochCallable(
                        x1, y1, triangle.x1(), triangle.y1(), color, currentLevel - 1
                ));
                completionService.submit(new SmallKochCallable(
                        triangle.x1(), triangle.y1(), triangle.x2(), triangle.y2(), color, currentLevel - 1
                ));
                completionService.submit(new SmallKochCallable(
                        triangle.x2(), triangle.y2(), triangle.x3(), triangle.y3(), color, currentLevel - 1
                ));
                completionService.submit(new SmallKochCallable(
                        triangle.x3(), triangle.y3(), x2, y2, color, currentLevel - 1
                ));
            } else {
                // add lines only when final stage has been reached
                var paths = List.of(
                        new ColoredLine(x1, y1, triangle.x1(), triangle.y1(), color),
                        new ColoredLine(triangle.x1(), triangle.y1(), triangle.x2(), triangle.y2(), color),
                        new ColoredLine(triangle.x2(), triangle.y2(), triangle.x3(), triangle.y3(), color),
                        new ColoredLine(triangle.x3(), triangle.y3(), x2, y2, color)

                );
                shapes.addAll(paths);
            }

            return shapes;
        }

    }
}
