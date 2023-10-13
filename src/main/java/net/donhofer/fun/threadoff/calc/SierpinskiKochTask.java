package net.donhofer.fun.threadoff.calc;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.data.InitialData;
import net.donhofer.fun.threadoff.ui.Triangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;

/**
 * combines two types of fractals: a sierpinski triangle and a koch flake
 * by extending a sierpinski triangle with Koch flakes
 */
public final class SierpinskiKochTask extends ThreadOffCalc implements GraphicalTask {
    private final Color strokeColor = Color.WHITE;
    private final Color bgColor = Color.CADETBLUE;
    double canvasWidth, canvasHeight;

    // the grade of this sierpinski triangle
    private final int grade = 7;

    public SierpinskiKochTask(CompletionService<List<Shape>> completionService, double canvasWidth, double canvasHeight) {
        // fixed multiplicity for this task
        super(completionService, 7);
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
    }


    @Override
    public void setCanvasDimensions(double canvasWidth, double canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
    }

    @Override
    public InitialData getTasks() {

        // calculations for initial triangle
        // Calculate margin and side length to center the snowflake in the canvas
        TriangleCoords triangle = CommonCalculations.getKochTriangleCoords(canvasWidth, canvasHeight);

        final int expectedTasks = calculateNumTasks();

        List<Callable<List<Shape>>> initialTasks = new ArrayList<>(3);
        initialTasks.add(getCallable(
                new TriangleCoords(triangle.x1(), triangle.y1(), triangle.x2(), triangle.y2(), triangle.x3(), triangle.y3()),
                grade
        ));

        // draw background to contrast the white triangles
        Rectangle bg = new Rectangle(0, 0, canvasWidth, canvasHeight);
        bg.setFill(bgColor);
        return new InitialData(expectedTasks, initialTasks, List.of(bg));
    }

    public int calculateNumTasks(){
        // todo fix this, didn't think it through
        return (int) (Math.pow(7, grade+1) - 1);
    }

    private Callable<List<Shape>> getCallable(TriangleCoords triangle, int currentLevel) {
        return new SierpinskiKochCallable(triangle, currentLevel, true);
    }

    class SierpinskiKochCallable implements Callable<List<Shape>> {
        private final TriangleCoords triangle;
        private final int currentLevel;
        private final boolean grow;
        public SierpinskiKochCallable(TriangleCoords triangle, int currentLevel, boolean grow) {
            this.triangle = triangle;
            this.currentLevel = currentLevel;
            this.grow = grow;
        }

        @Override
        public List<Shape> call() throws Exception {
            if (Thread.currentThread().isInterrupted()) return Collections.emptyList();
            return splitAndGrowTriangle(triangle);
        }

