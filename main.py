import cv2
import time
import logging
import sys
import traceback
import pygame
import threading
import json
import os
from pathlib import Path
from typing import Optional
from datetime import datetime

# FIX pentru emoji pe Windows - PRIMUL LUCRU!
if sys.platform == "win32":
    try:
        os.system("chcp 65001 >nul 2>&1")  # Set UTF-8 console
    except:
        pass

# Import improved modules
try:
    from modules.config_loader import ConfigLoader
    from modules.detector import EmoteDetector  # Updated to use your custom detector
    from modules.video_player import VideoAudioPlayer, PlayerState
    from modules.virtualcam import VirtualCameraManager

    print("[✓] All modules imported successfully")
except ImportError as e:
    print(f"[❌] Import error: {e}")
    print("Make sure all module files exist and are correct")
    input("Press Enter to exit...")
    sys.exit(1)


class SystemTrayManager:
    """Simple system tray simulation with console feedback."""

    def __init__(self, app):
        self.app = app
        self.running = True
        self.background_thread = None

    def start_background_monitoring(self):
        """Start background monitoring thread."""
        self.background_thread = threading.Thread(target=self._background_worker, daemon=True)
        self.background_thread.start()
        print("[🔄] Background monitoring started")

    def _background_worker(self):
        """Background worker for system monitoring."""
        last_stats_time = time.time()
        stats_interval = 300  # 5 minutes

        while self.running and self.app.running:
            try:
                current_time = time.time()

                # Periodic stats logging
                if current_time - last_stats_time > stats_interval:
                    self._log_background_stats()
                    last_stats_time = current_time

                # Check virtual camera health
                if self.app.virtual_camera and not self.app.virtual_camera.is_open:
                    print("[⚠️] Virtual camera disconnected, attempting reconnect...")
                    self.app._reconnect_virtual_camera()

                time.sleep(10)  # Check every 10 seconds

            except Exception as e:
                print(f"[❌] Background monitoring error: {e}")
                time.sleep(30)

    def _log_background_stats(self):
        """Log background statistics."""
        if self.app.virtual_camera:
            fps = self.app._calculate_fps()
            print(f"[📊] Background Stats - FPS: {fps:.1f}, Frames sent: {self.app.virtual_camera.frame_count}")

    def stop(self):
        """Stop background monitoring."""
        self.running = False
        print("[🛑] Background monitoring stopped")


class ConfigurationManager:
    """Advanced configuration management with auto-save and validation."""

    def __init__(self, config_path: str):
        self.config_path = config_path
        self.settings_path = "settings.json"
        self.backup_path = "config_backup.yaml"
        self.default_settings = {
            "auto_start_camera": True,
            "minimize_to_tray": False,
            "detection_sensitivity": 1.0,
            "hold_time": 1.0,  # Reduced for your gestures
            "cooldown_time": 2.0,  # Reduced for better responsiveness
            "video_quality": "HD",
            "auto_reconnect": True,
            "debug_mode": False,
            "branding_enabled": True,
            "last_run": None
        }
        self.settings = self.load_settings()

    def load_settings(self) -> dict:
        """Load user settings from JSON file."""
        try:
            if Path(self.settings_path).exists():
                with open(self.settings_path, 'r') as f:
                    settings = json.load(f)
                # Merge with defaults for new settings
                merged = self.default_settings.copy()
                merged.update(settings)
                return merged
            else:
                return self.default_settings.copy()
        except Exception as e:
            print(f"[⚠️] Error loading settings: {e}")
            return self.default_settings.copy()

    def save_settings(self):
        """Save current settings to JSON file."""
        try:
            self.settings["last_run"] = datetime.now().isoformat()
            with open(self.settings_path, 'w') as f:
                json.dump(self.settings, f, indent=2)
        except Exception as e:
            print(f"[⚠️] Error saving settings: {e}")

    def backup_config(self):
        """Create backup of emote configuration."""
        try:
            if Path(self.config_path).exists():
                import shutil
                shutil.copy2(self.config_path, self.backup_path)
                print(f"[💾] Configuration backed up to {self.backup_path}")
        except Exception as e:
            print(f"[⚠️] Error backing up config: {e}")


def show_startup_banner():
    """Show application startup banner with your gestures."""
    banner = """
╔══════════════════════════════════════════════════════════════╗
║                      🎭 EMOTESTREAM 2.0 🎭                   ║
║                                                              ║
║              AI-Powered Gesture Recognition                  ║
║              Virtual Camera for Discord & Streaming         ║
║                                                              ║
║  👋 Hands Up         🤲 Hands on Head    🎻 Violin Gesture   ║
║  ✌️ Peace Out        🖕 Middle Finger    🔫 Shot in Head     ║
║                                                              ║
║  Made with ❤️ for creators and streamers                    ║
╚══════════════════════════════════════════════════════════════╝
    """
    print(banner)


