import pyvirtualcam
import cv2
import numpy as np
from typing import Optional, Tuple
import logging
from enum import Enum

class CameraState(Enum):
    CLOSED = "closed"
    OPEN = "open"
    ERROR = "error"

class VirtualCameraManager:
    """Independent virtual camera management for Discord/streaming apps."""
    
    def __init__(self, width: int = 1280, height: int = 720, fps: int = 30, 
                 device_name: str = None, logger: Optional[logging.Logger] = None):
        self.width = width
        self.height = height
        self.fps = fps
        self.device_name = device_name or "EmoteStream Virtual Camera"
        self.logger = logger or logging.getLogger(__name__)
        
        self.cam: Optional[pyvirtualcam.Camera] = None
        self.state = CameraState.CLOSED
        self._frame_count = 0
        
        # Try different backends for virtual camera
        self.backends = ['obs', 'unitycapture', 'dshow']
        self.active_backend = None
        
    def __enter__(self):
        self.open()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
    
    def open(self) -> bool:
        """Open virtual camera with fallback backends."""
        try:
            if self.state == CameraState.OPEN:
                self.logger.warning("Camera already open")
                return True
            
            # Try different backends until one works
            for backend in self.backends:
                try:
                    self.logger.info(f"Trying backend: {backend}")
                    
                    if backend == 'obs':
                        # Try OBS Virtual Camera (default)
                        self.cam = pyvirtualcam.Camera(
                            width=self.width, 
                            height=self.height, 
                            fps=self.fps
                        )
                    elif backend == 'unitycapture':
                        # Try Unity Capture (Windows DirectShow)
                        self.cam = pyvirtualcam.Camera(
                            width=self.width, 
                            height=self.height, 
                            fps=self.fps,
                            backend='unitycapture'
                        )
                    elif backend == 'dshow':
                        # Try DirectShow backend
                        self.cam = pyvirtualcam.Camera(
                            width=self.width, 
                            height=self.height, 
                            fps=self.fps,
                            backend='dshow'
                        )
                    
                    # Test if camera works
                    test_frame = np.zeros((self.height, self.width, 3), dtype=np.uint8)
                    self.cam.send(test_frame)
                    
                    self.active_backend = backend
                    self.state = CameraState.OPEN
                    self._frame_count = 0
                    
                    device_info = getattr(self.cam, 'device', f'{backend} virtual camera')
                    self.logger.info(f"Virtual camera opened with {backend}: {device_info} ({self.width}x{self.height} @ {self.fps}fps)")
                    
                    # Send initialization frame
                    self._send_init_frame()
                    
                    return True
                    
                except Exception as e:
                    self.logger.warning(f"Backend {backend} failed: {e}")
                    continue
            
            # If all backends failed
            raise RuntimeError("All virtual camera backends failed")
            
        except Exception as e:
            self.logger.error(f"Failed to open virtual camera: {e}")
            self.state = CameraState.ERROR
            return False
    
    def _send_init_frame(self):
        """Send initialization frame to make camera visible in apps."""
        try:
            # Create a branded init frame
            init_frame = np.zeros((self.height, self.width, 3), dtype=np.uint8)
            
            # Background gradient
            for y in range(self.height):
                color_value = int(30 + (y / self.height) * 50)
                init_frame[y, :] = [color_value, color_value//2, color_value//3]
            
            # Main text
            text = "EmoteStream Camera"
            font = cv2.FONT_HERSHEY_SIMPLEX
            font_scale = 2
            thickness = 3
            
            # Center the text
            (text_width, text_height), baseline = cv2.getTextSize(text, font, font_scale, thickness)
            x = (self.width - text_width) // 2
            y = (self.height + text_height) // 2
            
            # Add text with outline
            cv2.putText(init_frame, text, (x+2, y+2), font, font_scale, (0, 0, 0), thickness+2)  # Shadow
            cv2.putText(init_frame, text, (x, y), font, font_scale, (255, 255, 255), thickness)
            
            # Status text
            status_text = f"Ready - {self.width}x{self.height}@{self.fps}fps"
            cv2.putText(init_frame, status_text, (x, y+60), cv2.FONT_HERSHEY_SIMPLEX, 
                       0.8, (200, 200, 200), 2)
            
            # Backend info
            backend_text = f"Backend: {self.active_backend}"
            cv2.putText(init_frame, backend_text, (20, self.height-30), cv2.FONT_HERSHEY_SIMPLEX, 
                       0.6, (150, 150, 150), 1)
            
            # Send the frame
            self.send_frame(init_frame, auto_convert=False)
            
        except Exception as e:
            self.logger.warning(f"Failed to send init frame: {e}")
    
    def send_frame(self, frame: np.ndarray, auto_convert: bool = True) -> bool:
        """Send frame to virtual camera."""
        if self.state != CameraState.OPEN or not self.cam:
            self.logger.error("Camera not open")
            return False
        
        try:
            # Validate and prepare frame
            processed_frame = self._prepare_frame(frame, auto_convert)
            if processed_frame is None:
                return False
            
            # Send frame
            self.cam.send(processed_frame)
            self.cam.sleep_until_next_frame()
            
            self._frame_count += 1
            return True
            
        except Exception as e:
            self.logger.error(f"Error sending frame: {e}")
            return False
    
    def _prepare_frame(self, frame: np.ndarray, auto_convert: bool) -> Optional[np.ndarray]:
        """Prepare frame for virtual camera."""
        try:
            # Check if frame is valid
            if frame is None or frame.size == 0:
                self.logger.warning("Empty frame received")
                return None
            
            # Resize if necessary (high quality interpolation)
            if frame.shape[:2] != (self.height, self.width):
                frame = cv2.resize(frame, (self.width, self.height), 
                                 interpolation=cv2.INTER_LANCZOS4)
            
            # Convert color space if needed
            if auto_convert and len(frame.shape) == 3 and frame.shape[2] == 3:
                # Convert BGR to RGB for virtual camera
                frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            
            # Ensure proper data type
            frame = frame.astype(np.uint8)
            
            # Clamp values to valid range
            frame = np.clip(frame, 0, 255)
            
            return frame
            
        except Exception as e:
            self.logger.error(f"Error preparing frame: {e}")
            return None
    
    def send_blank_frame(self, color: Tuple[int, int, int] = (0, 40, 80)) -> bool:
        """Send a blank frame with specified color."""
        blank_frame = np.full((self.height, self.width, 3), color, dtype=np.uint8)
        
        # Add "No Signal" text
        text = "No Signal"
        font = cv2.FONT_HERSHEY_SIMPLEX
        font_scale = 1.5
        thickness = 2
        
        (text_width, text_height), baseline = cv2.getTextSize(text, font, font_scale, thickness)
        x = (self.width - text_width) // 2
        y = (self.height + text_height) // 2
        
        cv2.putText(blank_frame, text, (x, y), font, font_scale, (255, 255, 255), thickness)
        
        return self.send_frame(blank_frame, auto_convert=False)
    
    def close(self):
        """Close virtual camera."""
        if self.cam:
            try:
                # Send a final frame
                self.send_blank_frame((0, 0, 0))
                
                # Close camera
                self.cam = None
                self.state = CameraState.CLOSED
                self.logger.info(f"Virtual camera closed after {self._frame_count} frames")
            except Exception as e:
                self.logger.error(f"Error closing virtual camera: {e}")
        else:
            self.logger.info("Virtual camera already closed")
    
    @property
    def is_open(self) -> bool:
        return self.state == CameraState.OPEN
    
    @property
    def device_info(self) -> Optional[str]:
        if self.cam:
            return getattr(self.cam, 'device', f'{self.active_backend} virtual camera')
        return None
    
    @property
    def frame_count(self) -> int:
        return self._frame_count
    
    def get_stats(self) -> dict:
        """Get camera statistics."""
        return {
            "state": self.state.value,
            "backend": self.active_backend,
            "resolution": f"{self.width}x{self.height}",
            "fps": self.fps,
            "frames_sent": self._frame_count,
            "device": self.device_info
        }
    
    def test_camera(self) -> bool:
        """Test virtual camera functionality."""
        try:
            if not self.is_open:
                if not self.open():
                    return False
            
            # Send test pattern
            test_frame = self._create_test_pattern()
            result = self.send_frame(test_frame, auto_convert=False)
            
            self.logger.info(f"Camera test {'passed' if result else 'failed'}")
            return result
            
        except Exception as e:
            self.logger.error(f"Camera test failed: {e}")
            return False
    
    def _create_test_pattern(self) -> np.ndarray:
        """Create a test pattern frame."""
        frame = np.zeros((self.height, self.width, 3), dtype=np.uint8)
        
        # Color bars
        bar_width = self.width // 7
        colors = [
            (255, 255, 255),  # White
            (255, 255, 0),    # Yellow
            (0, 255, 255),    # Cyan
            (0, 255, 0),      # Green
            (255, 0, 255),    # Magenta
            (255, 0, 0),      # Red
            (0, 0, 255),      # Blue
        ]
        
        for i, color in enumerate(colors):
            x1 = i * bar_width
            x2 = min((i + 1) * bar_width, self.width)
            frame[:, x1:x2] = color
        
        # Add test text
        cv2.putText(frame, "EmoteStream Test Pattern", (50, 100), 
                   cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 0, 0), 3)
        
        return frame