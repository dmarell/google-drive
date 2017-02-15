/*
 * Created by Daniel Marell 16-03-23 21:30
 */
package se.marell.googledrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class GoogleDriveService {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static Logger logger = LoggerFactory.getLogger(GoogleDriveService.class);
    private final List<String> scopes = Arrays.asList(
            DriveScopes.DRIVE
    );
    private HttpTransport httpTransport;
    private Drive drive;

    @Autowired
    private Environment environment;

    private String applicationName;

    @PostConstruct
    public void init() throws IOException {
        applicationName = environment.getRequiredProperty("google-drive.application-name");
        if (httpTransport == null) {
            try {
                httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("newTrustedTransport failed", e);
            }
        }
    }

    public Drive getDrive() throws IOException {
        if (drive == null) {
            init();
            drive = new Drive.Builder(
                    httpTransport, JSON_FACTORY, authorize())
                    .setApplicationName(applicationName)
                    .build();
        }
        return drive;
    }

    public Credential authorize() throws IOException {
        return GoogleCredential.fromStream(GoogleDriveService.class.getResourceAsStream("/google-drive-client-secret.json"))
                .createScoped(scopes);
    }

    public void deleteFile(String fileId) throws IOException {
        getDrive().files().delete(fileId).execute();
    }

    public FileList getFilesInFolder(String folderId, int pageSize, String nextPageToken) throws IOException {
        return getDrive().files().list()
                .setQ("'" + folderId + "' in parents and trashed=false")
                .setPageSize(pageSize)
                .setPageToken(nextPageToken)
                .execute();
    }

    public com.google.api.services.drive.model.File searchFile(String folderId, String filename) throws IOException {
        Drive.Files.List request = getDrive().files().list()
                .setQ("'" + folderId + "' in parents and trashed=false");
        do {
            FileList files = request.execute();
            for (com.google.api.services.drive.model.File f : files.getFiles()) {
                if (f.getName().equals(filename)) {
                    return f;
                }
            }
            request.setPageToken(files.getNextPageToken());
        } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        return null;
    }

    public com.google.api.services.drive.model.File getFolderId(String sharedFolderName, List<String> folderNames) throws IOException {
        String s = sharedFolderName != null ? "sharedWithMe=true and " : "'root' in parents and ";
        Drive.Files.List request = getDrive().files().list()
                .setQ(s + "trashed=false");
        String folderName;
        if (sharedFolderName != null) {
            folderName = sharedFolderName;
        } else {
            folderName = folderNames.get(0);
            folderNames = folderNames.subList(1, folderNames.size());
        }
        FileList list = request.execute();
        for (com.google.api.services.drive.model.File f : list.getFiles()) {
            if (f.getName().equals(folderName)) {
                if (folderNames.size() == 0) {
                    return f;
                }
                return getFolderIdRecursive(f.getId(), folderNames);
            }
        }
        return null;
    }

    private com.google.api.services.drive.model.File getFolderIdRecursive(String folderId, List<String> folderNames) throws IOException {
        Drive.Files.List request = getDrive().files().list()
                .setQ(String.format("'%s' in parents and trashed=false", folderId));
        FileList list = request.execute();
        String folderName = folderNames.get(0);
        for (com.google.api.services.drive.model.File f : list.getFiles()) {
            if (f.getName().equals(folderName)) {
                if (folderNames.size() > 1) {
                    return getFolderIdRecursive(f.getId(), folderNames.subList(1, folderNames.size()));
                }
                return f;
            }
        }
        return null;
    }

    public com.google.api.services.drive.model.File createFolder(String folderName, String parentFolderId) throws IOException {
        com.google.api.services.drive.model.File metaData = new com.google.api.services.drive.model.File()
                .setName(folderName)
                .setMimeType("application/vnd.google-apps.folder")
                .setParents(Collections.singletonList(parentFolderId));
        return getDrive().files().create(metaData).execute();
    }

    public com.google.api.services.drive.model.File uploadFile(String filename, String mimeType, String folderId, InputStream in, long fileSize,
                                                               MediaHttpUploaderProgressListener progressListener) throws IOException {
        InputStreamContent uploadContent = new InputStreamContent(mimeType, new BufferedInputStream(in));
        uploadContent.setLength(fileSize);

        com.google.api.services.drive.model.File metaData = new com.google.api.services.drive.model.File()
                .setName(filename)
                .setMimeType(mimeType)
                .setParents(Collections.singletonList(folderId));

        Drive.Files.Create request = getDrive().files().create(metaData, uploadContent);
        if (progressListener != null) {
            request.getMediaHttpUploader().setProgressListener(progressListener);
        }
        return request.execute();
    }

    public void downloadFile(String fileId, OutputStream out, MediaHttpDownloaderProgressListener progressListener) throws IOException {
        Drive.Files.Get request = getDrive().files().get(fileId);
        if (progressListener != null) {
            request.getMediaHttpDownloader().setProgressListener(progressListener);
        }
        request.executeMediaAndDownloadTo(out);
    }


//    /** @deprecated */
//    public List<com.google.api.services.drive.model.File> readAllFiles() throws IOException {
//        List<com.google.api.services.drive.model.File> result = new ArrayList<>();
//        Drive.Files.List request = getDrive().files().list();
//        do {
//            try {
//                FileList files = request.execute();
//                result.addAll(files.getItems());
//                request.setPageToken(files.getNextPageToken());
//            } catch (IOException e) {
//                logger.error("An error occurred: " + e);
//                request.setPageToken(null);
//            }
//        } while (request.getPageToken() != null && request.getPageToken().length() > 0);
//        return result;
//    }
//
//    /** @deprecated */
//    public List<com.google.api.services.drive.model.File> readFilesInFolder(String folderId) throws IOException {
//        List<com.google.api.services.drive.model.File> result = new ArrayList<>();
//        Drive.Files.List request = getDrive().files().list();
//        do {
//            try {
//                FileList files = request.execute();
//                for (com.google.api.services.drive.model.File f: files.getItems()) {
//                    if (isInFolder(folderId, f)) {
//                        result.add(f);
//                    }
//                }
//                request.setPageToken(files.getNextPageToken());
//            } catch (IOException e) {
//                logger.error("An error occurred: " + e);
//                request.setPageToken(null);
//            }
//        } while (request.getPageToken() != null && request.getPageToken().length() > 0);
//        return result;
//    }
//
//    private static boolean isInFolder(String folderId, com.google.api.services.drive.model.File f) {
//        List<ParentReference> parents = f.getParents();
//        if (parents != null) {
//            for (ParentReference p : parents) {
//                if (p.getId().equals(folderId)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    public String uploadImage(String folderId, String filename, MediaType mediaType, InputStream in) throws IOException {
//        InputStreamContent mediaContent = new InputStreamContent(mediaType.toString(), new BufferedInputStream(in));
//        Drive drive = getDrive();
//        ParentReference p = new ParentReference();
//        p.setId(folderId);
//        com.google.api.services.drive.model.File metaData = new com.google.api.services.drive.model.File();
//        metaData.setParents(Arrays.asList(p));
//        metaData.setTitle(filename);
//        Drive.Files.Insert request = drive.files().insert(metaData, mediaContent);
//        request.getMediaHttpUploader().setProgressListener(new SilentProgressListener());
//        return request.execute().getId();
//    }
//
//    public String createFolder(String parentFolderId, String folderTitle) throws IOException {
//        Drive drive = getDrive();
//        ParentReference p = new ParentReference();
//        p.setId(parentFolderId);
//        com.google.api.services.drive.model.File metaData = new com.google.api.services.drive.model.File();
//        metaData.setParents(Arrays.asList(p));
//        metaData.setTitle(folderTitle);
//        metaData.setMimeType("application/vnd.google-apps.folder");
//        Drive.Files.Insert request = drive.files().insert(metaData);
//        return request.execute().getId();
//    }
//
//    class SilentProgressListener implements MediaHttpUploaderProgressListener {
//        public void progressChanged(MediaHttpUploader uploader) throws IOException {
//            switch (uploader.getUploadState()) {
//                case INITIATION_STARTED:
//                    logger.trace("Initiation has started!");
//                    break;
//                case INITIATION_COMPLETE:
//                    logger.trace("Initiation is complete!");
//                    break;
//                case MEDIA_IN_PROGRESS:
//                    logger.trace("Progress: {}", uploader.getProgress());
//                    break;
//                case MEDIA_COMPLETE:
//                    logger.trace("Upload is complete!");
//            }
//        }
//    }
}
