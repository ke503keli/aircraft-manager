package fleet.ui;

import fleet.Aircraft;
import fleet.AssetStatus;
import fleet.Component;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DashboardController {

    @FXML private Label aircraftCountLabel;
    @FXML private Label aircraftServiceableLabel;
    @FXML private Label aircraftIssuesLabel;
    @FXML private Label aircraftUnserviceableLabel;

    @FXML private Label componentCountLabel;
    @FXML private Label componentServiceableLabel;
    @FXML private Label componentIssuesLabel;
    @FXML private Label componentUnserviceableLabel;

    @FXML private Label installedCountLabel;
    @FXML private Label inventoryCountLabel;

    private FleetApp app;

    public void setApp(FleetApp app) {
        this.app = app;
    }

    public void setData(List<Aircraft> aircraft, List<Component> inventoryComponents) {
        aircraftCountLabel.setText(String.valueOf(aircraft.size()));

        Map<AssetStatus, Long> aircraftByStatus = aircraft.stream()
                .collect(Collectors.groupingBy(Aircraft::getStatus, Collectors.counting()));
        aircraftServiceableLabel.setText(String.valueOf(aircraftByStatus.getOrDefault(AssetStatus.SERVICEABLE, 0L)));
        aircraftIssuesLabel.setText(String.valueOf(aircraftByStatus.getOrDefault(AssetStatus.SERVICEABLE_WITH_ISSUES, 0L)));
        aircraftUnserviceableLabel.setText(String.valueOf(aircraftByStatus.getOrDefault(AssetStatus.UNSERVICEABLE, 0L)));

        List<Component> installedComponents = aircraft.stream()
                .flatMap(a -> a.getComponents().stream())
                .collect(Collectors.toList());

        List<Component> allComponents = Stream.concat(installedComponents.stream(), inventoryComponents.stream())
                .collect(Collectors.toList());

        componentCountLabel.setText(String.valueOf(allComponents.size()));

        Map<AssetStatus, Long> componentByStatus = allComponents.stream()
                .collect(Collectors.groupingBy(Component::getStatus, Collectors.counting()));
        componentServiceableLabel.setText(String.valueOf(componentByStatus.getOrDefault(AssetStatus.SERVICEABLE, 0L)));
        componentIssuesLabel.setText(String.valueOf(componentByStatus.getOrDefault(AssetStatus.SERVICEABLE_WITH_ISSUES, 0L)));
        componentUnserviceableLabel.setText(String.valueOf(componentByStatus.getOrDefault(AssetStatus.UNSERVICEABLE, 0L)));

        installedCountLabel.setText(String.valueOf(installedComponents.size()));
        inventoryCountLabel.setText(String.valueOf(inventoryComponents.size()));
    }

    @FXML
    private void onViewAircraft() {
        if (app != null) {
            app.showAircraftList();
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
    private void onAddComponent() {
        if (app != null) {
            app.showAddComponentForm();
        }
    }
}