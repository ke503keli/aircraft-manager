package fleet.ui;

import fleet.Component;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.List;

public class ComponentListController {

    @FXML private TableView<Component> componentTable;
    @FXML private TableColumn<Component, String> serialColumn;
    @FXML private TableColumn<Component, String> nameColumn;
    @FXML private TableColumn<Component, String> statusColumn;
    @FXML private TableColumn<Component, String> locationColumn;
    @FXML private Button viewDetailsButton;

    private FleetApp app;

    @FXML
    private void initialize() {
        serialColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSerialNumber()));
        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));
        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus().toString()));
        locationColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLocationSummary()));

        viewDetailsButton.disableProperty().bind(
                componentTable.getSelectionModel().selectedItemProperty().isNull());

        componentTable.setRowFactory(tv -> {
            TableRow<Component> row = new TableRow<>();
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

    public void setComponentList(List<Component> components) {
        componentTable.setItems(FXCollections.observableArrayList(components));
    }

    @FXML
    private void onViewDetails() {
        Component selected = componentTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openDetails(selected);
        }
    }

    @FXML
    private void onAddComponent() {
        if (app != null) {
            app.showAddComponentForm();
        }
    }

    @FXML
    private void onDashboard() {
        if (app != null) {
            app.showDashboard();
        }
    }

    @FXML
    private void onBackToAircraft() {
        if (app != null) {
            app.showAircraftList();
        }
    }

    private void openDetails(Component component) {
        if (app != null) {
            app.showComponentDetail(component);
        }
    }
}