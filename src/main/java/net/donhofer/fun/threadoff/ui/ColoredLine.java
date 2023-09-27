package net.donhofer.fun.threadoff.ui;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class ColoredLine extends Line {
    public ColoredLine(double startX, double startY, double endX, double endY, Color color) {
        super(startX, startY, endX, endY);
        setStroke(color);
    }
}
