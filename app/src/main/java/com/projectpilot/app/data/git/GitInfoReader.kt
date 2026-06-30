package com.projectpilot.app.data.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure-Kotlin Git metadata reader.
 * Reads .git/HEAD, .git/refs/heads/*, .git/logs/HEAD, and runs a lightweight
 * diff via file timestamps (no JGit dependency — keeps the APK small).
 *
 * Safe by design: only READS files inside the project's .git directory.
 */
@Singleton
class GitInfoReader @Inject constructor() {

    data class GitInfo(
        val isGitRepo: Boolean,
        val branch: String? = null,
        val lastCommitShort: String? = null,
        val lastCommitMessage: String? = null,
        val lastCommitAuthor: String? = null,
        val lastCommitEpoch: Long? = null,
        val changedFilesApprox: Int = 0,
        val remoteUrl: String? = null
    )

    suspend fun read(projectDir: File): GitInfo = withContext(Dispatchers.IO) {
        val gitDir = File(projectDir, ".git")
        if (!gitDir.isDirectory) return@withContext GitInfo(isGitRepo = false)

        // 1) HEAD → branch
        val headFile = File(gitDir, "HEAD")
        val headLine = runCatching { headFile.readText().trim() }.getOrNull()
        val branch = when {
            headLine == null -> null
            headLine.startsWith("ref: refs/heads/") ->
                headLine.removePrefix("ref: refs/heads/").trim()
            else -> "detached@${headLine.take(7)}"
        }

        // 2) Last commit SHA + message via packed-refs OR loose ref OR log
        val sha = resolveHeadSha(gitDir, headLine)
        val log = parseLatestLogEntry(File(gitDir, "logs/HEAD"))

        // 3) remote.origin.url from config
        val remote = parseRemoteUrl(File(gitDir, "config"))

        // 4) cheap "changed files" estimate — count files in working tree newer
        //    than .git/index. NOT perfect, but avoids shelling out.
        val indexMtime = File(gitDir, "index").lastModified().takeIf { it > 0 }
        val changed = if (indexMtime != null) {
            countNewerFiles(projectDir, indexMtime, limit = 500)
        } else 0

        GitInfo(
            isGitRepo = true,
            branch = branch,
            lastCommitShort = sha?.take(7),
            lastCommitMessage = log?.message,
            lastCommitAuthor = log?.author,
            lastCommitEpoch = log?.epoch,
            changedFilesApprox = changed,
            remoteUrl = remote
        )
    }

    private fun resolveHeadSha(gitDir: File, headLine: String?): String? {
        if (headLine == null) return null
        if (!headLine.startsWith("ref:")) return headLine
        val refPath = headLine.removePrefix("ref:").trim()
        val refFile = File(gitDir, refPath)
        if (refFile.isFile) return runCatching { refFile.readText().trim() }.getOrNull()
        // fallback: packed-refs
        val packed = File(gitDir, "packed-refs")
        if (!packed.isFile) return null
        return runCatching {
            packed.useLines { lines ->
                lines.firstOrNull { it.endsWith(" $refPath") }?.substringBefore(" ")
            }
        }.getOrNull()
    }

    private data class LogEntry(val author: String?, val epoch: Long?, val message: String?)

    private fun parseLatestLogEntry(logFile: File): LogEntry? {
        if (!logFile.isFile) return null
        // each line: "<old> <new> <name> <email> <epoch> <tz>\t<msg>"
        val last = runCatching { logFile.readLines().lastOrNull { it.isNotBlank() } }.getOrNull()
            ?: return null
        val tabSplit = last.split("\t", limit = 2)
        val header = tabSplit[0]
        val msg = tabSplit.getOrNull(1)
        val parts = header.split(" ")
        // header structure: oldSha newSha "Name <email>" epoch tz
        // Reconstruct name + email reliably:
        val emailIdx = parts.indexOfFirst { it.startsWith("<") && it.endsWith(">") }
        val author = if (emailIdx >= 3) parts.subList(2, emailIdx + 1).joinToString(" ") else null
        val epoch = parts.getOrNull(emailIdx + 1)?.toLongOrNull()?.times(1000)
        return LogEntry(author, epoch, msg)
    }

    private fun parseRemoteUrl(configFile: File): String? {
        if (!configFile.isFile) return null
        var inOrigin = false
        return runCatching {
            configFile.useLines { lines ->
                for (raw in lines) {
                    val line = raw.trim()
                    if (line.startsWith("[remote ")) inOrigin = line.contains("\"origin\"")
                    else if (inOrigin && line.startsWith("url"))
                        return@useLines line.substringAfter("=").trim()
                }
                null
            }
        }.getOrNull()
    }

    private fun countNewerFiles(root: File, sinceMs: Long, limit: Int): Int {
        var n = 0
        val stack = ArrayDeque<File>().apply { add(root) }
        val skip = setOf(".git", "node_modules", ".gradle", "build", "dist",
                        "target", ".venv", "venv", "__pycache__", ".next")
        while (stack.isNotEmpty() && n < limit) {
            val cur = stack.removeLast()
            val children = cur.listFiles() ?: continue
            for (c in children) {
                if (c.isDirectory) {
                    if (c.name !in skip && !c.name.startsWith(".")) stack.add(c)
                } else if (c.lastModified() > sinceMs) {
                    n++
                    if (n >= limit) break
                }
            }
        }
        return n
    }
}
