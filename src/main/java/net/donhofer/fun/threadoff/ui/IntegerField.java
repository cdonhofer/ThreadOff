package net.donhofer.fun.threadoff.ui;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

import java.util.function.UnaryOperator;

public class IntegerField extends TextField {
    private final int minValue, maxValue;
    public IntegerField(int defaultValue, int minValue, int maxValue) {
        super("" + defaultValue);
        this.minValue = minValue;
        this.maxValue = maxValue;

        this.setTextFormatter(
                new TextFormatter<>(new IntegerStringConverter(), defaultValue, integerFilter));

        this.textProperty().addListener((final ObservableValue<? extends String> ov, final String oldValue, final String newValue) -> {
            int newNumber = Integer.parseInt(newValue.length() > 0 ? newValue : ""+minValue);
            this.setText("" + Math.max(minValue, Math.min(maxValue, newNumber)));
        });
    }

    public void toggle(boolean show) {
        this.setVisible(show);
        this.setManaged(show);
    }
    public void show() {
        toggle(true);
    }

    public void hide() {
        toggle(false);
    }

    static UnaryOperator<TextFormatter.Change> integerFilter = change -> {
        String newText = change.getControlNewText();
        if (newText.matches("([1-9][0-9]*)?")) {
            return change;
        }
        return null;
    };
}