def simple_detector_test():
    """Enhanced detector test for YOUR gestures."""
    print("\n[🧪] Starting YOUR Custom Detector Test...")
    print("─" * 60)

    try:
        # Test config loading
        print("[1/4] Loading YOUR configuration...")
        import yaml
        with open("emotes/emotes.yaml", "r") as f:
            emotes = yaml.safe_load(f)
        print(f"[✓] Config loaded: {list(emotes.keys())}")

        # Test detector creation
        print("[2/4] Creating YOUR custom detector...")
        detector = EmoteDetector(emotes, hold_time=1.0)  # Faster for testing
        print("[✓] YOUR detector created successfully")

        # Test camera
        print("[3/4] Testing camera access...")
        cap = cv2.VideoCapture(0)
        if not cap.isOpened():
            raise RuntimeError("Cannot access camera")
        print("[✓] Camera access confirmed")

        print("[4/4] Starting YOUR gesture detection loop...")
        print("\n🎮 CONTROLS:")
        print("  'q' = Quit test")
        print("  'd' = Toggle debug mode")
        print("  'h' = Show help")
        print("\n🎭 YOUR GESTURES TO TRY:")
        print("  👋 Hands Up (above head)")
        print("  🤲 Hands on Head (covering ears)")
        print("  🎻 Violin Gesture (one hand up, one extended)")
        print("  ✌️ Peace Out (V sign with fingers)")
        print("  🖕 Middle Finger (middle finger extended)")
        print("  🔫 Shot in Head (hand to temple)")
        print("\n" + "─" * 60)

        frame_count = 0
        detection_count = 0
        start_time = time.time()

        while True:
            ret, frame = cap.read()
            if not ret:
                print("[❌] Cannot read camera frame")
                break

            frame_count += 1
            frame = cv2.flip(frame, 1)  # Mirror effect

            try:
                # Process frame with YOUR detector
                results = detector.process_frame(frame)
                emote_detected, status = detector.detect_emote_with_status(results)

                # Draw landmarks for debugging
                detector.draw_pose_landmarks(frame, results)

                # Show status every 30 frames
                if status and frame_count % 30 == 0:
                    progress_bar = "█" * int(status['progress'] * 20) + "░" * (20 - int(status['progress'] * 20))
                    print(f"[🎯] {status['text']} [{progress_bar}]")

                # Show detection
                if emote_detected:
                    detection_count += 1
                    elapsed = time.time() - start_time
                    print(f"[🎉] DETECTION #{detection_count}: {emote_detected['name'].upper()} (at {elapsed:.1f}s)")

                # Enhanced UI
                display_frame = frame.copy()

                # Add status overlay
                if status:
                    # Progress bar
                    bar_width = 300
                    bar_height = 20
                    progress = int(status['progress'] * bar_width)
                    cv2.rectangle(display_frame, (20, 50), (20 + bar_width, 50 + bar_height), (50, 50, 50), -1)
                    cv2.rectangle(display_frame, (20, 50), (20 + progress, 50 + bar_height), (0, 255, 0), -1)
                    cv2.putText(display_frame, status['text'], (20, 45), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255),
                                2)

                if emote_detected:
                    cv2.putText(display_frame, f"🎉 DETECTED: {emote_detected['name'].upper()}", (20, 100),
                                cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 3)

                # Add YOUR gestures info
                cv2.putText(display_frame, "YOUR Gestures: violin, peace_out, middle_finger, shot_in_head", (20, 130),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 0), 1)

                # Add instructions
                cv2.putText(display_frame, "Press 'q' to quit, 'd' for debug", (20, display_frame.shape[0] - 20),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

                cv2.imshow("🧪 YOUR EmoteStream Detector Test", display_frame)

                # Handle keys
                key = cv2.waitKey(1) & 0xFF
                if key == ord('q'):
                    print("\n[👋] Test completed by user")
                    break
                elif key == ord('d'):
                    detector.toggle_debug()
                elif key == ord('h'):
                    print("\n📖 YOUR GESTURE HELP:")
                    print("  🎻 Violin: One hand up (bow), one extended (violin)")
                    print("  ✌️ Peace: V sign with index and middle finger")
                    print("  🖕 Middle: Only middle finger extended")
                    print("  🔫 Shot: Hand near temple/side of head")
                    print("  Make clear, deliberate gestures")
                    print("  Hold gestures for 1 second")

            except Exception as e:
                print(f"[❌] Frame processing error: {e}")
                break

        # Test summary
        elapsed = time.time() - start_time
        print(f"\n📊 YOUR TEST SUMMARY:")
        print(f"  Duration: {elapsed:.1f}s")
        print(f"  Frames processed: {frame_count}")
        print(f"  YOUR detections: {detection_count}")
        print(f"  Average FPS: {frame_count / elapsed:.1f}")

        # Cleanup
        cap.release()
        cv2.destroyAllWindows()
        print("[✓] YOUR test completed successfully")
        return True

    except Exception as e:
        print(f"[❌] Test failed: {e}")
        traceback.print_exc()
        return False


class EmoteStreamApp:
    """Enhanced EmoteStream application for YOUR custom gestures."""

    def __init__(self, config_path: str = "emotes/emotes.yaml"):
        # Enhanced configuration management
        self.config_manager = ConfigurationManager(config_path)
        self.config_path = config_path
        self.emotes = {}

        # Setup enhanced logging - FĂRĂ EMOJI!
        self.setup_logging()
        self.logger = logging.getLogger(__name__)

        # System tray manager
        self.tray_manager = SystemTrayManager(self)

        # Components
        self.config_loader = None
        self.detector: Optional[EmoteDetector] = None
        self.video_player = None
        self.virtual_camera = None
        self.physical_camera: Optional[cv2.VideoCapture] = None

        # Enhanced application state
        self.running = False
        self.current_emote = None
        self.last_triggered = 0
        self.cooldown = self.config_manager.settings.get("cooldown_time", 2.0)  # Faster cooldown
        self.is_playing_emote = False
        self.auto_reconnect = self.config_manager.settings.get("auto_reconnect", True)

        # Statistics and monitoring
        self.frame_count = 0
        self.start_time = None
        self.error_count = 0
        self.detection_count = 0
        self.last_health_check = time.time()
        self._last_results = None  # Store last results for preview

        # Quality of life features
        self.minimized = False
        self.show_preview = True
        self.branding_enabled = self.config_manager.settings.get("branding_enabled", True)

    def setup_logging(self):
        """FIXED logging setup - eliminates emoji problems completely."""
        log_dir = Path("logs")
        log_dir.mkdir(exist_ok=True)

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        log_file = log_dir / f"emotestream_{timestamp}.log"

        # Custom formatter care elimină emoji automat
        class NoEmojiFormatter(logging.Formatter):
            def format(self, record):
                # Elimină automat emoji din toate mesajele
                import re
                msg = str(record.getMessage())
                # Pattern complet pentru emoji
                emoji_pattern = re.compile("["
                                           u"\U0001F600-\U0001F64F"  # emoticons
                                           u"\U0001F300-\U0001F5FF"  # symbols & pictographs
                                           u"\U0001F680-\U0001F6FF"  # transport & map symbols
                                           u"\U0001F1E0-\U0001F1FF"  # flags (iOS)
                                           u"\U00002702-\U000027B0"  # dingbats
                                           u"\U000024C2-\U0001F251"  # other symbols
                                           u"\U0001F900-\U0001F9FF"  # supplemental symbols
                                           "]+", flags=re.UNICODE)

                clean_msg = emoji_pattern.sub('', msg).strip()
                record.msg = clean_msg
                record.args = None

                return super().format(record)

        try:
            # File handler safe
            file_handler = logging.FileHandler(log_file, encoding='utf-8')
            file_handler.setFormatter(NoEmojiFormatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))

            # Console handler cu formatter safe
            console_handler = logging.StreamHandler(sys.stdout)
            console_handler.setFormatter(NoEmojiFormatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))

            # Configurare root logger
            root_logger = logging.getLogger()
            root_logger.setLevel(logging.INFO)

            # Clear existing handlers
            for handler in root_logger.handlers[:]:
                root_logger.removeHandler(handler)

            root_logger.addHandler(file_handler)
            root_logger.addHandler(console_handler)

            print(f"[📝] Logging to: {log_file}")

        except Exception as e:
            # Fallback ultra-simplu
            logging.basicConfig(
                level=logging.INFO,
                format='%(asctime)s - %(levelname)s - %(message)s',
                handlers=[logging.StreamHandler(sys.stdout)]
            )
            print(f"[⚠️] Logging fallback: {e}")

    def initialize(self) -> bool:
        """Enhanced initialization with better error handling."""
        try:
            self.logger.info("Initializing YOUR EmoteStream 2.0...")  # NO EMOJI

            # Backup configuration
            self.config_manager.backup_config()

            # Initialize components step by step
            steps = [
                ("Config Loader", self._init_config_loader),
                ("YOUR Configuration", self._load_config),
                ("YOUR Gesture Detector", self._init_detector),
                ("Physical Camera", self._initialize_physical_camera),
                ("Video Player", self._init_video_player),
                ("Virtual Camera", self._init_virtual_camera),
                ("Audio System", self._init_audio),
                ("Background Services", self._init_background_services)
            ]

            for step_name, step_func in steps:
                print(f"[⏳] Initializing {step_name}...")
                if not step_func():
                    self.logger.error(f"Failed to initialize {step_name}")
                    return False
                print(f"[✓] {step_name} ready")

            self.logger.info("YOUR EmoteStream 2.0 initialized successfully!")  # NO EMOJI
            self._show_startup_summary()

            return True

        except Exception as e:
            self.logger.error(f"Initialization failed: {e}")  # NO EMOJI
            traceback.print_exc()
            return False

    def _init_config_loader(self) -> bool:
        """Initialize configuration loader."""
        try:
            self.config_loader = ConfigLoader(self.logger)
            return True
        except Exception as e:
            self.logger.error(f"Config loader initialization failed: {e}")
            return False

    def _load_config(self) -> bool:
        """Load YOUR emote configuration - FIXED pentru doar video_path."""
        try:
            # LOADING MANUAL PENTRU A EVITA VALIDAREA audio_path
            import yaml

            # Încarcă direct YAML fără validare strictă
            with open(self.config_path, 'r', encoding='utf-8') as f:
                raw_config = yaml.safe_load(f)

            # Validare custom care NU cere audio_path
            validated_emotes = {}
            for emote_name, emote_data in raw_config.items():
                try:
                    # Verifică doar câmpurile esențiale
                    if 'gesture' not in emote_data:
                        self.logger.warning(f"Missing 'gesture' field for emote '{emote_name}' - skipping")
                        continue

                    if 'video_path' not in emote_data:
                        self.logger.warning(f"Missing 'video_path' field for emote '{emote_name}' - skipping")
                        continue

                    # Verifică dacă fișierul video există
                    video_path = Path(emote_data['video_path'])
                    if not video_path.exists():
                        self.logger.warning(f"Video file not found for '{emote_name}': {video_path} - skipping")
                        continue

                    # Verifică tipul de gesture
                    gesture_type = emote_data['gesture'].get('type')
                    valid_gestures = ['hands_up', 'hands_on_head', 'violin_gesture', 'peace_out', 'middle_finger',
                                      'shot_in_head']
                    if gesture_type not in valid_gestures:
                        self.logger.warning(f"Unknown gesture type for '{emote_name}': {gesture_type} - skipping")
                        continue

                    # Adaugă emote-ul valid
                    validated_emotes[emote_name] = emote_data
                    self.logger.info(f"Loaded emote: {emote_name} ({gesture_type})")

                except Exception as e:
                    self.logger.error(f"Validation failed for emote '{emote_name}': {e} - skipping")
                    continue

            self.emotes = validated_emotes
            self.logger.info(f"Successfully loaded {len(self.emotes)} emote configurations: {list(self.emotes.keys())}")

            if len(self.emotes) == 0:
                self.logger.error("No valid emotes loaded! Check emotes.yaml file and video paths.")
                return False

            return True

        except Exception as e:
            self.logger.error(f"Failed to load YOUR configuration: {e}")
            return False

    def _init_detector(self) -> bool:
        """Initialize YOUR gesture detector."""
        try:
            if len(self.emotes) == 0:
                raise ValueError("No emotes available for detector initialization")

            hold_time = self.config_manager.settings.get("hold_time", 1.0)  # Faster for your gestures
            self.detector = EmoteDetector(
                emote_configs=self.emotes,
                hold_time=hold_time,
                cooldown_time=self.cooldown,
                logger=self.logger
            )
            return True
        except Exception as e:
            self.logger.error(f"YOUR detector initialization failed: {e}")
            return False

    def _init_video_player(self) -> bool:
        """Initialize video player."""
        try:
            self.video_player = VideoAudioPlayer(self.logger)
            self.video_player.set_error_callback(self._on_video_error)
            return True
        except Exception as e:
            self.logger.error(f"Video player initialization failed: {e}")
            return False

    def _init_virtual_camera(self) -> bool:
        """Initialize virtual camera with enhanced settings."""
        try:
            quality = self.config_manager.settings.get("video_quality", "HD")
            width, height = (1280, 720) if quality == "HD" else (640, 480)

            self.virtual_camera = VirtualCameraManager(
                width=width, height=height, fps=30,
                device_name="EmoteStream Virtual Camera",
                logger=self.logger
            )

            if not self.virtual_camera.open():
                raise RuntimeError("Failed to open virtual camera")

            self.logger.info(f"Virtual camera ready: {self.virtual_camera.device_info}")  # NO EMOJI
            return True

        except Exception as e:
            self.logger.error(f"Virtual camera initialization failed: {e}")
            return False

    def _init_audio(self) -> bool:
        """Initialize audio system."""
        try:
            pygame.mixer.init()
            self.logger.info("Audio system ready")  # NO EMOJI
            return True
        except Exception as e:
            self.logger.error(f"Audio initialization failed: {e}")
            return False

    def _init_background_services(self) -> bool:
        """Initialize background services."""
        try:
            if self.config_manager.settings.get("auto_start_camera", True):
                self.tray_manager.start_background_monitoring()
            return True
        except Exception as e:
            self.logger.error(f"Background services initialization failed: {e}")
            return False

    def _initialize_physical_camera(self) -> bool:
        """Enhanced physical camera initialization."""
        try:
            self.physical_camera = cv2.VideoCapture(0)
            if not self.physical_camera.isOpened():
                raise RuntimeError("Cannot access physical camera")

            # Enhanced camera settings
            quality = self.config_manager.settings.get("video_quality", "HD")
            if quality == "HD":
                self.physical_camera.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
                self.physical_camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
            else:
                self.physical_camera.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
                self.physical_camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

            self.physical_camera.set(cv2.CAP_PROP_FPS, 30)
            self.physical_camera.set(cv2.CAP_PROP_AUTOFOCUS, 1)  # Enable autofocus
            self.physical_camera.set(cv2.CAP_PROP_AUTO_EXPOSURE, 1)  # Enable auto exposure

            # Get actual properties
            actual_width = int(self.physical_camera.get(cv2.CAP_PROP_FRAME_WIDTH))
            actual_height = int(self.physical_camera.get(cv2.CAP_PROP_FRAME_HEIGHT))

            self.logger.info(f"Physical camera ready: {actual_width}x{actual_height}")  # NO EMOJI
            return True

        except Exception as e:
            self.logger.error(f"Physical camera initialization failed: {e}")
            return False

    def _show_startup_summary(self):
        """Show enhanced startup summary with YOUR gestures."""
        summary = f"""
╔══════════════════════════════════════════════════════════════╗
║                    🎭 YOUR EMOTESTREAM READY! 🎭             ║
╠══════════════════════════════════════════════════════════════╣
║ 📺 Virtual Camera: {self.virtual_camera.active_backend:<30} ║
║ 🎥 Resolution: {self.virtual_camera.width}x{self.virtual_camera.height} @ 30fps                     ║
║ 🎭 YOUR Emotes: {len(self.emotes):<36} ║
║ 🔊 Audio System: Ready                                      ║
║ 🤖 AI Detection: Active                                     ║
╠══════════════════════════════════════════════════════════════╣
║                       🎮 YOUR CONTROLS                      ║
║ q = Quit          │ d = Debug        │ s = Stats           ║
║ r = Reload        │ h = Help         │ m = Minimize        ║
║ b = Toggle Brand  │ p = Toggle Preview                     ║
╠══════════════════════════════════════════════════════════════╣
║                      🎭 YOUR GESTURES                       ║
║ 👋 Hands Up       🤲 Hands on Head    🎻 Violin Gesture     ║
║ ✌️ Peace Out       🖕 Middle Finger    🔫 Shot in Head       ║
║ Hold each gesture for 1 second to trigger!                 ║
╠══════════════════════════════════════════════════════════════╣
║ 📺 DISCORD SETUP:                                           ║
║ Settings → Voice & Video → Camera                           ║
║ Select: "EmoteStream Virtual Camera"                        ║
╚══════════════════════════════════════════════════════════════╝
        """
        print(summary)

    def run(self):
        """Enhanced main application loop for YOUR gestures."""
        try:
            if not self.initialize():
                self.logger.error("Initialization failed")  # NO EMOJI
                input("Press Enter to exit...")
                return

            self.running = True
            self.start_time = time.time()

            print("\n🚀 YOUR EmoteStream is now running!")
            print("📺 Virtual camera is available in Discord and other apps")
            print("🎭 Try YOUR custom gestures in front of your camera!")

            while self.running:
                if not self._process_frame():
                    break

                # Enhanced keyboard handling
                self._handle_keyboard_input()

                # Periodic health checks
                self._perform_health_checks()

        except KeyboardInterrupt:
            self.logger.info("Application interrupted by user")  # NO EMOJI
        except Exception as e:
            self.logger.error(f"Unexpected error: {e}")  # NO EMOJI
            traceback.print_exc()
        finally:
            self.cleanup()

    def _handle_keyboard_input(self):
        """Enhanced keyboard input handling."""
        key = cv2.waitKey(1) & 0xFF

        if key == ord('q'):
            self.logger.info("Exit requested by user")  # NO EMOJI
            self.running = False
        elif key == ord('r'):
            self._reload_config()
        elif key == ord('s'):
            self._show_enhanced_stats()
        elif key == ord('d'):
            if self.detector:
                self.detector.toggle_debug()
        elif key == ord('h'):
            self._show_help()
        elif key == ord('m'):
            self._toggle_minimize()
        elif key == ord('p'):
            self._toggle_preview()
        elif key == ord('b'):
            self._toggle_branding()
        elif key == ord('c'):
            self._test_virtual_camera()

    def _perform_health_checks(self):
        """Perform periodic system health checks."""
        now = time.time()
        if now - self.last_health_check > 30:  # Every 30 seconds
            self.last_health_check = now

            # Check virtual camera health
            if self.auto_reconnect and self.virtual_camera and not self.virtual_camera.is_open:
                self.logger.warning("Virtual camera disconnected, attempting reconnect...")  # NO EMOJI
                self._reconnect_virtual_camera()

    def _reconnect_virtual_camera(self):
        """Attempt to reconnect virtual camera."""
        try:
            if self.virtual_camera:
                self.virtual_camera.close()

            time.sleep(2)  # Wait before reconnecting

            if self.virtual_camera.open():
                self.logger.info("Virtual camera reconnected successfully")  # NO EMOJI
            else:
                self.logger.error("Failed to reconnect virtual camera")  # NO EMOJI

        except Exception as e:
            self.logger.error(f"Virtual camera reconnection failed: {e}")  # NO EMOJI

    def _process_frame(self) -> bool:
        """Enhanced frame processing with YOUR gesture detection."""
        try:
            # Read frame from physical camera
            ret, frame = self.physical_camera.read()
            if not ret:
                self.error_count += 1
                if self.error_count > 10:
                    self.logger.error("Too many camera read errors")  # NO EMOJI
                    return False
                return True

            self.error_count = 0  # Reset error count on successful read
            self.frame_count += 1

            # Mirror effect
            frame = cv2.flip(frame, 1)

            # Process frame for YOUR emote detection
            results = self.detector.process_frame(frame)
            self._last_results = results  # Store for preview
            emote_detected, status = self.detector.detect_emote_with_status(results)

            # Handle emote detection
            if emote_detected:
                self.detection_count += 1
                self.logger.info(f"YOUR emote detected: {emote_detected['name']} (#{self.detection_count})")  # NO EMOJI
                self._handle_emote_detection(emote_detected)
                return True

            # Prepare output frame
            output_frame = self._prepare_output_frame(frame.copy())

            # Send to virtual camera
            if not self.virtual_camera.send_frame(output_frame):
                self.logger.warning("Failed to send frame to virtual camera")  # NO EMOJI

            # Show preview if enabled
            if self.show_preview and not self.minimized:
                self._show_enhanced_preview(frame, status, emote_detected)

            return True

        except Exception as e:
            self.logger.error(f"Frame processing error: {e}")  # NO EMOJI
            return True  # Continue running despite errors

    def _prepare_output_frame(self, frame):
        """Enhanced frame preparation with quality improvements."""
        try:
            # Apply quality enhancements
            if self.config_manager.settings.get("video_quality") == "HD":
                # Enhance image quality for HD
                frame = cv2.GaussianBlur(frame, (1, 1), 0)  # Subtle smoothing

            # Add branding if enabled
            if self.branding_enabled:
                frame = self._add_enhanced_branding(frame)

            return frame

        except Exception as e:
            self.logger.error(f"Frame preparation error: {e}")  # NO EMOJI
            return frame

    def _add_enhanced_branding(self, frame):
        """Enhanced branding with better visual design."""
        try:
            # More sophisticated branding
            brand_text = "EmoteStream"
            font = cv2.FONT_HERSHEY_SIMPLEX
            font_scale = 0.7
            thickness = 2

            # Get text dimensions
            (text_width, text_height), baseline = cv2.getTextSize(brand_text, font, font_scale, thickness)

            # Position in bottom-right with padding
            padding = 25
            x = frame.shape[1] - text_width - padding
            y = frame.shape[0] - padding

            # Create modern semi-transparent background
            overlay = frame.copy()

            # Rounded rectangle background (approximation)
            bg_padding = 12
            cv2.rectangle(overlay,
                          (x - bg_padding, y - text_height - bg_padding),
                          (x + text_width + bg_padding, y + bg_padding),
                          (30, 30, 30), -1)

            # Blend overlay
            alpha = 0.6
            cv2.addWeighted(overlay, alpha, frame, 1 - alpha, 0, frame)

            # Add text with subtle shadow
            cv2.putText(frame, brand_text, (x + 1, y + 1), font, font_scale, (0, 0, 0), thickness)  # Shadow
            cv2.putText(frame, brand_text, (x, y), font, font_scale, (255, 255, 255), thickness)  # Main text

            # Add status indicator
            if self.is_playing_emote:
                cv2.circle(frame, (x - 20, y - text_height // 2), 6, (0, 255, 0), -1)  # Green dot when playing

            return frame

        except Exception as e:
            self.logger.error(f"Branding error: {e}")  # NO EMOJI
            return frame

    def _show_enhanced_preview(self, frame, status, emote_detected):
        """Enhanced preview window for YOUR gestures."""
        try:
            if not self.show_preview:
                return

            preview = frame.copy()

            # Draw landmarks if available
            if self._last_results and hasattr(self.detector, 'draw_pose_landmarks'):
                self.detector.draw_pose_landmarks(preview, self._last_results)

            # Modern UI overlay
            overlay = preview.copy()

            # Top status bar
            cv2.rectangle(overlay, (0, 0), (preview.shape[1], 120), (20, 20, 20), -1)
            alpha = 0.8
            cv2.addWeighted(overlay, alpha, preview, 1 - alpha, 0, preview)

            # Status information
            if status:
                # Progress bar
                bar_width = 250
                bar_height = 8
                progress = int(status['progress'] * bar_width)

                # Background bar
                cv2.rectangle(preview, (15, 45), (15 + bar_width, 45 + bar_height), (100, 100, 100), -1)
                # Progress bar
                cv2.rectangle(preview, (15, 45), (15 + progress, 45 + bar_height), (0, 255, 0), -1)

                # Status text
                cv2.putText(preview, status['text'], (15, 35),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)

            if emote_detected:
                cv2.putText(preview, f"🎉 {emote_detected['name'].upper()}", (15, 90),
                            cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 255, 0), 3)

            # YOUR gestures info
            gesture_icons = "👋🤲🎻✌️🖕🔫"
            cv2.putText(preview, f"YOUR Gestures: {gesture_icons}", (15, 110),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (200, 200, 200), 1)

            # System info
            info_text = [
                f"FPS: {self._calculate_fps():.1f}",
                f"YOUR Detections: {self.detection_count}",
                f"Camera: {self.virtual_camera.active_backend if self.virtual_camera else 'None'}"
            ]

            for i, text in enumerate(info_text):
                cv2.putText(preview, text, (preview.shape[1] - 200, 20 + i * 20),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 255, 255), 1)

            cv2.imshow("🎭 YOUR EmoteStream 2.0 - Preview", preview)

        except Exception as e:
            self.logger.error(f"Preview error: {e}")  # NO EMOJI

    def _handle_emote_detection(self, emote_detected):
        """Enhanced emote detection handling for YOUR gestures."""
        now = time.time()

        # Check cooldown
        if (now - self.last_triggered) < self.cooldown:
            return

        self.logger.info(f"Triggering YOUR emote: {emote_detected['name']}")  # NO EMOJI
        self._play_emote_enhanced(emote_detected)
        self.last_triggered = now

    def _play_emote_enhanced(self, emote_detected):
        """Enhanced emote playback - ONLY MP4 (no separate audio)."""
        try:
            emote_name = emote_detected['name']
            video_path = emote_detected.get('video_path')

            # Only check for video path (no separate audio)
            if not video_path:
                self.logger.error(f"Missing video path for YOUR emote: {emote_name}")  # NO EMOJI
                return

            # Validate video file exists
            if not Path(video_path).exists():
                self.logger.error(f"Video file not found: {video_path}")  # NO EMOJI
                return

            self.is_playing_emote = True
            self.current_emote = emote_name

            self.logger.info(f"Loading YOUR emote: {video_path}")  # NO EMOJI

            # ONLY video loading (MP4 with embedded audio)
            video_cap = cv2.VideoCapture(video_path)
            if not video_cap.isOpened():
                self.logger.error(f"Cannot open YOUR video: {video_path}")  # NO EMOJI
                self.is_playing_emote = False
                return

            # Get video properties
            fps = video_cap.get(cv2.CAP_PROP_FPS) or 30
            total_frames = int(video_cap.get(cv2.CAP_PROP_FRAME_COUNT))
            duration = total_frames / fps if fps > 0 else 0

            self.logger.info(
                f"Playing YOUR emote: {fps:.1f} FPS, {total_frames} frames, {duration:.1f}s duration")  # NO EMOJI

            frame_delay = 1.0 / fps
            start_time = time.time()
            frame_count = 0

            # Enhanced video playback loop (no audio sync needed)
            while video_cap.isOpened() and self.is_playing_emote and self.running:
                ret, frame = video_cap.read()
                if not ret:
                    self.logger.info("YOUR video playback completed")  # NO EMOJI
                    break

                # Enhanced frame processing
                frame = cv2.resize(frame, (self.virtual_camera.width, self.virtual_camera.height))
                frame = cv2.flip(frame, 1)

                # Add enhanced branding to video
                if self.branding_enabled:
                    frame = self._add_enhanced_branding(frame.copy())

                # Send to virtual camera
                if not self.virtual_camera.send_frame(frame):
                    self.logger.warning("Failed to send YOUR video frame")  # NO EMOJI

                # Enhanced preview during video playback
                if self.show_preview and not self.minimized:
                    self._show_video_preview(frame, emote_name, frame_count, total_frames)

                # Handle keyboard input during video
                key = cv2.waitKey(1) & 0xFF
                if key == ord('q'):
                    self.logger.info("YOUR video playback interrupted by user")  # NO EMOJI
                    self.running = False
                    break
                elif key == ord(' '):  # Spacebar to skip
                    self.logger.info("YOUR video skipped by user")  # NO EMOJI
                    break

                # Precise frame timing
                frame_count += 1
                expected_time = frame_count * frame_delay
                elapsed_time = time.time() - start_time
                sleep_time = expected_time - elapsed_time

                if sleep_time > 0:
                    time.sleep(sleep_time)

            # Cleanup (no audio to stop)
            video_cap.release()
            self.is_playing_emote = False
            self.current_emote = None

            self.logger.info(f"YOUR emote '{emote_name}' playback completed")  # NO EMOJI

        except Exception as e:
            self.logger.error(f"YOUR emote playback error: {e}")  # NO EMOJI
            traceback.print_exc()
            self.is_playing_emote = False
            self.current_emote = None

    def _show_video_preview(self, frame, emote_name, current_frame, total_frames):
        """Show enhanced preview during YOUR video playback."""
        try:
            preview = frame.copy()

            # Video progress overlay
            overlay = preview.copy()
            cv2.rectangle(overlay, (0, 0), (preview.shape[1], 100), (20, 20, 20), -1)
            alpha = 0.8
            cv2.addWeighted(overlay, alpha, preview, 1 - alpha, 0, preview)

            # Video info
            cv2.putText(preview, f"🎬 PLAYING YOUR: {emote_name.upper()}", (15, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 255, 0), 2)

            # Progress bar
            if total_frames > 0:
                progress = current_frame / total_frames
                bar_width = 300
                bar_height = 8
                progress_pixels = int(progress * bar_width)

                cv2.rectangle(preview, (15, 50), (15 + bar_width, 50 + bar_height), (100, 100, 100), -1)
                cv2.rectangle(preview, (15, 50), (15 + progress_pixels, 50 + bar_height), (0, 255, 0), -1)

                cv2.putText(preview, f"{current_frame}/{total_frames} ({progress * 100:.1f}%)", (15, 75),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 255, 255), 1)

            # Controls info
            cv2.putText(preview, "Press SPACE to skip, Q to quit", (15, preview.shape[0] - 15),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.4, (200, 200, 200), 1)

            cv2.imshow("🎭 YOUR EmoteStream 2.0 - Preview", preview)

        except Exception as e:
            self.logger.error(f"Video preview error: {e}")  # NO EMOJI

    def _show_enhanced_stats(self):
        """Show comprehensive application statistics."""
        runtime = time.time() - self.start_time if self.start_time else 0
        fps = self._calculate_fps()

        stats = f"""
╔══════════════════════════════════════════════════════════════╗
║                📊 YOUR EMOTESTREAM STATISTICS                ║
╠══════════════════════════════════════════════════════════════╣
║ ⏱️  Runtime: {runtime / 60:.1f} minutes ({runtime:.1f}s)                    ║
║ 🎬 Frames processed: {self.frame_count:,}                            ║
║ 📈 Average FPS: {fps:.1f}                                     ║
║ 🎯 YOUR detections: {self.detection_count}                            ║
║ ❌ Error count: {self.error_count}                                ║
║ 🎭 Current emote: {(self.current_emote or 'None'):<25} ║
║ 🎮 Playing emote: {('Yes' if self.is_playing_emote else 'No'):<25} ║
╠══════════════════════════════════════════════════════════════╣
║                    🎥 VIRTUAL CAMERA INFO                    ║
║ Backend: {(self.virtual_camera.active_backend if self.virtual_camera else 'None'):<40} ║
║ Resolution: {self.virtual_camera.width if self.virtual_camera else 0}x{self.virtual_camera.height if self.virtual_camera else 0} @ 30fps                           ║
║ Frames sent: {self.virtual_camera.frame_count if self.virtual_camera else 0:,}                                 ║
║ Status: {('Connected' if self.virtual_camera and self.virtual_camera.is_open else 'Disconnected'):<40} ║
╠══════════════════════════════════════════════════════════════╣
║                      ⚙️  SETTINGS                           ║
║ Quality: {self.config_manager.settings.get('video_quality', 'Unknown'):<40} ║
║ Hold time: {self.config_manager.settings.get('hold_time', 0):.1f}s                               ║
║ Cooldown: {self.cooldown:.1f}s                                   ║
║ Auto-reconnect: {('Yes' if self.auto_reconnect else 'No'):<25} ║
║ Branding: {('Enabled' if self.branding_enabled else 'Disabled'):<30} ║
║ Preview: {('Shown' if self.show_preview else 'Hidden'):<30} ║
╚══════════════════════════════════════════════════════════════╝
        """
        print(stats)

    def _show_help(self):
        """Show comprehensive help information for YOUR gestures."""
        help_text = f"""
╔══════════════════════════════════════════════════════════════╗
║                     🆘 YOUR EMOTESTREAM HELP                 ║
╠══════════════════════════════════════════════════════════════╣
║                        🎮 CONTROLS                          ║
║ q = Quit application     │ d = Toggle debug mode           ║
║ s = Show statistics      │ r = Reload configuration        ║
║ h = Show this help       │ m = Minimize/restore window     ║
║ p = Toggle preview       │ b = Toggle branding             ║
║ c = Test virtual camera  │ SPACE = Skip current video      ║
╠══════════════════════════════════════════════════════════════╣
║                      🎭 YOUR GESTURES                       ║
║ 👋 Hands Up: Raise both hands above your head              ║
║    - Keep arms separated and clearly visible               ║
║    - Hold position for {self.config_manager.settings.get('hold_time', 1.0)} second                         ║
║                                                              ║
║ 🤲 Hands on Head: Place hands on/near your ears            ║
║    - Cover ears or touch sides of head                     ║
║    - Hold position for {self.config_manager.settings.get('hold_time', 1.0)} second                         ║
║                                                              ║
║ 🎻 Violin Gesture: One hand up (bow), one extended (neck)   ║
║    - Right hand up + left extended OR left up + right ext  ║
║    - Hold position for {self.config_manager.settings.get('hold_time', 1.0)} second                         ║
║                                                              ║
║ ✌️ Peace Out: V sign with index and middle finger          ║
║    - Only index and middle extended, others folded         ║
║    - Hold position for {self.config_manager.settings.get('hold_time', 1.0)} second                         ║
║                                                              ║
║ 🖕 Middle Finger: Extend only middle finger                ║
║    - Middle finger up, all other fingers folded            ║
║    - Hold position for {self.config_manager.settings.get('hold_time', 1.0)} second                         ║
║                                                              ║
║ 🔫 Shot in Head: Hand near temple/side of head             ║
║    - Place hand close to ear/temple area                   ║
║    - Hold position for {self.config_manager.settings.get('hold_time', 1.0)} second                         ║
╠══════════════════════════════════════════════════════════════╣
║                    📺 DISCORD SETUP                         ║
║ 1. Open Discord Settings                                    ║
║ 2. Go to Voice & Video                                      ║
║ 3. Select Camera: "EmoteStream Virtual Camera"              ║
║ 4. Test with video call or camera preview                   ║
╠══════════════════════════════════════════════════════════════╣
║                      🔧 TROUBLESHOOTING                     ║
║ • Camera not detected: Check if another app is using it    ║
║ • Virtual camera missing: Restart Discord/streaming app    ║
║ • Gestures not working: Ensure good lighting & visibility  ║
║ • Hand gestures not detected: Make clear finger positions  ║
║ • Performance issues: Lower quality in settings.json       ║
╚══════════════════════════════════════════════════════════════╝
        """
        print(help_text)

    def _toggle_minimize(self):
        """Toggle window minimization."""
        self.minimized = not self.minimized
        if self.minimized:
            cv2.destroyAllWindows()
            print("🪟 Preview minimized - press 'm' to restore")
        else:
            print("🪟 Preview restored")
        self.logger.info(f"Preview {'minimized' if self.minimized else 'restored'}")

    def _toggle_preview(self):
        """Toggle preview window visibility."""
        self.show_preview = not self.show_preview
        if not self.show_preview:
            cv2.destroyAllWindows()
        self.logger.info(f"Preview {'enabled' if self.show_preview else 'disabled'}")
        print(f"🪟 Preview {'enabled' if self.show_preview else 'disabled'}")

    def _toggle_branding(self):
        """Toggle branding watermark."""
        self.branding_enabled = not self.branding_enabled
        self.config_manager.settings["branding_enabled"] = self.branding_enabled
        self.config_manager.save_settings()
        self.logger.info(f"Branding {'enabled' if self.branding_enabled else 'disabled'}")
        print(f"🏷️ Branding {'enabled' if self.branding_enabled else 'disabled'}")

    def _test_virtual_camera(self):
        """Test virtual camera with test pattern."""
        if self.virtual_camera:
            print("🧪 Testing virtual camera...")
            self.virtual_camera.test_camera()
        else:
            print("❌ Virtual camera not available")

    def _reload_config(self):
        """Enhanced configuration reloading for YOUR gestures."""
        try:
            self.logger.info("Reloading YOUR configuration...")  # NO EMOJI
            print("🔄 Reloading YOUR configuration...")

            # Backup current config
            old_emotes = self.emotes.copy()
            old_settings = self.config_manager.settings.copy()

            # Reload emote config
            if self._load_config():
                # Update detector with YOUR gestures
                hold_time = self.config_manager.settings.get("hold_time", 1.0)
                self.detector = EmoteDetector(
                    emote_configs=self.emotes,
                    hold_time=hold_time,
                    cooldown_time=self.cooldown,
                    logger=self.logger
                )

                # Reload user settings
                self.config_manager.settings = self.config_manager.load_settings()
                self.cooldown = self.config_manager.settings.get("cooldown_time", 2.0)
                self.branding_enabled = self.config_manager.settings.get("branding_enabled", True)

                self.logger.info("YOUR configuration reloaded successfully")  # NO EMOJI
                print("✅ YOUR configuration reloaded successfully")
                print(f"📄 Loaded {len(self.emotes)} YOUR emotes: {list(self.emotes.keys())}")
            else:
                # Restore backup on failure
                self.emotes = old_emotes
                self.config_manager.settings = old_settings
                self.logger.error("Failed to reload YOUR configuration, restored backup")  # NO EMOJI
                print("❌ Failed to reload YOUR configuration, restored backup")

        except Exception as e:
            self.logger.error(f"YOUR configuration reload error: {e}")  # NO EMOJI
            print(f"❌ YOUR configuration reload error: {e}")

    def _calculate_fps(self) -> float:
        """Calculate current FPS."""
        if self.start_time and self.frame_count > 0:
            elapsed = time.time() - self.start_time
            return self.frame_count / elapsed if elapsed > 0 else 0
        return 0

    def _on_video_error(self, error):
        """Enhanced video error handling."""
        self.logger.error(f"Video player error: {error}")  # NO EMOJI
        self.current_emote = None
        self.is_playing_emote = False
        print(f"❌ Video error: {error}")

    def cleanup(self):
        """Enhanced cleanup with progress indication."""
        print("\n🧹 Cleaning up YOUR EmoteStream...")
        self.logger.info("Starting YOUR application cleanup...")  # NO EMOJI

        self.running = False
        self.is_playing_emote = False

        # Stop background services
        if hasattr(self, 'tray_manager'):
            print("  🔄 Stopping background services...")
            self.tray_manager.stop()

        # Save settings
        if hasattr(self, 'config_manager'):
            print("  💾 Saving YOUR settings...")
            self.config_manager.save_settings()

        # Stop audio (if any)
        try:
            print("  🔇 Stopping audio...")
            pygame.mixer.music.stop()
            pygame.mixer.quit()
        except:
            pass

        # Cleanup cameras
        if self.physical_camera:
            print("  📹 Releasing physical camera...")
            self.physical_camera.release()

        if self.virtual_camera:
            print("  🎥 Closing virtual camera...")
            self.virtual_camera.close()

        # Close windows
        print("  🪟 Closing windows...")
        cv2.destroyAllWindows()

        # Final statistics
        if self.start_time:
            runtime = time.time() - self.start_time
            print(f"\n📊 YOUR SESSION SUMMARY:")
            print(f"  Runtime: {runtime / 60:.1f} minutes")
            print(f"  Frames processed: {self.frame_count:,}")
            print(f"  YOUR detections: {self.detection_count}")
            print(f"  Average FPS: {self._calculate_fps():.1f}")

        self.logger.info("YOUR application cleanup completed")  # NO EMOJI
        print("✅ YOUR EmoteStream cleanup completed")
        print("👋 Thank you for using YOUR EmoteStream!")


