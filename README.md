# 🎭 EmoteStream - AI Gesture Recognition for Discord

**Transform your gestures into epic video reactions for Discord, streaming, and video calls!**

## 🚀 Quick Start

```bash
pip install -r requirements.txt
python main.py
```

## 🎬 What it does

EmoteStream uses AI to detect your hand gestures and triggers custom video reactions:

- 👋 **Hands Up** → Celebration video
- 🤲 **Hands on Head** → Facepalm video  
- 🎻 **Violin Gesture** → World's saddest violin
- ✌️ **Peace Out** → Peace sign video
- 🖕 **Middle Finger** → Sassy response video
- 🔫 **Shot in Head** → Dramatic reaction video

## 📺 Discord Integration

Creates a virtual camera called **"EmoteStream Virtual Camera"** that you can select in:
- Discord (Settings → Voice & Video → Camera)
- OBS Studio
- Zoom, Teams, etc.

## 🎯 Features

- ✅ **AI-powered gesture detection** (MediaPipe + OpenCV)
- ✅ **6 custom gestures** with custom videos
- ✅ **Virtual camera integration** for Discord/streaming
- ✅ **Real-time preview** window
- ✅ **Customizable hold times** and cooldowns

## 📋 Requirements

- Python 3.9+
- Webcam for gesture detection
- Video files in `assets/video/` folder

## 🛠️ Installation

1. **Clone the repository**
```bash
git clone https://github.com/JustEdward01/emotestream.git
cd emotestream
```

2. **Install dependencies**
```bash
pip install -r requirements.txt
```

3. **Add your videos**
Place MP4 files in `assets/video/`:
```
assets/video/
├── hands_up.mp4
├── hands_on_head.mp4  
├── saddest_violin.mp4
├── peace_out.mp4
├── middle_finger.mp4
└── shot_in_head.mp4
```

4. **Run EmoteStream**
```bash
python main.py
```

## 🎮 Controls

While running:
- `q` = Quit
- `d` = Toggle debug mode  
- `s` = Show statistics
- `h` = Help
- `r` = Reload configuration

## 🎯 How to Use

1. **Start EmoteStream**: `python main.py`
2. **Setup Discord**: Settings → Voice & Video → Select "EmoteStream Virtual Camera"
3. **Make gestures**: Hold gestures for 1 second to trigger videos
4. **Enjoy**: Your friends see epic reactions when you gesture!

## ⚙️ Configuration

Edit `emotes/emotes.yaml` to customize:
- Video paths
- Gesture types
- Hold times
- Descriptions

## 🔧 Troubleshooting

**Camera not detected?**
- Make sure no other app is using your camera
- Check camera permissions

**Virtual camera not in Discord?**
- Install OBS or OBS Virtual Camera
- Restart Discord after installation

**Gestures not detected?**
- Ensure good lighting
- Make clear, deliberate gestures
- Hold gestures for 1+ seconds

## 🎬 Adding Custom Videos

1. Add your MP4 files to `assets/video/`
2. Update paths in `emotes/emotes.yaml`
3. Restart EmoteStream

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Add your gesture or improvement
4. Submit a pull request

## 📜 License

MIT License - Feel free to use and modify!

---

**Made with ❤️ for streamers and Discord users who want to add some fun to their video calls!**

🎭 **Happy EmoteStreaming!** ✨
