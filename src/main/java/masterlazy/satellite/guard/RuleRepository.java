package masterlazy.satellite.guard;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.WithReadWriteLock;
import masterlazy.satellite.guard.model.ConditionEntry;
import masterlazy.satellite.guard.model.RuleEntry;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class RuleRepository extends WithReadWriteLock {
    @Override
    protected String getClassName() { return RuleRepository.class.getName(); }

    private static final String FILE_NAME = "rules.json";
    private final List<RuleEntry> ruleList = new ArrayList<>();
    private final Path jsonFile;

    public RuleRepository(String baseDir) {
        jsonFile = Paths.get(baseDir, FILE_NAME);
        if (Files.exists(jsonFile)) {
            load();
        } else {
            save();
            Satellite.LOGGER.warn("[Satellite] {} not found; creating a new one.", jsonFile);
        }
    }

    public boolean hasEntry(RuleEntry entry) {
        return withReadLock(() -> ruleList.contains(entry)).orElse(false);
    }

    @Nullable
    public RuleEntry getEntry(int priority) {
        return withReadLock(() -> ruleList.get(priority)).orElse(null);
    }

    @Nullable
    public RuleEntry getEntry(String id) {
        return withReadLock(()->{
            for (RuleEntry entry : ruleList) {
                if (entry.id().equalsIgnoreCase(id)) return entry;
            }
            return null;
        }).orElse(null);
    }

    @Nullable
    public RuleEntry[] getAllEntries() {
        return withReadLock(() -> ruleList.toArray(RuleEntry[]::new)).orElse(null);
    }

    public int getEntryCount() {
        return withReadLock(ruleList::size).orElse(0);
    }

    public void addEntry(RuleEntry entry, int index) {
        withWriteLock(() -> ruleList.add(index, entry));
    }

    public boolean removeEntry(RuleEntry entry) {
        return withWriteLockB(() -> ruleList.remove(entry));
    }

    public void replaceEntry(RuleEntry oldEntry, RuleEntry newEntry) {
        withWriteLock(() -> {
            int idx = ruleList.indexOf(oldEntry);
            ruleList.set(idx, newEntry);
        });
    }

    public boolean save() {
        return withWriteLockB(() -> {
            try (BufferedWriter writer = Files.newBufferedWriter(jsonFile, StandardCharsets.UTF_8)) {
                Satellite.GSON.toJson(ruleList, writer);
                return true;
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to write {}", jsonFile, e);
                return false;
            }
        });
    }

    public void load() {
        withWriteLock(() -> {
            if (Files.notExists(jsonFile)) {
                Satellite.LOGGER.warn("[Satellite] {} not found; will clear ruleset list in memory.", jsonFile);
                ruleList.clear();
                return;
            }

            RuleEntry[] loaded;
            try (BufferedReader reader = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
                loaded = Satellite.GSON.fromJson(reader, RuleEntry[].class);
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to parse {}. Keeping current data.", jsonFile, e);
                return;
            }
            if (loaded == null) {
                Satellite.LOGGER.error("[Satellite] {} is empty or invalid.", jsonFile);
                return;
            }

            boolean hasNull = false;
            for (RuleEntry rule : loaded) {
                if (hasNullField(rule)) {
                    hasNull = true;
                    break;
                }
            }
            if (hasNull) {
                Satellite.LOGGER.warn("[Satellite] One or more rules are incomplete in {}.", jsonFile);
            }

            ruleList.clear();
            ruleList.addAll(List.of(loaded));
            Satellite.LOGGER.info("[Satellite] Loaded {} ({} rules)", jsonFile, ruleList.size());
        });
    }

    public boolean hasNullField(RuleEntry rule) {
        if (rule.id() == null) return true;
        if (rule.description() == null) return true;
        if (rule.conditions() == null) return true;
        for (ConditionEntry condition : rule.conditions()) {
            if (condition == null) return true;
            if (condition.type() == null) return true;
            if (condition.value() == null) return true;
        }
        return false;
    }
}
