package hgds.epicgrief.api.database.impl;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import hgds.epicgrief.api.database.SQL;

public class SQLite implements SQL {
   private String url;
   private File datafolder;

   public SQLite(File datafolder) {
      this.datafolder = datafolder;
   }

   public void connect() {
      try {
         Class.forName("org.sqlite.JDBC").newInstance();
         this.url = "jdbc:sqlite:" + this.datafolder.getAbsolutePath();
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex2) {
         ((ReflectiveOperationException)ex2).printStackTrace();
      }

   }

   public void disconnect() {
   }

   public Connection getConnection() {
      try {
         return DriverManager.getConnection(this.url);
      } catch (SQLException e) {
         e.printStackTrace();
         return null;
      }
   }
}
