package org.conch.db;

import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;
import org.conch.Conch;
import org.h2.tools.Shell;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DbBackupTask extends Task{
    private static String defaultPath = Conch.getStringProperty("sharder.db.backup.path");


    @Override
    public boolean canBePaused() {
        return true;
    }

    @Override
    public boolean canBeStopped() {
        return true;
    }

    @Override
    public boolean supportsStatusTracking() {
        return true;
    }

    @Override
    public boolean supportsCompletenessTracking() {
        return true;
    }

    @Override
    public void execute(TaskExecutionContext taskExecutionContext) throws RuntimeException {
        execute(null, null);
    }

    public static String execute(String path, String fileName) {
        String pathStr;
        String fileNameStr;
        Conch.getBlockchain().updateLock();
        Conch.getBlockchain().readLock();
        try {
            pathStr = path==null?defaultPath:path;
            if(fileName == null) {
                LocalDateTime currentDateTime = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmSS");
                String now = currentDateTime.format(dateTimeFormatter);
                int height = Conch.getBlockchain().getHeight();
                fileNameStr = "sharder_db_backup_"+now+"_"+ height +".zip";
            }else {
                fileNameStr = fileName + (fileName.contains(".zip")?"":".zip");
            }
            File file = new File(pathStr + (pathStr.endsWith(File.separator) ? "" : File.separator) +  fileNameStr);
            String sql = "SCRIPT TO '" + pathStr + (pathStr.endsWith(File.separator) ? "" : File.separator) +  fileNameStr +"' COMPRESSION ZIP";
            Shell shell = new Shell();
            shell.runTool(Db.db.getConnection(), "-sql", sql);
            return file.getAbsolutePath();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            Conch.getBlockchain().updateUnlock();
            Conch.getBlockchain().readUnlock();
        }

    }
}