        /**
         * this splits the triangle into four parts (sierpinski)
         * it also grows the triangle on the sides using the KochFlake algorithm, i.e. on each side of the triangle
         * we "grow" another sierpinski triangle; the side is kept as is for the cool visual effect, which somewhat
         * simplifies the task, as nothing needs to be removed
         * @param triangle the initial triangle
         * @return shape for the cut-out triangle in the center of the initial triangle
         */
        private List<Shape> splitAndGrowTriangle(TriangleCoords triangle) {

            Point p1 = new Point(triangle.x1(), triangle.y1());
            Point p2 = new Point(triangle.x2(), triangle.y2());
            Point p3 = new Point(triangle.x3(), triangle.y3());

            // calculate cut-out triangle (points p4, p5, p6)
            Point p4 = CommonCalculations.getCenter(p1.x(), p1.y(), p2.x(), p2.y());
            Point p5 = CommonCalculations.getCenter(p1.x(), p1.y(), p3.x(), p3.y());
            Point p6 = CommonCalculations.getCenter(p2.x(), p2.y(), p3.x(), p3.y());

            // target level not reached, continue with each subsection
            if (currentLevel > 0) {
                // split part: create smaller triangles
                completionService.submit(new SierpinskiKochCallable(new TriangleCoords(
                        p1.x(), p1.y(),
                        p4.x(), p4.y(),
                        p5.x(), p5.y()
                ), currentLevel - 1, false));
                completionService.submit(new SierpinskiKochCallable(new TriangleCoords(
                        p4.x(), p4.y(),
                        p6.x(), p6.y(),
                        p2.x(), p2.y()
                ), currentLevel - 1, false));
                completionService.submit(new SierpinskiKochCallable(new TriangleCoords(
                        p5.x(), p5.y(),
                        p3.x(), p3.y(),
                        p6.x(), p6.y()
                ), currentLevel - 1, false));

                // grow part: create adjacent triangles
                if (grow) {
                    TriangleCoords triangle1 = CommonCalculations.splitLineKoch(p1.x(), p1.y(), p2.x(), p2.y());
                    TriangleCoords triangle2 = CommonCalculations.splitLineKoch(p2.x(), p2.y(), p3.x(), p3.y());
                    TriangleCoords triangle3 = CommonCalculations.splitLineKoch(p3.x(), p3.y(), p1.x(), p1.y());

                    // TODO add further tasks, so the triangles will grow also on the first and fourth line segment
                    // produced by the Koch split; this might have to be it's own recursive mechanism!?
                    // smaller triangles on the remaining line segments
                    TriangleCoords triangle4 = CommonCalculations.splitLineKoch(p1.x(), p1.y(), triangle1.x1(), triangle1.y1());
                    TriangleCoords triangle5 = CommonCalculations.splitLineKoch(triangle1.x3(), triangle1.y3(), p2.x(), p2.y());

                    TriangleCoords triangle6 = CommonCalculations.splitLineKoch(p2.x(), p2.y(), triangle2.x1(), triangle2.y1());
                    TriangleCoords triangle7 = CommonCalculations.splitLineKoch(triangle2.x3(), triangle2.y3(), p3.x(), p3.y());

                    // the third side must only grow on the original triangle, since all further triangles are
                    // already attached to a host triangle
                    if (currentLevel == grade) {
                        completionService.submit(new SierpinskiKochCallable(triangle3, currentLevel - 1, true));

                        TriangleCoords triangle8 = CommonCalculations.splitLineKoch(p3.x(), p3.y(), triangle3.x1(), triangle3.y1());
                        TriangleCoords triangle9 = CommonCalculations.splitLineKoch(triangle3.x3(), triangle3.y3(), p1.x(), p1.y());
                        completionService.submit(new SierpinskiKochCallable(triangle8, currentLevel - 1, true));
                        completionService.submit(new SierpinskiKochCallable(triangle9, currentLevel - 1, true));
                    }

                    completionService.submit(new SierpinskiKochCallable(triangle1, currentLevel - 1, true));
                    completionService.submit(new SierpinskiKochCallable(triangle2, currentLevel - 1, true));


                    completionService.submit(new SierpinskiKochCallable(triangle4, currentLevel - 1, true));
                    completionService.submit(new SierpinskiKochCallable(triangle5, currentLevel - 1, true));
                    completionService.submit(new SierpinskiKochCallable(triangle6, currentLevel - 1, true));
                    completionService.submit(new SierpinskiKochCallable(triangle7, currentLevel - 1, true));
                }
            }

            List<Shape> shapes  = new ArrayList<>();
            // draw initial bg color triangle
//            // TODO this is not reliable, I guess, due to the sort-order - that's the reason I introduced the initial shapes in InitialData
//            if (grow) {
//                Triangle bgTriangle = new Triangle(
//                        p1.x(), p1.y(),
//                        p2.x(), p2.y(),
//                        p3.x(), p3.y(),
//                        bgColor
//                );
//                shapes.add(bgTriangle);
//            }

            // add cut out triangle
            Triangle cutOutTriangle = new Triangle(
                    p4.x(), p4.y(),
                    p5.x(), p5.y(),
                    p6.x(), p6.y(),
                    strokeColor
            );
            shapes.add(cutOutTriangle);

            return shapes;
        }
    }

}
