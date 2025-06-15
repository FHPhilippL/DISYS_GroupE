package at.fhtw.usageservice.infrastructure;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimpleDataSource implements DataSource {
    private final String url, user, password;

    public SimpleDataSource(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override public Connection getConnection(String u, String p) throws SQLException {
        return DriverManager.getConnection(url, u, p);
    }

    // Die restlichen Methoden können leer bleiben oder Unsupported werfen
    // (für einfachen Use-Case ausreichend)
    @Override public <T> T unwrap(Class<T> iface) { throw new UnsupportedOperationException(); }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    @Override public java.io.PrintWriter getLogWriter() { return null; }
    @Override public void setLogWriter(java.io.PrintWriter out) {}
    @Override public void setLoginTimeout(int seconds) {}
    @Override public int getLoginTimeout() { return 0; }
    @Override public java.util.logging.Logger getParentLogger() { return null; }
}
