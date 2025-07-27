import yaml
import os
from pathlib import Path
from typing import Dict, Any, Optional
import logging

class ConfigLoader:
    def __init__(self, logger: Optional[logging.Logger] = None):
        self.logger = logger or logging.getLogger(__name__)
    
    def load_config(self, path: str = "emotes.yaml") -> Dict[str, Any]:
        """Load and validate emote configuration from YAML file."""
        try:
            config_path = Path(path)
            
            # Check if file exists
            if not config_path.exists():
                raise FileNotFoundError(f"Configuration file not found: {path}")
            
            # Load YAML
            with open(config_path, "r", encoding='utf-8') as f:
                config = yaml.safe_load(f)
            
            # Validate configuration
            validated_config = self._validate_config(config)
            self.logger.info(f"Successfully loaded {len(validated_config)} emote configurations")
            
            return validated_config
            
        except yaml.YAMLError as e:
            self.logger.error(f"YAML parsing error: {e}")
            raise
        except Exception as e:
            self.logger.error(f"Error loading configuration: {e}")
            raise
    
    def _validate_config(self, config: Dict[str, Any]) -> Dict[str, Any]:
        """Validate emote configuration structure and file paths."""
        validated = {}
        
        for emote_name, emote_data in config.items():
            try:
                # Validate required fields
                if 'gesture' not in emote_data:
                    raise ValueError(f"Missing 'gesture' field for emote '{emote_name}'")
                
                if 'video_path' not in emote_data:
                    raise ValueError(f"Missing 'video_path' field for emote '{emote_name}'")
                
                if 'audio_path' not in emote_data:
                    raise ValueError(f"Missing 'audio_path' field for emote '{emote_name}'")
                
                # Validate file paths exist
                video_path = Path(emote_data['video_path'])
                audio_path = Path(emote_data['audio_path'])
                
                if not video_path.exists():
                    self.logger.warning(f"Video file not found for '{emote_name}': {video_path}")
                
                if not audio_path.exists():
                    self.logger.warning(f"Audio file not found for '{emote_name}': {audio_path}")
                
                # Validate gesture type
                gesture_type = emote_data['gesture'].get('type')
                if gesture_type not in ['hands_up', 'hands_on_head']:
                    self.logger.warning(f"Unknown gesture type for '{emote_name}': {gesture_type}")
                
                validated[emote_name] = emote_data
                
            except Exception as e:
                self.logger.error(f"Validation failed for emote '{emote_name}': {e}")
                continue
        
        return validated
    
    def reload_config(self, path: str = "emotes.yaml") -> Dict[str, Any]:
        """Reload configuration (useful for runtime updates)."""
        return self.load_config(path)