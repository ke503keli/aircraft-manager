package fleet;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an aircraft: its own lifespan/flight-time/cycles, plus the
 * list of components currently installed on it.
 *
 * Every aircraft is created with a standard set of components already
 * installed (engine, propellers, landing gear, etc.) with filler details --
 * these behave exactly like any other component (can be removed, moved to
 * inventory, swapped out, serviced, etc.), they're just pre-populated so you
 * don't have to manually add them one by one every time. Since the user
 * isn't prompted individually for each of the 9 standard parts, each gets a
 * derived serial number built from the aircraft's own registration number
 * (e.g. "ENG-N12345") so they're still guaranteed unique across the fleet.
 */
public class Aircraft extends Asset {

    private double flightTime;   // cumulative hours flown, built up only via logFlight()
    private int cycles;          // number of takeoff/landing cycles, built up only via logFlight()
    private String location;
    private final List<Component> components = new ArrayList<>();

    /**
     * @param registrationNumber unique identifier for this aircraft, supplied
     *                           and validated by the caller (letters, digits,
     *                           '-' and '/' only).
     */
    public Aircraft(String registrationNumber, String name, String manufacturer, int manufacturingYear,
                    double lifespanHours, String description, String location) {
        super(registrationNumber, name, manufacturer, manufacturingYear, lifespanHours, description);
        this.flightTime = 0;
        this.cycles = 0;
        this.location = location;
        initializeStandardComponents();
    }

    @Override
    protected String getIdLabel() {
        return "Registration Number";
    }

    public String getRegistrationNumber() { return id; }

    /**
     * Installs the standard set of components every aircraft ships with.
     * Filler manufacturer/year/description are used since the user isn't
     * prompted for each one individually -- these can be edited afterward
     * like any other component.
     */
    private void initializeStandardComponents() {
        addStandardComponent("Engine", "Engine Bay", 2000, "ENG");
        addStandardComponent("Propellers", "Propeller Assembly", 1500, "PROP");
        addStandardComponent("Landing Gear", "Main Landing Gear", 3000, "LG");
        addStandardComponent("Fuel Control Unit", "Fuel System", 2500, "FCU");
        addStandardComponent("Fire Extinguisher", "Cabin - Fire Suppression", 5000, "FE");
        addStandardComponent("Main Gear Box", "Gearbox Housing", 2200, "MGB");
        addStandardComponent("Radios", "Cockpit Avionics", 4000, "RAD");
        addStandardComponent("Flight Data Computer", "Avionics Bay", 4000, "FDC");
        addStandardComponent("Strobe Lights", "Wingtip/Tail", 3000, "STL");
    }

    private void addStandardComponent(String name, String position, double lifespanHours, String serialPrefix) {
        String serialNumber = serialPrefix + "-" + this.id;
        Component c = new Component(
                serialNumber,
                name,
                this.manufacturer,
                this.manufacturingYear,
                lifespanHours,
                "Standard " + name.toLowerCase() + " installed at aircraft manufacture."
        );
        installComponent(c, position);
    }

    public double getFlightTime() { return flightTime; }
    public int getCycles() { return cycles; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public List<Component> getComponents() { return components; }

    /**
     * Records a real flight/usage entry: deducts the logged hours from the
     * aircraft's own lifespan AND from every currently installed component
     * (components only accumulate wear while installed), and adds to
     * cumulative flight time and cycle count. This is a log of something
     * that actually happened -- lifespan never changes any other way.
     */
    public void logFlight(double hours, int cyclesAdded) {
        if (hours < 0 || cyclesAdded < 0) throw new IllegalArgumentException("Values cannot be negative");
        this.useHours(hours);
        this.flightTime += hours;
        this.cycles += cyclesAdded;
        for (Component c : components) {
            c.useHours(hours);
        }
    }

    /** Installs a component (must currently be uninstalled, i.e. from inventory). */
    public void installComponent(Component c, String position) {
        if (c.isInstalled()) {
            throw new IllegalStateException(c.getName() + " is already installed on " + c.getInstalledOn().getName());
        }
        c.setPosition(position);
        c.setInstalledOn(this);
        components.add(c);
    }

    /** Removes a component from this aircraft, freeing it up (caller should add it to Inventory). */
    public void removeComponent(Component c) {
        if (!components.contains(c)) {
            throw new IllegalArgumentException(c.getName() + " is not installed on " + this.name);
        }
        components.remove(c);
        c.setPosition(null);
        c.setInstalledOn(null);
    }

    /** Convenience method to move a component directly to another aircraft. */
    public void moveComponentTo(Component c, Aircraft target, String newPosition) {
        this.removeComponent(c);
        target.installComponent(c, newPosition);
    }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(super.getDetails());
        sb.append("Location: ").append(location).append("\n");
        sb.append("Flight Time (H:MM): ").append(formatHoursAsHM(flightTime)).append("\n");
        sb.append("Cycles: ").append(cycles).append("\n");
        sb.append("Components (").append(components.size()).append("):\n");
        for (Component c : components) {
            sb.append("  - [").append(c.getSerialNumber()).append("] ").append(c.getName())
                    .append(" [").append(c.getPosition()).append("] - ")
                    .append(c.getStatus()).append("\n");
        }
        return sb.toString();
    }
}