## Google Drive Interface

Java interface for some simple file operations with Google Drive. It was made for uploading images from a server 
to Google Photos.

## Release notes
* Version 1.0.0 - 2016-03-22
  * First release

### Maven usage

```
    <repositories>
      <repository>
        <id>marell</id>
        <url>http://marell.se/artifactory/libs-release</url>
      </repository>
    </repositories>
...
    <dependency>
        <groupId>se.marell.google-drive</groupId>
        <artifactId>google-drive</artifactId>
        <version>1.0.0</version>
    </dependency>
```

### Setup

Create an application name and a service-account-id using Google Developer Console.

Put in application.yaml:

google-drive.application-name: ...
google-drive.service-account-id: ...

### Code example

```
   @Autowired
   private GoogleDriveService driveService;
   ...
   java.io.File imageFile = new java.io.File("image.jpg");
   InputStream in = new BufferedInputStream(new FileInputStream(imageFile));
   driveService.uploadImage(FOLDER_ID, imageFile.getName(), MediaType.JPEG, in);
```

See DriveDemo.java for more examples.
