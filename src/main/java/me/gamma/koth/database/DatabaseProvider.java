package me.gamma.koth.database;

import java.sql.Connection;

public interface DatabaseProvider {
    void initialize() throws Exception;
    void shutdown();
    Connection getConnection();
}