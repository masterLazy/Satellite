package masterlazy.satellite.guard;

import com.google.common.io.Files;
import masterlazy.satellite.Satellite;
import masterlazy.satellite.guard.model.ConditionEntry;
import masterlazy.satellite.guard.model.RuleEntry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RuleRepository {
    private static final String FILE_NAME = "rules.json";
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<RuleEntry> ruleList = new ArrayList<>();
    private final File jsonFile;

    public RuleRepository(String baseDir) {
        jsonFile = new File(baseDir + FILE_NAME);
        if (jsonFile.exists()) {
            load();
        } else {
            save();
            Satellite.LOGGER.warn("[Satellite] {} not found; creating a new one.", jsonFile);
        }
    }

    public RuleEntry getEntry(int priority) {
        lock.readLock().lock();
        try {
            return ruleList.get(priority);
        } finally {
            lock.readLock().unlock();
        }
    }

    public RuleEntry[] getAllEntry() {
        lock.readLock().lock();
        try {
            return ruleList.toArray(RuleEntry[]::new);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getEntryCount() {
        lock.readLock().lock();
        try {
            return ruleList.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addEntry(RuleEntry entry, int index) {
        lock.writeLock().lock();
        try {
            ruleList.add(index, entry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean removeEntry(RuleEntry entry) {
        lock.writeLock().lock();
        try {
            return ruleList.remove(entry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void replaceEntry(RuleEntry oldEntry, RuleEntry newEntry) {
        lock.writeLock().lock();
        try {
            int idx = ruleList.indexOf(oldEntry);
            ruleList.set(idx, newEntry);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void save() {
        lock.readLock().lock();
        try {
            try (BufferedWriter writer = Files.newWriter(jsonFile, StandardCharsets.UTF_8)) {
                Satellite.GSON.toJson(ruleList, writer);
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to write {}", jsonFile, e);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void load() {
        lock.writeLock().lock();
        try {
            if (!jsonFile.exists()) {
                Satellite.LOGGER.warn("[Satellite] {} not found; will clear ruleset list in memory.", jsonFile);
                ruleList.clear();
                return;
            }

            RuleEntry[] loaded;
            try (BufferedReader reader = Files.newReader(jsonFile, StandardCharsets.UTF_8)) {
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean hasNullField(RuleEntry rule) {
        if (rule.id() == null) return true;
        if (rule.description() == null) return true;
        if (rule.conditions() == null) return true;
        for (ConditionEntry condition : rule.conditions()) {
            if (condition.type() == null) return true;
            if (condition.value() == null) return true;
        }
        return false;
    }
}
