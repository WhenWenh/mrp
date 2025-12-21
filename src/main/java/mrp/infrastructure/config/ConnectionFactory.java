package mrp.infrastructure.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    //PGPASSWORD="$POSTGRES_PASSWORD" psql -U mrp -d mrp
    private static String url   = getenv("DB_URL", "jdbc:postgresql://localhost:5432/mrp");
    private static String user  = getenv("DB_USER", "mrp");
    private static String pass  = getenv("DB_PASSWORD", "mrp");

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    private static String getenv(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }

    private ConnectionFactory() { }
}
