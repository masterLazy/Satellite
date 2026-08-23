package masterlazy.satellite.client.remote.cli;

import masterlazy.satellite.remote.payload.CommandS2CPayload;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;

public interface ShellContext {
    @Nullable String token();
    void renewToken();
    BufferedReader getReader();

    /**
     * Write string without flush
     */
    void write(String s);

    /**
     * Flush output buffer
     */
    void flush();

    /**
     * Set input suggestions
     */
    void setSuggestions(String[] s);

    /**
     * Write character
     */
    default void write(char c) {
        write(String.valueOf(c));
    }

    /**
     * Write string and flush
     */
    default void print(String s) {
        write(s);
        flush();
    }

    /**
     * Write string, start newline and flush
     */
    default void println(String s) {
        print(s + "\r\n");
    }

    default void reportFailure(String action, CommandS2CPayload payload) {
        if (payload.results().length > 0) {
            println("\033[31mFailed to "+action+": "+payload.status().name()+": "+payload.results()[0]+"\033[0m");
        } else {
            println("\033[31mFailed to "+action+": "+payload.status().name()+"\033[0m");
        }
    }
}
