package hgds.epicgrief.epiccoins.core;

/*
 *
 *  * Copyright (c) 2018 Davide Wietlisbach on 24.11.18 16:49
 *
 */

import hgds.epicgrief.epiccoins.core.config.Config;
import hgds.epicgrief.epiccoins.core.manager.MessageManager;
import hgds.epicgrief.epiccoins.core.player.CoinPlayerManager;
import hgds.epicgrief.epiccoins.core.storage.CoinStorage;
import hgds.epicgrief.epiccoins.core.storage.storage.StorageType;
import hgds.epicgrief.epiccoins.core.storage.storage.json.JsonCoinStorage;
import hgds.epicgrief.epiccoins.core.storage.storage.mongodb.MongoDBCoinStorage;
import hgds.epicgrief.epiccoins.core.storage.storage.sql.mysql.MySQLCoinStorage;
import hgds.epicgrief.epiccoins.core.storage.storage.sql.sqlite.SQLiteCoinStorage;

public class CoinSystem {

    private static CoinSystem INSTANCE;
    private final String version;
    private final EpicCoinsPlatform platform;
    private CoinPlayerManager playerManager;
    private CoinStorage storage;
    private Config config;

    public CoinSystem(EpicCoinsPlatform platform) {
        INSTANCE = this;
        this.version = "1.1";
        this.platform = platform;

        new MessageManager("EpicCoins");
        System.out.println("["+MessageManager.getInstance().system_name+"] plugin is starting");
        System.out.println("["+MessageManager.getInstance().system_name+"] EpicCoins v"+this.version+" by hgds_2");

        systemBootstrap();

        System.out.println("["+MessageManager.getInstance().system_name+"] plugin successfully started");
    }

    private void systemBootstrap(){
        this.platform.getFolder().mkdirs();

        this.config = new Config(this.platform);
        this.config.loadConfig();

        this.playerManager = new CoinPlayerManager();

        setupStorage();
    }

    private void setupStorage() {
        if(this.config.storageType == StorageType.MYSQL) this.storage = new MySQLCoinStorage(this.config);
        else if(this.config.storageType == StorageType.SQLITE) this.storage = new SQLiteCoinStorage(this.config);
        else if(this.config.storageType == StorageType.MONGODB) this.storage = new MongoDBCoinStorage(this.config);
        else if(this.config.storageType == StorageType.JSON) this.storage = new JsonCoinStorage(this.config);

        if(this.storage != null && this.storage.connect()) {
            System.out.println(MessageManager.getInstance().system_name+"Used Storage: "+this.config.storageType.toString());
            return;
        }
        System.out.println(MessageManager.getInstance().system_name+"Used Backup Storage: "+StorageType.SQLITE.toString());
        this.storage = new SQLiteCoinStorage(this.config);
    }

    public void reload(){
        System.out.println("["+MessageManager.getInstance().system_name+"] plugin is reloading");
        System.out.println("["+MessageManager.getInstance().system_name+"] EpicCoins v"+this.version+" by hgds_2");
        if(this.storage != null) this.storage.disconnect();
        systemBootstrap();
        System.out.println("["+MessageManager.getInstance().system_name+"] plugin successfully reloaded");
    }

    public void shutdown(){
        System.out.println("["+MessageManager.getInstance().system_name+"] plugin is stopping");
        System.out.println("["+MessageManager.getInstance().system_name+"] EpicCoins v"+this.version+" by hgds_2");
        if(this.storage != null) this.storage.disconnect();
        System.out.println("["+MessageManager.getInstance().system_name+"] plugin successfully stopped");
    }

    public String getVersion() {
        return version;
    }

    public EpicCoinsPlatform getPlatform() {
        return platform;
    }

    public CoinPlayerManager getPlayerManager() {
        return playerManager;
    }

    public CoinStorage getStorage() {
        return storage;
    }

    public Config getConfig() {
        return config;
    }

    public static CoinSystem getInstance() {
        return INSTANCE;
    }
}
