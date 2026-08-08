package database;

import io.ebean.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnection.class);

    public static Connection getConnection() {
        try {
            Connection connection = DB.getDefault().dataSource().getConnection();
            // Legacy callers expect every standalone statement to commit immediately.
            // Ebean's pooled connections default to auto-commit off, which leaves login
            // updates holding row locks because most legacy callers do not commit.
            if (!connection.getAutoCommit()) {
                connection.setAutoCommit(true);
            }
            return connection;
        } catch (SQLException e) {
            LOGGER.error("获取数据库连接出错", e);
            throw new DatabaseException(e);
        }
    }

    public static void closeAll() throws SQLException {
        DB.getDefault().shutdown();
    }
}
