package net.donhofer.fun.threadoff.ui;

import javafx.scene.control.Label;

/**
 * value displaying a key and value(text), with support to change only the value
 */
public class KeyValueLabel extends Label {
    private final String key;
    private String value;
    public KeyValueLabel(String key, String value) {
        super(buildText(key, value));
        getStyleClass().add("value");
        this.key = key;
        this.value = value;
    }

    private static String buildText(String key, String label) {
        return String.format("%s: %s", key, label);
    }

    public void setValue(String value) {
        this.value = value;
        setText(buildText(key, value));
    }

}
