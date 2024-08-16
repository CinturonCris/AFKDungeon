package c.cris.AFKDungeon.AFKDungeon.utils;

public class DropConfig {
    private final String type;
    private final String id;
    private final int chance;
    private final String command;

    public DropConfig(String type, String id, int chance, String command) {
        this.type = type;
        this.id = id;
        this.chance = chance;
        this.command = command;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public int getChance() {
        return chance;
    }

    public String getCommand() {
        return command;
    }
}
