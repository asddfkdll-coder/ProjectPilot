#!/data/data/com.termux/files/usr/bin/bash
# ProjectPilot — build APK directly on your phone with Termux
set -e

# حفظ مجلد المشروع بشكل صحيح
PROJECT_DIR="$(cd "$(dirname "$(realpath "$0")")" && pwd)"
echo "Project directory: $PROJECT_DIR"

echo "==> 1/5  Installing toolchain (one-time, ~400 MB)"
pkg update -y && pkg install -y openjdk-21 gradle wget unzip

echo "==> 2/5  Setting up Android SDK"
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
if [ ! -d latest ]; then
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmd.zip
    unzip -q cmd.zip && mv cmdline-tools latest && rm cmd.zip
fi
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "==> 3/5  Configuring project"
cd "$PROJECT_DIR"
echo "sdk.dir=$ANDROID_HOME" > local.properties

echo "==> 4/5  Building debug APK"
if [ -f "$PROJECT_DIR/gradlew" ]; then
    chmod +x "$PROJECT_DIR/gradlew"
    ./gradlew :app:assembleDebug --no-daemon
else
    gradle :app:assembleDebug --no-daemon
fi

echo "==> 5/5  Done!"
APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK" ]; then
    ls -lh "$APK"
    echo ""
    echo "✅ To install on this phone:"
    echo "   pm install -r $APK"
    echo ""
    echo "✅ Or copy to Downloads:"
    echo "   cp $APK ~/storage/downloads/"
else
    echo "⚠️ APK not found at expected path"
    find "$PROJECT_DIR/app/build" -name "*.apk" 2>/dev/null || true
fi
