package at.fhtw.usageservice.infrastructure;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A minimal implementation of the {@link DataSource} interface.
 * This class provides a simple way to establish database connections using JDBC.
 * It is useful for basic setups or testing environments without a full connection pool.
 */
public class SimpleDataSource implements DataSource {

    private final String url;
    private final String user;
    private final String password;

    /**
     * Constructs the data source with the necessary database connection details.
     *
     * @param url      JDBC connection URL to the database
     * @param user     username for database authentication
     * @param password password for database authentication
     */
    public SimpleDataSource(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Gets a new connection using the credentials provided at construction time.
     */
    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Gets a new connection using the specified credentials.
     */
    @Override
    public Connection getConnection(String u, String p) throws SQLException {
        return DriverManager.getConnection(url, u, p);
    }

    // --- Unused or unsupported DataSource methods for this simple implementation ---

    @Override
    public <T> T unwrap(Class<T> iface) {
        throw new UnsupportedOperationException("unwrap not supported");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return false;
    }

    @Override
    public java.io.PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(java.io.PrintWriter out) {
        // No-op
    }

    @Override
    public void setLoginTimeout(int seconds) {
        // No-op
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public java.util.logging.Logger getParentLogger() {
        return null;
    }
}
