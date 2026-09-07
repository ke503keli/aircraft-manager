package fleet.ui;

import fleet.Component;
import fleet.persistence.ComponentRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.sql.SQLException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class AddComponentController {

    /** Mirrors Main.java's ID_CHARSET_REGEX -- see the same note in AddAircraftController. */
    private static final String ID_CHARSET_REGEX = "[A-Za-z0-9\\-/]+";

    @FXML private TextField serialField;
    @FXML private TextField nameField;
    @FXML private TextField manufacturerField;
    @FXML private TextField yearField;
    @FXML private TextField lifespanField;
    @FXML private TextArea descriptionArea;
    @FXML private Label errorLabel;

    private final ComponentRepository componentRepository = new ComponentRepository();

    private FleetApp app;

    public void setApp(FleetApp app) {
        this.app = app;
    }

    @FXML
    private void onSave() {
        List<String> errors = new ArrayList<>();

        String serial = serialField.getText().trim();
        String name = nameField.getText().trim();
        String manufacturer = manufacturerField.getText().trim();
        String yearText = yearField.getText().trim();
        String lifespanText = lifespanField.getText().trim();
        String description = descriptionArea.getText().trim();

        // --- Serial number: non-blank, charset, has letter/digit, unique ---
        if (serial.isEmpty()) {
            errors.add("Serial number can't be empty.");
        } else if (!serial.matches(ID_CHARSET_REGEX)) {
            errors.add("Serial number can only contain letters, numbers, '-' and '/'.");
        } else if (serial.chars().noneMatch(Character::isLetterOrDigit)) {
            errors.add("Serial number must include at least one letter or number.");
        } else {
            try {
                if (componentRepository.findBySerialNumber(serial).isPresent()) {
                    errors.add("\"" + serial + "\" is already in use. Choose a unique serial number.");
                }
            } catch (SQLException e) {
                errors.add("Could not check serial number uniqueness: " + e.getMessage());
            }
        }

        if (name.isEmpty()) errors.add("Component name can't be empty.");
        if (manufacturer.isEmpty()) errors.add("Manufacturer can't be empty.");

        // --- Manufacturing year: whole number, 1900 through current year ---
        int year = 0;
        int currentYear = Year.now().getValue();
        try {
            year = Integer.parseInt(yearText);
            if (year < 1900 || year > currentYear) {
                errors.add("Manufacturing year must be between 1900 and " + currentYear + ".");
            }
        } catch (NumberFormatException e) {
            errors.add("Manufacturing year must be a whole number.");
        }

        // --- Lifespan: H:MM format, must be greater than 0:00 ---
        double lifespan = 0;
        Double parsedLifespan = parseDurationHM(lifespanText, errors, "Lifespan");
        if (parsedLifespan != null) {
            lifespan = parsedLifespan;
            if (lifespan <= 0) {
                errors.add("Lifespan must be greater than 0:00.");
            }
        }

        if (!errors.isEmpty()) {
            errorLabel.setText(String.join("\n", errors));
            return;
        }

        try {
            Component component = new Component(serial, name, manufacturer, year, lifespan, description);
            componentRepository.save(component);
        } catch (SQLException e) {
            showFatalError("Failed to save the new component", e.getMessage());
            return;
        }

        if (app != null) {
            app.showComponentList();
        }
    }

    @FXML
    private void onCancel() {
        if (app != null) {
            app.showComponentList();
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