package eu.rtsketo.sakurastats.dbobjects;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public
class WarDay {
    @NonNull
    @PrimaryKey
    private Long warDay;
    private String tag;


    @NonNull
    public Long getWarDay() { return warDay; }
    public void setWarDay(@NonNull Long warDay) { this.warDay = warDay; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public WarDay(long warDay, String tag) {
        this.warDay = warDay;
        this.tag = tag;
    }
}
