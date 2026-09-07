package fleet.ui;

import fleet.Aircraft;
import fleet.Component;
import fleet.persistence.AircraftRepository;
import fleet.persistence.ComponentRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AircraftDetailController {

    @FXML private Label titleLabel;
    @FXML private TextArea detailsArea;

    private final AircraftRepository aircraftRepository = new AircraftRepository();
    private final ComponentRepository componentRepository = new ComponentRepository();

    private FleetApp app;
    private Aircraft aircraft;

    public void setApp(FleetApp app) {
        this.app = app;
    }

    public void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
        titleLabel.setText(aircraft.getName() + " (" + aircraft.getRegistrationNumber() + ")");
        detailsArea.setText(aircraft.getDetails());
    }

    @FXML
    private void onBack() {
        if (app != null) {
            app.showAircraftList();
        }
    }

    @FXML
    private void onDeleteAircraft() {
        // --- First confirmation: plain yes/no ---
        Alert firstConfirm = new Alert(Alert.AlertType.CONFIRMATION);
        firstConfirm.setTitle("Confirm Deletion");
        firstConfirm.setHeaderText("Permanently delete this aircraft?");
        firstConfirm.setContentText("\"" + aircraft.getName() + "\" (" + aircraft.getRegistrationNumber()
                + "). This cannot be undone.");
        Optional<ButtonType> firstResult = firstConfirm.showAndWait();
        if (firstResult.isEmpty() || firstResult.get() != ButtonType.OK) {
            return; // Cancelled -- silently do nothing, same as the terminal.
        }

        // --- Second confirmation: must type back the exact registration number ---
        TextInputDialog typedConfirm = new TextInputDialog();
        typedConfirm.setTitle("Confirm Deletion");
        typedConfirm.setHeaderText("To confirm, type the exact registration number of the aircraft to delete.");
        typedConfirm.setContentText("Registration Number (" + aircraft.getRegistrationNumber() + "):");
        Optional<String> typedResult = typedConfirm.showAndWait();

        if (typedResult.isEmpty() || !typedResult.get().equalsIgnoreCase(aircraft.getRegistrationNumber())) {
            showInfo("Deletion Cancelled", "The registration number did not match. Nothing was deleted.");
            return;
        }

        // --- Detach installed components back to inventory BEFORE deleting the aircraft row ---
        // Mirrors Main.deleteAircraft(): the component table's CHECK constraint requires
        // installed_on and position to be null TOGETHER, and SQLite's ON DELETE SET NULL
        // cascade only clears installed_on -- so both are cleared explicitly here first.
        try {
            List<Component> installed = new ArrayList<>(aircraft.getComponents());
            for (Component c : installed) {
                aircraft.removeComponent(c);
                componentRepository.save(c);
            }
            aircraftRepository.delete(aircraft.getRegistrationNumber());
        } catch (SQLException e) {
            showError("Failed to delete aircraft", e.getMessage());
            return;
        }

        if (app != null) {
            app.showAircraftList();
        }
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notice");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onLogFlight() {
        if (app != null) {
            app.showLogFlightForm(aircraft);
        }
    }

    @FXML
    private void onPerformMaintenance() {
        try {
            aircraft.performMaintenance();
            aircraftRepository.save(aircraft);
        } catch (SQLException e) {
            showError("Failed to save maintenance record", e.getMessage());
            return;
        }
        setAircraft(aircraft); // refresh the details text to show the reset lifespan/status
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}