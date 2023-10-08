package net.donhofer.fun.threadoff.calc;

import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.data.InitialData;
import net.donhofer.fun.threadoff.ui.Triangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;

public final class SierpinskiTask extends ThreadOffCalc implements GraphicalTask {
    private final Color strokeColor = Color.WHITE;
    private final Color bgColor = Color.BLACK;
    double canvasWidth, canvasHeight;

    // the grade of this sierpinski triangle
    private final int grade = 10;

    public SierpinskiTask(CompletionService<List<Shape>> completionService, double canvasWidth, double canvasHeight) {
        // fixed multiplicity for this task
        super(completionService, 10);
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
                new TriangleCoords(x1, y1, x2, y2, x3, y3),
                grade
        ));
        Triangle baseTriangle = new Triangle(
                x1, y1,
                x2, y2,
                x3, y3,
                bgColor
        );
        return new InitialData(expectedTasks, initialTasks, List.of(baseTriangle));
    }

    public int calculateNumTasks(){
        return (int) (Math.pow(3, grade+1) - 1)/2;
    }

    private Callable<List<Shape>> getCallable(TriangleCoords triangle, int currentLevel) {
        return new SierpinskiCallable(triangle, currentLevel);
    }

    class SierpinskiCallable implements Callable<List<Shape>> {
        private final TriangleCoords triangle;
        private final int currentLevel;

        public SierpinskiCallable(TriangleCoords triangle, int currentLevel) {
            this.triangle = triangle;
            this.currentLevel = currentLevel;
        }

        @Override
        public List<Shape> call() throws Exception {
            if (Thread.currentThread().isInterrupted()) return Collections.emptyList();
            return splitTriangle(triangle);
        }

        // todo list or no list?
        private List<Shape> splitTriangle(TriangleCoords triangle) {

            Point p1 = new Point(triangle.x1(), triangle.y1());
            Point p2 = new Point(triangle.x2(), triangle.y2());
            Point p3 = new Point(triangle.x3(), triangle.y3());

            // calculate cut-out triangle (points p4, p5, p6)
            Point p4 = getCenter(p1.x(), p1.y(), p2.x(), p2.y());
            Point p5 = getCenter(p1.x(), p1.y(), p3.x(), p3.y());
            Point p6 = getCenter(p2.x(), p2.y(), p3.x(), p3.y());

            // target level not reached, continue with each subsection
            if (currentLevel > 0) {
                completionService.submit(new SierpinskiCallable(new TriangleCoords(
                        p1.x(), p1.y(),
                        p4.x(), p4.y(),
                        p5.x(), p5.y()
                ), currentLevel - 1));
                completionService.submit(new SierpinskiCallable(new TriangleCoords(
                        p4.x(), p4.y(),
                        p6.x(), p6.y(),
                        p2.x(), p2.y()
                ), currentLevel - 1));
                completionService.submit(new SierpinskiCallable(new TriangleCoords(
                        p5.x(), p5.y(),
                        p3.x(), p3.y(),
                        p6.x(), p6.y()
                ), currentLevel - 1));
            }

            List<Shape> shapes  = new ArrayList<>();

            // add cut out triangle
            Triangle cutOutTriangle = new Triangle(
                    p4.x(), p4.y(),
                    p5.x(), p5.y(),
                    p6.x(), p6.y(),
                    strokeColor
            );
            shapes.add(cutOutTriangle);

//            System.out.println("shapes size: "+shapes.size());

            return shapes;
        }
    }

    private Point getCenter(double x1, double y1, double x2, double y2) {
        double deltaX1X2 = x2-x1;
        double deltaY1Y2 = y2-y1;
        double x3 = x1 + deltaX1X2 / 2;
        double y3 = y1 + deltaY1Y2 / 2;

        return new Point(x3, y3);
    }

    record TriangleCoords(double x1, double y1, double x2, double y2, double x3, double y3){}
    record Point(double x, double y) {}
}
