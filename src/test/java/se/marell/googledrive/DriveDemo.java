/*
 * Created by Daniel Marell 16-03-23 21:30
 */
package se.marell.googledrive;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
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

    /*
    //        java.io.File uploadFile = new java.io.File("garden.jpg");
//        t.createFile("garden.jpg", "image/jpeg", null, new FileInputStream(uploadFile), uploadFile.length());

//        OutputStream out = new FileOutputStream(new java.io.File("garden-downloaded.jpg"));
//        t.downloadFile();

//        t.listAllFiles();

//        t.createFolder("subfolder", null);
//
        File folderMixed = t.getFolderId("marell photo scan", Arrays.asList("mixed"));
        System.out.println("folderMixed: " + folderMixed);
        File subFolder = t.getFolderId(null, Arrays.asList("subfolder"));
        System.out.println("subFolder: " + subFolder);
        File folderScan = t.getFolderId("marell photo scan", new ArrayList<>());
        System.out.println("folderScan: " + folderScan);

//        t.createFolder("subfolder2", subFolder.getId());
//        File subFolder2 = t.getFolderId(null, Arrays.asList("subfolder", "subfolder2"));
//        System.out.println("subFolder2: " + subFolder2);

//        t.createFolder("scanfolder1", folderScan.getId());
        File scanfolder1 = t.getFolderId("marell photo scan", Arrays.asList("scanfolder1"));
        System.out.println("scanfolder1: " + scanfolder1);

//        t.createFolder("scanfolder2", scanfolder1.getId());
        File scanfolder2 = t.getFolderId("marell photo scan", Arrays.asList("scanfolder1", "scanfolder2"));
        System.out.println("scanfolder2: " + scanfolder2);

        File image = t.searchFile(folderMixed.getId(), "Kopia av IMG_20170101_005937.jpg");
        System.out.println("image: " + image);
        if (image != null) {
            t.downloadFile(image.getId(), new FileOutputStream(new java.io.File("image-downloaded.jpg")), null);
        }
//        FileList list = null;
//        do {
//            list = t.getFilesInFolder(folderMixed.getId(), list != null ? list.getNextPageToken() : null);
//            printFileList(list);
//        } while (list.getNextPageToken() != null);

//        t.test();
    }

    private void test() throws IOException {
        Drive.Files.List request = getDriveService().files().list()
                .setQ("'root' in parents and trashed=false");
//                .setQ("sharedWithMe=true and trashed=false");
        FileList list = request.execute();
        printFileList(list);
    }
    */

    @Autowired
    private GoogleDriveService driveService;

    @Ignore
    @Test
    public void listAllFiles() throws IOException {
//        List<File> files = driveService.readAllFiles();
//        logger.info("Number of files: " + files.size());
//        for (File f : files) {
//            logger.info("{}: {}", f.getTitle(), f);
//        }
    }

    @Ignore
    @Test
    public void listFilesInFolder() throws IOException {
//        List<File> files = driveService.readFilesInFolder(FOLDER_ID);
//        logger.info("Number of files: " + files.size());
//        printFileList(files);
    }

    @Ignore
    @Test
    public void createFolder() throws IOException {
        driveService.createFolder(FOLDER_ID, "nyfolder1");
    }

    @Ignore
    @Test
    public void shouldUploadImage() throws IOException {
//        java.io.File mediaFile = new java.io.File("test.jpg");
//        InputStream in = new BufferedInputStream(new FileInputStream(mediaFile));
//        driveService.uploadImage(FOLDER_ID, mediaFile.getName(), MediaType.JPEG, in);
    }

    private static void printFileList(FileList list) {
        System.out.println("filelist:");
        for (File f : list.getFiles()) {
            System.out.println("  " + f);
        }
    }

}
