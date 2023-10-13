package net.donhofer.fun.threadoff.calc;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import net.donhofer.fun.threadoff.data.InitialData;
import net.donhofer.fun.threadoff.ui.ColoredLine;
import net.donhofer.fun.threadoff.ui.Triangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;

import static net.donhofer.fun.threadoff.calc.CommonCalculations.getCenter;

/**
 * combines two types of fractals: a sierpinski triangle and a koch flake
 * by filling a Koch flake with Sierpinski triangles
 */
public final class KochSierpinskiTask extends ThreadOffCalc implements GraphicalTask {
    private final Color strokeColor = Color.WHITE;
    private final Color bgColor = Color.CADETBLUE;
    private final Color fillColor = Color.WHITE;
    private double canvasWidth, canvasHeight;
    private static final int grade = 8;

    public KochSierpinskiTask(CompletionService<List<Shape>> completionService, double canvasWidth, double canvasHeight) {
        super(completionService, grade);
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
    }

    public int calculateNumTasks() {
        return (int) (Math.pow(4, multiplicity+1)-1)* 6;
    }


    @Override
    public InitialData getTasks() {

        // calculations for initial lines
        // Calculate margin and side length to center the snowflake in the canvas
        final TriangleCoords triangle = CommonCalculations.getKochTriangleCoords(canvasWidth, canvasHeight);

        final int expectedTasks = calculateNumTasks();

        List<Callable<List<Shape>>> initialTasks = new ArrayList<>(3);
        initialTasks.add(new SmallKochCallable(
                triangle.x1(), triangle.y1(),
                triangle.x2(), triangle.y2(),
                strokeColor, multiplicity
        ));
        initialTasks.add(new SmallKochCallable(
                triangle.x2(), triangle.y2(),
                triangle.x3(), triangle.y3(),
                strokeColor, multiplicity
        ));
        initialTasks.add(new SmallKochCallable(
                triangle.x3(), triangle.y3(),
                triangle.x1(), triangle.y1(),
                strokeColor, multiplicity
        ));

        // initial sierpinski
        initialTasks.add(new SierpinskiCallable(
                new TriangleCoords(triangle.x1(), triangle.y1(), triangle.x2(), triangle.y2(), triangle.x3(), triangle.y3()),
                multiplicity
        ));


        // draw background to contrast the white triangles
        Rectangle initialBg = new Rectangle(0, 0, canvasWidth, canvasHeight);
        initialBg.setFill(bgColor);
        return new InitialData(expectedTasks, initialTasks, List.of(initialBg));
    }

    @Override
    public void setCanvasDimensions(double canvasWidth, double canvasHeight) {
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
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

                // fill opened space with sierpinski triangle
                completionService.submit(new SierpinskiCallable(
                        triangle,
                        currentLevel - 1
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
                    fillColor
            );
            shapes.add(cutOutTriangle);

            return shapes;
        }
    }
}
