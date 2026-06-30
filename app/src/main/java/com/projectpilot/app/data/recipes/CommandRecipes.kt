package com.projectpilot.app.data.recipes

import com.projectpilot.app.domain.model.ProjectType

/**
 * Curated Termux command "recipes" per project type.
 * Used in the Recipes screen to one-tap-execute well-known operations.
 *
 * Every recipe is a small, self-contained shell line — safe to be wrapped with bash -lc.
 */
data class Recipe(
    val title: String,
    val description: String,
    val command: String,
    val needsProjectDir: Boolean = true
)

object CommandRecipes {

    fun forType(type: ProjectType): List<Recipe> = when (type) {
        ProjectType.NODE -> listOf(
            Recipe("Install Node LTS", "Installs Node.js + npm in Termux",
                "pkg install -y nodejs", needsProjectDir = false),
            Recipe("Install deps", "Runs npm install", "npm install"),
            Recipe("Clean install", "Removes node_modules then reinstalls",
                "rm -rf node_modules package-lock.json && npm install"),
            Recipe("Audit & fix", "Security audit + fix", "npm audit fix"),
            Recipe("Run dev", "Starts development server", "npm run dev || npm start"),
            Recipe("Build", "Production build", "npm run build"),
            Recipe("Show outdated", "Lists outdated packages", "npm outdated || true")
        )
        ProjectType.PYTHON -> listOf(
            Recipe("Install Python", "Installs python in Termux",
                "pkg install -y python", needsProjectDir = false),
            Recipe("Create venv", "Creates virtual environment .venv",
                "python -m venv .venv"),
            Recipe("Install reqs", "pip install requirements", 
                "pip install -r requirements.txt"),
            Recipe("Django migrate", "Runs Django migrations",
                "python manage.py migrate"),
            Recipe("Django run", "Starts Django dev server",
                "python manage.py runserver 0.0.0.0:8000"),
            Recipe("FastAPI run", "uvicorn dev server",
                "uvicorn main:app --host 0.0.0.0 --port 8000 --reload"),
            Recipe("Flask run", "Starts Flask",
                "FLASK_APP=app.py flask run --host 0.0.0.0 --port 5000"),
            Recipe("Freeze deps", "Updates requirements.txt",
                "pip freeze > requirements.txt")
        )
        ProjectType.PHP -> listOf(
            Recipe("Install PHP", "PHP + composer in Termux",
                "pkg install -y php composer", needsProjectDir = false),
            Recipe("Composer install", "Install dependencies", "composer install"),
            Recipe("Laravel serve", "Run Laravel dev server",
                "php artisan serve --host=0.0.0.0 --port=8000"),
            Recipe("Laravel migrate", "Run migrations", "php artisan migrate"),
            Recipe("Laravel cache clear", "Clear caches",
                "php artisan optimize:clear"),
            Recipe("Plain PHP server", "Built-in server in current dir",
                "php -S 0.0.0.0:8080 -t .")
        )
        ProjectType.JAVA -> listOf(
            Recipe("Install JDK", "OpenJDK 17 + Maven in Termux",
                "pkg install -y openjdk-17 maven", needsProjectDir = false),
            Recipe("Maven build", "Build skipping tests",
                "./mvnw -q package -DskipTests || mvn -q package -DskipTests"),
            Recipe("Spring run (mvn)", "Boot via mvnw",
                "./mvnw spring-boot:run"),
            Recipe("Spring run (gradle)", "Boot via gradlew",
                "./gradlew bootRun"),
            Recipe("Run jar", "Run built jar (target/*.jar)",
                "java -jar target/*.jar")
        )
        ProjectType.GO -> listOf(
            Recipe("Install Go", "Go toolchain in Termux",
                "pkg install -y golang", needsProjectDir = false),
            Recipe("Mod download", "Resolve modules", "go mod download"),
            Recipe("Mod tidy", "Clean modules", "go mod tidy"),
            Recipe("Run", "Run main package", "go run ."),
            Recipe("Build", "Compile binary", "go build -o app .")
        )
        ProjectType.RUST -> listOf(
            Recipe("Install Rust", "Rust toolchain in Termux",
                "pkg install -y rust", needsProjectDir = false),
            Recipe("Build", "Cargo build", "cargo build"),
            Recipe("Run", "Cargo run", "cargo run"),
            Recipe("Test", "Cargo tests", "cargo test"),
            Recipe("Release build", "Optimized build", "cargo build --release")
        )
        ProjectType.DOCKER -> listOf(
            Recipe("Setup proot Debian", "Install proot-distro + Debian (needed for docker)",
                "pkg install -y proot-distro && proot-distro install debian",
                needsProjectDir = false),
            Recipe("Compose up", "Run docker compose", "docker compose up"),
            Recipe("Compose down", "Stop", "docker compose down"),
            Recipe("Build image", "Build Dockerfile",
                "docker build -t app ."),
            Recipe("List containers", "All containers", "docker ps -a")
        )
        ProjectType.DOTNET -> listOf(
            Recipe("Restore", "dotnet restore", "dotnet restore"),
            Recipe("Run", "dotnet run", "dotnet run"),
            Recipe("Build", "dotnet build", "dotnet build -c Release"),
            Recipe("Test", "dotnet test", "dotnet test")
        )
        ProjectType.RUBY -> listOf(
            Recipe("Install Ruby", "Ruby in Termux",
                "pkg install -y ruby", needsProjectDir = false),
            Recipe("Bundle install", "Install gems", "bundle install"),
            Recipe("Rails server", "Start Rails",
                "bin/rails server -b 0.0.0.0 -p 3000"),
            Recipe("Rake migrate", "DB migrations",
                "bin/rails db:migrate"),
            Recipe("Rack", "Plain Rack app",
                "rackup -o 0.0.0.0 -p 9292")
        )
        ProjectType.STATIC_HTML -> listOf(
            Recipe("Serve via Python", "Quick static server",
                "python -m http.server 8000"),
            Recipe("Serve via npx", "serve package",
                "npx --yes serve -l 8000")
        )
        ProjectType.UNKNOWN -> listOf(
            Recipe("List files", "What's here?", "ls -la"),
            Recipe("Disk usage", "Top-level sizes", "du -sh * 2>/dev/null | sort -h")
        )
    }
}
