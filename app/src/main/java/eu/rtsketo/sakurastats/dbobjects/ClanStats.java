package eu.rtsketo.sakurastats.dbobjects;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public
class ClanStats {
    @NonNull
    @PrimaryKey
    private String tag;

    @NonNull private String name;
    @NonNull private String state;
    private String badge;

    private int maxParticipants;
    private int estimatedWins;
    private double extraWins;
    private int warTrophies;
    private int actualWins;
    private int remaining;
    private int crowns;

    private String clan1;
    private String clan2;
    private String clan3;
    private String clan4;

    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
    public void setEstimatedWins(int estimatedWins) { this.estimatedWins = estimatedWins; }
    public void setWarTrophies(int warTrophies) { this.warTrophies = warTrophies; }
    public void setActualWins(int actualWins) { this.actualWins = actualWins; }
    public void setExtraWins(double extraWins) { this.extraWins = extraWins; }
    public void setRemaining(int remaining) { this.remaining = remaining; }
    public void setCrowns(int crowns) { this.crowns = crowns; }
    public int getMaxParticipants() { return maxParticipants; }
    public void setBadge(String badge) { this.badge = badge; }
    public void setState(String state) { this.state = state; }
    public void setClan4(String clan4) { this.clan4 = clan4; }
    public void setClan2(String clan2) { this.clan2 = clan2; }
    public void setClan3(String clan3) { this.clan3 = clan3; }
    public void setClan1(String clan1) { this.clan1 = clan1; }
    public int getEstimatedWins() { return estimatedWins; }
    public double getExtraWins() { return extraWins; }



    public void setName(String name) { this.name = name; }
    public int getWarTrophies() { return warTrophies; }
    public void setTag(String tag) { this.tag = tag; }
    public int getActualWins() { return actualWins; }
    public int getRemaining() { return remaining; }
    public String getClan3() { return clan3; }
    public String getClan4() { return clan4; }
    public String getClan2() { return clan2; }
    public String getClan1() { return clan1; }
    public String getState() { return state; }
    public String getBadge() { return badge; }
    public int getCrowns() { return crowns; }
    public String getName() { return name; }
    public String getTag() { return tag; }
    public ClanStats() { /* Needed by DAO */ }
}