def main():
    """Enhanced application entry point for YOUR gestures."""
    # Clear screen and show banner
    os.system('cls' if os.name == 'nt' else 'clear')
    show_startup_banner()

    try:
        # Interactive startup
        print("\n🚀 Welcome to YOUR EmoteStream 2.0!")
        print("AI-powered gesture recognition with YOUR custom gestures")

        # Quick test option
        test_first = input("\n🧪 Would you like to test YOUR gesture detection first? (y/N): ").lower().strip()
        if test_first in ['y', 'yes']:
            print("\n" + "=" * 60)
            if simple_detector_test():
                print("\n✅ YOUR detector test completed successfully!")

                # Ask about continuing
                continue_app = input("\n➡️  Continue to main application? (Y/n): ").lower().strip()
                if continue_app in ['n', 'no']:
                    print("👋 Thanks for testing YOUR EmoteStream!")
                    return 0
            else:
                print("\n❌ YOUR detector test failed!")
                input("Press Enter to continue anyway, or Ctrl+C to exit...")

        print("\n" + "=" * 60)
        print("🚀 Starting YOUR EmoteStream 2.0...")

        # Create and run application
        app = EmoteStreamApp(config_path="emotes/emotes.yaml")
        app.run()

    except KeyboardInterrupt:
        print("\n👋 Application interrupted by user")
    except Exception as e:
        print(f"\n💥 Fatal error: {e}")
        traceback.print_exc()
        input("\nPress Enter to exit...")
        return 1

    return 0


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)