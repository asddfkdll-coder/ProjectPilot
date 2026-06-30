package com.projectpilot.app.termux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge to Termux via the documented RUN_COMMAND Intent API.
 *
 * REQUIREMENTS for this to work on the device:
 *   1) Termux installed from F-Droid (NOT Google Play — that build is outdated).
 *   2) In Termux: edit ~/.termux/termux.properties and add:
 *          allow-external-apps = true
 *      then run: termux-reload-settings
 *   3) Our manifest declares <uses-permission android:name="com.termux.permission.RUN_COMMAND" />.
 *
 * Security notes:
 *   - We send commands as ARGUMENTS array, NOT joined into a shell string, so user-supplied
 *     project paths can't inject shell metacharacters.
 *   - We still validate the workdir to prevent obviously malicious values.
 */
@Singleton
class TermuxCommandRunner @Inject constructor(
    @ApplicationContext private val ctx: Context
) {

    sealed class Result {
        data object Ok : Result()
        data object TermuxNotInstalled : Result()
        data class Failed(val message: String) : Result()
    }

    fun isTermuxInstalled(): Boolean = try {
        ctx.packageManager.getPackageInfo(TERMUX_PKG, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    /**
     * Runs a shell command inside Termux.
     *
     * @param command  the executable. Most users will pass "/data/data/com.termux/files/usr/bin/bash"
     *                 and put the actual command in [args] as ["-lc", "npm start"]. We default to that
     *                 if [command] is null so callers can simply pass a shell line.
     * @param shellLine convenience: if [command] is null, this string is run via bash -lc.
     * @param workdir  absolute path inside the Termux filesystem (must NOT be null/empty for safety).
     * @param background  true = no UI session opens; false = opens a Termux session and shows output.
     */
    fun run(
        shellLine: String? = null,
        command: String? = null,
        args: Array<String>? = null,
        workdir: String,
        background: Boolean = false,
        sessionLabel: String? = null,
        onResult: ((Int, Bundle?) -> Unit)? = null
    ): Result {
        if (!isTermuxInstalled()) return Result.TermuxNotInstalled
        val safeWorkdir = sanitizeWorkdir(workdir)
            ?: return Result.Failed("Invalid workdir")

        val (execPath, execArgs) = when {
            command != null -> command to (args ?: emptyArray())
            shellLine != null -> {
                // If background is true, we wrap the command to echo the PID to stdout.
                // Termux RUN_COMMAND captures stdout/stderr and returns it via ResultReceiver.
                val finalCmd = if (background) {
                    // Start process in background, echo PID immediately, then wait for it.
                    // We use { ... } & to ensure we get the correct background PID.
                    "{ $shellLine; } & PID=$!; echo \"PP_PID:$PID\"; wait $PID"
                } else {
                    shellLine
                }
                BASH to arrayOf("-lc", finalCmd)
            }
            else -> return Result.Failed("Either shellLine or command must be provided")
        }

        val intent = Intent().apply {
            // Targeted component — required by Termux for explicit external invocation.
            component = ComponentName(TERMUX_PKG, TERMUX_RUN_SERVICE)
            action = ACTION_RUN_COMMAND
            putExtra(EXTRA_PATH, execPath)
            putExtra(EXTRA_ARGUMENTS, execArgs)
            putExtra(EXTRA_WORKDIR, safeWorkdir)
            putExtra(EXTRA_BACKGROUND, background)
            putExtra(EXTRA_SESSION_ACTION, if (background) "1" else "0") // 0 = Keep session, 1 = Finish session
            if (sessionLabel != null) putExtra(EXTRA_COMMAND_LABEL, sessionLabel)
            
            if (onResult != null) {
                val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                        onResult(resultCode, resultData)
                    }
                }
                putExtra(EXTRA_RESULT_RECEIVER, receiver)
            }
        }

        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { ctx.startForegroundService(intent) } catch (e: Exception) { return Result.Failed(e.message ?: "Service start failed") }
            } else {
                try { ctx.startService(intent) } catch (e: Exception) { return Result.Failed(e.message ?: "Service start failed") }
            }
            Result.Ok
        }.getOrElse { Result.Failed(it.message ?: "Failed to start Termux service") }
    }

    /** Convenience: run a project's install command. */
    fun runInstall(projectPath: String, installCommand: String): Result =
        run(shellLine = installCommand, workdir = projectPath, background = false,
            sessionLabel = "Install")

    /** Convenience: start a project server (opens visible Termux session so logs are seen). */
    fun runServer(projectPath: String, runCommand: String, sessionLabel: String = "Server"): Result =
        run(shellLine = runCommand, workdir = projectPath, background = false,
            sessionLabel = sessionLabel)

    /** Convenience: start a project server in the background (no UI session). */
    fun runBackgroundServer(projectPath: String, runCommand: String, sessionLabel: String = "Server"): Result =
        run(shellLine = runCommand, workdir = projectPath, background = true,
            sessionLabel = sessionLabel)

    /** Convenience: stop a process by PID inside Termux. */
    fun killPid(pid: Int): Result =
        run(shellLine = "kill -TERM $pid || kill -KILL $pid", workdir = HOME, background = true,
            sessionLabel = "Kill $pid")

    /** Very conservative path sanitizer (defense-in-depth). */
    private fun sanitizeWorkdir(input: String): String? {
        if (input.isBlank()) return null
        if (input.length > 1024) return null
        // Disallow newline / null / shell metacharacters that could lead to injection.
        // We allow spaces, dashes, and underscores which are common in project paths.
        val forbidden = setOf('\n', '\u0000', ';', '`', '$', '|', '&', '>', '<', '(', ')', '{', '}', '[', ']', '*', '?', '!', '\\', '"', '\'')
        if (input.any { it in forbidden }) return null
        return input
    }

    companion object {
        const val TERMUX_PKG = "com.termux"
        const val TERMUX_RUN_SERVICE = "com.termux.app.RunCommandService"
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

        const val EXTRA_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
        const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"
        const val EXTRA_RESULT_RECEIVER = "com.termux.RUN_COMMAND_RESULT_RECEIVER"

        const val BASH = "/data/data/com.termux/files/usr/bin/bash"
        const val HOME = "/data/data/com.termux/files/home"
    }
}
