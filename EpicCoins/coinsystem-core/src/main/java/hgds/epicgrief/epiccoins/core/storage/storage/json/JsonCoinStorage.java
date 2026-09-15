package hgds.epicgrief.epiccoins.core.storage.storage.json;


import hgds.epicgrief.epiccoins.core.config.Config;
import hgds.epicgrief.epiccoins.core.player.CoinPlayer;
import hgds.epicgrief.epiccoins.core.storage.CoinStorage;
import hgds.epicgrief.epiccoins.core.utils.Document;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/*
 *
 *  * Copyright (c) 2018 Davide Wietlisbach on 24.11.18 16:20
 *
 */

public class JsonCoinStorage implements CoinStorage {

    private AtomicInteger nextID;
    private File file;
    private Document data;
    private List<CoinPlayer> players;

    public JsonCoinStorage(Config config) {
        new File(config.dataFolder).mkdirs();
        this.file = new File(config.dataFolder, "players.json");
        if(file.exists() && file.isFile()) this.data = Document.loadData(file);
        else this.data = new Document();
        this.data.appendDefault("players",new LinkedList<>());
        this.players = this.data.getObject("players", new TypeToken<List<CoinPlayer>>(){}.getType());
        if(this.players == null) this.players = new LinkedList<>();
        nextID = new AtomicInteger(this.players.size()+1);
    }

    @Override
    public boolean connect() {
        return true;
    }

    @Override
    public void disconnect() {}

    @Override
    public boolean isConnected() {
        return true;
    }


    @Override
    public CoinPlayer getPlayer(int id) throws Exception {
        Iterator<CoinPlayer> iterator = this.players.iterator();
        CoinPlayer player;
        while(iterator.hasNext() &&(player = iterator.next()) != null) if(player.getID() == id) return player;
        return null;
    }

    @Override
    public CoinPlayer getPlayer(UUID uuid) {
        Iterator<CoinPlayer> iterator = this.players.iterator();
        CoinPlayer player;
        while(iterator.hasNext() &&(player = iterator.next()) != null) if(player.getUUID().equals(uuid)) return player;
        return null;
    }

    @Override
    public CoinPlayer getPlayer(String name) {
        Iterator<CoinPlayer> iterator = this.players.iterator();
        CoinPlayer player;
        while(iterator.hasNext() &&(player = iterator.next()) != null) if(player.getName().equalsIgnoreCase(name)) return player;
        return null;
    }

    @Override
    public List<CoinPlayer> getPlayers() {
        return new LinkedList<>(this.players);
    }

    @Override
    public List<CoinPlayer> getTopPlayers(int maxReturnSize) {
        LinkedList<CoinPlayer> list = new LinkedList<>(this.players);
        list.sort(Comparator.comparingLong(CoinPlayer::getCoins).reversed());
        return list.subList(0, Math.min(maxReturnSize, list.size()));
    }

    @Override
    public CoinPlayer createPlayer(CoinPlayer player) {
        this.players.add(player);
        player.setIDSimpled(this.nextID.getAndIncrement());
        save();
        return player;
    }
    public void savePlayer(CoinPlayer coinPlayer) {
        Iterator<CoinPlayer > iterator = this.players.iterator();
        CoinPlayer player;
        while(iterator.hasNext() &&(player = iterator.next()) != null) {
            if(player.getUUID().equals(coinPlayer.getUUID())) {
                iterator.remove();
                this.players.add(coinPlayer);
                save();
                return;
            }
        }
    }
    @Override
    public void updateCoins(UUID uuid, long coins) {
        CoinPlayer player = getPlayer(uuid);
        if(player == null) return;
        player.setCoinsSimpled(coins);
        savePlayer(player);
    }
    @Override
    public void updateColor(UUID uuid, String color) {
        CoinPlayer player = getPlayer(uuid);
        if(player == null) return;
        player.setColorSimpled(color);
        savePlayer(player);
    }
    @Override
    public void updateInformations(UUID uuid, String name, String color, long lastLogin) {
        CoinPlayer player = getPlayer(uuid);
        if(player == null) return;
        player.setNameSimpled(name);
        player.setLastLoginSimpled(lastLogin);
        player.setColorSimpled(color);
        savePlayer(player);
    }
    public void save(){
        this.data.append("players",this.players);
        this.data.saveData(this.file);
    }
}
