package hgds.epicgrief.epiccoins.core.storage.storage.sql.sqlite;

/*
 *
 *  * Copyright (c) 2018 Philipp Elvin Friedhoff on 17.11.18 20:38
 *
 */

import hgds.epicgrief.epiccoins.core.config.Config;
import hgds.epicgrief.epiccoins.core.manager.MessageManager;
import hgds.epicgrief.epiccoins.core.storage.storage.sql.SQLCoinStorage;
import hgds.epicgrief.epiccoins.core.storage.storage.sql.table.Table;
import com.zaxxer.hikari.HikariConfig;

import java.io.File;
import java.io.IOException;

public class SQLiteCoinStorage extends SQLCoinStorage {

    public SQLiteCoinStorage(Config config) {
        super(config);
    }
    @Override
    public void connect(Config config) {
        new File(config.dataFolder).mkdirs();
        try {
            new File(config.dataFolder,"players.db").createNewFile();
        } catch (IOException exception) {
            System.err.println("Could not create SQLite database file ("+exception.getMessage()+").");
        }
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:"+new File(config.dataFolder, "players.db").getPath());
        setDataSource(hikariConfig);
    }
    @Override
    public void loadDriver() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println(MessageManager.getInstance().system_name+"Could not load SQLiteCoinStorage driver.");
        }
    }
    @Override
    public void createTable(Table table) {
        table.create()
                .create("`ID` INTEGER PRIMARY KEY AUTOINCREMENT")
                .create("`uuid` varchar(120) NOT NULL")
                .create("`name` varchar(32) NOT NULL")
                .create("`color` varchar(32) NOT NULL")
                .create("`firstLogin` varchar(60) NOT NULL")
                .create("`lastLogin` varchar(60) NOT NULL")
                .create("`coins` INTEGER NOT NULL")
                .execute();
    }
}
