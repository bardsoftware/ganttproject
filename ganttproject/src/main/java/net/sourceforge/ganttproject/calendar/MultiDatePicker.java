package net.sourceforge.ganttproject.calendar;

import com.sun.javafx.scene.control.DatePickerContent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.skin.DatePickerSkin;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public class MultiDatePicker extends DatePicker {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private ArrayList<LocalDate> selectedDates = new ArrayList();
    private LocalDate lastSelectedDate = null;

  public MultiDatePicker() {
        super();
        setSkin(new DatePickerSkin(this));
        onRangeSelectionMode();

  }

    public MultiDatePicker onRangeSelectionMode() {

        EventHandler<MouseEvent> mouseClickedEventHandler = (MouseEvent clickEvent) -> {
            if (clickEvent.getButton() == MouseButton.PRIMARY) {

                if (lastSelectedDate == null) {
                    this.selectedDates.add(this.getValue());
                } else {
                    this.selectedDates.clear();

                    LocalDate startDate;
                    LocalDate endDate;

                    if (this.getValue().isAfter(lastSelectedDate)) {
                        startDate = lastSelectedDate;
                        endDate = this.getValue();
                    } else {
                        startDate = this.getValue();
                        endDate = lastSelectedDate;
                    }

                    do {
                        selectedDates.add(startDate);
                        startDate = startDate.plusDays(1);
                    } while (!startDate.isAfter(endDate));
                }

                this.lastSelectedDate = this.getValue();
            }

            DatePickerContent datePickerContent = (DatePickerContent)getPopupContent();
            datePickerContent.updateDayCells();

            clickEvent.consume();
        };

        this.setDayCellFactory((DatePicker param) -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                //...
                if (item != null && !empty) {
                    //...
                    addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedEventHandler);
                } else {
                    //...
                    removeEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedEventHandler);
                }

                if (!selectedDates.isEmpty() && selectedDates.contains(item)) {
                    if (Objects.equals(item, selectedDates.toArray()[0]) || Objects.equals(item, selectedDates.toArray()[selectedDates.size() - 1])) {
                        setStyle("-fx-background-color: rgba(3, 169, 1, 0.7);");
                    } else {
                        setStyle("-fx-background-color: rgba(3, 169, 244, 0.7);");
                    }
                } else {
                    setStyle(null);
                }
            }
        });
        return this;
    }


    public Node getPopupContent() {
        return ((DatePickerSkin)this.getSkin()).getPopupContent();
    }

    public List<LocalDate> getSelectedDates() {
        return this.selectedDates;
    }
}
