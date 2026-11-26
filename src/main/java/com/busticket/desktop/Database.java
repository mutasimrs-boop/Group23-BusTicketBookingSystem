package com.busticket.desktop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() { }
    public static Connection connect() throws SQLException {
        String url = setting("ORACLE_URL", "jdbc:oracle:thin:@//localhost:1521/FREEPDB1");
        String user = setting("ORACLE_USER", "BUS_TICKET");
        String password = setting("ORACLE_PASSWORD", "change_me");
        return DriverManager.getConnection(url, user, password);
    }
    public static String setting(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
