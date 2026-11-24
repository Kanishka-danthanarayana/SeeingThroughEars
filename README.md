# SeeingThroughEars 👁️🔊
**Offline AI Vision Assistant for Blind People — Android App**

An Android app that turns the phone camera into a real-time guide for visually impaired users using two on-device AI models. **Fully offline — no internet or cloud required.**

---

## How It Works

| Component | Model | Role |
|---|---|---|
| Object Detection | **YOLOv8m-WorldV2** (TFLite, GPU) | Detects objects in real-time from the camera |
| Language Model | **Gemma 2B IT** (MediaPipe, GPU INT4) | Turns detected objects into natural spoken guidance |
| Voice Output | Android TTS | Speaks descriptions aloud to the user |

### Interaction
- **Real-time mode**: YOLO continuously detects objects and speaks them aloud
- **Single tap**: Captures the current scene and asks Gemma to describe it conversationally for a blind user

---

## Tech Stack
- **Language**: Kotlin (Android)
- **AI inference**: TensorFlow Lite (YOLO), MediaPipe GenAI (Gemma)
- **Camera**: CameraX
- **On-device LLM**: Gemma 2B IT (GPU INT4 quantized)
- **Object detection**: YOLOv8m-WorldV2 (1,203-class open-vocabulary model)
- **Min SDK**: 24 (Android 7.0) | **Target SDK**: 34

---

## ⚠️ Model Files (Not Included in Git)

The model files are too large for GitHub (combined ~1.4 GB). You must download them manually and place them in `app/src/main/assets/`.

| File | Size | Source |
|---|---|---|
| `gemma-2b-it-gpu-int4.bin` | ~1.3 GB | [Google AI Edge / Kaggle](https://www.kaggle.com/models/google/gemma/tfLite/gemma-2b-it-gpu-int4) |
| `yolov8m-worldv2.tflite` | ~117 MB | [Ultralytics / YOLO-World](https://docs.ultralytics.com/models/yolo-world/) |

After downloading, your assets folder should look like:
```
app/src/main/assets/
├── gemma-2b-it-gpu-int4.bin
├── yolov8m-worldv2.tflite
└── labels.txt
```

---

## Setup & Build

1. Clone the repo
2. Download and place the model files (see above)
3. Open in Android Studio
4. Build & run on a physical Android device (GPU required for Gemma inference)

> **Note:** Gemma 2B GPU inference requires a device with a supported GPU. Emulators will not work.

---

## Project Structure

```
app/src/main/java/com/ears/vision/
├── MainActivity.kt      # Camera, gesture handling, TTS orchestration
├── GemmaManager.kt      # MediaPipe Gemma 2B LLM inference
├── ObjectDetector.kt    # YOLOv8 TFLite inference + NMS
└── OverlayView.kt       # Bounding box overlay on camera preview
```

---

## Author
**Kanishka Danthanarayana**  
Staff Validation Engineer | AI Automation & Testing  
[LinkedIn](https://linkedin.com/in/kanishkadanthanarayana)
