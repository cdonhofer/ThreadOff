package net.donhofer.fun.threadoff.calc;

public class CommonCalculations {
    /**
     * retrieve the optimal coordinates for the initial triangle points of a Koch flake which fills the available space
     * @param canvasWidth canvas width
     * @param canvasHeight canvas height
     * @return the triangle points' coordinates
     */
    static TriangleCoords getKochTriangleCoords(double canvasWidth, double canvasHeight) {
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
        TriangleCoords triangle = new TriangleCoords(x1, y1, x2, y2, x3, y3);
        return triangle;
    }

    /**
     * returns the three 2D points that form the new triangle "growing" out of the given line
     * the center part of the given line, i.e. the one to be removed in a classical Koch Flake,
     * is the vertex x3,y3 -> x5,y5
     * @param x1 x coord of the line's left point facing outward
     * @param y1 y coord of the line's left point facing outward
     * @param x2 x coord of the line's right point facing outward
     * @param y2 y coord of the line's right point facing outward
     * @return the points' coordinates
     */
    static TriangleCoords splitLineKoch(double x1, double y1, double x2, double y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;

        double x3 = x1 + deltaX / 3;
        double y3 = y1 + deltaY / 3;

        double x4 = (0.5 * (x1+ x2) + Math.sqrt(3) * (y1- y2)/6);
        double y4 = (0.5 * (y1+ y2) + Math.sqrt(3) * (x2 -x1)/6);

        double x5 = x1 + 2 * deltaX / 3;
        double y5 = y1 + 2 * deltaY / 3;
        return new TriangleCoords(x3, y3, x4, y4, x5, y5);
    }

    /**
     * get the center point of a line
     * @param x1 x1
     * @param y1 y1
     * @param x2 x2
     * @param y2 y2
     * @return the center point's coordinates
     */
    static Point getCenter(double x1, double y1, double x2, double y2) {
        double deltaX1X2 = x2-x1;
        double deltaY1Y2 = y2-y1;
        double x3 = x1 + deltaX1X2 / 2;
        double y3 = y1 + deltaY1Y2 / 2;

        return new Point(x3, y3);
    }

}
