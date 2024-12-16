package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ViewDBContents {

    private static final String URL = "jdbc:sqlite:restaurant.db";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL)) {
            if (conn != null) {
                System.out.println("Connected to the database.");
                
                Statement stmt = conn.createStatement();
                ResultSet tables = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
                
                while (tables.next()) {
                    String tableName = tables.getString("name");
                    System.out.println("Table: " + tableName);
                    
                    ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
                    int columnCount = rs.getMetaData().getColumnCount();
                    
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            System.out.print(rs.getMetaData().getColumnName(i) + ": " + rs.getString(i) + "\t");
                        }
                        System.out.println();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 

