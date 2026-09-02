package fleet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTest {

    @Test
    void newComponentIsNotInstalledByDefault() {
        Component c = new Component("CP-100", "Radio", "Garmin", 2020, 500.0, "Test radio");
        assertFalse(c.isInstalled());
        assertNull(c.getPosition());
        assertNull(c.getInstalledOn());
        assertEquals("in inventory", c.getLocationSummary());
    }

    @Test
    void correctPositionThrowsWhenComponentIsNotInstalled() {
        Component c = new Component("CP-101", "Radio", "Garmin", 2020, 500.0, "Test radio");
        assertThrows(IllegalStateException.class, () -> c.correctPosition("Cockpit"));
    }

    @Test
    void correctPositionSucceedsWhenInstalled() {
        Aircraft a = new Aircraft("N99999", "Test Plane", "Cessna", 2018, 200.0, "Test", "Hangar 2");
        Component c = new Component("CP-102", "Radio", "Garmin", 2020, 500.0, "Test radio");
        a.installComponent(c, "Cockpit - Left");

        c.correctPosition("Cockpit - Right");

        assertEquals("Cockpit - Right", c.getPosition());
        assertTrue(c.isInstalled(), "Correcting the position label should not change installation state");
        assertSame(a, c.getInstalledOn());
    }

    @Test
    void locationSummaryReflectsInstalledAircraft() {
        Aircraft a = new Aircraft("N55555", "Test Plane 2", "Piper", 2019, 150.0, "Test", "Hangar 3");
        Component c = new Component("CP-103", "Radio", "Garmin", 2020, 500.0, "Test radio");
        a.installComponent(c, "Cockpit");

        assertTrue(c.getLocationSummary().contains("N55555"));
        assertTrue(c.getLocationSummary().contains("Test Plane 2"));
    }

    @Test
    void useHoursRejectsNegativeInput() {
        Component c = new Component("CP-104", "Radio", "Garmin", 2020, 500.0, "Test radio");
        assertThrows(IllegalArgumentException.class, () -> c.useHours(-5.0));
    }
}