package hgds.epicgrief.api.database.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import hgds.epicgrief.api.database.SQL;

public class MySQL implements SQL {
   private String user;
   private String password;
   private int port;
   private String DB;
   private String host;
   private String url;

   public MySQL(String user, String password, String host, int port, String DB) {
      this.user = user;
      this.password = password;
      this.host = host;
      this.port = port;
      this.DB = DB;
   }

   public Connection getConnection() {
      try {
         return DriverManager.getConnection(this.url, this.user, this.password);
      } catch (SQLException e) {
         e.printStackTrace();
         return null;
      }
   }

   public void connect() {
      try {
         Class.forName("com.mysql.jdbc.Driver").newInstance();
         this.url = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.DB + "?useSSL=false";
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

   public void disconnect() {
   }
}
