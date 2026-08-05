package masterlazy.satellite.auth;

import masterlazy.satellite.auth.model.RegisterEntry;
import masterlazy.satellite.auth.model.RegisterJson;
import masterlazy.satellite.session.SessionManager;
import net.minecraft.server.level.ServerPlayer;

public class AuthManager {
    private final RegisterJson registerJson;

    public final AuthHandler handler;

    public AuthManager(String baseDir, SessionManager sessionManager) {
        registerJson = new RegisterJson(baseDir + "register.json");
        handler = new AuthHandler(this, sessionManager);
    }

    public boolean isRegistered(ServerPlayer player) {
        return isRegistered(player.getName().getString());
    }

    public boolean isRegistered(String username) {
        return registerJson.hasEntry(username);
    }

    public String[] getRegisteredPlayerNames() {
        return registerJson.getRegisteredNames();
    }

    public void reloadJson() {
        registerJson.load();
    }

    public void savePassword(String username, String password) {
        registerJson.putAndSave(new RegisterEntry(username, AuthUtil.getHash(password)));
    }

    public boolean isCorrectPassword(String username, String password) {
        RegisterEntry registerEntry = registerJson.getEntry(username);
        if (registerEntry == null) return false;
        return registerEntry.pwd_hash().equals(AuthUtil.getHash(password));
    }
}
