package eu.rtsketo.sakurastats;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
class WarDay {
    @NonNull
    @PrimaryKey
    private long warDay;
    private String tag;


    @NonNull
    public long getWarDay() { return warDay; }
    public void setWarDay(@NonNull long warDay) { this.warDay = warDay; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public WarDay(long warDay, String tag) {
        this.warDay = warDay;
        this.tag = tag;
    }
}
