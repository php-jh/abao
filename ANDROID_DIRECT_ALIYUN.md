# 安卓直连阿里云结构说明

当前安卓项目目录：

```text
android/
  settings.gradle
  build.gradle
  app/
    build.gradle
    src/main/
      AndroidManifest.xml
      assets/www/          # 当前网页页面
      java/com/abao/speaking/
        MainActivity.java  # WebView 壳 + JS Bridge
        WavRecorder.java   # 原生录音，16k PCM/WAV
        AliyunConfig.java  # 阿里云配置占位
        AliyunNlsClient.java # 直连阿里云 NLS 一句话识别
```

## 现在已实现

安卓 App 打开本地网页：

```text
file:///android_asset/www/index.html
```

网页点击 `课中演练 -> 开始录入`：

```text
JS
  -> window.AliyunNlsAndroid.startRecording()
  -> Android 原生录音
  -> 再次点击停止
  -> window.AliyunNlsAndroid.stopRecording()
  -> Android 上传 WAV 到阿里云 NLS
  -> 识别结果回传 window.setRecognizedText("...")
  -> 填入学生回答 textarea
```

## 需要你后面填写的位置

打开：

```text
android/app/src/main/java/com/abao/speaking/AliyunConfig.java
```

填写：

```java
static final String NLS_APP_KEY = "nOBt4SZhSXTB7ynx";
static final String ACCESS_KEY_ID = "填写新的临时AccessKeyId";
static final String ACCESS_KEY_SECRET = "填写新的临时AccessKeySecret";
static final String NLS_TOKEN = "填写NLS临时Token";
```

当前代码为了快，先使用 `NLS_TOKEN` 直连识别。

如果 Token 过期，需要重新生成并替换：

```java
static final String NLS_TOKEN = "新的Token";
```

## Android Studio 打包

1. 打开 Android Studio
2. Open 项目目录：

```text
C:\Users\Administrator\Documents\外包\android
```

3. 等 Gradle 同步完成
4. 修改 `AliyunConfig.java`
5. 连接手机或模拟器
6. 点击 Run

生成 APK：

```text
Build -> Build Bundle(s) / APK(s) -> Build APK(s)
```

## 权限

已配置：

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

第一次点击录音时，手机会弹出麦克风权限。

## 注意

- 这是“时间紧演示用”的直连方案。
- AccessKey / Token 放在 APK 里有泄露风险。
- 请使用临时 RAM 用户。
- 演示结束后立即禁用或删除 AccessKey。
- 如果后续正式使用，建议改回“App -> 后端 -> 阿里云”的安全方案。

## 后续 AI 评分接入点

当前页面仍然使用本地模拟评分。

后续要接百炼/Qwen，可以加：

```text
Android 原生调用百炼
  -> 回传 JS window.setAiScore(...)
```

或者更推荐：

```text
App -> 后端 /api/score -> 百炼/Qwen
```
