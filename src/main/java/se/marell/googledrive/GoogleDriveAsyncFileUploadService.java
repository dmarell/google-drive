/*
 * Created by Daniel Marell 01/05/16.
 */
package se.marell.googledrive;

import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
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

class UploadSlot {
    private long startTime;
    private Future<String> future;

    UploadSlot(Future<String> future) {
        this.startTime = System.currentTimeMillis();
        this.future = future;
    }

    long getStartTime() {
        return startTime;
    }

    Future<String> getFuture() {
        return future;
    }
}

@Service
class GoogleDriveFileUploadQueueService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private List<UploadSlot> pendingUploads = new ArrayList<>();

    public synchronized int getNumPendingUploads() {
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
        return pendingUploads.size();
    }

    public void addUpload(Future<String> fileUrl) {
        pendingUploads.add(new UploadSlot(fileUrl));
    }
}

/**
 * Supports async file upload using GoogleDriveService.
 */
@Service
public class GoogleDriveAsyncFileUploadService {
    @Autowired
    private GoogleDriveFileUploadQueueService queueService;

    @Autowired
    private GoogleDriveService driveService;


    @Async
    public Future<String> uploadFile(String filename, String mimeType, String folderId, InputStream in, long fileSize,
                                     MediaHttpUploaderProgressListener progressListener) throws IOException {
        Future<String> link = new AsyncResult<>(driveService.uploadFile(
                filename, mimeType, folderId, in, fileSize, progressListener).getWebContentLink());
        queueService.addUpload(link);
        return link;
    }
}
