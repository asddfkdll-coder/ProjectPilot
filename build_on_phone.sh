#!/data/data/com.termux/files/usr/bin/bash
# ProjectPilot — build APK directly on your phone with Termux.
# Tested on Termux from F-Droid (Android 10+, arm64).

set -e
echo "==> 1/5  Installing toolchain (one-time, ~400 MB)"
pkg update -y && pkg install -y openjdk-21 gradle wget unzip

echo "==> 2/5  Setting up Android SDK"
mkdir -p ~/android-sdk/cmdline-tools && cd ~/android-sdk/cmdline-tools
if [ ! -d latest ]; then
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmd.zip
    unzip -q cmd.zip && mv cmdline-tools latest && rm cmd.zip
fi
export ANDROID_HOME=$HOME/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "==> 3/5  Configuring project"
cd "$(dirname "$0")"
echo "sdk.dir=$ANDROID_HOME" > local.properties

echo "==> 4/5  Building debug APK (5–15 min, depending on phone)"
gradle :app:assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx1500m"

echo "==> 5/5  Done!"
APK=app/build/outputs/apk/debug/app-debug.apk
ls -lh "$APK"
echo
echo "To install on this phone:"
echo "    pm install -r $PWD/$APK     # may require: termux-setup-storage"
echo "Or copy to Downloads and tap to install."
