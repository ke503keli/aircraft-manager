package fleet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FleetManagerTest {

    private FleetManager fleetManager;

    @BeforeEach
    void setUp() {
        fleetManager = new FleetManager();
    }

    @Test
    void findAircraftByRegistrationNumberReturnsEmptyWhenNoneExist() {
        Optional<Aircraft> result = fleetManager.findAircraftByRegistrationNumber("N00000");
        assertTrue(result.isEmpty());
    }

    @Test
    void findAircraftByRegistrationNumberFindsAnExactCaseInsensitiveMatch() {
        Aircraft a = new Aircraft("N12345", "Falcon", "Cessna", 2015, 100.0, "desc", "Hangar 1");
        fleetManager.addAircraft(a);

        Optional<Aircraft> result = fleetManager.findAircraftByRegistrationNumber("n12345");

        assertTrue(result.isPresent());
        assertSame(a, result.get());
    }

    @Test
    void findComponentBySerialNumberFindsInstalledComponents() {
        Aircraft a = new Aircraft("N33333", "Falcon", "Cessna", 2015, 100.0, "desc", "Hangar 1");
        fleetManager.addAircraft(a);

        String engineSerial = a.getComponents().get(0).getSerialNumber();
        Optional<Component> result = fleetManager.findComponentBySerialNumber(engineSerial);

        assertTrue(result.isPresent());
        assertEquals("Engine", result.get().getName());
    }

    /**
     * This is the direct regression test for the original bug: several
     * aircraft each ship with a component literally named "Engine". A name
     * search must surface ALL of them, not silently return just one.
     */
    @Test
    void findComponentsByNameReturnsEveryMatchAcrossMultipleAircraft() {
        Aircraft a1 = new Aircraft("N10001", "Falcon One", "Cessna", 2015, 100.0, "desc", "Hangar 1");
        Aircraft a2 = new Aircraft("N10002", "Falcon Two", "Cessna", 2016, 100.0, "desc", "Hangar 2");
        fleetManager.addAircraft(a1);
        fleetManager.addAircraft(a2);

        List<Component> engines = fleetManager.findComponentsByName("Engine");

        assertEquals(2, engines.size(), "Both aircraft's default engines should be found");

        boolean fromA1 = engines.stream().anyMatch(c -> c.getSerialNumber().endsWith("N10001"));
        boolean fromA2 = engines.stream().anyMatch(c -> c.getSerialNumber().endsWith("N10002"));
        assertTrue(fromA1 && fromA2, "Results should include the engine from each aircraft, not just one");
    }

    @Test
    void getAllComponentsIncludesBothInstalledAndInventoryComponents() {
        Aircraft a = new Aircraft("N44444", "Falcon", "Cessna", 2015, 100.0, "desc", "Hangar 1");
        fleetManager.addAircraft(a); // contributes 9 installed components

        Component spare = new Component("CP-300", "Spare Radio", "Garmin", 2021, 400.0, "Spare");
        fleetManager.getInventory().addComponent(spare); // contributes 1 more

        assertEquals(10, fleetManager.getAllComponents().size());
    }
}