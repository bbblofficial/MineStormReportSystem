package com.minestorm.reportsystem.common.db;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlTask<T> {
    T run(Connection conn) throws SQLException;
}
