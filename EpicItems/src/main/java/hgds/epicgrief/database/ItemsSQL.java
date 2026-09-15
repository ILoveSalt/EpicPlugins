package hgds.epicgrief.database;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.inventory.ItemStack;
import hgds.epicgrief.api.database.SQL;

public class ItemsSQL {
   private SQL sql;
   private Map<Long, SQLPickaxe> loadedPickaxes;

   public void load(SQL sql) {
      this.sql = sql;
      this.loadedPickaxes = new ConcurrentHashMap<>();
      sql.update("CREATE TABLE IF NOT EXISTS pickaxes (id LONG, brokenBlocks LONG)");
      sql.query("SELECT * FROM `pickaxes`", (resultSet) -> {
         try {
            while(resultSet.next()) {
               long id = resultSet.getLong("id");
               this.loadedPickaxes.put(id, new SQLPickaxe(id, resultSet.getInt("brokenBlocks")));
            }
         } catch (SQLException e) {
            e.printStackTrace();
         }

         return Void.TYPE;
      });
   }

   public void close() {
      if (this.sql != null) {
         this.sql.disconnect();
      }

   }

   public long getUnicalId() {
      long unicalId;
      for(unicalId = (long)this.loadedPickaxes.size(); this.loadedPickaxes.containsKey(unicalId); ++unicalId) {
      }

      return unicalId;
   }

   public void save() {
      for(Map.Entry<Long, SQLPickaxe> longSQLPickaxeEntry : this.loadedPickaxes.entrySet()) {
         if (longSQLPickaxeEntry.getValue().isUpdated()) {
            this.sql.update(String.format("DELETE FROM `pickaxes` WHERE `id` = %d", longSQLPickaxeEntry.getKey()));
            if (!longSQLPickaxeEntry.getValue().isDelete()) {
               this.sql.update(String.format("INSERT INTO `pickaxes` (`id`, `brokenBlocks`) VALUES (%d, %d)", longSQLPickaxeEntry.getKey(), longSQLPickaxeEntry.getValue().getBrokenBlocks()));
            }
         }
      }

   }

   public long getIdFromItemStack(ItemStack itemStack) {
      long id = -1L;
      if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()) {
         for(String s : itemStack.getItemMeta().getDisplayName().split("§7\\(")) {
            String finalS = s.replace(")", "");

            try {
               id = (long)Integer.parseInt(finalS);
               break;
            } catch (NumberFormatException var10) {
            }
         }
      }

      return id;
   }

   public SQLPickaxe loadPickaxe(ItemStack itemStack) {
      long id = this.getIdFromItemStack(itemStack);
      if (id == -1L) {
         return null;
      } else {
         return this.loadedPickaxes.containsKey(id) ? (SQLPickaxe)this.loadedPickaxes.get(id) : this.sql.query(String.format("SELECT * FROM `pickaxes` WHERE `id` = %d", id), (resultSet) -> {
            try {
               SQLPickaxe sqlPickaxe;
               if (resultSet.next()) {
                  sqlPickaxe = new SQLPickaxe(id, resultSet.getInt("brokenBlocks"));
               } else {
                  sqlPickaxe = new SQLPickaxe(id, 0);
                  sqlPickaxe.update();
               }

               this.loadedPickaxes.put(id, sqlPickaxe);
               return sqlPickaxe;
            } catch (SQLException e) {
               throw new RuntimeException(e);
            }
         });
      }
   }
}
