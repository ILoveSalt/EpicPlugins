package hgds.epicgrief.epiccoins.core;

/*
 *
 *  * Copyright (c) 2018 Davide Wietlisbach on 24.11.18 16:53
 *
 */

import hgds.epicgrief.epiccoins.core.event.CoinChangeEventResult;
import hgds.epicgrief.epiccoins.core.event.CoinsUpdateCause;
import hgds.epicgrief.epiccoins.core.player.CoinPlayer;

import java.io.File;

public interface EpicCoinsPlatform {

    String getPlatformName();

    String getServerVersion();

    File getFolder();

    String getColor(CoinPlayer player);

    CoinChangeEventResult executeCoinChangeEvent(CoinPlayer player, long oldCoins, long newCoins, CoinsUpdateCause cause, String message);
}
