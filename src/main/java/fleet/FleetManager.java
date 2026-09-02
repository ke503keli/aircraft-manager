package fleet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Top-level manager tying together the fleet of aircraft and the shared
 * component inventory. This is the class a future UI/service layer (or a
 * database-backed persistence layer) would sit on top of.
 */
public class FleetManager {

    private final List<Aircraft> aircraftList = new ArrayList<>();
    private final Inventory inventory = new Inventory();

    public List<Aircraft> getAircraftList() { return aircraftList; }
    public Inventory getInventory() { return inventory; }

    public void addAircraft(Aircraft a) { aircraftList.add(a); }

    public Optional<Aircraft> findAircraftByName(String name) {
        return aircraftList.stream()
                .filter(a -> a.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public Optional<Aircraft> findAircraftByRegistrationNumber(String registrationNumber) {
        return aircraftList.stream()
                .filter(a -> a.getRegistrationNumber().equalsIgnoreCase(registrationNumber))
                .findFirst();
    }

    /** Every component in the system: installed on any aircraft, plus everything in inventory. */
    public List<Component> getAllComponents() {
        List<Component> all = new ArrayList<>();
        for (Aircraft a : aircraftList) {
            all.addAll(a.getComponents());
        }
        all.addAll(inventory.getComponents());
        return all;
    }

    /**
     * Finds a single component by its permanent, guaranteed-unique serial
     * number. This is the reliable way to identify one specific component
     * when multiple share the same name (e.g. the default "Engine" installed
     * on several different aircraft).
     */
    public Optional<Component> findComponentBySerialNumber(String serialNumber) {
        return getAllComponents().stream()
                .filter(c -> c.getSerialNumber().equalsIgnoreCase(serialNumber))
                .findFirst();
    }

    /**
     * Finds every component whose name matches (case-insensitive), across
     * inventory and every aircraft. Deliberately returns ALL matches rather
     * than silently picking one -- names are not unique (e.g. every aircraft
     * ships with a component named "Engine"), so callers must handle the
     * case of more than one result.
     */
    public List<Component> findComponentsByName(String name) {
        return getAllComponents().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
    }
}