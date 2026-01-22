package app.ultradev.hytalegradle

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import org.apache.tools.ant.taskdefs.condition.Os
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

object HytaleInstallation {
    fun getHytaleBasePath(home: Path, patchline: String): Path {
        return home.resolve("install/$patchline/package/game/latest")
    }

    fun detectHytaleHome(): Path {
        val basePath = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            val basePath = Path(
                Advapi32Util.registryGetStringValue(
                    WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Hypixel Studios\\Hytale", "GameInstallPath"
                )
            )
            if (!basePath.exists()) {
                error("Could not find Hytale installation.")
            }
            basePath
        } else if (Os.isFamily(Os.FAMILY_MAC)) {
            val basePath = Path("${System.getProperty("user.home")}/Library/Application Support/Hytale")
            if (!basePath.exists()) {
                error("Could not find Hytale installation.")
            }
            basePath
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            val basePath = Path("${System.getProperty("user.home")}/.var/app/com.hypixel.HytaleLauncher/data/Hytale")
            if (!basePath.exists()) {
                error("Could not find Hytale installation.")
            }
            basePath
        } else {
            error("Unsupported operating system")
        }

        return basePath
    }
}