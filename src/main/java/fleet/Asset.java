package fleet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Abstract base class capturing everything common to Aircraft and Component:
 * identity/manufacturer info, lifespan tracking (in hours), status, and a
 * projected next-maintenance date/time.
 *
 * The unique identifier (id) is supplied by the CALLER at construction --
 * for Aircraft it's a registration number, for Component a serial number.
 * It is validated and typed by Main before it ever reaches this class; Asset
 * just stores it and labels it via the abstract getIdLabel() so the details
 * report reads "Registration Number: ..." for an Aircraft and
 * "Serial Number: ..." for a Component, even though it's one shared field
 * and mechanism underneath.
 *
 * Lifespan only ever decreases through explicit logged usage (see
 * Aircraft.logFlight) -- there is no automatic/calendar-based decay. The
 * next-maintenance date/time is a direct calculation, not an assumption:
 * "right now" (at the moment it's calculated) plus however many hours of
 * lifespan remain.
 *
 * Kept abstract and generic on purpose so future asset types (e.g. Engine,
 * Ground Support Equipment) can extend it later without reworking this class.
 */
public abstract class Asset {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    protected final String id;
    protected String name;
    protected String manufacturer;
    protected int manufacturingYear;
    protected double lifespanHours;         // hours remaining until maintenance is due
    protected double originalLifespanHours; // value lifespan resets to after maintenance
    protected AssetStatus status;
    protected String description;           // may be empty -- user can decline to provide one
    protected LocalDateTime nextMaintenanceDateTime;

    /**
     * @param id permanent unique identifier, supplied and validated by the
     *           caller before construction (a registration number for
     *           Aircraft, a serial number for Component). Immutable after.
     */
    protected Asset(String id, String name, String manufacturer, int manufacturingYear,
                    double lifespanHours, String description) {
        this.id = id;
        this.name = name;
        this.manufacturer = manufacturer;
        this.manufacturingYear = manufacturingYear;
        this.lifespanHours = lifespanHours;
        this.originalLifespanHours = lifespanHours;
        this.description = description == null ? "" : description;
        this.status = AssetStatus.SERVICEABLE;
        recalculateNextMaintenanceDateTime();
    }

    /**
     * Second constructor used ONLY for reconstructing an asset from persisted
     * (database) state -- accepts every field explicitly, including status
     * and originalLifespanHours, rather than deriving them fresh. This is
     * deliberately separate from the constructor above: creating a brand new
     * asset should always start SERVICEABLE with originalLifespanHours equal
     * to lifespanHours, but restoring an existing one must preserve whatever
     * its real, possibly-different values already were.
     */
    protected Asset(String id, String name, String manufacturer, int manufacturingYear,
                    double lifespanHours, double originalLifespanHours,
                    AssetStatus status, String description) {
        this.id = id;
        this.name = name;
        this.manufacturer = manufacturer;
        this.manufacturingYear = manufacturingYear;
        this.lifespanHours = lifespanHours;
        this.originalLifespanHours = originalLifespanHours;
        this.description = description == null ? "" : description;
        this.status = status;
        recalculateNextMaintenanceDateTime();
    }

    /**
     * Each subclass names its identifier differently for the details report
     * (e.g. "Registration Number" vs "Serial Number") even though it's the
     * same underlying field/mechanism.
     */
    protected abstract String getIdLabel();

    /**
     * Next maintenance = current date/time (at the moment this runs) plus
     * however many hours of lifespan remain. No usage-rate assumption --
     * it's a direct addition of the remaining hours themselves.
     */
    protected void recalculateNextMaintenanceDateTime() {
        long secondsRemaining = Math.round(Math.max(lifespanHours, 0) * 3600);
        this.nextMaintenanceDateTime = LocalDateTime.now().plusSeconds(secondsRemaining);
    }

    /**
     * Formats a decimal-hours value as H:MM (e.g. 1.5666... -> "1:34"). Used
     * for every field that works strictly in hours, so the whole program is
     * consistent about how it presents time, matching the H:MM format used
     * when hours are entered (see Main's duration-prompt helpers).
     */
    public static String formatHoursAsHM(double totalHours) {
        long totalMinutes = Math.round(Math.max(totalHours, 0) * 60);
        long h = totalMinutes / 60;
        long m = totalMinutes % 60;
        return h + ":" + String.format("%02d", m);
    }

    /**
     * Deducts hours of use from this asset's lifespan. Only ever called as
     * part of logging real usage (see Aircraft.logFlight) -- never on a timer.
     * Automatically flips status to UNSERVICEABLE once lifespan is exhausted.
     */
    public void useHours(double hours) {
        if (hours < 0) throw new IllegalArgumentException("Hours cannot be negative");
        this.lifespanHours -= hours;
        if (this.lifespanHours <= 0) {
            this.lifespanHours = 0;
            this.status = AssetStatus.UNSERVICEABLE;
        }
        recalculateNextMaintenanceDateTime();
    }

    /** Resets lifespan back to its original value after maintenance is performed. */
    public void performMaintenance() {
        this.lifespanHours = this.originalLifespanHours;
        this.status = AssetStatus.SERVICEABLE;
        recalculateNextMaintenanceDateTime();
    }

    /** Resets lifespan to a new value (e.g. an upgraded/overhauled component). */
    public void performMaintenance(double newLifespanHours) {
        this.originalLifespanHours = newLifespanHours;
        this.lifespanHours = newLifespanHours;
        this.status = AssetStatus.SERVICEABLE;
        recalculateNextMaintenanceDateTime();
    }

    // ---------- Getters / setters ----------

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public int getManufacturingYear() { return manufacturingYear; }
    public void setManufacturingYear(int manufacturingYear) { this.manufacturingYear = manufacturingYear; }

    public double getLifespanHours() { return lifespanHours; }

    /**
     * Directly corrects the current remaining lifespan value -- for fixing a
     * data-entry mistake, not for recording usage (useHours()) or servicing
     * (performMaintenance()). Recalculates next maintenance since it depends
     * on this value.
     */
    public void setLifespanHours(double lifespanHours) {
        if (lifespanHours < 0) throw new IllegalArgumentException("Lifespan cannot be negative");
        this.lifespanHours = lifespanHours;
        recalculateNextMaintenanceDateTime();
    }

    public double getOriginalLifespanHours() { return originalLifespanHours; }

    /**
     * Directly corrects the lifespan value this asset resets to whenever
     * performMaintenance() is called -- again, a data-entry fix, not a
     * maintenance event in itself.
     */
    public void setOriginalLifespanHours(double originalLifespanHours) {
        if (originalLifespanHours < 0) throw new IllegalArgumentException("Lifespan cannot be negative");
        this.originalLifespanHours = originalLifespanHours;
    }

    /**
     * Status changes automatically in two places: performMaintenance() sets
     * it back to SERVICEABLE, and useHours() sets it to UNSERVICEABLE once
     * lifespan hits zero. setStatus() is also exposed for a manual override
     * (see Main's confirmation-gated "change status" action), but the
     * automatic triggers are the primary way status changes.
     */
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }

    public LocalDateTime getNextMaintenanceDateTime() { return nextMaintenanceDateTime; }

    /**
     * Template method: builds the common portion of the details report.
     * Subclasses override and extend this with their own specific fields.
     */
    public String getDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append(getIdLabel()).append(": ").append(id).append("\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("Manufacturer: ").append(manufacturer).append("\n");
        sb.append("Manufacturing Year: ").append(manufacturingYear).append("\n");
        sb.append("Lifespan Remaining (H:MM): ").append(formatHoursAsHM(lifespanHours)).append("\n");
        sb.append("Status: ").append(status).append("\n");
        sb.append("Next Maintenance Due: ").append(nextMaintenanceDateTime.format(DISPLAY_FORMAT)).append("\n");
        sb.append("Description: ").append(description.isEmpty() ? "(none provided)" : description).append("\n");
        return sb.toString();
    }
}
