package com.projectpilot.app.data.scanner

import com.projectpilot.app.domain.model.DetectionResult
import com.projectpilot.app.domain.model.ProjectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ScanItem(val dir: File, val result: DetectionResult)

@Singleton
class ProjectScanner @Inject constructor() {

    private val detectors: List<ProjectDetector> = listOf(
        DockerProjectDetector(),
        NodeProjectDetector(),
        PythonProjectDetector(),
        PhpProjectDetector(),
        JavaProjectDetector(),
        GoProjectDetector(),
        RustProjectDetector(),
        DotNetProjectDetector(),
        RubyProjectDetector(),
        StaticProjectDetector()
    ).sortedBy { it.priority }

    /** Detect a single directory. Returns UNKNOWN if no detector matches. */
    suspend fun detectSingle(dir: File): DetectionResult = withContext(Dispatchers.IO) {
        if (!dir.isDirectory || !dir.canRead()) return@withContext unknown()
        detectors.firstNotNullOfOrNull { runCatching { it.detect(dir) }.getOrNull() }
            ?: unknown()
    }

    /**
     * Scan a parent directory up to [maxDepth] looking for project roots.
     * A directory is a "project root" if any detector matches; subdirectories of a
     * detected root are not scanned further (depth-pruned).
     */
    suspend fun scanTree(
        root: File,
        maxDepth: Int = 4,
        skipHidden: Boolean = true,
        skipNames: Set<String> = DEFAULT_SKIP
    ): List<ScanItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ScanItem>()
        if (!root.isDirectory || !root.canRead()) return@withContext result
        fun walk(dir: File, depth: Int) {
            if (depth > maxDepth) return
            val det = detectors.firstNotNullOfOrNull { runCatching { it.detect(dir) }.getOrNull() }
            if (det != null && det.type != ProjectType.UNKNOWN) {
                result += ScanItem(dir, det)
                return  // do not descend into detected project
            }
            val children = dir.listFiles() ?: return
            for (c in children) {
                if (!c.isDirectory) continue
                if (skipHidden && c.name.startsWith(".")) continue
                if (c.name in skipNames) continue
                if (!c.isInside(root)) continue
                walk(c, depth + 1)
            }
        }
        walk(root, 0)
        result
    }

    private fun unknown() = DetectionResult(ProjectType.UNKNOWN, notes = "No known markers found")

    companion object {
        val DEFAULT_SKIP = setOf(
            "node_modules", "vendor", ".git", ".gradle", ".idea", "build",
            "dist", "out", "target", "__pycache__", ".venv", "venv",
            ".next", ".nuxt", ".cache", "coverage", "tmp"
        )
    }
}
