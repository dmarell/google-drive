/*
 * Created by Daniel Marell 16-03-23 21:30
 */
package se.marell.googledrive;

import com.google.api.services.drive.model.File;
import com.google.common.net.MediaType;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(GoogleDriveConfig.class)
@TestPropertySource(properties = {
        // Create application name and service account id with Google Developer Console.
        // Also create a key file and copy to the root directory and call it "google-api.p12"
        "google-drive.application-name: myapp",
        "google-drive.service-account-id: myserviceaccount"
})
public class DriveDemo {
    private static Logger logger = LoggerFactory.getLogger(DriveDemo.class);

    // Folder id is the last part of the url in the browser when viewing the folder in Google Drive
    private static String FOLDER_ID = "myfolderid";

    @Autowired
    private GoogleDriveService driveService;

    @Ignore
    @Test
    public void listAllFiles() throws IOException {
        List<File> files = driveService.readAllFiles();
        logger.info("Number of files: " + files.size());
        for (File f : files) {
            logger.info("{}: {}", f.getTitle(), f);
        }
    }

    @Ignore
    @Test
    public void listFilesInFolder() throws IOException {
        List<File> files = driveService.readFilesInFolder(FOLDER_ID);
        logger.info("Number of files: " + files.size());
        for (File f : files) {
            logger.info("{}: {}", f.getTitle(), f);
        }
    }

    @Ignore
    @Test
    public void createFolder() throws IOException {
        driveService.createFolder(FOLDER_ID, "nyfolder1");
    }

    @Ignore
    @Test
    public void shouldUploadImage() throws IOException {
        java.io.File mediaFile = new java.io.File("test.jpg");
        InputStream in = new BufferedInputStream(new FileInputStream(mediaFile));
        driveService.uploadImage(FOLDER_ID, mediaFile.getName(), MediaType.JPEG, in);
    }
}
