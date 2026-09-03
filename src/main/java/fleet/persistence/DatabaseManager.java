package fleet.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the raw JDBC connection and schema setup for the SQLite database.
 * Nothing in here knows about Aircraft/Component/etc. -- this is purely the
 * "talk to the database file" layer. Repository classes (built next) will
 * sit on top of this and handle translating to/from domain objects.
 */
public final class DatabaseManager {

    // Relative path -- the .db file will be created in whatever directory
    // the program is run from (the project root, when run via Maven/IntelliJ).
    private static final String DB_URL = "jdbc:sqlite:aircraft-manager.db";

    private DatabaseManager() {
        // utility class -- never instantiated
    }

    /**
     * Opens a new connection with foreign key enforcement turned on.
     * SQLite disables foreign key checks by default, PER CONNECTION -- so
     * this pragma has to run every single time a connection is opened, not
     * just once when the database file is first created.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement pragma = conn.createStatement()) {
            pragma.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Creates both tables if they don't already exist. Safe to call every
     * time the program starts -- CREATE TABLE IF NOT EXISTS is a no-op on
     * subsequent runs once the schema is already in place.
     */
    public static void initializeSchema() throws SQLException {
        String createAircraft = """
                CREATE TABLE IF NOT EXISTS aircraft (
                    registration_number TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    manufacturer TEXT NOT NULL,
                    manufacturing_year INTEGER NOT NULL CHECK (manufacturing_year >= 1900),
                    lifespan_hours REAL NOT NULL CHECK (lifespan_hours >= 0),
                    original_lifespan_hours REAL NOT NULL CHECK (original_lifespan_hours >= 0),
                    status TEXT NOT NULL CHECK (status IN ('SERVICEABLE','SERVICEABLE_WITH_ISSUES','UNSERVICEABLE')),
                    description TEXT NOT NULL DEFAULT '',
                    location TEXT NOT NULL,
                    flight_time REAL NOT NULL DEFAULT 0 CHECK (flight_time >= 0),
                    cycles INTEGER NOT NULL DEFAULT 0 CHECK (cycles >= 0)
                );
                """;

        String createComponent = """
                CREATE TABLE IF NOT EXISTS component (
                    serial_number TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    manufacturer TEXT NOT NULL,
                    manufacturing_year INTEGER NOT NULL CHECK (manufacturing_year >= 1900),
                    lifespan_hours REAL NOT NULL CHECK (lifespan_hours >= 0),
                    original_lifespan_hours REAL NOT NULL CHECK (original_lifespan_hours >= 0),
                    status TEXT NOT NULL CHECK (status IN ('SERVICEABLE','SERVICEABLE_WITH_ISSUES','UNSERVICEABLE')),
                    description TEXT NOT NULL DEFAULT '',
                    position TEXT,
                    installed_on TEXT REFERENCES aircraft(registration_number) ON DELETE SET NULL,
                    CHECK ((installed_on IS NULL AND position IS NULL) OR (installed_on IS NOT NULL AND position IS NOT NULL))
                );
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createAircraft);
            stmt.execute(createComponent);
        }
    }

    /**
     * Queries the database's own metadata to print exactly what tables and
     * columns actually exist -- a real, code-based confirmation rather than
     * inferring success from the .db file simply existing or its size.
     */
    public static void printSchema() throws SQLException {
        try (Connection conn = getConnection()) {
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            try (java.sql.ResultSet tables = meta.getTables(null, null, null, new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    System.out.println("Table: " + tableName);
                    try (java.sql.ResultSet columns = meta.getColumns(null, null, tableName, null)) {
                        while (columns.next()) {
                            String colName = columns.getString("COLUMN_NAME");
                            String colType = columns.getString("TYPE_NAME");
                            String nullable = columns.getString("IS_NULLABLE");
                            System.out.println("    " + colName + " (" + colType + ", nullable=" + nullable + ")");
                        }
                    }
                }
            }
        }
    }

    /**
     * Standalone entry point for verifying the database layer in isolation,
     * before any repository classes exist. Run THIS class directly (not
     * fleet.Main) to confirm the driver loads, the file gets created, and
     * the schema comes out correctly -- with zero risk to the working
     * terminal application while we build this out.
     */
    public static void main(String[] args) {
        try {
            initializeSchema();
            System.out.println("Schema initialized successfully.");
            System.out.println("Database file location: " + new java.io.File("aircraft-manager.db").getAbsolutePath());
            System.out.println();
            System.out.println("--- Verifying actual schema via JDBC metadata ---");
            printSchema();
        } catch (SQLException e) {
            System.out.println("Failed to initialize schema: " + e.getMessage());
        }
    }
}
