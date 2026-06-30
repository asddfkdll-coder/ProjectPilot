# 📋 جدول تشغيل السيرفرات حسب نوع المشروع

> هذه الجداول مضمّنة داخل التطبيق كمنطق Detector + تظهر للمستخدم في شاشة تفاصيل المشروع.

## Node.js / JavaScript / TypeScript

| Framework | تثبيت Termux | تثبيت المتطلبات | أمر التشغيل | البورت |
|---|---|---|---|---|
| Express | `pkg install nodejs` | `npm install` | `npm start` | 3000 |
| Next.js | `pkg install nodejs` | `npm install` | `npm run dev` | 3000 |
| Nest.js | `pkg install nodejs` | `npm install` | `npm run start:dev` | 3000 |
| Vite/Vue | `pkg install nodejs` | `npm install` | `npm run dev` | 5173 |
| Angular | `pkg install nodejs` | `npm install` | `npm start` | 4200 |
| CRA | `pkg install nodejs` | `npm install` | `npm start` | 3000 |

## Python

| Framework | تثبيت Termux | تثبيت المتطلبات | أمر التشغيل | البورت |
|---|---|---|---|---|
| Django | `pkg install python` | `pip install -r requirements.txt` | `python manage.py runserver 0.0.0.0:8000` | 8000 |
| Flask | `pkg install python` | `pip install -r requirements.txt` | `FLASK_APP=app.py flask run --host 0.0.0.0` | 5000 |
| FastAPI | `pkg install python` | `pip install -r requirements.txt` | `uvicorn main:app --host 0.0.0.0 --port 8000 --reload` | 8000 |

> 💡 يُنصح باستخدام virtualenv: `python -m venv .venv && source .venv/bin/activate`

## PHP

| Framework | تثبيت Termux | تثبيت المتطلبات | أمر التشغيل | البورت |
|---|---|---|---|---|
| Laravel | `pkg install php composer` | `composer install` | `php artisan serve --host=0.0.0.0 --port=8000` | 8000 |
| WordPress | `pkg install php php-mysql` | — | `php -S 0.0.0.0:8080 -t .` | 8080 |
| Plain PHP | `pkg install php` | — | `php -S 0.0.0.0:8080 -t .` | 8080 |

## Java / Spring

| Framework | تثبيت Termux | تثبيت المتطلبات | أمر التشغيل | البورت |
|---|---|---|---|---|
| Spring Boot (Maven) | `pkg install openjdk-17 maven` | `./mvnw package -DskipTests` | `./mvnw spring-boot:run` | 8080 |
| Spring Boot (Gradle) | `pkg install openjdk-17` | `./gradlew build -x test` | `./gradlew bootRun` | 8080 |
| Plain Java | `pkg install openjdk-17 maven` | `mvn -q package` | `mvn -q exec:java` | — |

## Go

| Framework | تثبيت Termux | تثبيت المتطلبات | أمر التشغيل | البورت |
|---|---|---|---|---|
| Go module | `pkg install golang` | `go mod download` | `go run .` | 8080 |

## Rust

| Framework | تثبيت Termux | تثبيت المتطلبات | أمر التشغيل | البورت |
|---|---|---|---|---|
| Cargo | `pkg install rust` | `cargo build` | `cargo run` | 8000 |

## Docker

| Framework | تثبيت Termux | تثبيت المتطلبات | أمر التشغيل | البورت |
|---|---|---|---|---|
| Docker Compose | `pkg install proot-distro && proot-distro install debian` | داخل debian: `apt install docker.io` | `docker compose up` | 8080 |
| Dockerfile | كما أعلاه | — | `docker build -t app . && docker run --rm -p 8080:8080 app` | 8080 |

> ⚠️ Docker على Android بدون Root يعمل عبر `proot-distro` فقط، والأداء محدود.

## .NET

| Framework | تثبيت Termux | تثبيت المتطلبات | أمر التشغيل | البورت |
|---|---|---|---|---|
| ASP.NET / Console | تثبيت dotnet SDK يدوياً (راجع docs Microsoft لـ ARM64) | `dotnet restore` | `dotnet run` | 5000 |

## Ruby / Rails

| Framework | تثبيت Termux | تثبيت المتطلبات | أمر التشغيل | البورت |
|---|---|---|---|---|
| Rails | `pkg install ruby` | `bundle install` | `bin/rails server -b 0.0.0.0 -p 3000` | 3000 |
| Rack (Sinatra) | `pkg install ruby` | `bundle install` | `rackup -o 0.0.0.0 -p 9292` | 9292 |

## Static HTML/CSS

| النوع | الأمر | البورت |
|---|---|---|
| Static | `python -m http.server 8000` أو `npx serve -l 8000` | 8000 |
