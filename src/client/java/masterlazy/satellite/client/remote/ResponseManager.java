package masterlazy.satellite.client.remote;

import masterlazy.satellite.remote.HasRequestId;
import masterlazy.satellite.Satellite;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class ResponseManager <PayloadT extends HasRequestId> {
    private final ConcurrentHashMap<UUID, CompletableFuture<PayloadT>> pendingFutures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PayloadT> earlyResponses = new ConcurrentHashMap<>();

    private final CompletableFuture<PayloadT> CANCELLED_FUTURE = new CompletableFuture<>();
    public ResponseManager() {
        CANCELLED_FUTURE.cancel(false);
    }

    public void handle(PayloadT payload, Context context) {
        UUID requestId = payload.requestId();
        CompletableFuture<PayloadT> future = pendingFutures.remove(requestId);
        if (future != null) {
            if (future == CANCELLED_FUTURE) return;
            future.complete(payload);
        } else {
            earlyResponses.put(requestId, payload);
        }
    }

    public Future<PayloadT> responseFor(UUID requestId) {
        PayloadT early = earlyResponses.remove(requestId);
        if (early != null) {
            return CompletableFuture.completedFuture(early);
        }
        CompletableFuture<PayloadT> future = new CompletableFuture<>();
        CompletableFuture<PayloadT> existing = pendingFutures.putIfAbsent(requestId, future);

        if (existing != null) {
            Satellite.LOGGER.warn("[Satellite Client] Multiple waiting for same response {}", requestId);
            return existing;
        }
        future.whenComplete((r, e) -> {
            if (future.isCancelled()) {
                pendingFutures.put(requestId, CANCELLED_FUTURE);
            } else {
                pendingFutures.remove(requestId, future);
            }
        });
        return future;
    }
}
