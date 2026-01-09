package mrp.infrastructure.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {

    private static String url  = requireEnv("DB_URL");
    private static String user = requireEnv("DB_USER");
    private static String pass = requireEnv("DB_PASSWORD");

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    private static String requireEnv(String k) {
        String v = System.getenv(k);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("missing environment variable: " + k);
        }
        return v;
    }

    private ConnectionFactory() { }
}
