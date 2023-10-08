package net.donhofer.fun.threadoff.ui;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * custom shape introduced to ease handling compared to Polygon, which is quite complicated when used with a Canvas
 */
public class Triangle extends Polygon {

    private final double[] yCoords;
    private final double[] xCoords;

    public Triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        super();
        xCoords = new double[] {x1, x2, x3};
        yCoords = new double[] {y1, y2, y3};
        setStroke(color);
    }

    public double[] getYCoords() {
        return yCoords;
    }

    public double[] getXCoords() {
        return xCoords;
    }
}
