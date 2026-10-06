package com.minestorm.reportsystem.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface SqlTask<T> {
    T run(Connection c) throws SQLException;
}
