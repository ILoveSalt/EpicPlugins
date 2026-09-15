package hgds.epicgrief.database;

public class SQLPickaxe {
   private int brokenBlocks;
   private long id;
   private boolean updated;
   private boolean delete;

   public SQLPickaxe(long id, int brokenBlocks) {
      this.id = id;
      this.brokenBlocks = brokenBlocks;
   }

   public int getBrokenBlocks() {
      return this.brokenBlocks;
   }

   public long getId() {
      return this.id;
   }

   public void setBrokenBlocks(int brokenBlocks) {
      if (brokenBlocks != this.brokenBlocks) {
         this.update();
      }

      this.brokenBlocks = brokenBlocks;
   }

   public void update() {
      this.updated = true;
   }

   public void setDelete(boolean delete) {
      this.delete = delete;
      this.update();
   }

   public boolean isDelete() {
      return this.delete;
   }

   public boolean isUpdated() {
      return this.updated;
   }
}
