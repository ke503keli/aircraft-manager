package fleet.ui;

import fleet.Aircraft;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.List;

public class AircraftListController {

    @FXML private TableView<Aircraft> aircraftTable;
    @FXML private TableColumn<Aircraft, String> registrationColumn;
    @FXML private TableColumn<Aircraft, String> nameColumn;
    @FXML private TableColumn<Aircraft, String> statusColumn;
    @FXML private TableColumn<Aircraft, String> locationColumn;
    @FXML private Button viewDetailsButton;
    @FXML



    private FleetApp app;

    /**
     * Called automatically by FXMLLoader after all @FXML fields are injected
     * -- this is where per-screen setup that doesn't depend on data (column
     * wiring, selection listeners) belongs, as opposed to setAircraftList(),
     * which depends on data the caller provides afterward.
     */
    @FXML
    private void initialize() {
        registrationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRegistrationNumber()));
        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));
        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus().toString()));
        locationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLocation()));

        // "View Details" only makes sense once something is selected.
        viewDetailsButton.disableProperty().bind(
                aircraftTable.getSelectionModel().selectedItemProperty().isNull());

        // Double-click a row as a shortcut to the same action as the button.
        aircraftTable.setRowFactory(tv -> {
            TableRow<Aircraft> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    openDetails(row.getItem());
                }
            });
            return row;
        });
    }

    public void setApp(FleetApp app) {
        this.app = app;
    }

    public void setAircraftList(List<Aircraft> aircraftList) {
        aircraftTable.setItems(FXCollections.observableArrayList(aircraftList));
    }

    @FXML
    private void onViewDetails() {
        Aircraft selected = aircraftTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openDetails(selected);
        }
    }

    @FXML
    private void onViewComponents() {
        if (app != null) {
            app.showComponentList();
        }
    }

    @FXML
    private void onAddAircraft() {
        if (app != null) {
            app.showAddAircraftForm();
        }
    }

    @FXML
    private void onDashboard() {
        if (app != null) {
            app.showDashboard();
        }
    }

    private void openDetails(Aircraft aircraft) {
        if (app != null) {
            app.showAircraftDetail(aircraft);
        }
    }
}