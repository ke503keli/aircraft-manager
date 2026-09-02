package fleet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Terminal-based entry point / menu system for the Aircraft & Component Manager.
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final FleetManager fleetManager = new FleetManager();

    // Registration/serial numbers may only contain letters, digits, '-' and '/'.
    private static final String ID_CHARSET_REGEX = "[A-Za-z0-9\\-/]+";

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1": addAircraft(); break;
                    case "2": addComponentToInventory(); break;
                    case "3": viewAllAircraft(); break;
                    case "4": viewInventory(); break;
                    case "5": viewAircraftDetails(); break;
                    case "6": viewComponentDetails(); break;
                    case "7": installComponentFromInventory(); break;
                    case "8": removeComponentToInventory(); break;
                    case "9": moveComponentBetweenAircraft(); break;
                    case "10": logFlight(); break;
                    case "11": performMaintenance(); break;
                    case "12": changeStatus(); break;
                    case "13": generalOverview(); break;
                    case "14": editAircraft(); break;
                    case "15": editComponent(); break;
                    case "16": deleteAircraft(); break;
                    case "17": deleteComponent(); break;
                    case "0": running = false; break;
                    default: System.out.println("Invalid option, try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
        System.out.println("Exiting. Goodbye.");
    }

    private static void printMenu() {
        System.out.println("===== Aircraft & Component Manager =====");
        System.out.println(" 1. Add Aircraft");
        System.out.println(" 2. Add Component (to Inventory)");
        System.out.println(" 3. View All Aircraft");
        System.out.println(" 4. View Inventory");
        System.out.println(" 5. View Aircraft Details");
        System.out.println(" 6. View Component Details");
        System.out.println(" 7. Install Component (Inventory -> Aircraft)");
        System.out.println(" 8. Remove Component (Aircraft -> Inventory)");
        System.out.println(" 9. Move Component (Aircraft -> Another Aircraft)");
        System.out.println("10. Log Flight / Usage (Aircraft)");
        System.out.println("11. Perform Maintenance (Aircraft or Component)");
        System.out.println("12. Change Status (Aircraft or Component)");
        System.out.println("13. General Overview (Everything)");
        System.out.println("14. Edit Aircraft (fix a field)");
        System.out.println("15. Edit Component (fix a field)");
        System.out.println("16. Delete Aircraft (Permanent)");
        System.out.println("17. Delete Component (Permanent)");
        System.out.println(" 0. Exit");
        System.out.print("Choose an option: ");
    }

    // ---------- Input helpers ----------

    /** Accepts any string, including blank -- used only where blank is a meaningful, safe answer. */
    private static String promptString(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    /** Re-prompts until the user enters something other than blank/whitespace. */
    private static String promptRequiredString(String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("This field can't be left empty. Please enter a value.");
        }
    }

    /** Simple y/n prompt, returning true only for an explicit yes. */
    private static boolean promptYesNo(String label) {
        String input = promptString(label + " (y/n)");
        return input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
    }

    /**
     * Asks whether the user wants to provide a description at all; if not,
     * returns an empty string rather than forcing one. If yes, requires a
     * non-blank value (declining and then typing nothing would be pointless).
     */
    private static String promptOptionalDescription() {
        if (!promptYesNo("Would you like to add a description?")) {
            return "";
        }
        return promptRequiredString("Description");
    }

    /** Re-prompts until the input parses as a real, finite number (rejects text, blank, NaN, Infinity). */
    private static double promptDouble(String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (!Double.isFinite(value)) {
                    System.out.println("Please enter a regular finite number.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    /** Re-prompts until the input parses as a whole number. */
    private static int promptInt(String label) {
        while (true) {
            System.out.print(label + ": ");
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    /** Re-prompts until the whole number entered is zero or greater. */
    private static int promptNonNegativeInt(String label) {
        while (true) {
            int value = promptInt(label);
            if (value >= 0) return value;
            System.out.println("Please enter a whole number that isn't negative.");
        }
    }

    /** Re-prompts until the year entered is a plausible manufacturing year (1900 through the current year). */
    private static int promptYear(String label) {
        int currentYear = java.time.Year.now().getValue();
        while (true) {
            int year = promptInt(label);
            if (year >= 1900 && year <= currentYear) return year;
            System.out.println("Please enter a year between 1900 and " + currentYear + ".");
        }
    }

    /**
     * Re-prompts until the user enters a duration as hours:minutes (e.g. "1:34"
     * for 1 hour 34 minutes), then converts it to decimal hours for internal use.
     * Guaranteed non-negative by construction (both parts are checked >= 0
     * before being combined) -- this is the base parser every hours-based
     * field in the program is built on.
     */
    private static double promptDurationHM(String label) {
        while (true) {
            System.out.print(label + " (H:MM, e.g. 1:34 for 1 hour 34 minutes): ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split(":");
            if (parts.length != 2) {
                System.out.println("Please enter the time as hours:minutes, e.g. 1:34.");
                continue;
            }
            try {
                int hoursPart = Integer.parseInt(parts[0].trim());
                int minutesPart = Integer.parseInt(parts[1].trim());
                if (hoursPart < 0 || minutesPart < 0 || minutesPart > 59) {
                    System.out.println("Hours must be 0 or more, and minutes must be between 0 and 59.");
                    continue;
                }
                return hoursPart + (minutesPart / 60.0);
            } catch (NumberFormatException e) {
                System.out.println("Please enter the time as whole-number hours:minutes, e.g. 1:34.");
            }
        }
    }

    /** Same as promptDurationHM, but re-prompts again if the result is exactly zero. */
    private static double promptPositiveDurationHM(String label) {
        while (true) {
            double value = promptDurationHM(label);
            if (value > 0) return value;
            System.out.println("Please enter a duration greater than 0:00.");
        }
    }

    /**
     * Validates a registration/serial number: non-blank, only letters, digits,
     * '-' and '/', and must contain at least one letter or digit (rejects a
     * value made purely of separators, which is meaningless as an identifier).
     * alreadyExists is supplied by the caller since aircraft and components
     * are checked for uniqueness against different collections.
     */
    private static String promptUniqueIdentifier(String label, java.util.function.Predicate<String> alreadyExists) {
        while (true) {
            String input = promptRequiredString(label + " (letters, numbers, '-' and '/' only)");
            if (!input.matches(ID_CHARSET_REGEX)) {
                System.out.println("Only letters, numbers, '-' and '/' are allowed -- no spaces or other symbols.");
                continue;
            }
            boolean hasLetterOrDigit = input.chars().anyMatch(Character::isLetterOrDigit);
            if (!hasLetterOrDigit) {
                System.out.println("Must include at least one letter or number, not just separators.");
                continue;
            }
            if (alreadyExists.test(input)) {
                System.out.println("\"" + input + "\" is already in use. Please enter a unique value.");
                continue;
            }
            return input;
        }
    }

    // ---------- Menu actions ----------

    private static void addAircraft() {
        String registrationNumber = promptUniqueIdentifier("Aircraft Registration Number",
                candidate -> fleetManager.findAircraftByRegistrationNumber(candidate).isPresent());
        String name = promptRequiredString("Aircraft name");
        String manufacturer = promptRequiredString("Manufacturer");
        int year = promptYear("Manufacturing year");
        double lifespan = promptPositiveDurationHM("Lifespan (time until next maintenance)");
        String location = promptRequiredString("Location");
        String description = promptOptionalDescription();

        Aircraft aircraft = new Aircraft(registrationNumber, name, manufacturer, year, lifespan, description, location);
        fleetManager.addAircraft(aircraft);
        System.out.println("Aircraft \"" + name + "\" (" + registrationNumber + ") added.");
    }

    private static void addComponentToInventory() {
        String serialNumber = promptUniqueIdentifier("Component Serial Number",
                candidate -> fleetManager.findComponentBySerialNumber(candidate).isPresent());
        String name = promptRequiredString("Component name");
        String manufacturer = promptRequiredString("Manufacturer");
        int year = promptYear("Manufacturing year");
        double lifespan = promptPositiveDurationHM("Lifespan (time until next maintenance)");
        String description = promptOptionalDescription();

        Component component = new Component(serialNumber, name, manufacturer, year, lifespan, description);
        fleetManager.getInventory().addComponent(component);
        System.out.println("Component \"" + name + "\" (" + serialNumber + ") added to inventory.");
    }

    private static void viewAllAircraft() {
        List<Aircraft> list = fleetManager.getAircraftList();
        if (list.isEmpty()) {
            System.out.println("No aircraft in the fleet yet.");
            return;
        }
        System.out.println("Fleet (" + list.size() + " aircraft):");
        for (Aircraft a : list) {
            System.out.println(" - [" + a.getRegistrationNumber() + "] " + a.getName() + " | Status: " + a.getStatus()
                    + " | Location: " + a.getLocation()
                    + " | Lifespan left: " + Asset.formatHoursAsHM(a.getLifespanHours()));
        }
    }

    private static void viewInventory() {
        List<Component> list = fleetManager.getInventory().getComponents();
        if (list.isEmpty()) {
            System.out.println("Inventory is empty.");
            return;
        }
        System.out.println("Inventory (" + list.size() + " components):");
        for (Component c : list) {
            System.out.println(" - [" + c.getSerialNumber() + "] " + c.getName() + " | Status: " + c.getStatus()
                    + " | Lifespan left: " + Asset.formatHoursAsHM(c.getLifespanHours()));
        }
    }

    private static void viewAircraftDetails() {
        Aircraft a = selectAircraft();
        if (a == null) return;
        System.out.println("---- Aircraft Details ----");
        System.out.print(a.getDetails());
    }

    private static void viewComponentDetails() {
        Component c = selectAnyComponent();
        if (c == null) return;
        System.out.println("---- Component Details ----");
        System.out.print(c.getDetails());
    }

    private static void installComponentFromInventory() {
        Component c = selectInventoryComponent();
        if (c == null) return;
        Aircraft a = selectAircraft();
        if (a == null) return;
        String position = promptRequiredString("Install position on aircraft");
        fleetManager.getInventory().moveToAircraft(c, a, position);
        System.out.println(c.getName() + " installed on " + a.getName() + " at position " + position + ".");
    }

    private static void removeComponentToInventory() {
        Aircraft a = selectAircraft();
        if (a == null) return;
        Component c = selectInstalledComponent(a);
        if (c == null) return;
        a.removeComponent(c);
        fleetManager.getInventory().addComponent(c);
        System.out.println(c.getName() + " removed from " + a.getName() + " and returned to inventory.");
    }

    private static void moveComponentBetweenAircraft() {
        System.out.println("Select the SOURCE aircraft (currently holding the component):");
        Aircraft source = selectAircraft();
        if (source == null) return;
        Component c = selectInstalledComponent(source);
        if (c == null) return;
        System.out.println("Select the DESTINATION aircraft:");
        Aircraft destination = selectAircraft();
        if (destination == null) return;
        if (source == destination) {
            System.out.println("Source and destination are the same aircraft.");
            return;
        }
        String newPosition = promptRequiredString("New position on destination aircraft");
        source.moveComponentTo(c, destination, newPosition);
        System.out.println(c.getName() + " moved from " + source.getName() + " to " + destination.getName() + ".");
    }

    private static void logFlight() {
        Aircraft a = selectAircraft();
        if (a == null) return;
        double hours = promptPositiveDurationHM("Time to log for this flight/usage");
        int cycles = promptNonNegativeInt("Cycles to add (takeoff/landing pairs)");
        a.logFlight(hours, cycles);
        System.out.println("Logged " + Asset.formatHoursAsHM(hours) + " (" + cycles + " cycles) for " + a.getName() + ". "
                + "Remaining lifespan: " + Asset.formatHoursAsHM(a.getLifespanHours()) + ". Status: " + a.getStatus());
    }

    private static void changeStatus() {
        System.out.println("Change status of (1) an Aircraft or (2) a Component?");
        String type = scanner.nextLine().trim();
        Asset target;
        if (type.equals("1")) {
            target = selectAircraft();
        } else if (type.equals("2")) {
            target = selectAnyComponent();
        } else {
            System.out.println("Invalid choice.");
            return;
        }
        if (target == null) return;

        AssetStatus[] options = AssetStatus.values();
        System.out.println("Current status of " + target.getName() + ": " + target.getStatus());
        System.out.println("Select new status:");
        for (int i = 0; i < options.length; i++) {
            System.out.println(" " + (i + 1) + ". " + options[i]);
        }
        int index = promptInt("Status number") - 1;
        if (index < 0 || index >= options.length) {
            System.out.println("Invalid selection.");
            return;
        }
        AssetStatus chosen = options[index];
        if (!promptYesNo("Confirm: change " + target.getName() + " from " + target.getStatus() + " to " + chosen + "?")) {
            System.out.println("Status change cancelled.");
            return;
        }
        target.setStatus(chosen);
        System.out.println(target.getName() + " status set to " + target.getStatus() + ".");
    }

    private static void performMaintenance() {
        System.out.println("Maintain (1) an Aircraft or (2) a Component?");
        String type = scanner.nextLine().trim();
        if (type.equals("1")) {
            Aircraft a = selectAircraft();
            if (a == null) return;
            a.performMaintenance();
            System.out.println(a.getName() + " serviced. Lifespan reset to " + Asset.formatHoursAsHM(a.getLifespanHours()) + ".");
        } else if (type.equals("2")) {
            Component c = selectAnyComponent();
            if (c == null) return;
            c.performMaintenance();
            System.out.println(c.getName() + " serviced. Lifespan reset to " + Asset.formatHoursAsHM(c.getLifespanHours()) + ".");
        } else {
            System.out.println("Invalid choice.");
        }
    }

    /**
     * Prints full details for every logged aircraft, every installed
     * component, and every spare component in inventory -- a one-shot
     * snapshot of the entire system's current state. Installed and
     * inventory components are mutually exclusive sets (a component is
     * always exactly one or the other), so nothing is listed twice.
     */
    private static void generalOverview() {
        List<Aircraft> aircraftListLocal = fleetManager.getAircraftList();
        List<Component> inventoryComponents = fleetManager.getInventory().getComponents();

        System.out.println("========================================");
        System.out.println("            GENERAL OVERVIEW");
        System.out.println("========================================");

        System.out.println("\n--- AIRCRAFT (" + aircraftListLocal.size() + ") ---");
        if (aircraftListLocal.isEmpty()) {
            System.out.println("No aircraft logged.");
        } else {
            for (Aircraft a : aircraftListLocal) {
                System.out.println("\n[Aircraft] " + a.getName());
                System.out.print(a.getDetails());
            }
        }

        System.out.println("\n--- INSTALLED COMPONENTS ---");
        boolean anyInstalled = false;
        for (Aircraft a : aircraftListLocal) {
            for (Component c : a.getComponents()) {
                anyInstalled = true;
                System.out.println("\n[Component] " + c.getName());
                System.out.print(c.getDetails());
            }
        }
        if (!anyInstalled) {
            System.out.println("No components currently installed on any aircraft.");
        }

        System.out.println("\n--- INVENTORY (" + inventoryComponents.size() + " spare components) ---");
        if (inventoryComponents.isEmpty()) {
            System.out.println("Inventory is empty.");
        } else {
            for (Component c : inventoryComponents) {
                System.out.println("\n[Component] " + c.getName());
                System.out.print(c.getDetails());
            }
        }
    }

    // ---------- Edit actions (fixing basic human-error mistakes) ----------

    private static void editAircraft() {
        Aircraft a = selectAircraft();
        if (a == null) return;
        System.out.println("Editing \"" + a.getName() + "\" (" + a.getRegistrationNumber() + "). Current details:");
        System.out.print(a.getDetails());
        System.out.println("Choose a field to fix:");
        System.out.println(" 1. Name");
        System.out.println(" 2. Manufacturer");
        System.out.println(" 3. Manufacturing Year");
        System.out.println(" 4. Current Lifespan Remaining");
        System.out.println(" 5. Lifespan Reset Value (applied on next maintenance)");
        System.out.println(" 6. Location");
        System.out.println(" 7. Description");
        String choice = promptString("Field number");
        switch (choice) {
            case "1": a.setName(promptRequiredString("New name")); break;
            case "2": a.setManufacturer(promptRequiredString("New manufacturer")); break;
            case "3": a.setManufacturingYear(promptYear("New manufacturing year")); break;
            case "4": a.setLifespanHours(promptDurationHM("New current lifespan remaining")); break;
            case "5": a.setOriginalLifespanHours(promptPositiveDurationHM("New lifespan reset value")); break;
            case "6": a.setLocation(promptRequiredString("New location")); break;
            case "7": a.setDescription(promptRequiredString("New description")); break;
            default:
                System.out.println("Invalid selection. No changes made.");
                return;
        }
        System.out.println("\"" + a.getName() + "\" updated.");
    }

    private static void editComponent() {
        Component c = selectAnyComponent();
        if (c == null) return;
        System.out.println("Editing \"" + c.getName() + "\" (" + c.getSerialNumber() + "). Current details:");
        System.out.print(c.getDetails());
        System.out.println("Choose a field to fix:");
        System.out.println(" 1. Name");
        System.out.println(" 2. Manufacturer");
        System.out.println(" 3. Manufacturing Year");
        System.out.println(" 4. Current Lifespan Remaining");
        System.out.println(" 5. Lifespan Reset Value (applied on next maintenance)");
        System.out.println(" 6. Description");
        if (c.isInstalled()) {
            System.out.println(" 7. Position label (typo fix only -- use option 9 on the main menu to actually move it)");
        }
        String choice = promptString("Field number");
        switch (choice) {
            case "1": c.setName(promptRequiredString("New name")); break;
            case "2": c.setManufacturer(promptRequiredString("New manufacturer")); break;
            case "3": c.setManufacturingYear(promptYear("New manufacturing year")); break;
            case "4": c.setLifespanHours(promptDurationHM("New current lifespan remaining")); break;
            case "5": c.setOriginalLifespanHours(promptPositiveDurationHM("New lifespan reset value")); break;
            case "6": c.setDescription(promptRequiredString("New description")); break;
            case "7":
                if (c.isInstalled()) {
                    c.correctPosition(promptRequiredString("New position label"));
                } else {
                    System.out.println("Invalid selection. No changes made.");
                    return;
                }
                break;
            default:
                System.out.println("Invalid selection. No changes made.");
                return;
        }
        System.out.println("\"" + c.getName() + "\" updated.");
    }

    // ---------- Delete actions (permanent -- two-layer confirmation) ----------

    /**
     * Shared confirmation gate for any permanent deletion: first a plain
     * yes/no, then a second layer requiring the exact identifier (registration
     * or serial number) to be typed back. An identifier is used rather than
     * name because names aren't guaranteed unique (e.g. every aircraft ships
     * with a component literally named "Engine"), so typing a name back
     * wouldn't reliably confirm which specific record is about to be
     * destroyed -- the identifier always does.
     */
    private static boolean confirmPermanentDeletion(String typeLabel, String name, String identifierLabel, String identifierValue) {
        String firstConfirm = promptString("Are you sure you want to permanently delete this " + typeLabel
                + ", \"" + name + "\" (" + identifierValue + ")? This cannot be undone. (y/n)");
        if (!firstConfirm.equalsIgnoreCase("y") && !firstConfirm.equalsIgnoreCase("yes")) {
            System.out.println("Deletion cancelled.");
            return false;
        }
        String typedValue = promptString("To confirm, type the exact " + identifierLabel
                + " of the " + typeLabel + " to delete (" + identifierValue + ")");
        if (!typedValue.equalsIgnoreCase(identifierValue)) {
            System.out.println(identifierLabel + " did not match \"" + identifierValue + "\" exactly. Deletion cancelled.");
            return false;
        }
        return true;
    }

    private static void deleteAircraft() {
        Aircraft a = selectAircraft();
        if (a == null) return;
        if (!confirmPermanentDeletion("aircraft", a.getName(), "registration number", a.getRegistrationNumber())) return;

        // Detach any installed components back to inventory first, rather than
        // silently losing them when the aircraft record disappears.
        List<Component> installed = new ArrayList<>(a.getComponents());
        for (Component c : installed) {
            a.removeComponent(c);
            fleetManager.getInventory().addComponent(c);
        }
        fleetManager.getAircraftList().remove(a);
        System.out.println("Aircraft \"" + a.getName() + "\" (" + a.getRegistrationNumber() + ") permanently deleted. "
                + installed.size() + " component(s) were moved to inventory rather than deleted.");
    }

    private static void deleteComponent() {
        Component c = selectAnyComponent();
        if (c == null) return;
        if (!confirmPermanentDeletion("component", c.getName(), "serial number", c.getSerialNumber())) return;

        if (c.isInstalled()) {
            c.getInstalledOn().removeComponent(c);
        } else {
            fleetManager.getInventory().removeComponent(c);
        }
        System.out.println("Component \"" + c.getName() + "\" (" + c.getSerialNumber() + ") permanently deleted.");
    }

    // ---------- Selection helpers ----------

    private static Aircraft selectAircraft() {
        List<Aircraft> list = fleetManager.getAircraftList();
        if (list.isEmpty()) {
            System.out.println("No aircraft available.");
            return null;
        }
        System.out.println("Available aircraft:");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(" " + (i + 1) + ". [" + list.get(i).getRegistrationNumber() + "] " + list.get(i).getName());
        }
        int index = promptInt("Select aircraft number") - 1;
        if (index < 0 || index >= list.size()) {
            System.out.println("Invalid selection.");
            return null;
        }
        return list.get(index);
    }

    private static Component selectInventoryComponent() {
        List<Component> list = fleetManager.getInventory().getComponents();
        if (list.isEmpty()) {
            System.out.println("Inventory is empty.");
            return null;
        }
        System.out.println("Inventory components:");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(" " + (i + 1) + ". [" + list.get(i).getSerialNumber() + "] " + list.get(i).getName());
        }
        int index = promptInt("Select component number") - 1;
        if (index < 0 || index >= list.size()) {
            System.out.println("Invalid selection.");
            return null;
        }
        return list.get(index);
    }

    private static Component selectInstalledComponent(Aircraft a) {
        List<Component> list = a.getComponents();
        if (list.isEmpty()) {
            System.out.println(a.getName() + " has no installed components.");
            return null;
        }
        System.out.println("Components installed on " + a.getName() + ":");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(" " + (i + 1) + ". [" + list.get(i).getSerialNumber() + "] " + list.get(i).getName() + " [" + list.get(i).getPosition() + "]");
        }
        int index = promptInt("Select component number") - 1;
        if (index < 0 || index >= list.size()) {
            System.out.println("Invalid selection.");
            return null;
        }
        return list.get(index);
    }

    /**
     * Lets the user identify a component by serial number or by name,
     * searching across both inventory and every aircraft's installed
     * components. Since names are not guaranteed unique (e.g. every aircraft
     * ships with a component named "Engine"), a name match that returns more
     * than one result is shown as a list -- with serial number and current
     * location -- so the user can pick the exact one they mean, rather than
     * silently guessing.
     */
    private static Component selectAnyComponent() {
        String query = promptString("Component name or serial number");

        Optional<Component> bySerial = fleetManager.findComponentBySerialNumber(query);
        if (bySerial.isPresent()) return bySerial.get();

        List<Component> matches = fleetManager.findComponentsByName(query);
        if (matches.isEmpty()) {
            System.out.println("No component found with that name or serial number.");
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }

        System.out.println("Multiple components named \"" + query + "\" found:");
        for (int i = 0; i < matches.size(); i++) {
            Component c = matches.get(i);
            System.out.println(" " + (i + 1) + ". [" + c.getSerialNumber() + "] " + c.getName() + " - " + c.getLocationSummary());
        }
        int index = promptInt("Select number") - 1;
        if (index < 0 || index >= matches.size()) {
            System.out.println("Invalid selection.");
            return null;
        }
        return matches.get(index);
    }
}
