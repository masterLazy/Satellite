package masterlazy.satellite.remote.pipeline;

import masterlazy.satellite.Satellite;
import masterlazy.satellite.remote.FileSession;
import masterlazy.satellite.remote.RemoteService;
import masterlazy.satellite.remote.RemoteSession;
import masterlazy.satellite.remote.RemoteSessionManager;
import masterlazy.satellite.remote.model.FilePayloadType;
import masterlazy.satellite.remote.model.Request;
import masterlazy.satellite.remote.model.Status;
import masterlazy.satellite.remote.payload.FileC2SPayload;
import masterlazy.satellite.remote.payload.FileS2CPayload;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.*;

public class FileHandler implements PayloadHandler<FileC2SPayload> {
    private final RemoteService service;
    private final RemoteSessionManager remoteSessionManager;

    static class SendTask {
        public Request<FileC2SPayload> request;
        public Path file;
        public int part; // From 1
        public int partEnd;
        public SendTask(Request<FileC2SPayload> request, Path file, int part, int partEnd) {
            this.request = request;
            this.file = file;
            this.part = part;
            this.partEnd = partEnd;
        }
    }

    private final ConcurrentLinkedQueue<UUID> activeSessions = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<UUID, ConcurrentLinkedQueue<SendTask>> sessionQueues = new ConcurrentHashMap<>();
    @SuppressWarnings("FieldCanBeLocal")
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // This is the best parameters I found: 128KiB * 64 = 8MiB/batch
    public static final int PART_BYTES = 128*1024;
    public static final int BATCH_SIZE = 64;
    public static final int BYTES_LIMIT_PER_SECOND = 25*1024*1024; // 25 MiB/s

    public FileHandler(RemoteService service, RemoteSessionManager remoteSessionManager) {
        this.service = service;
        this.remoteSessionManager = remoteSessionManager;
        double period = 1000 / (BYTES_LIMIT_PER_SECOND * 1.0 / PART_BYTES);
        scheduler.scheduleAtFixedRate(this::sendTick, 0, Math.round(period), TimeUnit.MILLISECONDS);
    }

    @Override
    public void handle(Request<FileC2SPayload> request) {
        FileC2SPayload payload = request.payload();
        // Verify
        if (service.verifyToken(request, s -> {
            if (s == Status.TOO_MANY_REQUEST) {
                RemoteSession session = remoteSessionManager.getValid(payload.token());
                if (session != null) {
                    respond(request, FilePayloadType.WAIT, session.getTryAfterSecond(), null);
                }
            }
            respond(request, FilePayloadType.INTERRUPT, 0, null);
        })) {
            return;
        }
        FileSession session = service.getFileSession(request.payload().sessionId());
        if (session == null || !session.checkOwnership(payload.token(), request.sender())) {
            respond(request, FilePayloadType.INTERRUPT, 0, null);
            return;
        }
        // End of verify
        session.refresh();
        if (payload.payloadType() == FilePayloadType.FETCH) {
            UUID sessionId = payload.sessionId();
            ConcurrentLinkedQueue<SendTask> queue = sessionQueues.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>());
            queue.offer(new SendTask(request, session.getFile(), payload.arg(), payload.arg() + BATCH_SIZE - 1));
            if (!activeSessions.contains(sessionId)) {
                activeSessions.offer(sessionId);
            }
        }
        // TODO: 处理更多 type
    }

    private void sendTick() {
        UUID sessionId = activeSessions.poll();
        if (sessionId == null) return;
        ConcurrentLinkedQueue<SendTask> queue = sessionQueues.get(sessionId);
        if (queue == null || queue.isEmpty()) return;
        // Send
        SendTask task = queue.poll();
        try {
            long fileSize = Files.size(task.file);
            long begin = (long) (task.part-1) * PART_BYTES;
            if (begin > fileSize || begin < 0) { // No feedback to client; otherwise causes payload left in queue
                sessionQueues.remove(sessionId);
                return;
            }
            try (RandomAccessFile raf = new RandomAccessFile(task.file.toString(), "r")) {
                raf.seek(begin);
                byte[] data = new byte[PART_BYTES];
                int len = raf.read(data);
                if (len != PART_BYTES) {
                    data = Arrays.copyOfRange(data, 0, len);
                }
                if (len == PART_BYTES) {
                    respond(task.request, FilePayloadType.TRANSFER, task.part, data);
                } else {
                    respond(task.request, FilePayloadType.TRANSFER, -task.part, data);
                }
            }
            if (task.part < task.partEnd) {
                task.part++;
                queue.offer(task);
            }
        } catch (IOException e) {
            Satellite.LOGGER.error("[Satellite] Failed to handle send task",e);
            respond(task.request, FilePayloadType.INTERRUPT, 0, null);
        }
        // End of send
        if (!queue.isEmpty()) {
            activeSessions.offer(sessionId);
        } else {
            sessionQueues.remove(sessionId);
        }
    }

    private static void respond(Request<FileC2SPayload> request, FilePayloadType payloadType, int arg, byte @Nullable [] data) {
        FileS2CPayload feedback = new FileS2CPayload(request.payload().sessionId(), payloadType, arg, data == null ? new byte[0] : data);
        request.respond(feedback);
    }
}
