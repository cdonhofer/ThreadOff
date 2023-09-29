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
            List<Shape> shapes = new ArrayList<>(5);
            // add subsequent tasks
            if (currentLevel > 0 && !Thread.currentThread().isInterrupted()) {
                completionService.submit(new SmallKochCallable(
                        x1, y1, x3, y3, color, currentLevel - 1
                ));
                completionService.submit(new SmallKochCallable(
                        x3, y3, x4, y4, color, currentLevel - 1
                ));
                completionService.submit(new SmallKochCallable(
                        x4, y4, x5, y5, color, currentLevel - 1
                ));
                completionService.submit(new SmallKochCallable(
                        x5, y5, x2, y2, color, currentLevel - 1
                ));
            } else {
                // add lines only when final stage has been reached
                var paths = List.of(
                        new ColoredLine(x1, y1, x3, y3, color),
                        new ColoredLine(x3, y3, x4, y4, color),
                        new ColoredLine(x4, y4, x5, y5, color),
                        new ColoredLine(x5, y5, x2, y2, color)

                );
                shapes.addAll(paths);
            }

            return shapes;
        }
    }
}
