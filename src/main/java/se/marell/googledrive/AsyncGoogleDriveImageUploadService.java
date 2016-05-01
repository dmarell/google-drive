/*
 * Created by Daniel Marell 01/05/16.
 */
package se.marell.googledrive;

import com.google.common.net.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Supports async image upload using GoogleDriveService.
 */
@Service
public class AsyncGoogleDriveImageUploadService {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private GoogleDriveService driveService;

    private List<UploadSlot> pendingUploads = new ArrayList<>();

    @Async
    public Future<String> uploadImage(String folderId, String filename, MediaType mediaType, InputStream in) throws IOException {
        Future<String> r = new AsyncResult<>(driveService.uploadImage(folderId, filename, mediaType, in));
        pendingUploads.add(new UploadSlot(r));
        return r;
    }

    public synchronized void check() {
        Iterator<UploadSlot> iter = pendingUploads.iterator();
        while (iter.hasNext()) {
            UploadSlot s = iter.next();
            if (s.getFuture().isDone()) {
                log.debug("Upload ready, {} milliseconds", System.currentTimeMillis() - s.getStartTime());
                iter.remove();
            }
        }
        if (!pendingUploads.isEmpty()) {
            log.debug("Pending uploads: {}", pendingUploads.size());
        }
    }

    public synchronized int getNumPendingUploads() {
        check();
        return pendingUploads.size();
    }

    private static class UploadSlot {
        private long startTime;
        private Future<String> future;

        public UploadSlot(Future<String> future) {
            this.startTime = System.currentTimeMillis();
            this.future = future;
        }

        public long getStartTime() {
            return startTime;
        }

        public Future<String> getFuture() {
            return future;
        }
    }
}
