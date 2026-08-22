package masterlazy.satellite.migrate;

import masterlazy.satellite.migrate.model.PlayerData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CLIMain {
    public static void main(String[] args) throws IOException {
        Charset charset = StandardCharsets.UTF_8;
        if (System.console() != null) {
            charset = System.console().charset();
        }
        System.setOut(new PrintStream(System.out, true, charset));
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, charset));

        // Init
        UUIDRepository.load();

        // Welcome
        System.out.println("Welcome to \033[36mSATELLITE\033[0m Migration helper!");
        System.out.println("* Satellite may enforce using offline UUID. If your server has ever run in online mode, this may cause a problem.");
        System.out.println("* I will help you make sure players' data is loaded properly by migrate online UUIDs to offline ones.\r\n");

        System.out.println("* Let's start by locating the game:");
        String path;
        Path base, playerData, advancements;
        while (true) {
            System.out.print("Path to Minecraft game> ");
            path = reader.readLine();
            try {
                base = Paths.get(path);
            } catch (InvalidPathException e) {
                System.out.println("\033[31mInvalid path.\033[0m");
                continue;
            }
            if (!Files.exists(base)) {
                System.out.println("\033[31mDirectory '"+path+"' not exist.\033[0m");
                continue;
            }
            if (!Files.isDirectory(base)) {
                System.out.println("\033[31mGiven path is not a directory.\033[0m");
                continue;
            }
            playerData = Paths.get(path, "world", "playerdata");
            if (!Files.exists(playerData) || !Files.isDirectory(playerData)) {
                System.out.println("\033[31mFailed to locate world/playerdata.\033[0m");
                continue;
            }
            advancements = Paths.get(path, "world", "advancements");
            if (!Files.exists(advancements) || !Files.isDirectory(advancements)) {
                System.out.println("\033[31mFailed to locate world/advancements.\033[0m");
                continue;
            }
            break;
        }

        HashMap<UUID, PlayerData> players = new HashMap<>();
        MigrateUtils.mergeFromDir(players, playerData, ".dat");
        MigrateUtils.mergeFromDir(players, advancements, ".json");

        System.out.println("Detected "+players.size()+" players, parsing...");

        MigrateUtils.parseFile(players, Paths.get(path, "usercache.json"));
        MigrateUtils.parseFile(players, Paths.get(path, "ops.json"));
        MigrateUtils.parseFile(players, Paths.get(path, "whitelist.json"));
        MigrateUtils.parseFile(players, Paths.get(path, "banned-players.json"));
        MigrateUtils.parseLogDir(players, Paths.get(path, "logs"));


        System.out.println("Found "+ players.size()+" UUIDs:");
        System.out.println("<last join> <UUID> --> <name>");
        List<PlayerData> playerList = new java.util.ArrayList<>(players.values().stream().toList());
        playerList.sort(Comparator.comparing(
                PlayerData::lastJoin,
                Comparator.nullsLast(Comparator.reverseOrder()))
        );
        int matched = 0, unmatched = 0;
        for (var i : playerList) {
            System.out.println(i);
            if (i.name() != null) matched++;
            else unmatched++;
        }
        System.out.println(matched+" matched, "+unmatched+" unmatched");

        UUIDRepository.save();
    }
}
