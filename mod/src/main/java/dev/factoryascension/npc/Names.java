package dev.factoryascension.npc;

import net.minecraft.util.RandomSource;

/** Names that sound like the people you'd meet out there: scavs with nicknames, call signs, player-style handles. */
final class Names {
    private Names() {
    }

    static final String[] FIRST = {"Vasya", "Dima", "Kolya", "Sasha", "Misha", "Lyosha", "Petya", "Zhenya", "Grisha", "Tolik", "Vova", "Seryoga", "Fedya",
        "Igor", "Boris", "Oleg", "Yuri", "Stas", "Roma", "Artyom", "Kostya", "Slava", "Tima", "Lev"};
    static final String[] LAST = {"Tetris", "Shovel", "Kettle", "Bucket", "Mushroom", "Cabbage", "Pipe", "Bolt", "Gopnik", "Spade", "Matches",
        "Valenok", "Samovar", "Kirpich", "Pelmen", "Tank", "Radio", "Sneaker", "Brick", "Stool"};
    static final String[] CALL = {"Kestrel", "Viper", "Rook", "Hammer", "Nomad", "Wraith", "Saber", "Talon", "Mako", "Bishop", "Havoc", "Jackal",
        "Raven", "Specter", "Warden", "Ronin", "Fang", "Ash", "Cinder", "Vulture", "Grim", "Echo", "Atlas", "Banshee"};
    static final String[] HANDLE = {"xX_%s_Xx", "%s_TTV", "%s%d", "the_%s", "%s_Gaming", "Not%s", "%s_IRL", "Big%s", "%s.exe", "%s_Main"};
    static final String[] WORD = {"Sniper", "Rat", "Chad", "Goblin", "Toaster", "Potato", "Wolf", "Ghost", "Reaper", "Noob", "Pro", "Camper",
        "Gremlin", "Bandit", "Hobo", "Doggo", "Shadow", "Viking", "Lurker", "Dad"};

    static String make(OpType t, RandomSource r) {
        return switch (t.faction) {
            case SCAV -> r.nextInt(3) == 0 ? handle(r) : FIRST[r.nextInt(FIRST.length)] + " " + LAST[r.nextInt(LAST.length)];
            case RAIDER -> "Raider " + CALL[r.nextInt(CALL.length)];
            case ROGUE -> t == OpType.HUNTER ? "Hunter " + CALL[r.nextInt(CALL.length)] : CALL[r.nextInt(CALL.length)] + " " + (1 + r.nextInt(4)) + "-" + (1 + r.nextInt(4));
            case CULT -> "The " + new String[] {"Pale", "Silent", "Hollow", "Ashen", "Blind", "Hungry"}[r.nextInt(6)] + " " + new String[] {"Priest", "Brother", "Sister", "One", "Watcher"}[r.nextInt(5)];
            case BANDIT -> t == OpType.WARLORD ? new String[] {"Warlord Kaban", "Warlord Reshala", "Warlord Shturman", "Warlord Glukhar", "Warlord Killa", "Warlord Tagilla"}[r.nextInt(6)]
                : handle(r);
        };
    }

    static String handle(RandomSource r) {
        String f = HANDLE[r.nextInt(HANDLE.length)];
        String w = WORD[r.nextInt(WORD.length)];
        return f.contains("%d") ? String.format(f, w, 10 + r.nextInt(989)) : String.format(f, w);
    }
}
