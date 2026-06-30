package com.projectpilot.app.data.scanner

import com.projectpilot.app.domain.model.DetectionResult
import java.io.File

/**
 * Strategy interface: each implementation knows how to detect ONE project type.
 * Detectors are ordered by priority — first match wins.
 */
interface ProjectDetector {
    val priority: Int          // lower number = higher priority
    fun detect(dir: File): DetectionResult?
}

/** Safe helper: reads up to 256KB of a text file. Returns null on error. */
internal fun File.safeReadText(maxBytes: Long = 256 * 1024): String? = try {
    if (!isFile || length() > maxBytes) null else readText(Charsets.UTF_8)
} catch (_: Throwable) { null }

/** True only if path is inside dir (prevents traversal). */
internal fun File.isInside(dir: File): Boolean = try {
    canonicalPath.startsWith(dir.canonicalPath + File.separator) ||
        canonicalPath == dir.canonicalPath
} catch (_: Throwable) { false }
