/*
 * Created by Daniel Marell 16-03-23 21:30
 */
package se.marell.googledrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.common.net.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.io.File;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class GoogleDriveService {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final List<String> scopes = Arrays.asList(
            DriveScopes.DRIVE
    );

    private static Logger logger = LoggerFactory.getLogger(GoogleDriveService.class);

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
        try {
            GoogleCredential credential = new GoogleCredential.Builder()
                    .setTransport(httpTransport)
                    .setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId(getServiceAccountId())
                    .setServiceAccountScopes(scopes)
                    .setServiceAccountPrivateKeyFromP12File(new File("google-api.p12"))  // notasecret
                    .build();
            return credential;
        } catch (GeneralSecurityException e) {
            throw new IOException("GeneralSecurityException", e);
        }
    }

    public String getServiceAccountId() {
        return environment.getRequiredProperty("google-drive.service-account-id");
    }

    public List<com.google.api.services.drive.model.File> readAllFiles() throws IOException {
        List<com.google.api.services.drive.model.File> result = new ArrayList<>();
        Drive.Files.List request = getDrive().files().list();
        do {
            try {
                FileList files = request.execute();
                result.addAll(files.getItems());
                request.setPageToken(files.getNextPageToken());
            } catch (IOException e) {
                logger.error("An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        return result;
    }

    public List<com.google.api.services.drive.model.File> readFilesInFolder(String folderId) throws IOException {
        List<com.google.api.services.drive.model.File> result = new ArrayList<>();
        Drive.Files.List request = getDrive().files().list();
        do {
            try {
                FileList files = request.execute();
                for (com.google.api.services.drive.model.File f: files.getItems()) {
                    if (isInFolder(folderId, f)) {
                        result.add(f);
                    }
                }
                request.setPageToken(files.getNextPageToken());
            } catch (IOException e) {
                logger.error("An error occurred: " + e);
                request.setPageToken(null);
            }
        } while (request.getPageToken() != null && request.getPageToken().length() > 0);
        return result;
    }

    private static boolean isInFolder(String folderId, com.google.api.services.drive.model.File f) {
        List<ParentReference> parents = f.getParents();
        if (parents != null) {
            for (ParentReference p : parents) {
                if (p.getId().equals(folderId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String uploadImage(String folderId, String filename, MediaType mediaType, InputStream in) throws IOException {
        InputStreamContent mediaContent = new InputStreamContent(mediaType.toString(), new BufferedInputStream(in));
        Drive drive = getDrive();
        ParentReference p = new ParentReference();
        p.setId(folderId);
        com.google.api.services.drive.model.File metaData = new com.google.api.services.drive.model.File();
        metaData.setParents(Arrays.asList(p));
        metaData.setTitle(filename);
        Drive.Files.Insert request = drive.files().insert(metaData, mediaContent);
        request.getMediaHttpUploader().setProgressListener(new SilentProgressListener());
        return request.execute().getId();
    }

    public String createFolder(String parentFolderId, String folderTitle) throws IOException {
        Drive drive = getDrive();
        ParentReference p = new ParentReference();
        p.setId(parentFolderId);
        com.google.api.services.drive.model.File metaData = new com.google.api.services.drive.model.File();
        metaData.setParents(Arrays.asList(p));
        metaData.setTitle(folderTitle);
        metaData.setMimeType("application/vnd.google-apps.folder");
        Drive.Files.Insert request = drive.files().insert(metaData);
        return request.execute().getId();
    }

    class SilentProgressListener implements MediaHttpUploaderProgressListener {
        public void progressChanged(MediaHttpUploader uploader) throws IOException {
            switch (uploader.getUploadState()) {
                case INITIATION_STARTED:
                    logger.trace("Initiation has started!");
                    break;
                case INITIATION_COMPLETE:
                    logger.trace("Initiation is complete!");
                    break;
                case MEDIA_IN_PROGRESS:
                    logger.trace("Progress: {}", uploader.getProgress());
                    break;
                case MEDIA_COMPLETE:
                    logger.trace("Upload is complete!");
            }
        }
    }
}
