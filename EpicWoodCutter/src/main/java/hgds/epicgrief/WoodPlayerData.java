package hgds.epicgrief;

import java.util.UUID;

final class WoodPlayerData {

    private int backpack;
    private double salary;
    private long lastCutAt;

    WoodPlayerData() {
    }

    WoodPlayerData(int backpack, double salary, long lastCutAt) {
        this.backpack = backpack;
        this.salary = salary;
        this.lastCutAt = lastCutAt;
    }

    int getBackpack() {
        return backpack;
    }

    void setBackpack(int backpack) {
        this.backpack = backpack;
    }

    double getSalary() {
        return salary;
    }

    void setSalary(double salary) {
        this.salary = salary;
    }

    long getLastCutAt() {
        return lastCutAt;
    }

    void setLastCutAt(long lastCutAt) {
        this.lastCutAt = lastCutAt;
    }

    void addTree(double earn) {
        backpack++;
        salary += earn;
    }

    void resetJobProgress() {
        backpack = 0;
        salary = 0.0;
    }

    static String path(UUID uuid) {
        return uuid.toString();
    }
}
