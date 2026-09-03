package fleet.persistence;

import fleet.Aircraft;
import fleet.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Standalone smoke test for AircraftRepository/ComponentRepository, run
 * directly (not through fleet.Main) so we can confirm the persistence layer
 * actually works end-to-end -- save, reload, and verify components survive
 * the round trip correctly attached -- before wiring it into the real
 * terminal application at all.
 */
public class RepositorySmokeTest {

    public static void main(String[] args) throws SQLException {
        DatabaseManager.initializeSchema();

        AircraftRepository aircraftRepo = new AircraftRepository();
        ComponentRepository componentRepo = new ComponentRepository();

        // Create a brand-new aircraft the normal way -- this auto-installs
        // the 9 standard components, exactly like it would in the real app.
        Aircraft original = new Aircraft("N-SMOKE1", "Smoke Test Aircraft", "TestCo",
                2020, 500.0, "Created for a persistence smoke test", "Test Hangar");

        System.out.println("Created aircraft with " + original.getComponents().size() + " components.");

        // Log some usage before saving, so we can confirm non-default values
        // (not just the values it was created with) survive the round trip.
        original.logFlight(12.5, 3);
        System.out.println("After logging a flight: lifespan = " + original.getLifespanHours()
                + " hrs, flightTime = " + original.getFlightTime() + " hrs, cycles = " + original.getCycles());

        // Save the aircraft itself, then every one of its components.
        aircraftRepo.save(original);
        for (Component c : original.getComponents()) {
            componentRepo.save(c);
        }
        System.out.println("Saved aircraft and all " + original.getComponents().size() + " components.");

        // Now reload from scratch, as if this were a fresh program start --
        // find the aircraft row, then separately load and attach its components.
        Optional<Aircraft> reloaded = aircraftRepo.findByRegistrationNumber("N-SMOKE1");
        if (reloaded.isEmpty()) {
            System.out.println("FAILED: could not find the aircraft after saving it.");
            return;
        }
        Aircraft loadedAircraft = reloaded.get();
        System.out.println("Reloaded aircraft: " + loadedAircraft.getName()
                + " | lifespan = " + loadedAircraft.getLifespanHours()
                + " | flightTime = " + loadedAircraft.getFlightTime()
                + " | cycles = " + loadedAircraft.getCycles());
        System.out.println("Components attached immediately after reload (should be 0, before loading them): "
                + loadedAircraft.getComponents().size());

        List<Component> attached = componentRepo.loadAndAttachComponentsFor(loadedAircraft);
        System.out.println("Components loaded and attached: " + attached.size());
        System.out.println("Aircraft's own component list now shows: " + loadedAircraft.getComponents().size());

        // Sanity checks -- print explicit PASS/FAIL rather than just data dumps.
        boolean lifespanMatches = Math.abs(loadedAircraft.getLifespanHours() - original.getLifespanHours()) < 0.0001;
        boolean componentCountMatches = loadedAircraft.getComponents().size() == original.getComponents().size();
        boolean allInstalledCorrectly = attached.stream().allMatch(c -> c.isInstalled() && c.getInstalledOn() == loadedAircraft);

        System.out.println();
        System.out.println("Lifespan survived round trip correctly: " + lifespanMatches);
        System.out.println("Component count matches: " + componentCountMatches);
        System.out.println("Every reloaded component correctly re-installed: " + allInstalledCorrectly);

        // Clean up after ourselves so re-running this test doesn't collide
        // with leftover data from the previous run.
        for (Component c : loadedAircraft.getComponents()) {
            componentRepo.delete(c.getSerialNumber());
        }
        aircraftRepo.delete("N-SMOKE1");
        System.out.println();
        System.out.println("Cleanup complete -- test data removed.");
    }
}
