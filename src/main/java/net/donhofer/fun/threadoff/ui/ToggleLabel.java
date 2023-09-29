package net.donhofer.fun.threadoff.ui;

import javafx.scene.control.Label;

public class ToggleLabel extends Label {
    public ToggleLabel(String s) {
        super(s);
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
}
