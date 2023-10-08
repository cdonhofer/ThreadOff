package net.donhofer.fun.threadoff.data;

import javafx.scene.shape.Shape;

import java.util.List;
import java.util.concurrent.Callable;

public record InitialData(int expectedTasks, List<? extends Callable<List<Shape>>> initialTasks, List<Shape> initialShapes) {
}
