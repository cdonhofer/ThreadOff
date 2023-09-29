package net.donhofer.fun.threadoff.ui;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import net.donhofer.fun.threadoff.data.SelectableTask;

import java.util.List;
import java.util.Optional;

public class TaskComboBox extends ComboBox<SelectableTask> {
    public TaskComboBox(List<SelectableTask> selectableTasks) {
        super(FXCollections.observableArrayList(selectableTasks));
        this.setCellFactory(lv -> new TaskListCell());
        this.setButtonCell(new TaskListCell());
    }

    private static class TaskListCell extends ListCell<SelectableTask> {
        @Override
        protected void updateItem(SelectableTask item, boolean empty) {
            super.updateItem(item, empty);
            setText(Optional.ofNullable(item).map(SelectableTask::name).orElse(null));
        }
    }
}
