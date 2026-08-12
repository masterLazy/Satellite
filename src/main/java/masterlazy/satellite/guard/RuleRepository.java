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
import java.util.function.Supplier;

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

    public boolean hasEntry(RuleEntry entry) {
        return Boolean.TRUE.equals(withReadLock(() -> ruleList.contains(entry)));
    }

    public RuleEntry getEntry(int priority) {
        return withReadLock(() -> ruleList.get(priority));
    }

    public RuleEntry getEntry(String id) {
        return withReadLock(()->{
            for (RuleEntry entry : ruleList) {
                if (entry.id().equalsIgnoreCase(id)) return entry;
            }
            return null;
        });
    }

    public RuleEntry[] getAllEntries() {
        return withReadLock(() -> ruleList.toArray(RuleEntry[]::new));
    }

    public int getEntryCount() {
        Integer res = withReadLock(ruleList::size);
        if (res == null) return 0;
        return res;
    }

    public void addEntry(RuleEntry entry, int index) {
        withWriteLock(() -> ruleList.add(index, entry));
    }

    public boolean removeEntry(RuleEntry entry) {
        return Boolean.TRUE.equals(withWriteLock(() -> ruleList.remove(entry)));
    }

    public void replaceEntry(RuleEntry oldEntry, RuleEntry newEntry) {
        withWriteLock(() -> {
            int idx = ruleList.indexOf(oldEntry);
            ruleList.set(idx, newEntry);
        });
    }

    public boolean save() {
        return Boolean.TRUE.equals(withWriteLock(() -> {
            try (BufferedWriter writer = Files.newWriter(jsonFile, StandardCharsets.UTF_8)) {
                Satellite.GSON.toJson(ruleList, writer);
                return true;
            } catch (Exception e) {
                Satellite.LOGGER.error("[Satellite] Failed to write {}", jsonFile, e);
                return false;
            }
        }));
    }

    public void load() {
        withWriteLock(() -> {
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

    // Helpers

    private <T> T withReadLock(Supplier<T> task) {
        lock.readLock().lock();
        try {
            return task.get();
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when reading RuleRepository", e);
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    private <T> T withWriteLock(Supplier<T> task) {
        lock.writeLock().lock();
        try {
            return task.get();
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when writing to RuleRepository", e);
        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    private void withWriteLock(Runnable task) {
        lock.writeLock().lock();
        try {
            task.run();
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when writing to RuleRepository", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
