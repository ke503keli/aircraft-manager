package fleet.persistence;

import fleet.Aircraft;
import fleet.AssetStatus;
import fleet.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Translates between the `component` table and Component domain objects.
 *
 * Component.setPosition()/setInstalledOn() are package-private (deliberately
 * -- see Component.java), so this repository, living in a different package,
 * can't and shouldn't call them directly. Instead, attaching a loaded
 * component to its aircraft goes through the existing PUBLIC
 * Aircraft.installComponent(), which internally handles the package-private
 * plumbing correctly. This repository never bypasses that invariant.
 */
public class ComponentRepository {

    public void save(Component c) throws SQLException {
        String sql = """
                INSERT INTO component
                    (serial_number, name, manufacturer, manufacturing_year,
                     lifespan_hours, original_lifespan_hours, status, description,
                     position, installed_on)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(serial_number) DO UPDATE SET
                    name = excluded.name,
                    manufacturer = excluded.manufacturer,
                    manufacturing_year = excluded.manufacturing_year,
                    lifespan_hours = excluded.lifespan_hours,
                    original_lifespan_hours = excluded.original_lifespan_hours,
                    status = excluded.status,
                    description = excluded.description,
                    position = excluded.position,
                    installed_on = excluded.installed_on
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getSerialNumber());
            ps.setString(2, c.getName());
            ps.setString(3, c.getManufacturer());
            ps.setInt(4, c.getManufacturingYear());
            ps.setDouble(5, c.getLifespanHours());
            ps.setDouble(6, c.getOriginalLifespanHours());
            ps.setString(7, c.getStatus().name());
            ps.setString(8, c.getDescription());

            if (c.getPosition() != null) {
                ps.setString(9, c.getPosition());
            } else {
                ps.setNull(9, Types.VARCHAR);
            }
            if (c.isInstalled()) {
                ps.setString(10, c.getInstalledOn().getRegistrationNumber());
            } else {
                ps.setNull(10, Types.VARCHAR);
            }
            ps.executeUpdate();
        }
    }

    public Optional<Component> findBySerialNumber(String serialNumber) throws SQLException {
        String sql = "SELECT * FROM component WHERE serial_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serialNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Loads every component persisted as installed on the given aircraft,
     * and attaches each one to it via the public installComponent() method
     * (which correctly sets position/installedOn AND adds it to the
     * aircraft's own list) -- rather than returning a disconnected list the
     * caller would have to attach manually.
     */
    public List<Component> loadAndAttachComponentsFor(Aircraft aircraft) throws SQLException {
        List<Component> attached = new ArrayList<>();
        String sql = "SELECT * FROM component WHERE installed_on = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, aircraft.getRegistrationNumber());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Component c = mapRow(rs);
                    String position = rs.getString("position");
                    aircraft.installComponent(c, position);
                    attached.add(c);
                }
            }
        }
        return attached;
    }

    /** Every component currently persisted as unattached (in inventory). */
    public List<Component> findInInventory() throws SQLException {
        List<Component> result = new ArrayList<>();
        String sql = "SELECT * FROM component WHERE installed_on IS NULL";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public void delete(String serialNumber) throws SQLException {
        String sql = "DELETE FROM component WHERE serial_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, serialNumber);
            ps.executeUpdate();
        }
    }

    /**
     * Builds a bare, uninstalled Component from a row -- core stats only.
     * Position/installed-on attachment (when relevant) happens separately
     * via Aircraft.installComponent(), never here.
     */
    private Component mapRow(ResultSet rs) throws SQLException {
        return Component.fromPersistedState(
                rs.getString("serial_number"),
                rs.getString("name"),
                rs.getString("manufacturer"),
                rs.getInt("manufacturing_year"),
                rs.getDouble("lifespan_hours"),
                rs.getDouble("original_lifespan_hours"),
                AssetStatus.valueOf(rs.getString("status")),
                rs.getString("description")
        );
    }
}
