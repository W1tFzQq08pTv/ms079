package client;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

public class MapleKeyLayoutTest {

    @Test
    public void savesAndRetriesInsideTheCallersTransaction() throws Exception {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            int originalCount = countKeys(connection);

            MapleKeyLayout layout = new MapleKeyLayout();
            layout.changeKey(42, (byte) 1, 2001004);
            long version = layout.saveKeys(connection, 1);
            assertEquals(1, countKeys(connection));
            connection.rollback();

            assertEquals(originalCount, countKeys(connection));
            assertEquals(version, layout.saveKeys(connection, 1));
            assertEquals(1, countKeys(connection));
            connection.rollback();
        }
    }

    @Test
    public void persistsRemovingTheLastKey() throws Exception {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            MapleKeyLayout layout = new MapleKeyLayout();
            layout.changeKey(42, (byte) 1, 2001004);
            layout.changeKey(42, (byte) 0, 0);
            layout.saveKeys(connection, 1);
            assertEquals(0, countKeys(connection));
            connection.rollback();
        }
    }

    @Test(timeout = 10000)
    public void repeatedSavesDoNotWaitForASecondConnection() throws Exception {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            int originalCount = countKeys(connection);
            MapleKeyLayout layout = new MapleKeyLayout();

            for (int i = 0; i < 20; i++) {
                layout.changeKey(42, (byte) 1, 2001004 + i);
                layout.saveKeys(connection, 1);
                assertEquals(1, countKeys(connection));
                connection.rollback();
                assertEquals(originalCount, countKeys(connection));
            }
        }
    }

    private Connection openConnection() throws Exception {
        Class.forName("org.h2.Driver");
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:key-layout;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS keymap ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "characterid INT NOT NULL, "
                    + "`key` INT NOT NULL, "
                    + "`type` INT NOT NULL, "
                    + "`action` INT NOT NULL)");
        }
        return connection;
    }

    private int countKeys(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM keymap WHERE characterid = 1")) {
            result.next();
            return result.getInt(1);
        }
    }
}
