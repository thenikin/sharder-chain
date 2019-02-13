package org.conch.tools;

import org.apache.commons.io.FileUtils;
import org.conch.util.FileUtil;
import org.conch.util.Https;
import org.conch.util.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-29
 */
public class ClientUpgradeTool {
    private static final String UPGRADE_SERVER = "https://resource.sharder.io";

    public static Thread fetchUpgradePackageThread(String version) {
        String url = UPGRADE_SERVER + "/sharder-hub/release/cos-hub-" + version +".zip";
        File projectPath = new File("temp/");
        File archive = new File(projectPath, "cos-hub-" + version + ".zip");
        Thread fetchUpgradePackageThread = new Thread(
                () -> {
                    try {
                        if (!archive.exists()) {
                            Logger.logDebugMessage("[UPGRADE CLIENT] Get upgrade package:" + archive.getName());
                            FileUtils.copyURLToFile(new URL(url), archive);
                        }
                        FileUtil.unzipAndReplace(archive, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }

        );
        fetchUpgradePackageThread.setDaemon(true);
        fetchUpgradePackageThread.start();
        return fetchUpgradePackageThread;
    }

    public static void fetchUpgradePackage(String version) throws IOException {
        String url = UPGRADE_SERVER + "/sharder-hub/release/cos-hub-" + version +".zip";
        File projectPath = new File("temp/");
        File archive = new File(projectPath, "cos-hub-" + version + ".zip");
        if (!archive.exists()) {
            Logger.logDebugMessage("[UPGRADE CLIENT] Get upgrade package:" + archive.getName());
            FileUtils.copyURLToFile(new URL(url), archive);
        }
        FileUtil.unzipAndReplace(archive, true);
        try {
            // TODO[Enh] support windows command
            Runtime.getRuntime().exec("chmod -R +x ~/sharder-hub/");
        } catch (Exception e) {
            Logger.logErrorMessage("Failed to run after start script: chmod -R +x ~/sharder-hub/" , e);
        }
    }

    public static String fetchLastHubVersion() throws IOException {
        String url = UPGRADE_SERVER + "/sharder-hub/release/lastest-version";
        return Https.httpRequest(url,"GET", null);
    }
}