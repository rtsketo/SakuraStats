package eu.rtsketo.sakurastats.dbobjects;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public
class ClanPlayer {

    @NonNull
    @PrimaryKey
    private String tag;
    private String clan;
    private long last;
    private int score;
    private int trophies;
    private int smc;
    private int legendary;
    private int magical;
    private String role;

    @NonNull
    public String getTag() { return tag; }
    public long getLast() { return last; }
    public int getSmc() { return smc; }
    public int getScore() { return score; }
    public String getClan() { return clan; }
    public String getRole() { return role; }
    public int getTrophies() { return trophies; }
    public int getLegendary() { return legendary; }
    public int getMagical() { return magical; }
    public void setRole(String role) { this.role = role; }
    public void setTag(@NonNull String tag) { this.tag = tag; }
    public void setScore(int score) { this.score = score; }
    public void setLast(long last) { this.last = last; }
    public void setClan(String clan) { this.clan = clan; }
    public void setTrophies(int trophies) { this.trophies = trophies; }
    public void setSmc(int smc) { this.smc = smc; }
    public void setLegendary(int legendary) { this.legendary = legendary; }
    public void setMagical(int magical) { this.magical = magical; }

    public ClanPlayer() { /* Needed by DAO */ }

}
