package no.ntnu.okse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

public class OpenfireXMPPServerDBManager {

  /**
   * Deletes the local database for the Openfire XMPP server.
   */
  public static void deleteDatabase() {
    if (OpenfireXMPPServerFactory.serverRunning()) {
      throw new IllegalStateException("Cannot delete database with server running");
    }
    File dbFile = new File(getDatabaseLocation());

    if (dbFile.exists()) {
      deleteFolder(dbFile);
    }
  }

  /**
   * Deletes a folder by recursively deleting the files it contains
   *
   * @param folder The folder to delete
   */
  private static void deleteFolder(File folder) {
    File[] subfiles = folder.listFiles();
    if (subfiles != null) {
      Arrays.stream(subfiles).forEach(OpenfireXMPPServerDBManager::deleteFolder);
    }
    folder.delete();
  }

  /**
   * Finds the location of the Openfire database
   *
   * @return The path to the Openfire database directory
   */
  private static String getDatabaseLocation() {
    return getConfigHomePath() + File.separator + "embedded-db/";
  }

  /**
   * Copies a copy of the config file to a file with the name required by Openfire. Although ugly it
   * is required due to Openfire changing the config file after creating the database. Not allowing
   * easy creation and deletion of databases
   *
   * @throws IOException If the config file cannot be moved
   */
  public static void setupConfigFile() throws IOException {
    Files.copy(
        new File(getConfigHomePath() + File.separator + "conf" + File.separator + "config.xml")
            .toPath(),
        new File(getConfigHomePath() + File.separator + "conf" + File.separator + "openfire.xml")
            .toPath(), StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * Finds the location of the database copy for when running in test mode
   *
   * @return The path to the database copy
   */
  private static String getDatabaseCopyLocation() {
    return getConfigHomePath() + File.separator + "backup/";
  }

  /**
   * Finds the config home path for the Openfire server
   *
   * @return The home path for the Openfire server
   */
  private static String getConfigHomePath() {
    InputStream settingsXML = null;
    String homePath = null;
    try {
      settingsXML = OpenfireXMPPServerDBManager.class
          .getResourceAsStream("/openfire_init.xml");
      SAXReader reader = new SAXReader();
      Document doc = reader.read(settingsXML);
      homePath = doc.getRootElement().getText();
    } catch (DocumentException de) {
      de.printStackTrace();
    } finally {
      if (settingsXML != null) {
        try {
          settingsXML.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    if (homePath == null) {
      throw new IllegalStateException("Openfire home directory not configured");
    }

    return homePath;
  }

  /**
   * Sets the database to test mode by moving the current database
   *
   * @throws IOException If moving the current database is not possible
   */
  public static void setupTestDB() throws IOException {
    if (OpenfireXMPPServerFactory.serverRunning()) {
      throw new IllegalStateException("Cannot setup test database with server running");
    }

    File dbFile = new File(getDatabaseLocation());
    File dbCopyFile = new File(getDatabaseCopyLocation());

    if (dbCopyFile.exists()) {
      throw new IllegalStateException("Test DB already setup");
    }

    if (dbFile.exists()) {
      Files.move(dbFile.toPath(), dbCopyFile.toPath());
    }
  }

  /**
   * Sets the database to normal mode by deleting the test database and moving the real database
   * back
   *
   * @throws IOException If moving the real database is not possible
   */
  public static void teardownTestDB() throws IOException {
    if (OpenfireXMPPServerFactory.serverRunning()) {
      throw new IllegalStateException("Cannot teardown test database with server running");
    }

    File dbFile = new File(getDatabaseLocation());
    File dbCopyFile = new File(getDatabaseCopyLocation());

    if (dbFile.exists()) {
      deleteFolder(dbFile);
    }

    if (dbCopyFile.exists()) {
      Files.move(dbCopyFile.toPath(), dbFile.toPath());
    }
  }

}
