package fleet.persistence;

import fleet.Aircraft;
import fleet.AssetStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Translates between the `aircraft` table and Aircraft domain objects.
 * Deliberately does NOT touch components at all -- that's ComponentRepository's
 * job. Loading a fully-assembled Aircraft (with its components attached) is
 * an orchestration concern one layer up, not something either repository
 * does alone.
 */
public class AircraftRepository {

    public void save(Aircraft a) throws SQLException {
        String sql = """
                INSERT INTO aircraft
                    (registration_number, name, manufacturer, manufacturing_year,
                     lifespan_hours, original_lifespan_hours, status, description,
                     location, flight_time, cycles)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(registration_number) DO UPDATE SET
                    name = excluded.name,
                    manufacturer = excluded.manufacturer,
                    manufacturing_year = excluded.manufacturing_year,
                    lifespan_hours = excluded.lifespan_hours,
                    original_lifespan_hours = excluded.original_lifespan_hours,
                    status = excluded.status,
                    description = excluded.description,
                    location = excluded.location,
                    flight_time = excluded.flight_time,
                    cycles = excluded.cycles
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getRegistrationNumber());
            ps.setString(2, a.getName());
            ps.setString(3, a.getManufacturer());
            ps.setInt(4, a.getManufacturingYear());
            ps.setDouble(5, a.getLifespanHours());
            ps.setDouble(6, a.getOriginalLifespanHours());
            ps.setString(7, a.getStatus().name());
            ps.setString(8, a.getDescription());
            ps.setString(9, a.getLocation());
            ps.setDouble(10, a.getFlightTime());
            ps.setInt(11, a.getCycles());
            ps.executeUpdate();
        }
    }

    public Optional<Aircraft> findByRegistrationNumber(String registrationNumber) throws SQLException {
        String sql = "SELECT * FROM aircraft WHERE registration_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registrationNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    /** Every aircraft row, reconstructed WITHOUT components attached yet. */
    public List<Aircraft> findAll() throws SQLException {
        List<Aircraft> result = new ArrayList<>();
        String sql = "SELECT * FROM aircraft";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    public void delete(String registrationNumber) throws SQLException {
        String sql = "DELETE FROM aircraft WHERE registration_number = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registrationNumber);
            ps.executeUpdate();
        }
    }

    private Aircraft mapRow(ResultSet rs) throws SQLException {
        return Aircraft.fromPersistedState(
                rs.getString("registration_number"),
                rs.getString("name"),
                rs.getString("manufacturer"),
                rs.getInt("manufacturing_year"),
                rs.getDouble("lifespan_hours"),
                rs.getDouble("original_lifespan_hours"),
                AssetStatus.valueOf(rs.getString("status")),
                rs.getString("description"),
                rs.getString("location"),
                rs.getDouble("flight_time"),
                rs.getInt("cycles")
        );
    }
}
