workflows:
  android-workflow:
    name: SplitTrip Android Build
    max_build_duration: 60
    environment:
      java: 17
    scripts:
      - name: Set up local.properties
        script: echo "sdk.dir=$ANDROID_SDK_ROOT" > "$CM_BUILD_DIR/local.properties"
      - name: Download Gradle wrapper jar
        script: |
          mkdir -p gradle/wrapper
          curl -L "https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar" \
            -o gradle/wrapper/gradle-wrapper.jar
      - name: Make gradlew executable
        script: chmod +x gradlew
      - name: Build debug APK
        script: ./gradlew assembleDebug --stacktrace
    artifacts:
      - app/build/outputs/apk/debug/app-debug.apk
