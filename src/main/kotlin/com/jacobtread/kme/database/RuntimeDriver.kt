package com.jacobtread.kme.database

import com.jacobtread.kme.data.Constants
import com.jacobtread.kme.utils.logging.Logger
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Wrapper for SQL drivers that are loaded at runtime.
 * Attempting to register the drivers without this wrapper
 * causes them not to be registered correctly
 *
 * @param driver The drivers that this is wrapping
 */
class RuntimeDriver(private val driver: Driver) : Driver by driver {

    companion object {

        /**
         * Creates a new MySQL connection. This includes creating the
         * runtime driver for MySQL. The application will forcibily close
         * if the connection could not be made
         *
         * @param host The host address of the MySQL server
         * @param port The port of the MySQL server
         * @param user The username of the MySQL server
         * @param password The password of the MySQL server
         * @param database The database within the MySQL server
         * @return The created connection
         */
        fun createMySQLConnection(
            host: String,
            port: Int,
            user: String,
            password: String,
            database: String,
        ): Connection {
            createMavenDriver(
                "mysql",
                "mysql-connector-java",
                Constants.MYSQL_VERSION,
                "com.mysql.cj.jdbc.Driver"
            )
            try {
                return DriverManager.getConnection("jdbc:mysql://${host}:${port}/${database}", user, password)
            } catch (e: SQLException) {
                Logger.fatal("Unable to connect to SQLite database", e)
            }
        }

        /**
         * Creates a new SQLite connection. This includes creating a runtime driver
         * for SQLite. The application will forcibily close if the connection could
         * not be made
         *
         * @param file The file path to the SQLite database file
         * @return The created connection
         */
        fun createSQLiteConnection(file: String): Connection {
            createMavenDriver(
                "org.xerial",
                "sqlite-jdbc",
                Constants.SQLITE_VERSION,
                "org.sqlite.JDBC"
            )
            val path = Paths.get(file).toAbsolutePath()
            val parentDir = path.parent
            if (Files.notExists(parentDir)) Files.createDirectories(parentDir)
            try {
                return DriverManager.getConnection("jdbc:sqlite:$file")
            } catch (e: SQLException) {
                Logger.fatal("Unable to connect to SQLite database", e)
            }
        }

        /**
         * Creates a runtime driver using a JDBC library hosted
         * on a maven repository.
         *
         * @param group The group id of the package
         * @param name The name of the package
         * @param version The version of the package
         * @param driverClass The driver class name for loading
         */
        private fun createMavenDriver(group: String, name: String, version: String, driverClass: String) {
            val groupEncoded = group.replace('.', '/')
            createRuntimeDriver(
                "https://repo1.maven.org/maven2/$groupEncoded/$name/$version/$name-$version.jar",
                driverClass,
                "$name.jar"
            )
        }

        /**
         * Creates a database driver that is loaded at runtime. It downloads the provided
         * library if not already downloaded and then registers the driver.
         *
         * @param url The url where the driver can be downloaded from
         * @param clazz The java class for the driver
         * @param fileName The file name for the downloaded jar file
         */
        private fun createRuntimeDriver(
            url: String,
            clazz: String,
            fileName: String,
        ) {
            val driversPath = Paths.get("drivers")
            if (Files.notExists(driversPath)) Files.createDirectories(driversPath)
            val path = driversPath.resolve(fileName)
            if (Files.notExists(path)) {
                downloadDriver(url, fileName, path)
            }
            // Load the jar file and create the wrapped runtime driver
            val classLoader = URLClassLoader.newInstance(arrayOf(path.toUri().toURL()))
            classLoader.loadClass(clazz)
            val driver: Driver = Class.forName(clazz, true, classLoader)
                .getDeclaredConstructor()
                .newInstance() as Driver
            DriverManager.registerDriver(RuntimeDriver(driver))
        }

        /**
         * Downloads the JDBC driver jar stored at the provided
         * url and saves it to the provided file path
         *
         * @param url The url where the jar file is hosted
         * @param fileName The file name to save it as
         * @param path The path to save to the file to
         */
        private fun downloadDriver(url: String, fileName: String, path: Path) {
            Logger.info("Database driver not installed. Downloading $fileName...")
            try {
                URL(url).openStream().use { input ->
                    Files.newOutputStream(path, StandardOpenOption.CREATE_NEW).use { output ->
                        input.copyTo(output)
                        Logger.info("Download Completed.")
                    }
                }
            } catch (e: IOException) {
                Logger.fatal("Failed to downlaod database driver", e)
            }
        }
    }
}
