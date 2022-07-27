package com.jacobtread.kme.database

import com.jacobtread.kme.utils.logging.Logger
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.sql.Driver
import java.sql.DriverManager

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
         * Creates a database driver that is loaded at runtime. It downloads the provided
         * library if not already downloaded and then registers the driver.
         *
         * @param url The url where the driver can be downloaded from
         * @param clazz The java class for the driver
         * @param fileName The file name for the downloaded jar file
         */
        fun createRuntimeDriver(
            url: String,
            clazz: String,
            fileName: String,
        ) {
            val driversPath = Paths.get("drivers")
            if (Files.notExists(driversPath)) Files.createDirectories(driversPath)
            val path = driversPath.resolve(fileName)
            if (Files.notExists(path)) {
                Logger.info("Database driver not installed. Downloading $fileName...")
                try {
                    URL(url).openStream().use { input ->
                        Files.newOutputStream(path, StandardOpenOption.CREATE_NEW).use { output ->
                            input.copyTo(output)
                            Logger.info("Download Completed.")
                        }
                    }
                } catch (e: Exception) {
                    Logger.fatal("Failed to downlaod database driver", e)
                }
            }
            // Load the jar file and create the wrapped runtime driver
            val classLoader = URLClassLoader.newInstance(arrayOf(path.toUri().toURL()))
            classLoader.loadClass(clazz)
            val driver: Driver = Class.forName(clazz, true, classLoader)
                .getDeclaredConstructor()
                .newInstance() as Driver
            DriverManager.registerDriver(RuntimeDriver(driver))
        }
    }

}