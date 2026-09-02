package fleet;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all spare (unattached) components. Enforces that a component
 * cannot exist in inventory while also installed on an aircraft.
 */
public class Inventory {

    private final List<Component> components = new ArrayList<>();

    public void addComponent(Component c) {
        if (c.isInstalled()) {
            throw new IllegalStateException(c.getName() + " is currently installed and must be removed from its aircraft first.");
        }
        if (components.contains(c)) {
            throw new IllegalStateException(c.getName() + " is already in inventory.");
        }
        components.add(c);
    }

    public void removeComponent(Component c) {
        if (!components.remove(c)) {
            throw new IllegalArgumentException(c.getName() + " not found in inventory.");
        }
    }

    /** Moves a component straight from inventory onto an aircraft. */
    public void moveToAircraft(Component c, Aircraft target, String position) {
        if (!components.contains(c)) {
            throw new IllegalArgumentException(c.getName() + " not found in inventory.");
        }
        components.remove(c);
        target.installComponent(c, position);
    }

    public List<Component> getComponents() { return components; }
}