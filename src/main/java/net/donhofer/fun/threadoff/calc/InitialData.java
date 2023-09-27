package net.donhofer.fun.threadoff.calc;

import javafx.scene.shape.Shape;

import java.util.List;
import java.util.concurrent.Callable;

public record InitialData(int expectedTasks, List<? extends Callable<List<Shape>>> initialTasks) {
}
