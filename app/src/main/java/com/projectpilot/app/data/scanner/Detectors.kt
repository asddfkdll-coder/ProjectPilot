package com.projectpilot.app.data.scanner

import com.projectpilot.app.domain.model.DetectionResult
import com.projectpilot.app.domain.model.ProjectType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

private val LENIENT_JSON = Json { ignoreUnknownKeys = true; isLenient = true }

// -------------------- Node.js --------------------
class NodeProjectDetector : ProjectDetector {
    override val priority = 10
    override fun detect(dir: File): DetectionResult? {
        val pkg = File(dir, "package.json").takeIf { it.exists() } ?: return null
        val raw = pkg.safeReadText() ?: return null
        val obj = runCatching { LENIENT_JSON.parseToJsonElement(raw).jsonObject }.getOrNull()

        val deps = if (obj != null) {
            (obj["dependencies"] as? JsonObject)?.keys.orEmpty() +
            (obj["devDependencies"] as? JsonObject)?.keys.orEmpty()
        } else {
            emptySet()
        }
        val scripts = (obj?.get("scripts") as? JsonObject)

        val framework = when {
            "next" in deps -> "Next.js"
            "@nestjs/core" in deps -> "Nest.js"
            "express" in deps -> "Express"
            "fastify" in deps -> "Fastify"
            "react" in deps && ("react-scripts" in deps || "vite" in deps) -> "React"
            "vite" in deps -> "Vite"
            "vue" in deps -> "Vue"
            "@angular/core" in deps -> "Angular"
            "nuxt" in deps -> "Nuxt"
            "svelte" in deps -> "Svelte"
            else -> "Node.js"
        }
        val runCmd = when {
            scripts?.get("dev") != null -> "npm run dev"
            scripts?.get("start") != null -> "npm start"
            scripts?.get("serve") != null -> "npm run serve"
            else -> "node ${File(dir, "index.js").takeIf { it.exists() }?.name ?: "."}"
        }
        val port = when (framework) {
            "Next.js", "React" -> 3000
            "Vite", "Vue", "Svelte" -> 5173
            "Nuxt" -> 3000
            "Angular" -> 4200
            "Nest.js" -> 3000
            else -> 3000
        }
        return DetectionResult(
            type = ProjectType.NODE,
            framework = framework,
            installCommand = "npm install",
            runCommand = runCmd,
            defaultPort = port,
            dependencies = deps.toList().take(40),
            notes = "Install Node in Termux: pkg install nodejs"
        )
    }
}

// -------------------- Python --------------------
class PythonProjectDetector : ProjectDetector {
    override val priority = 20
    override fun detect(dir: File): DetectionResult? {
        val hasReq = File(dir, "requirements.txt").exists()
        val hasPyproject = File(dir, "pyproject.toml").exists()
        val managePy = File(dir, "manage.py")
        val appPy = File(dir, "app.py")
        val mainPy = File(dir, "main.py")
        if (!hasReq && !hasPyproject && !managePy.exists() && !appPy.exists() && !mainPy.exists())
            return null

        val reqContent = File(dir, "requirements.txt").safeReadText().orEmpty()
        val pyprojectContent = File(dir, "pyproject.toml").safeReadText().orEmpty()
        val allContent = reqContent + "\n" + pyprojectContent

        val deps = allContent.lines()
            .mapNotNull { line ->
                line.substringBefore("==").substringBefore(">").substringBefore("[").trim()
                    .takeIf { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("[") }
            }

        val framework = when {
            managePy.exists() || deps.any { it.equals("django", true) } -> "Django"
            deps.any { it.equals("fastapi", true) } -> "FastAPI"
            deps.any { it.equals("flask", true) } || appPy.exists() -> "Flask"
            deps.any { it.equals("streamlit", true) } -> "Streamlit"
            else -> "Python"
        }
        val runCmd = when (framework) {
            "Django" -> "python manage.py runserver 0.0.0.0:8000"
            "FastAPI" -> "uvicorn main:app --host 0.0.0.0 --port 8000 --reload"
            "Flask" -> "FLASK_APP=app.py flask run --host 0.0.0.0 --port 5000"
            "Streamlit" -> "streamlit run main.py --server.address 0.0.0.0 --server.port 8501"
            else -> "python ${if (mainPy.exists()) "main.py" else "app.py"}"
        }
        val port = when (framework) { 
            "Django" -> 8000 
            "FastAPI" -> 8000 
            "Flask" -> 5000 
            "Streamlit" -> 8501
            else -> 8000 
        }
        return DetectionResult(
            type = ProjectType.PYTHON,
            framework = framework,
            installCommand = "pip install -r requirements.txt",
            runCommand = runCmd,
            defaultPort = port,
            dependencies = deps.take(40),
            notes = "Use a virtualenv: python -m venv .venv && source .venv/bin/activate"
        )
    }
}

// -------------------- PHP --------------------
class PhpProjectDetector : ProjectDetector {
    override val priority = 30
    override fun detect(dir: File): DetectionResult? {
        val composer = File(dir, "composer.json")
        val artisan = File(dir, "artisan")
        val wpConfig = File(dir, "wp-config.php")
        val anyPhp = dir.listFiles()?.any { it.extension.equals("php", true) } == true
        if (!composer.exists() && !artisan.exists() && !wpConfig.exists() && !anyPhp) return null

        val framework = when {
            artisan.exists() -> "Laravel"
            wpConfig.exists() -> "WordPress"
            else -> "PHP"
        }
        val runCmd = when (framework) {
            "Laravel" -> "php artisan serve --host=0.0.0.0 --port=8000"
            "WordPress" -> "php -S 0.0.0.0:8080 -t ."
            else -> "php -S 0.0.0.0:8080 -t ."
        }
        return DetectionResult(
            type = ProjectType.PHP,
            framework = framework,
            installCommand = if (composer.exists()) "composer install" else null,
            runCommand = runCmd,
            defaultPort = if (framework == "Laravel") 8000 else 8080,
            notes = "Termux: pkg install php php-mysql composer"
        )
    }
}

