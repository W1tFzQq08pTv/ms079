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
        Class.forName("com.mysql.cj.jdbc.Driver");
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
        Class.forName("com.mysql.cj.jdbc.Driver");
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
        Class.forName("com.mysql.cj.jdbc.Driver");
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
        return DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/ms079?characterEncoding=UTF-8", "root", "123456");
    }

    private int countKeys(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM keymap WHERE characterid = 1")) {
            result.next();
            return result.getInt(1);
        }
    }
}
