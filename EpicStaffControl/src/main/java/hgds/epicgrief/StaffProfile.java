package hgds.epicgrief;

import java.util.UUID;

public final class StaffProfile {
    private final UUID uniqueId;
    private String lastName;
    private String rank;
    private long workedSeconds;
    private long shiftStartedAt;
    private long lastSalaryAt;
    private int punishments;

    public StaffProfile(UUID uniqueId, String lastName) {
        this.uniqueId = uniqueId;
        this.lastName = lastName;
        this.rank = "";
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank == null ? "" : rank;
    }

    public long getWorkedSeconds() {
        return workedSeconds;
    }

    public void setWorkedSeconds(long workedSeconds) {
        this.workedSeconds = Math.max(0, workedSeconds);
    }

    public long getShiftStartedAt() {
        return shiftStartedAt;
    }

    public void setShiftStartedAt(long shiftStartedAt) {
        this.shiftStartedAt = Math.max(0, shiftStartedAt);
    }

    public long getLastSalaryAt() {
        return lastSalaryAt;
    }

    public void setLastSalaryAt(long lastSalaryAt) {
        this.lastSalaryAt = Math.max(0, lastSalaryAt);
    }

    public int getPunishments() {
        return punishments;
    }

    public void setPunishments(int punishments) {
        this.punishments = Math.max(0, punishments);
    }

    public boolean isOnDuty() {
        return shiftStartedAt > 0;
    }

    public long getTotalWorkedSeconds(long now) {
        if (!isOnDuty()) {
            return workedSeconds;
        }
        return workedSeconds + Math.max(0, now - shiftStartedAt);
    }

    public void finishShift(long now) {
        if (!isOnDuty()) {
            return;
        }
        workedSeconds = getTotalWorkedSeconds(now);
        shiftStartedAt = 0;
    }
}
