package c.cris.AFKDungeon.AFKDungeon.utils;

import java.util.List;
import java.util.Map;

public class MobConfig {
    private final int chance;
    private final String name;
    private final Map<String, DropConfig> drops;
    private final List<String> commands;

    public MobConfig(int chance, String name, Map<String, DropConfig> drops, List<String> commands) {
        this.chance = chance;
        this.name = name;
        this.drops = drops;
        this.commands = commands;
    }

    public int getChance() {
        return chance;
    }

    public String getName() {
        return name;
    }

    public Map<String, DropConfig> getDrops() {
        return drops;
    }

    public List<String> getCommands() {
        return commands;
    }
}
