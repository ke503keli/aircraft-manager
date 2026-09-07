package fleet.ui;

import fleet.Aircraft;
import fleet.Component;
import fleet.persistence.AircraftRepository;
import fleet.persistence.ComponentRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import java.util.Optional;

import java.sql.SQLException;

public class ComponentDetailController {

    @FXML private Label titleLabel;
    @FXML private TextArea detailsArea;
    @FXML private Button installButton;
    @FXML private Button removeButton;

    private final AircraftRepository aircraftRepository = new AircraftRepository();
    private final ComponentRepository componentRepository = new ComponentRepository();

    private FleetApp app;
    private Component component;

    public void setApp(FleetApp app) {
        this.app = app;
    }

    public void setComponent(Component component) {
        this.component = component;
        titleLabel.setText(component.getName() + " (" + component.getSerialNumber() + ")");
        detailsArea.setText(component.getDetails());

        // The two actions are mutually exclusive by definition -- a component
        // is either installed or in inventory, never both/neither.
        installButton.setVisible(!component.isInstalled());
        installButton.setManaged(!component.isInstalled());
        removeButton.setVisible(component.isInstalled());
        removeButton.setManaged(component.isInstalled());

    }

    @FXML private Button deleteButton;

    @FXML
    private void onBack() {
        if (app != null) {
            app.showComponentList();
        }
    }

    @FXML
    private void onInstallOnAircraft() {
        if (app != null) {
            app.showInstallComponentForm(component);
        }
    }

    @FXML
    private void onRemoveToInventory() {
        // Mirrors Main.removeComponentToInventory(): detach from its aircraft
        // (clears position/installedOn together, per Component's own invariant),
        // then save -- no separate Inventory object needed, since "in inventory"
        // is just installed_on IS NULL once the row is saved.
        Aircraft currentAircraft = component.getInstalledOn();
        try {
            currentAircraft.removeComponent(component);
            componentRepository.save(component);
        } catch (SQLException e) {
            showError("Failed to move component to inventory", e.getMessage());
            return;
        }

        setComponent(component); // refresh this screen to reflect the new state
    }

    @FXML
    private void onPerformMaintenance() {
        try {
            component.performMaintenance();
            componentRepository.save(component);
        } catch (SQLException e) {
            showError("Failed to save maintenance record", e.getMessage());
            return;
        }
        setComponent(component); // refresh details text and button visibility
    }

    @FXML
    private void onDeleteComponent() {
        Alert firstConfirm = new Alert(Alert.AlertType.CONFIRMATION);
        firstConfirm.setTitle("Confirm Deletion");
        firstConfirm.setHeaderText("Permanently delete this component?");
        firstConfirm.setContentText("\"" + component.getName() + "\" (" + component.getSerialNumber()
                + "). This cannot be undone.");
        Optional<ButtonType> firstResult = firstConfirm.showAndWait();
        if (firstResult.isEmpty() || firstResult.get() != ButtonType.OK) {
            return;
        }

        TextInputDialog typedConfirm = new TextInputDialog();
        typedConfirm.setTitle("Confirm Deletion");
        typedConfirm.setHeaderText("To confirm, type the exact serial number of the component to delete.");
        typedConfirm.setContentText("Serial Number (" + component.getSerialNumber() + "):");
        Optional<String> typedResult = typedConfirm.showAndWait();

        if (typedResult.isEmpty() || !typedResult.get().equalsIgnoreCase(component.getSerialNumber())) {
            showInfo("Deletion Cancelled", "The serial number did not match. Nothing was deleted.");
            return;
        }

        // Mirrors Main.deleteComponent(): detach from its aircraft in memory if
        // installed (no separate save needed -- the row is about to be deleted
        // outright), then delete the row. No Inventory object to update on this
        // side for the uninstalled case; deleting the row handles that too.
        try {
            if (component.isInstalled()) {
                component.getInstalledOn().removeComponent(component);
            }
            componentRepository.delete(component.getSerialNumber());
        } catch (SQLException e) {
            showError("Failed to delete component", e.getMessage());
            return;
        }

        if (app != null) {
            app.showComponentList();
        }
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notice");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}