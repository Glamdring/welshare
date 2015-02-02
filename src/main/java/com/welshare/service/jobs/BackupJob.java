package com.welshare.service.jobs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.welshare.service.FileStorageService;
import com.welshare.util.Constants;

@Component
public class BackupJob {

    private static final Logger log = LoggerFactory.getLogger(BackupJob.class);
    private static final int ZIP_BUFFER = 2048;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("dd-MM-yyyy-HH-mm");

    @Resource(name="${filesystem.implementation}")
    private FileStorageService fileStorageService;

    @Inject
    private AbstractGraphDatabase graphDb;

    @Value("${database.url}")
    private String jdbcUrl;

    @Value("${database.username}")
    private String username;

    @Value("${database.password}")
    private String password;

    @Value("${backup.dir}")
    private String finalBackupDir;

    @Value("${database.perform.backup}")
    private boolean performBackup;

    private String baseDir;

    @PostConstruct
    public void init() {
         if (!finalBackupDir.endsWith(Constants.FORWARD_SLASH)) {
             throw new IllegalArgumentException("backup.dir property must end with a slash");
         }

         baseDir = System.getProperty("java.io.tmpdir") + finalBackupDir;
         new File(baseDir).mkdirs();
    }

    @Scheduled(cron = "0 0 0 * * ?") //every midnight
    public void run() {
        if (performBackup) {
            parseAndPerformMySQLBackup(username, password, jdbcUrl);
            performNeo4jBackup();
        }
    }

    private void performNeo4jBackup() {
        InputStream zipInput = null;
        try {
            String backupDirPath = System.getProperty("java.io.tmpdir") + "/neo4jbackup";
            File backupDir = new File(backupDirPath);
            //FileUtils.deleteRecursively(backupDir);
            FileUtils.deleteDirectory(backupDir);
            backupDir.mkdirs();
            FileUtils.copyDirectory(new File(graphDb.getStoreDir()), backupDir);
            String fileName = "graph-backup-" + DATE_TIME_FORMAT.print(new DateTime()) + ".zip";
            File backupFile = new File(System.getProperty("java.io.tmpdir") + fileName);
            ZipFile zip = new ZipFile(backupFile);
            ZipParameters params = new ZipParameters();
            zip.addFolder(backupDir, params);
            zipInput = new FileInputStream(backupFile);
            byte[] bytes = IOUtils.toByteArray(zipInput);
            fileStorageService.storeFile(finalBackupDir + fileName, bytes);
            FileUtils.deleteQuietly(backupFile);
        } catch (Exception e) {
            log.error("Error in graph db bckup", e);
        } finally {
            IOUtils.closeQuietly(zipInput);
        }

    }

    private void parseAndPerformMySQLBackup(String user, String password, String jdbcUrl) {
        String port = "3306";

        Pattern hostPattern = Pattern.compile("//((\\w)+)/");
        Matcher m = hostPattern.matcher(jdbcUrl);
        String host = null;
        if (m.find()) {
            host = m.group(1);
        }

        Pattern dbPattern = Pattern.compile("/((\\w)+)\\?");
        m = dbPattern.matcher(jdbcUrl);
        String db = null;
        if (m.find()) {
            db = m.group(1);
        }

        log.debug(host + ":" + port + ":" + user + ":***:" + db);

        try {
            createBackup(host, port, user, password, db);
        } catch (Exception ex) {
            log.error("Error during backup", ex);
        }

    }

    private void createBackup(String host, String port, String user, String password,
            String db) throws Exception {



        String fileName = "backup-" + DATE_TIME_FORMAT.print(new DateTime());
        String baseFilePath = new File(baseDir + fileName).getAbsolutePath();
        String sqlFilePath = baseFilePath + ".sql";

        String execString = "mysqldump --host=" + host + " --port=" + port + " --user="
                + user + (StringUtils.isNotBlank(password) ? " --password=" + password : "")
                + " --compact --complete-insert --extended-insert --single-transaction "
                + "--skip-comments --skip-triggers --default-character-set=utf8 " + db
                + " --result-file=" + sqlFilePath;

        Process process = Runtime.getRuntime().exec(execString);
        if (log.isDebugEnabled()) {
            log.debug("Output: " + IOUtils.toString(process.getInputStream()));
            log.debug("Error: " + IOUtils.toString(process.getErrorStream()));
        }
        if (process.waitFor() == 0) {

            zipBackup(baseFilePath);
        }

        File zipFile = new File(baseFilePath + ".zip");
        InputStream is = new BufferedInputStream(new FileInputStream(zipFile));
        fileStorageService.storeFile(finalBackupDir + fileName + ".zip", is, zipFile.length());

        // result = "SET FOREIGN_KEY_CHECKS = 0;\\n" + result
        // + "\\nSET FOREIGN_KEY_CHECKS = 1;";
    }

    private void zipBackup(String baseFileName) throws IOException {
        FileOutputStream fos = new FileOutputStream(baseFileName + ".zip");
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

        File entryFile = new File(baseFileName + ".sql");
        FileInputStream fi = new FileInputStream(entryFile);
        InputStream origin = new BufferedInputStream(fi, ZIP_BUFFER);
        ZipEntry entry = new ZipEntry("data.sql");
        zos.putNextEntry(entry);
        int count;
        byte[] data = new byte[ZIP_BUFFER];
        while ((count = origin.read(data, 0, ZIP_BUFFER)) != -1) {
            zos.write(data, 0, count);
        }
        origin.close();
        zos.close();

        entryFile.delete();
    }
}