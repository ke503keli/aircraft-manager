package fleet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AircraftTest {

    private Aircraft aircraft;

    @BeforeEach
    void setUp() {
        // Fresh aircraft before every test -- keeps tests independent of each
        // other and independent of execution order.
        aircraft = new Aircraft("N12345", "Falcon One", "Cessna", 2015, 100.0,
                "Test aircraft", "Hangar 1");
    }

    @Test
    void newAircraftShipsWithNineStandardComponents() {
        assertEquals(9, aircraft.getComponents().size());
    }

    @Test
    void standardComponentSerialNumbersAreDerivedFromRegistrationAndAreUnique() {
        long distinctSerials = aircraft.getComponents().stream()
                .map(Component::getSerialNumber)
                .distinct()
                .count();
        assertEquals(9, distinctSerials, "All 9 default component serials should be unique");

        boolean allContainRegistration = aircraft.getComponents().stream()
                .allMatch(c -> c.getSerialNumber().endsWith("N12345"));
        assertTrue(allContainRegistration, "Every default serial should be derived from the aircraft's registration");
    }

    @Test
    void logFlightReducesAircraftLifespanByExactHoursLogged() {
        double before = aircraft.getLifespanHours();
        aircraft.logFlight(10.0, 1);
        assertEquals(before - 10.0, aircraft.getLifespanHours(), 0.0001);
    }

    @Test
    void logFlightCascadesWearToEveryInstalledComponent() {
        Component engine = aircraft.getComponents().get(0);
        double engineBefore = engine.getLifespanHours();

        aircraft.logFlight(5.0, 1);

        assertEquals(engineBefore - 5.0, engine.getLifespanHours(), 0.0001,
                "Installed components should lose exactly the hours logged on the aircraft");
    }

    @Test
    void logFlightAccumulatesFlightTimeAndCycles() {
        aircraft.logFlight(3.5, 2);
        aircraft.logFlight(1.5, 1);

        assertEquals(5.0, aircraft.getFlightTime(), 0.0001);
        assertEquals(3, aircraft.getCycles());
    }

    @Test
    void logFlightRejectsNegativeHours() {
        assertThrows(IllegalArgumentException.class, () -> aircraft.logFlight(-1.0, 1));
    }

    @Test
    void lifespanCannotGoBelowZeroAndStatusFlipsToUnserviceable() {
        aircraft.logFlight(1000.0, 1); // far more than the 100.0 starting lifespan

        assertEquals(0.0, aircraft.getLifespanHours(), 0.0001);
        assertEquals(AssetStatus.UNSERVICEABLE, aircraft.getStatus());
    }

    @Test
    void performMaintenanceResetsLifespanAndStatus() {
        aircraft.logFlight(1000.0, 1); // exhaust it
        assertEquals(AssetStatus.UNSERVICEABLE, aircraft.getStatus());

        aircraft.performMaintenance();

        assertEquals(100.0, aircraft.getLifespanHours(), 0.0001);
        assertEquals(AssetStatus.SERVICEABLE, aircraft.getStatus());
    }

    @Test
    void removingAComponentClearsItsPositionAndInstalledOnReference() {
        Component engine = aircraft.getComponents().get(0);
        assertTrue(engine.isInstalled());

        aircraft.removeComponent(engine);

        assertFalse(engine.isInstalled());
        assertNull(engine.getPosition());
        assertNull(engine.getInstalledOn());
        assertEquals(8, aircraft.getComponents().size());
    }
}