package fleet.ui;

import fleet.Aircraft;
import fleet.Component;
import fleet.persistence.ComponentRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;

public class InstallComponentController {

    @FXML private Label componentLabel;
    @FXML private ComboBox<Aircraft> aircraftComboBox;
    @FXML private TextField positionField;
    @FXML private Label errorLabel;

    private final ComponentRepository componentRepository = new ComponentRepository();

    private FleetApp app;
    private Component component;

    public void setApp(FleetApp app) {
        this.app = app;
    }

    public void setComponent(Component component) {
        this.component = component;
        componentLabel.setText("Installing: " + component.getName() + " (" + component.getSerialNumber() + ")");
    }

    public void setAvailableAircraft(List<Aircraft> aircraft) {
        aircraftComboBox.setItems(FXCollections.observableArrayList(aircraft));
        // Show "Name (RegistrationNumber)" instead of Aircraft's default toString().
        aircraftComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Aircraft a) {
                return a == null ? "" : a.getName() + " (" + a.getRegistrationNumber() + ")";
            }
            @Override
            public Aircraft fromString(String s) {
                return null; // not needed -- selection only happens via the dropdown
            }
        });
    }

    @FXML
    private void onSave() {
        Aircraft selected = aircraftComboBox.getValue();
        String position = positionField.getText().trim();

        if (selected == null) {
            errorLabel.setText("Please select an aircraft.");
            return;
        }
        if (position.isEmpty()) {
            errorLabel.setText("Position can't be empty.");
            return;
        }

        // Mirrors Main.installComponentFromInventory(): install directly onto
        // the target aircraft, then save -- no Inventory object needed, since
        // the component simply stops being "in inventory" (installed_on becomes
        // non-null) the moment this save happens.
        try {
            selected.installComponent(component, position);
            componentRepository.save(component);
        } catch (SQLException e) {
            showFatalError("Failed to install component", e.getMessage());
            return;
        }

        if (app != null) {
            app.showComponentDetail(component);
        }
    }

    @FXML
    private void onCancel() {
        if (app != null) {
            app.showComponentDetail(component);
        }
    }

    private void showFatalError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}