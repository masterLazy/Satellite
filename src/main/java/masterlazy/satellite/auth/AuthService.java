package masterlazy.satellite.auth;

import masterlazy.satellite.auth.handler.CommandHandler;
import masterlazy.satellite.auth.handler.EventHandler;
import masterlazy.satellite.auth.model.RegisterEntry;
import masterlazy.satellite.session.SessionService;
import net.minecraft.server.level.ServerPlayer;

public class AuthService {
    private final RegisterRepository registerRepository;
    private final CommandHandler commandHandler;
    private final EventHandler eventHandler;

    public AuthService(String baseDir, SessionService sessionService) {
        registerRepository = new RegisterRepository(baseDir);
        commandHandler = new CommandHandler(this, sessionService);
        eventHandler = new EventHandler(this, sessionService);
    }

    public void onInitialize() {
        commandHandler.register();
        eventHandler.register();
    }

    public boolean isRegistered(ServerPlayer player) {
        return isRegistered(player.getName().getString());
    }

    public boolean isRegistered(String username) {
        return registerRepository.hasEntry(username);
    }

    public String[] getRegisteredNames() {
        return registerRepository.getEntryNames();
    }

    public void reloadRepository() {
        registerRepository.load();
    }

    public void savePassword(String username, String password) {
        registerRepository.putEntry(new RegisterEntry(username, AuthUtils.getHash(password)));
        registerRepository.save();
    }

    public boolean isCorrectPassword(String username, String password) {
        RegisterEntry registerEntry = registerRepository.getEntry(username);
        if (registerEntry == null) return false;
        return registerEntry.pwd_hash().equals(AuthUtils.getHash(password));
    }
}
