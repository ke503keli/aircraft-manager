package fleet.ui;

import fleet.Aircraft;
import fleet.Component;
import fleet.persistence.AircraftRepository;
import fleet.persistence.ComponentRepository;
import fleet.persistence.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FleetApp extends Application {

    private final AircraftRepository aircraftRepository = new AircraftRepository();
    private final ComponentRepository componentRepository = new ComponentRepository();
    private Stage primaryStage;


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        try {
            DatabaseManager.initializeSchema();
        } catch (SQLException e) {
            showError("Failed to initialize database", e.getMessage());
        }
        showDashboard();
        primaryStage.show();
    }

    public void showAircraftList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AircraftList.fxml"));
            Parent root = loader.load();

            AircraftListController controller = loader.getController();
            controller.setApp(this);

            List<Aircraft> aircraft = loadAllAircraftWithComponents();
            controller.setAircraftList(aircraft);

            primaryStage.setTitle("Aircraft Manager - Fleet (" + aircraft.size() + " aircraft)");
            primaryStage.setScene(new Scene(root, 720, 460));
        } catch (IOException e) {
            showError("Failed to load the aircraft list screen", e.getMessage());
        } catch (SQLException e) {
            showError("Failed to read aircraft from the database", e.getMessage());
        }
    }

    public void showAircraftDetail(Aircraft aircraft) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AircraftDetail.fxml"));
            Parent root = loader.load();

            AircraftDetailController controller = loader.getController();
            controller.setApp(this);
            controller.setAircraft(aircraft);

            primaryStage.setTitle("Aircraft Manager - " + aircraft.getRegistrationNumber());
            primaryStage.setScene(new Scene(root, 720, 520));
        } catch (IOException e) {
            showError("Failed to load the aircraft detail screen", e.getMessage());
        }
    }

    /**
     * Combined view across BOTH sources of components: every aircraft's
     * installed components, plus everything sitting in inventory. Sorted by
     * name then serial number purely for a stable, readable table order --
     * the underlying data has no inherent ordering.
     */
    public void showComponentList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ComponentList.fxml"));
            Parent root = loader.load();

            ComponentListController controller = loader.getController();
            controller.setApp(this);

            List<Aircraft> aircraft = loadAllAircraftWithComponents();
            List<Component> inventoryComponents = componentRepository.findInInventory();

            List<Component> allComponents = Stream.concat(
                            aircraft.stream().flatMap(a -> a.getComponents().stream()),
                            inventoryComponents.stream())
                    .sorted(Comparator.comparing(Component::getName)
                            .thenComparing(Component::getSerialNumber))
                    .collect(Collectors.toList());

            controller.setComponentList(allComponents);

            primaryStage.setTitle("Aircraft Manager - Components (" + allComponents.size() + ")");
            primaryStage.setScene(new Scene(root, 760, 480));
        } catch (IOException e) {
            showError("Failed to load the component list screen", e.getMessage());
        } catch (SQLException e) {
            showError("Failed to read components from the database", e.getMessage());
        }
    }

    public void showComponentDetail(Component component) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ComponentDetail.fxml"));
            Parent root = loader.load();

            ComponentDetailController controller = loader.getController();
            controller.setApp(this);
            controller.setComponent(component);

            primaryStage.setTitle("Aircraft Manager - " + component.getSerialNumber());
            primaryStage.setScene(new Scene(root, 720, 520));
        } catch (IOException e) {
            showError("Failed to load the component detail screen", e.getMessage());
        }
    }

    public void showDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashboard.fxml"));
            Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.setApp(this);

            List<Aircraft> aircraft = loadAllAircraftWithComponents();
            List<Component> inventoryComponents = componentRepository.findInInventory();
            controller.setData(aircraft, inventoryComponents);

            primaryStage.setTitle("Aircraft Manager - Dashboard");
            primaryStage.setScene(new Scene(root, 620, 480));
        } catch (IOException e) {
            showError("Failed to load the dashboard", e.getMessage());
        } catch (SQLException e) {
            showError("Failed to read fleet data", e.getMessage());
        }
    }

    /**
     * Loads every aircraft and attaches its real, persisted components --
     * shared by both showAircraftList() and showComponentList() so the
     * "fetch + attach" logic exists in exactly one place.
     */
    private List<Aircraft> loadAllAircraftWithComponents() throws SQLException {
        List<Aircraft> aircraft = aircraftRepository.findAll();
        for (Aircraft a : aircraft) {
            componentRepository.loadAndAttachComponentsFor(a);
        }
        return aircraft;
    }

    public void showAddAircraftForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddAircraft.fxml"));
            Parent root = loader.load();

            AddAircraftController controller = loader.getController();
            controller.setApp(this);

            primaryStage.setTitle("Aircraft Manager - Add Aircraft");
            primaryStage.setScene(new Scene(root, 480, 560));
        } catch (IOException e) {
            showError("Failed to load the add-aircraft screen", e.getMessage());
        }
    }

    public void showAddComponentForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddComponent.fxml"));
            Parent root = loader.load();

            AddComponentController controller = loader.getController();
            controller.setApp(this);

            primaryStage.setTitle("Aircraft Manager - Add Component");
            primaryStage.setScene(new Scene(root, 480, 500));
        } catch (IOException e) {
            showError("Failed to load the add-component screen", e.getMessage());
        }
    }

    public void showLogFlightForm(Aircraft aircraft) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("LogFlight.fxml"));
            Parent root = loader.load();

            LogFlightController controller = loader.getController();
            controller.setApp(this);
            controller.setAircraft(aircraft);

            primaryStage.setTitle("Aircraft Manager - Log Flight - " + aircraft.getRegistrationNumber());
            primaryStage.setScene(new Scene(root, 480, 360));
        } catch (IOException e) {
            showError("Failed to load the log-flight screen", e.getMessage());
        }
    }

    public void showInstallComponentForm(Component component) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("InstallComponent.fxml"));
            Parent root = loader.load();

            InstallComponentController controller = loader.getController();
            controller.setApp(this);
            controller.setComponent(component);
            controller.setAvailableAircraft(loadAllAircraftWithComponents());

            primaryStage.setTitle("Aircraft Manager - Install " + component.getSerialNumber());
            primaryStage.setScene(new Scene(root, 480, 420));
        } catch (IOException e) {
            showError("Failed to load the install-component screen", e.getMessage());
        } catch (SQLException e) {
            showError("Failed to load aircraft list", e.getMessage());
        }
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}