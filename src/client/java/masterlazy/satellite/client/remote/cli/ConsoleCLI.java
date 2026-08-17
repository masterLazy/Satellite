package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.client.SatelliteClient;
import masterlazy.satellite.client.remote.UnauthorizedException;
import masterlazy.satellite.remote.FeedManager;
import masterlazy.satellite.remote.model.CommandEnum;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.CommandS2CPayload;
import masterlazy.satellite.remote.payload.ConsoleFeedS2CPayload;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ConsoleCLI {
    private final SatelliteCLI cli;
    private final BufferedReader reader;
    private final OutputStream out;
    private static boolean isRunning = false;

    public ConsoleCLI(SatelliteCLI cli) {
        this.cli = cli;
        reader = cli.reader;
        out = cli.out;
    }

    public String getPrompt() {
        return "\033[32m"+SatelliteClient.getServerName()+"\033[0m:\033[36mconsole\033[0m# ";
    }

    public void run() throws ExecutionException, InterruptedException {
        if (isRunning) {
            System.out.println("\033[31mAnother instance is running. Exiting.\033[0m");
            return;
        }
        isRunning = true;
        Instant lastSubscribe = null;
        UUID lastFeedId = UUID.randomUUID();
        // Command input
        StringBuilder sb = new StringBuilder();
        boolean restoreInput;
        boolean enteredEsc = false, enteringSeq = false;
        int c, cursorAt = 0;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                restoreInput = false;
                try {
                    // Subscribe
                    if (lastSubscribe == null || lastSubscribe.plus(FeedManager.TIMEOUT_SUBSCRIBE).minus(Duration.ofSeconds(30)).isBefore(Instant.now())) {
                        if (!subscribe()) return;
                        lastSubscribe = Instant.now();
                    }
                    // Receive feed
                    // NOTE: When subscription succeed, server will log 'xxx subscribed...'
                    //       This will cause a feed payload, so we don't intend to fetch
                    ConsoleFeedS2CPayload feed = SatelliteClient.remoteClient.pollFeed();
                    if (feed != null) {
                        if (feed.feedId().compareTo(lastFeedId) == 0) continue; // Skip same feed
                        if (feed.parentId().compareTo(lastFeedId) == 0) {
                            System.out.print('\r');
                            System.out.print(normalize(feed.content()));
                        } else { // Re-fetch to sync
                            CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(cli.token, CommandEnum.FETCH_1000, null);
                            if (response == null) {
                                System.out.println("\033[31mFailed to sync console stream\033[0m");
                                continue;
                            } else if (response.status() == Status.UNAUTHORIZED) {
                                throw new UnauthorizedException();
                            } else if (response.status() != Status.OK || response.results().length < 1) {
                                System.out.println("\033[31mFailed to sync console stream\033[0m");
                                continue;
                            }
                            cli.clear();
                            System.out.print(normalize(response.results()[0]));
                        }
                        lastFeedId = feed.feedId();
                        restoreInput = true;
                    }
                    // Input
                    if (reader.ready()) {
                        c = reader.read();
                        if (enteredEsc) {
                            if (c == '[') enteringSeq = true;
                            enteredEsc = false;
                        } else if (enteringSeq) {
                            if (c == 'D' && cursorAt > 0) { // Left
                                System.out.print("\b");
                                cursorAt--;
                            } else if (c == 'C' && cursorAt < sb.length()) { // Right
                                System.out.print("\033[C");
                                cursorAt++;
                            }
                            enteringSeq = false;
                        } else if (c == '\r' || c == '\n') {
                            if (sb.toString().equalsIgnoreCase("quit")) {
                                System.out.print("\r\n");
                                break;
                            }
                            execute(sb.toString());
                            System.out.print("\r\n");
                            sb.setLength(0);
                            cursorAt = 0;
                            restoreInput = true;
                        } else if (c == '\003') { // Ctrl+C
                            System.out.print("^C\r\n");
                            break;
                        } else if (c == '\033') { // Esc
                            enteredEsc = true;
                        } else if (c == '\b' || c == 127) { // Backspace
                            if (cursorAt > 0) {
                                sb.deleteCharAt(cursorAt - 1);
                                cursorAt--;
                                restoreInput = true;
                            }
                        } else if (c == '\t') {
                            continue;
                        } else {
                            sb.insert(cursorAt, (char) c);
                            cursorAt++;
                            restoreInput = true;
                        }
                    }
                    // Restore input line
                    if (!restoreInput) continue;
                    out.write(("\r" + getPrompt() + sb + "\033[K").getBytes(StandardCharsets.UTF_8));
                    for (int i = sb.length() - 1; i >= cursorAt; i--) {
                        out.write('\b');
                    }
                    out.flush();
                } catch (UnauthorizedException e) {
                    cli.token = cli.tokenSupplier.get();
                    if (cli.token == null) throw e;
                } catch (InterruptedException | IOException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            unsubscribe();
            isRunning = false;
        }
    }

    public boolean subscribe() throws ExecutionException, InterruptedException, UnauthorizedException {
        CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(cli.token, CommandEnum.SUBSCRIBE, null);
        if (response == null) {
            System.out.println("\033[31mFailed to subscribe console stream\033[0m");
            return false;
        } else if (response.status() == Status.UNAUTHORIZED) {
            throw new UnauthorizedException();
        } else if (response.status() != Status.OK) {
            System.out.println("\033[31mFailed to subscribe console stream: "+response.status().name()+"\033[0m");
            return false;
        }
        return true;
    }

    public void unsubscribe() throws ExecutionException, InterruptedException {
        CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(cli.token, CommandEnum.UNSUBSCRIBE, null);
        if (response == null) {
            System.out.println("\033[31mFailed to unsubscribe console stream \033[0m");
        } else if (response.status() != Status.OK) {
            System.out.println("\033[31mFailed to unsubscribe console stream"+response.status().name()+"\033[0m");
        }
    }

    public void execute(String command) throws ExecutionException, InterruptedException {
        CommandS2CPayload response = SatelliteClient.remoteClient.sendAndWait(cli.token, CommandEnum.EXECUTE, new String[]{command});
        if (response == null) {
            System.out.println("\033[31mFailed to execute\033[0m");
        } else if (response.status() == Status.UNAUTHORIZED) {
            throw new UnauthorizedException();
        } else if (response.status() != Status.OK) {
            System.out.println("\033[31mFailed to execute: "+response.status().name()+"\033[0m");
        }
    }

    // Normalize line separator & decorate
    private String normalize(String raw) {
        return raw.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\n", System.lineSeparator())
                .replaceAll("\\[([^/\\]]+)/(DEBUG)]","\033[35m[$1/$2]\033[0m")
                .replaceAll("\\[([^/\\]]+)/(INFO)]","\033[32m[$1/$2]\033[0m")
                .replaceAll("\\[([^/\\]]+)/(WARN)]","\033[33m[$1/$2]\033[0m")
                .replaceAll("\\[([^/\\]]+)/(ERROR)]","\033[31m[$1/$2]\033[0m")
                .replaceAll("\\[([01]\\d|2[0-3]):([0-5]\\d):([0-5]\\d)]", "\033[34m[$1:$2:$3]\033[0m");
    }
}
