package hgds.epicgrief.api.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Function;

public interface SQL {
   void connect();

   default <T> T query(String query, Function<ResultSet, T> function) {
      if (query == null) {
         return null;
      } else {
         try (Connection connection = this.getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {
            return function.apply(resultSet);
         } catch (SQLException e) {
            e.printStackTrace();
            return null;
         }
      }
   }

   default void update(String query) {
      try (Connection connection = this.getConnection(); Statement statement = connection.createStatement()) {
         statement.executeUpdate(query);
      } catch (SQLException e) {
         e.printStackTrace();
      }

   }

   void disconnect();

   Connection getConnection();
}
