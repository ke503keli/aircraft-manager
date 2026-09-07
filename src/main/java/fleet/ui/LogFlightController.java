package fleet.ui;

import fleet.Aircraft;
import fleet.Asset;
import fleet.Component;
import fleet.persistence.AircraftRepository;
import fleet.persistence.ComponentRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LogFlightController {

    @FXML private Label aircraftLabel;
    @FXML private TextField hoursField;
    @FXML private TextField cyclesField;
    @FXML private Label errorLabel;

    private final AircraftRepository aircraftRepository = new AircraftRepository();
    private final ComponentRepository componentRepository = new ComponentRepository();

    private FleetApp app;
    private Aircraft aircraft;

    public void setApp(FleetApp app) {
        this.app = app;
    }

    public void setAircraft(Aircraft aircraft) {
        this.aircraft = aircraft;
        aircraftLabel.setText(aircraft.getName() + " (" + aircraft.getRegistrationNumber() + ") -- "
                + "Current lifespan: " + Asset.formatHoursAsHM(aircraft.getLifespanHours()));
    }

    @FXML
    private void onSave() {
        List<String> errors = new ArrayList<>();

        String hoursText = hoursField.getText().trim();
        String cyclesText = cyclesField.getText().trim();

        // --- Hours: H:MM format, must be greater than 0:00 (matches promptPositiveDurationHM) ---
        double hours = 0;
        Double parsedHours = parseDurationHM(hoursText, errors, "Time to log");
        if (parsedHours != null) {
            hours = parsedHours;
            if (hours <= 0) {
                errors.add("Time to log must be greater than 0:00.");
            }
        }

        // --- Cycles: whole number, zero or greater (matches promptNonNegativeInt) ---
        int cycles = 0;
        try {
            cycles = Integer.parseInt(cyclesText);
            if (cycles < 0) {
                errors.add("Cycles can't be negative.");
            }
        } catch (NumberFormatException e) {
            errors.add("Cycles must be a whole number.");
        }

        if (!errors.isEmpty()) {
            errorLabel.setText(String.join("\n", errors));
            return;
        }

        try {
            aircraft.logFlight(hours, cycles);
            aircraftRepository.save(aircraft);
            for (Component c : aircraft.getComponents()) {
                componentRepository.save(c);
            }
        } catch (SQLException e) {
            showFatalError("Failed to save the logged flight", e.getMessage());
            return;
        }

        if (app != null) {
            app.showAircraftDetail(aircraft);
        }
    }

    @FXML
    private void onCancel() {
        if (app != null) {
            app.showAircraftDetail(aircraft);
        }
    }

    /** Same H:MM parsing as AddAircraftController -- see that class for details. */
    private Double parseDurationHM(String input, List<String> errors, String fieldLabel) {
        String[] parts = input.split(":");
        if (parts.length != 2) {
            errors.add(fieldLabel + " must be in H:MM format, e.g. 1:34.");
            return null;
        }
        try {
            int hoursPart = Integer.parseInt(parts[0].trim());
            int minutesPart = Integer.parseInt(parts[1].trim());
            if (hoursPart < 0 || minutesPart < 0 || minutesPart > 59) {
                errors.add(fieldLabel + ": hours must be 0 or more, minutes must be 0-59.");
                return null;
            }
            return hoursPart + (minutesPart / 60.0);
        } catch (NumberFormatException e) {
            errors.add(fieldLabel + " must be whole-number hours:minutes, e.g. 1:34.");
            return null;
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