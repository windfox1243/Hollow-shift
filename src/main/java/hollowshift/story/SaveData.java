package hollowshift.story;

import hollowshift.engine.Action;
import hollowshift.engine.Input;
import hollowshift.engine.io.Json;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Settings and progress, stored as JSON in the user's home directory. */
public final class SaveData {
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".hollowshift", "save.json");

    public int shiftsUnlocked = 1;
    public boolean storyFinished;
    public boolean stayedBelow;
    /** Story notes read so far (1..10); all of them unlock the choice to stay below. */
    public final java.util.Set<Integer> notesRead = new java.util.TreeSet<>();
    public int bestEndless;
    public double master = 0.8;
    public double music = 0.7;
    public double sfx = 0.9;
    public boolean fullscreen;
    public boolean screenShake = true;
    public boolean jumpscare = true;
    public String language = "en";
    private final Map<String, Object> bindings = new LinkedHashMap<>();

    public static SaveData load() {
        SaveData s = new SaveData();
        try {
            if (Files.exists(FILE)) {
                Map<String, Object> m = Json.parseObject(Files.readString(FILE, StandardCharsets.UTF_8));
                s.shiftsUnlocked = Json.integer(m.get("shiftsUnlocked"), 1);
                s.storyFinished = Json.bool(m.get("storyFinished"), false);
                s.stayedBelow = Json.bool(m.get("stayedBelow"), false);
                for (Object o : Json.list(m.get("notesRead"))) s.notesRead.add(Json.integer(o, 0));
                s.bestEndless = Json.integer(m.get("bestEndless"), 0);
                s.master = Json.num(m.get("master"), s.master);
                s.music = Json.num(m.get("music"), s.music);
                s.sfx = Json.num(m.get("sfx"), s.sfx);
                s.fullscreen = Json.bool(m.get("fullscreen"), false);
                s.screenShake = Json.bool(m.get("screenShake"), true);
                s.jumpscare = Json.bool(m.get("jumpscare"), true);
                s.language = Json.str(m.get("language"), "en");
                s.bindings.putAll(Json.obj(m.get("bindings")));
            }
        } catch (Exception e) {
            System.err.println("save file unreadable, using defaults: " + e.getMessage());
        }
        return s;
    }

    public void save() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shiftsUnlocked", shiftsUnlocked);
        m.put("storyFinished", storyFinished);
        m.put("stayedBelow", stayedBelow);
        m.put("notesRead", new ArrayList<Object>(notesRead));
        m.put("bestEndless", bestEndless);
        m.put("master", master);
        m.put("music", music);
        m.put("sfx", sfx);
        m.put("fullscreen", fullscreen);
        m.put("screenShake", screenShake);
        m.put("jumpscare", jumpscare);
        m.put("language", language);
        m.put("bindings", bindings);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, Json.write(m), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("could not write save: " + e.getMessage());
        }
    }

    public static final int STORY_NOTES = 10;

    public boolean allNotesRead() {
        return notesRead.size() >= STORY_NOTES;
    }

    public void applyBindings(Input input) {
        input.resetBindings();
        for (Action a : Action.values()) {
            List<Object> keys = Json.list(bindings.get(a.name()));
            for (int i = 0; i < Math.min(2, keys.size()); i++) input.bind(a, i, Json.integer(keys.get(i), 0));
        }
    }

    public void storeBindings(Input input) {
        bindings.clear();
        for (Action a : Action.values()) {
            List<Object> keys = new ArrayList<>();
            for (int k : input.binding(a)) keys.add(k);
            bindings.put(a.name(), keys);
        }
    }
}
