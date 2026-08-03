package masterlazy.satellite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LangManager {
    private Map<String, String> lang = new HashMap<>();

    public LangManager() {
        ClassLoader classLoader = LangManager.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("assets/satellite/lang.json")) {
            if (inputStream == null) {
                Satellite.LOGGER.error("[Satellite] Failed to load lang.json");
                return;
            }
            Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            Type typeOfMap = new TypeToken<Map<String, String>>() {
            }.getType();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            lang = gson.fromJson(reader, typeOfMap);
        } catch (Exception e) {
            Satellite.LOGGER.error("[Satellite] Exception occurred when loading lang.json. Did you MODIFIED it wrongly?");
            Satellite.LOGGER.error(e.toString());
        }
    }

    public String get(String key) {
        if (lang.containsKey(key)) {
            return lang.get(key);
        } else {
            Satellite.LOGGER.error("[Satellite] Failed to load text {}", key);
            return "Error";
        }
    }
}
