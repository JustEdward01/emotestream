import sys
import traceback
import pygame
import cv2
import time
from typing import Optional, Callable
from enum import Enum
import logging
from pathlib import Path

class PlayerState(Enum):
    IDLE = "idle"
    PLAYING = "playing"
    PAUSED = "paused"
    STOPPED = "stopped"
    ERROR = "error"

class VideoAudioPlayer:
    """Handles synchronized video and audio playback."""
    
    def __init__(self, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
        self.state = PlayerState.IDLE
        self.video_cap = None
        self.audio_initialized = False
        self.fps = 30
        self.frame_delay = 1.0 / self.fps
        self._error_callback: Optional[Callable] = None
        
    def initialize_audio(self):
        """Initialize pygame mixer for audio."""
        if not self.audio_initialized:
            try:
                pygame.mixer.init()
                self.audio_initialized = True
                self.logger.info("Audio system initialized")
            except Exception as e:
                self.logger.error(f"Failed to initialize audio: {e}")
                raise
    
    def load_media(self, video_path: str, audio_path: str) -> bool:
        """Load video and audio files."""
        try:
            # Validate file paths
            if not Path(video_path).exists():
                raise FileNotFoundError(f"Video file not found: {video_path}")
            if not Path(audio_path).exists():
                raise FileNotFoundError(f"Audio file not found: {audio_path}")
            
            # Load video
            self.video_cap = cv2.VideoCapture(video_path)
            if not self.video_cap.isOpened():
                raise ValueError(f"Cannot open video file: {video_path}")
            
            # Get video properties
            self.fps = self.video_cap.get(cv2.CAP_PROP_FPS)
            if self.fps == 0:
                self.fps = 30  # fallback
                self.logger.warning(f"Could not detect FPS for {video_path}, using 30 FPS")
            
            self.frame_delay = 1.0 / self.fps
            
            # Load audio
            self.initialize_audio()
            pygame.mixer.music.load(audio_path)
            
            self.logger.info(f"Media loaded: {video_path} ({self.fps} FPS)")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to load media: {e}")
            self.state = PlayerState.ERROR
            return False
    
    def play(self, vcam, frame_callback: Optional[Callable] = None) -> bool:
        """Play video and audio synchronously."""
        if not self.video_cap or not self.audio_initialized:
            self.logger.error("Media not loaded")
            return False
        
        try:
            self.state = PlayerState.PLAYING
            
            # Start audio
            pygame.mixer.music.play()
            self.logger.info("Starting playback")
            
            # Reset video to beginning
            self.video_cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
            
            frame_count = 0
            start_time = time.time()
            
            while self.state == PlayerState.PLAYING:
                ret, frame = self.video_cap.read()
                if not ret:
                    # End of video
                    break
                
                # Resize frame if needed
                frame = cv2.resize(frame, (640, 480))
                
                # Apply frame callback if provided
                if frame_callback:
                    frame = frame_callback(frame)
                
                # Send to virtual camera
                try:
                    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                    vcam.send(rgb_frame)
                except Exception as e:
                    self.logger.error(f"Error sending frame to virtual camera: {e}")
                    break
                
                # Check if audio is still playing
                if not pygame.mixer.music.get_busy():
                    break
                
                # Frame timing
                frame_count += 1
                expected_time = frame_count * self.frame_delay
                elapsed_time = time.time() - start_time
                sleep_time = expected_time - elapsed_time
                
                if sleep_time > 0:
                    time.sleep(sleep_time)
            
            self.stop()
            self.logger.info("Playback completed")
            return True
            
        except Exception as e:
            self.logger.error(f"Error during playback: {e}")
            self.state = PlayerState.ERROR
            if self._error_callback:
                self._error_callback(e)
            return False
    
    def stop(self):
        """Stop playback."""
        if self.state == PlayerState.PLAYING:
            try:
                pygame.mixer.music.stop()
                if self.video_cap:
                    self.video_cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
                self.state = PlayerState.STOPPED
                self.logger.info("Playback stopped")
            except Exception as e:
                self.logger.error(f"Error stopping playback: {e}")
    
    def pause(self):
        """Pause playback."""
        if self.state == PlayerState.PLAYING:
            pygame.mixer.music.pause()
            self.state = PlayerState.PAUSED
            self.logger.info("Playback paused")
    
    def resume(self):
        """Resume playback."""
        if self.state == PlayerState.PAUSED:
            pygame.mixer.music.unpause()
            self.state = PlayerState.PLAYING
            self.logger.info("Playback resumed")
    
    def cleanup(self):
        """Clean up resources."""
        self.stop()
        if self.video_cap:
            self.video_cap.release()
            self.video_cap = None
        if self.audio_initialized:
            pygame.mixer.quit()
            self.audio_initialized = False
        self.state = PlayerState.IDLE
        self.logger.info("Player resources cleaned up")
    
    def set_error_callback(self, callback: Callable):
        """Set callback for error handling."""
        self._error_callback = callback
    
    @property
    def is_playing(self) -> bool:
        return self.state == PlayerState.PLAYING
    
    @property
    def current_state(self) -> PlayerState:
        return self.state