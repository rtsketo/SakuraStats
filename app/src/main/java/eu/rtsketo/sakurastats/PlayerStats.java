package eu.rtsketo.sakurastats;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class PlayerStats {
    @NonNull
    @PrimaryKey
    private String tag;

    @NonNull
    private String name;
    private String clan;
    private int wins;
    private int played;
    private int curWins;
    private int curPlay;
    private double ratio;
    private double norma;
    private boolean current;
    private int missed;
    private int cards;
    private int wars;
    private int chest;

    public int getWins() { return wins; }
    public int getChest() { return chest; }
    public String getClan() { return clan; }
    public int getPlayed() { return played; }
    public double getNorma() { return norma; }
    public double getRatio() { return ratio; }
    public int getCurPlay() { return curPlay; }
    public int getCurWins() { return curWins; }
    public boolean isCurrent() { return current; }
    @NonNull public String getTag() { return tag; }
    @NonNull public String getName() { return name; }
    public void setWins(int wins) { this.wins = wins; }
    public void setClan(String clan) { this.clan = clan; }
    public void setChest(int chest) { this.chest = chest; }
    public void setNorma(double norma) { this.norma = norma; }
    public void setRatio(double ratio) { this.ratio = ratio; }
    public void setPlayed(int played) { this.played = played; }
    public void setTag(@NonNull String tag) { this.tag = tag; }
    public void setName(@NonNull String name) { this.name = name; }
    public void setCurWins(int curWins) { this.curWins = curWins; }
    public void setCurPlay(int curPlay) { this.curPlay = curPlay; }
    public void setCurrent(boolean current) { this.current = current; }
    public void setMissed(int missed) { this.missed = missed; }
    public void setCards(int cards) { this.cards = cards; }
    public void setWars(int wars) { this.wars = wars; }
    public int getMissed() { return missed; }
    public int getWars() { return wars; }
    public int getCards() { return cards; }
    public PlayerStats() { }
}