// -------------------- Java (Maven / Gradle / Spring) --------------------
class JavaProjectDetector : ProjectDetector {
    override val priority = 40
    override fun detect(dir: File): DetectionResult? {
        val pom = File(dir, "pom.xml")
        val gradleKts = File(dir, "build.gradle.kts")
        val gradle = File(dir, "build.gradle")
        if (!pom.exists() && !gradleKts.exists() && !gradle.exists()) return null

        val content = (pom.safeReadText().orEmpty() + gradleKts.safeReadText().orEmpty() + gradle.safeReadText().orEmpty())
        val isSpring = content.contains("spring-boot", ignoreCase = true)
        val framework = if (isSpring) "Spring Boot" else "Java"

        val runCmd = when {
            isSpring && File(dir, "mvnw").exists() -> "./mvnw spring-boot:run"
            isSpring && File(dir, "gradlew").exists() -> "./gradlew bootRun"
            pom.exists() -> "mvn -q exec:java"
            else -> "./gradlew run"
        }
        return DetectionResult(
            type = ProjectType.JAVA,
            framework = framework,
            installCommand = if (pom.exists()) "mvn -q -DskipTests package" else "./gradlew build -x test",
            runCommand = runCmd,
            defaultPort = if (isSpring) 8080 else null,
            notes = "Termux: pkg install openjdk-17 maven"
        )
    }
}

// -------------------- Go --------------------
class GoProjectDetector : ProjectDetector {
    override val priority = 50
    override fun detect(dir: File): DetectionResult? {
        if (!File(dir, "go.mod").exists()) return null
        val main = dir.walkTopDown().maxDepth(5).firstOrNull { it.name == "main.go" } // Increased depth for better detection
        return DetectionResult(
            type = ProjectType.GO,
            framework = "Go",
            installCommand = "go mod download",
            runCommand = main?.let { "go run ${it.relativeTo(dir).path}" } ?: "go run .",
            defaultPort = 8080,
            notes = "Termux: pkg install golang"
        )
    }
}

// -------------------- Rust --------------------
class RustProjectDetector : ProjectDetector {
    override val priority = 60
    override fun detect(dir: File): DetectionResult? {
        if (!File(dir, "Cargo.toml").exists()) return null
        return DetectionResult(
            type = ProjectType.RUST,
            framework = "Cargo",
            installCommand = "cargo build",
            runCommand = "cargo run",
            defaultPort = 8000,
            notes = "Termux: pkg install rust"
        )
    }
}

// -------------------- Docker --------------------
class DockerProjectDetector : ProjectDetector {
    override val priority = 5   // higher priority — checked early
    override fun detect(dir: File): DetectionResult? {
        val df = File(dir, "Dockerfile")
        val compose = listOf("docker-compose.yml", "docker-compose.yaml", "compose.yml")
            .map { File(dir, it) }.firstOrNull { it.exists() }
        if (!df.exists() && compose == null) return null

        val run = if (compose != null) "docker compose up" else "docker build -t app . && docker run --rm -p 8080:8080 app"
        return DetectionResult(
            type = ProjectType.DOCKER,
            framework = if (compose != null) "Docker Compose" else "Docker",
            installCommand = null,
            runCommand = run,
            defaultPort = 8080,
            notes = "Docker on Termux requires proot-distro or root. Recommended: proot-distro install debian"
        )
    }
}

// -------------------- .NET --------------------
class DotNetProjectDetector : ProjectDetector {
    override val priority = 70
    override fun detect(dir: File): DetectionResult? {
        val csproj = dir.listFiles()?.firstOrNull { it.extension.equals("csproj", true) }
        val sln = dir.listFiles()?.firstOrNull { it.extension.equals("sln", true) }
        if (csproj == null && sln == null) return null
        return DetectionResult(
            type = ProjectType.DOTNET,
            framework = ".NET",
            installCommand = "dotnet restore",
            runCommand = "dotnet run",
            defaultPort = 5000,
            notes = "Install dotnet SDK manually in Termux (see Microsoft ARM64 instructions)"
        )
    }
}

// -------------------- Ruby --------------------
class RubyProjectDetector : ProjectDetector {
    override val priority = 80
    override fun detect(dir: File): DetectionResult? {
        if (!File(dir, "Gemfile").exists() && !File(dir, "config.ru").exists()) return null
        val isRails = File(dir, "bin/rails").exists() || File(dir, "config/application.rb").exists()
        return DetectionResult(
            type = ProjectType.RUBY,
            framework = if (isRails) "Rails" else "Ruby",
            installCommand = "bundle install",
            runCommand = if (isRails) "bin/rails server -b 0.0.0.0 -p 3000" else "rackup -o 0.0.0.0 -p 9292",
            defaultPort = if (isRails) 3000 else 9292,
            notes = "Termux: pkg install ruby"
        )
    }
}

// -------------------- Static HTML --------------------
class StaticProjectDetector : ProjectDetector {
    override val priority = 999   // last resort
    override fun detect(dir: File): DetectionResult? {
        if (!File(dir, "index.html").exists()) return null
        return DetectionResult(
            type = ProjectType.STATIC_HTML,
            framework = "Static",
            installCommand = null,
            runCommand = "python -m http.server 8000",
            defaultPort = 8000,
            notes = "Alternative: npx serve -l 8000"
        )
    }
}
