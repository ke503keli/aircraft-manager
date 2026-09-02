package fleet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new Inventory();
    }

    @Test
    void addComponentAcceptsAnUninstalledComponent() {
        Component c = new Component("CP-200", "Radio", "Garmin", 2020, 500.0, "Test radio");
        inventory.addComponent(c);
        assertEquals(1, inventory.getComponents().size());
    }

    @Test
    void addComponentRejectsAComponentThatIsAlreadyInstalled() {
        Aircraft a = new Aircraft("N11111", "Test Plane", "Cessna", 2018, 200.0, "Test", "Hangar 1");
        Component c = new Component("CP-201", "Radio", "Garmin", 2020, 500.0, "Test radio");
        a.installComponent(c, "Cockpit");

        assertThrows(IllegalStateException.class, () -> inventory.addComponent(c));
    }

    @Test
    void moveToAircraftRemovesFromInventoryAndInstallsOnTarget() {
        Aircraft a = new Aircraft("N22222", "Test Plane", "Cessna", 2018, 200.0, "Test", "Hangar 1");
        Component c = new Component("CP-202", "Radio", "Garmin", 2020, 500.0, "Test radio");
        inventory.addComponent(c);

        inventory.moveToAircraft(c, a, "Cockpit");

        assertEquals(0, inventory.getComponents().size(), "Component should no longer be in inventory");
        assertTrue(c.isInstalled());
        assertSame(a, c.getInstalledOn());
        assertTrue(a.getComponents().contains(c));
    }

    @Test
    void removeComponentThrowsWhenComponentIsNotActuallyInInventory() {
        Component c = new Component("CP-203", "Radio", "Garmin", 2020, 500.0, "Test radio");
        assertThrows(IllegalArgumentException.class, () -> inventory.removeComponent(c));
    }
}