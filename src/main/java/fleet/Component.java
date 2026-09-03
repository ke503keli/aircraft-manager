package fleet;

/**
 * Represents a component that can either sit in the Inventory (unattached)
 * or be installed on an Aircraft at a given position.
 *
 * Installation state (position / installedOn) is intentionally only mutable
 * via package-private setters, so Aircraft and Inventory are the only classes
 * allowed to change it -- this keeps "a component is either on exactly one
 * aircraft or in inventory, never both" enforced in one place.
 */
public class Component extends Asset {

    private String position;       // null if not currently installed
    private Aircraft installedOn;  // null if in inventory

    /**
     * @param serialNumber unique identifier for this component, supplied
     *                     and validated by the caller (letters, digits,
     *                     '-' and '/' only).
     */
    public Component(String serialNumber, String name, String manufacturer, int manufacturingYear,
                     double lifespanHours, String description) {
        super(serialNumber, name, manufacturer, manufacturingYear, lifespanHours, description);
        this.position = null;
        this.installedOn = null;
    }

    /**
     * Private constructor used only for reconstructing a component from
     * previously-persisted state (see fromPersistedState below). Position
     * and installedOn are deliberately left null here -- if the component
     * was actually installed, the repository re-attaches it afterward via
     * Aircraft.installComponent(), which is the one place that's allowed to
     * set both together correctly.
     */
    private Component(String serialNumber, String name, String manufacturer, int manufacturingYear,
                      double lifespanHours, double originalLifespanHours, AssetStatus status,
                      String description) {
        super(serialNumber, name, manufacturer, manufacturingYear, lifespanHours, description);
        this.setOriginalLifespanHours(originalLifespanHours);
        this.setStatus(status);
        this.position = null;
        this.installedOn = null;
    }

    /**
     * Reconstructs a Component from a database row's worth of state. This is
     * the one and only entry point the persistence layer should use for an
     * existing component -- regular code creating a brand-new component
     * should keep using the public constructor above instead. Whether it
     * gets re-installed on an aircraft is handled separately by the caller.
     */
    public static Component fromPersistedState(String serialNumber, String name, String manufacturer,
                                               int manufacturingYear, double lifespanHours, double originalLifespanHours,
                                               AssetStatus status, String description) {
        return new Component(serialNumber, name, manufacturer, manufacturingYear, lifespanHours,
                originalLifespanHours, status, description);
    }

    @Override
    protected String getIdLabel() {
        return "Serial Number";
    }

    public String getSerialNumber() { return id; }

    public boolean isInstalled() {
        return installedOn != null;
    }

    public String getPosition() { return position; }
    public Aircraft getInstalledOn() { return installedOn; }

    /**
     * Short human-readable description of where this component currently
     * is, used when disambiguating between multiple components that share
     * the same name (e.g. the default "Engine" on several aircraft).
     */
    public String getLocationSummary() {
        return isInstalled()
                ? "installed on " + installedOn.getName() + " (" + installedOn.getRegistrationNumber() + ")"
                : "in inventory";
    }

    // Package-private: only Aircraft/Inventory should flip installation state.
    void setPosition(String position) { this.position = position; }
    void setInstalledOn(Aircraft aircraft) { this.installedOn = aircraft; }

    /**
     * Corrects the position label for a data-entry mistake (e.g. a typo)
     * without touching which aircraft it's installed on. To actually move a
     * component to a different aircraft or position, use
     * Aircraft.moveComponentTo() instead.
     */
    public void correctPosition(String newPosition) {
        if (!isInstalled()) {
            throw new IllegalStateException(name + " is not installed, so it has no position to correct.");
        }
        this.position = newPosition;
    }

    @Override
    public String getDetails() {
        StringBuilder sb = new StringBuilder(super.getDetails());
        sb.append("Position: ").append(position == null ? "N/A (in inventory)" : position).append("\n");
        sb.append("Installed On: ").append(installedOn == null ? "None (in inventory)" : installedOn.getName() + " (" + installedOn.getRegistrationNumber() + ")").append("\n");
        return sb.toString();
    }
}
