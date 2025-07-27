import mediapipe as mp
import cv2
import time
import numpy as np

class EmoteDetector:
    def __init__(self, emote_configs, hold_time=0.8, cooldown_time=1.5, logger=None):
        self.emote_configs = emote_configs
        self.hold_time = hold_time  # Reduced for faster response
        self.cooldown_time = cooldown_time
        self.logger = logger
        
        # MediaPipe setup - Relaxed settings for better detection
        self.mp_pose = mp.solutions.pose
        self.mp_hands = mp.solutions.hands
        
        # Initialize detectors with more relaxed settings
        self.pose = self.mp_pose.Pose(
            static_image_mode=False, 
            model_complexity=1, 
            min_detection_confidence=0.6,  # Reduced for easier detection
            min_tracking_confidence=0.4    # Reduced for stability
        )
        
        self.hands = self.mp_hands.Hands(
            static_image_mode=False,
            max_num_hands=2,
            min_detection_confidence=0.6,  # Reduced for easier detection
            min_tracking_confidence=0.4    # Reduced for stability
        )
        
        self.mp_drawing = mp.solutions.drawing_utils

        # State tracking
        self.last_triggered = None
        self.last_emote_type = None
        self.detection_start_time = None
        self.active_candidate = None
        
        # Debug mode
        self.debug_mode = True
        self._debug_frame_count = 0
        
        # Gesture stability tracking - reduced for faster response
        self.gesture_history = []
        self.history_size = 2  # Reduced from 3 to 2

    def process_frame(self, frame):
        """Process frame with MediaPipe solutions"""
        image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        
        # Process with both detectors
        pose_results = self.pose.process(image_rgb)
        hands_results = self.hands.process(image_rgb)
        
        # Return combined results
        return {
            'pose': pose_results,
            'hands': hands_results
        }

    def draw_pose_landmarks(self, frame, results):
        """Draw landmarks for debugging (compatible with old interface)"""
        if 'pose' in results and results['pose'].pose_landmarks:
            self.mp_drawing.draw_landmarks(
                frame,
                results['pose'].pose_landmarks,
                self.mp_pose.POSE_CONNECTIONS,
                landmark_drawing_spec=self.mp_drawing.DrawingSpec(color=(0, 255, 0), thickness=2, circle_radius=3),
                connection_drawing_spec=self.mp_drawing.DrawingSpec(color=(255, 0, 0), thickness=2)
            )
        
        # Draw hand landmarks
        if 'hands' in results and results['hands'].multi_hand_landmarks:
            for hand_landmarks in results['hands'].multi_hand_landmarks:
                self.mp_drawing.draw_landmarks(
                    frame, hand_landmarks, self.mp_hands.HAND_CONNECTIONS,
                    landmark_drawing_spec=self.mp_drawing.DrawingSpec(color=(255, 255, 0), thickness=2, circle_radius=2),
                    connection_drawing_spec=self.mp_drawing.DrawingSpec(color=(0, 255, 255), thickness=2)
                )
        
        # Debug: Draw key points with labels
        if self.debug_mode:
            self._draw_debug_info(frame, results)

    def _draw_debug_info(self, frame, results):
        """Draw debug information on frame"""
        h, w, _ = frame.shape
        
        # Pose key points
        if 'pose' in results and results['pose'].pose_landmarks:
            landmarks = results['pose'].pose_landmarks.landmark
            key_points = {
                'L_WRIST': self.mp_pose.PoseLandmark.LEFT_WRIST,
                'R_WRIST': self.mp_pose.PoseLandmark.RIGHT_WRIST,
                'NOSE': self.mp_pose.PoseLandmark.NOSE,
                'L_EYE': self.mp_pose.PoseLandmark.LEFT_EYE,
                'R_EYE': self.mp_pose.PoseLandmark.RIGHT_EYE,
            }
            
            for name, landmark_id in key_points.items():
                landmark = landmarks[landmark_id]
                if landmark.visibility > 0.3:  # Reduced threshold
                    x = int(landmark.x * w)
                    y = int(landmark.y * h)
                    cv2.circle(frame, (x, y), 8, (255, 255, 0), -1)
                    cv2.putText(frame, name, (x+10, y), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 255, 0), 1)

    def detect_emote_with_status(self, results):
        """Detect emotes with multiple gesture types - improved responsiveness"""
        detected = None
        current_gesture = None
        
        # Check all gesture types
        for emote_name, emote_config in self.emote_configs.items():
            gesture = emote_config.get('gesture', {})
            gesture_type = gesture.get('type')
            
            if self._check_gesture(gesture_type, results):
                current_gesture = gesture_type
                detected = emote_config.copy()
                detected['name'] = emote_name
                break
        
        # Add to gesture history for stability
        self.gesture_history.append(current_gesture)
        if len(self.gesture_history) > self.history_size:
            self.gesture_history.pop(0)
        
        # Check if gesture is stable (appears in majority of recent frames)
        stable_gesture = None
        if current_gesture:
            recent_count = self.gesture_history.count(current_gesture)
            if recent_count >= 1:  # Even 1 detection is enough now
                stable_gesture = current_gesture

        # Handle timing logic
        if stable_gesture:
            now = time.time()

            if self.active_candidate != stable_gesture:
                self.active_candidate = stable_gesture
                self.detection_start_time = now
                if self.debug_mode:
                    print(f"[DEBUG] Started detecting {stable_gesture}")
                return None, {
                    "text": f"{stable_gesture}... (0.0s / {self.hold_time}s)",
                    "progress": 0.0,
                    "ready": False
                }

            elapsed = now - self.detection_start_time
            progress = min(elapsed / self.hold_time, 1.0)
            status = {
                "text": f"{stable_gesture}... ({elapsed:.1f}s / {self.hold_time}s)",
                "progress": progress,
                "ready": progress >= 1.0
            }

            if progress >= 1.0:
                if (self.last_emote_type != stable_gesture or 
                    (self.last_triggered and now - self.last_triggered > self.cooldown_time)):
                    self.last_triggered = now
                    self.last_emote_type = stable_gesture
                    self.active_candidate = None
                    self.gesture_history.clear()
                    if self.debug_mode:
                        print(f"[DEBUG] ✓ EMOTE TRIGGERED: {detected['name']}")
                    return detected, None

            return None, status
        else:
            self.active_candidate = None
            return None, None

    def _check_gesture(self, gesture_type, results):
        """Check specific gesture type"""
        try:
            # Original gestures (pose-based)
            if gesture_type == 'hands_up':
                return self._detect_hands_up(results)
            elif gesture_type == 'hands_on_head':
                return self._detect_hands_on_head(results)
            
            # NEW: Your custom gestures - IMPROVED
            elif gesture_type == 'violin_gesture':
                return self._detect_violin_gesture(results)
            elif gesture_type == 'peace_out':
                return self._detect_peace_out_improved(results)
            elif gesture_type == 'middle_finger':
                return self._detect_middle_finger_improved(results)
            elif gesture_type == 'shot_in_head':
                return self._detect_shot_in_head_improved(results)
            
            else:
                return False
                
        except Exception as e:
            if self.debug_mode:
                print(f"[DEBUG] Error detecting {gesture_type}: {e}")
            return False

    # ORIGINAL GESTURES (Working fine)
    def _detect_hands_on_head(self, results):
        """Enhanced hands on head detection"""
        if 'pose' not in results or not results['pose'].pose_landmarks:
            return False
            
        landmarks = results['pose'].pose_landmarks.landmark
        
        try:
            left_wrist = landmarks[self.mp_pose.PoseLandmark.LEFT_WRIST]
            right_wrist = landmarks[self.mp_pose.PoseLandmark.RIGHT_WRIST]
            left_ear = landmarks[self.mp_pose.PoseLandmark.LEFT_EAR]
            right_ear = landmarks[self.mp_pose.PoseLandmark.RIGHT_EAR]
            nose = landmarks[self.mp_pose.PoseLandmark.NOSE]
            
            # Check visibility - relaxed
            required_landmarks = [left_wrist, right_wrist, left_ear, right_ear, nose]
            if any(lm.visibility < 0.3 for lm in required_landmarks):  # Relaxed from 0.5
                return False
            
            # Calculate head center
            head_center_x = (left_ear.x + right_ear.x + nose.x) / 3
            head_center_y = (left_ear.y + right_ear.y + nose.y) / 3
            
            # Distance from wrists to head center
            left_dist = ((left_wrist.x - head_center_x) ** 2 + (left_wrist.y - head_center_y) ** 2) ** 0.5
            right_dist = ((right_wrist.x - head_center_x) ** 2 + (right_wrist.y - head_center_y) ** 2) ** 0.5
            
            # Both hands should be close to head - relaxed threshold
            threshold = 0.18  # Increased from 0.15
            hands_near_head = left_dist < threshold and right_dist < threshold
            
            if self.debug_mode and hands_near_head:
                print(f"[DEBUG] ✓ hands_on_head detected! L:{left_dist:.3f} R:{right_dist:.3f}")
            
            return hands_near_head
            
        except Exception as e:
            if self.debug_mode:
                print(f"[DEBUG] Error in hands_on_head: {e}")
            return False

    def _detect_hands_up(self, results):
        """Enhanced hands up detection"""
        if 'pose' not in results or not results['pose'].pose_landmarks:
            return False
            
        landmarks = results['pose'].pose_landmarks.landmark
        
        try:
            left_wrist = landmarks[self.mp_pose.PoseLandmark.LEFT_WRIST]
            right_wrist = landmarks[self.mp_pose.PoseLandmark.RIGHT_WRIST]
            nose = landmarks[self.mp_pose.PoseLandmark.NOSE]
            left_shoulder = landmarks[self.mp_pose.PoseLandmark.LEFT_SHOULDER]
            right_shoulder = landmarks[self.mp_pose.PoseLandmark.RIGHT_SHOULDER]
            
            # Check visibility - relaxed
            required_landmarks = [left_wrist, right_wrist, nose, left_shoulder, right_shoulder]
            if any(lm.visibility < 0.3 for lm in required_landmarks):
                return False
            
            # Hands above nose and shoulders - relaxed
            hands_above_nose = (left_wrist.y < nose.y - 0.03 and right_wrist.y < nose.y - 0.03)  # Relaxed from 0.05
            hands_above_shoulders = (left_wrist.y < left_shoulder.y and right_wrist.y < right_shoulder.y)
            
            # Hands separated - relaxed
            hands_separated = abs(left_wrist.x - right_wrist.x) > 0.15  # Relaxed from 0.2
            
            result = hands_above_nose and hands_above_shoulders and hands_separated
            
            if self.debug_mode and result:
                print("[DEBUG] ✓ hands_up detected!")
            
            return result
            
        except Exception as e:
            if self.debug_mode:
                print(f"[DEBUG] Error in hands_up: {e}")
            return False

    def _detect_violin_gesture(self, results):
        """Detect violin playing gesture - RELAXED version"""
        if 'pose' not in results or not results['pose'].pose_landmarks:
            return False
        
        landmarks = results['pose'].pose_landmarks.landmark
        
        try:
            left_wrist = landmarks[self.mp_pose.PoseLandmark.LEFT_WRIST]
            right_wrist = landmarks[self.mp_pose.PoseLandmark.RIGHT_WRIST]
            left_shoulder = landmarks[self.mp_pose.PoseLandmark.LEFT_SHOULDER]
            right_shoulder = landmarks[self.mp_pose.PoseLandmark.RIGHT_SHOULDER]
            nose = landmarks[self.mp_pose.PoseLandmark.NOSE]
            
            # Check visibility - relaxed
            required_landmarks = [left_wrist, right_wrist, left_shoulder, right_shoulder, nose]
            if any(lm.visibility < 0.3 for lm in required_landmarks):
                return False
            
            # RELAXED: One hand higher, one extended - easier conditions
            # Right hand up, left hand extended
            right_hand_higher = right_wrist.y < left_wrist.y - 0.05  # Just higher than left
            left_hand_side = left_wrist.x < left_shoulder.x - 0.05   # Slightly to the side
            
            # OR left hand up, right hand extended  
            left_hand_higher = left_wrist.y < right_wrist.y - 0.05   # Just higher than right
            right_hand_side = right_wrist.x > right_shoulder.x + 0.05 # Slightly to the side
            
            # Accept either configuration
            violin_gesture = (right_hand_higher and left_hand_side) or (left_hand_higher and right_hand_side)
            
            if self.debug_mode and violin_gesture:
                print("[DEBUG] ✓ violin_gesture detected!")
            
            return violin_gesture
            
        except Exception as e:
            if self.debug_mode:
                print(f"[DEBUG] Error in violin_gesture: {e}")
            return False

    # IMPROVED HAND GESTURES - Much easier detection
    def _detect_peace_out_improved(self, results):
        """IMPROVED Peace sign detection - easier to trigger"""
        # First try hand detection
        if 'hands' in results and results['hands'].multi_hand_landmarks:
            for hand_landmarks in results['hands'].multi_hand_landmarks:
                landmarks = hand_landmarks.landmark
                
                try:
                    # Get fingertips and joints
                    index_tip = landmarks[self.mp_hands.HandLandmark.INDEX_FINGER_TIP]
                    middle_tip = landmarks[self.mp_hands.HandLandmark.MIDDLE_FINGER_TIP]
                    ring_tip = landmarks[self.mp_hands.HandLandmark.RING_FINGER_TIP]
                    pinky_tip = landmarks[self.mp_hands.HandLandmark.PINKY_TIP]
                    
                    index_pip = landmarks[self.mp_hands.HandLandmark.INDEX_FINGER_PIP]
                    middle_pip = landmarks[self.mp_hands.HandLandmark.MIDDLE_FINGER_PIP]
                    
                    # RELAXED conditions
                    # Index and middle extended (relaxed threshold)
                    index_extended = index_tip.y < index_pip.y - 0.01  # Relaxed from 0.02
                    middle_extended = middle_tip.y < middle_pip.y - 0.01  # Relaxed from 0.02
                    
                    # Basic separation check
                    fingers_separated = abs(index_tip.x - middle_tip.x) > 0.02  # Relaxed from 0.03
                    
                    # Just check that index and middle are higher than ring/pinky
                    index_higher = index_tip.y < ring_tip.y and index_tip.y < pinky_tip.y
                    middle_higher = middle_tip.y < ring_tip.y and middle_tip.y < pinky_tip.y
                    
                    if (index_extended or index_higher) and (middle_extended or middle_higher) and fingers_separated:
                        if self.debug_mode:
                            print("[DEBUG] ✓ peace_out detected!")
                        return True
                
                except Exception as e:
                    if self.debug_mode:
                        print(f"[DEBUG] Error in peace_out hand detection: {e}")
                    continue
        
        # FALLBACK: Use pose detection for "V" shape with wrists
        if 'pose' in results and results['pose'].pose_landmarks:
            try:
                landmarks = results['pose'].pose_landmarks.landmark
                left_wrist = landmarks[self.mp_pose.PoseLandmark.LEFT_WRIST]
                right_wrist = landmarks[self.mp_pose.PoseLandmark.RIGHT_WRIST]
                nose = landmarks[self.mp_pose.PoseLandmark.NOSE]
                
                # Simple V shape: both hands up and separated
                both_hands_up = left_wrist.y < nose.y and right_wrist.y < nose.y
                hands_separated = abs(left_wrist.x - right_wrist.x) > 0.1
                
                if both_hands_up and hands_separated:
                    if self.debug_mode:
                        print("[DEBUG] ✓ peace_out detected (pose fallback)!")
                    return True
                    
            except Exception as e:
                if self.debug_mode:
                    print(f"[DEBUG] Error in peace_out pose fallback: {e}")
        
        return False

    def _detect_middle_finger_improved(self, results):
        """IMPROVED Middle finger detection - much easier"""
        # First try hand detection
        if 'hands' in results and results['hands'].multi_hand_landmarks:
            for hand_landmarks in results['hands'].multi_hand_landmarks:
                landmarks = hand_landmarks.landmark
                
                try:
                    # Get finger tips
                    middle_tip = landmarks[self.mp_hands.HandLandmark.MIDDLE_FINGER_TIP]
                    index_tip = landmarks[self.mp_hands.HandLandmark.INDEX_FINGER_TIP]
                    ring_tip = landmarks[self.mp_hands.HandLandmark.RING_FINGER_TIP]
                    pinky_tip = landmarks[self.mp_hands.HandLandmark.PINKY_TIP]
                    thumb_tip = landmarks[self.mp_hands.HandLandmark.THUMB_TIP]
                    
                    # SIMPLE: Middle finger is highest
                    middle_highest = (middle_tip.y < index_tip.y - 0.02 and 
                                    middle_tip.y < ring_tip.y - 0.02 and 
                                    middle_tip.y < pinky_tip.y - 0.02 and
                                    middle_tip.y < thumb_tip.y - 0.02)
                    
                    if middle_highest:
                        if self.debug_mode:
                            print("[DEBUG] ✓ middle_finger detected!")
                        return True
                
                except Exception as e:
                    if self.debug_mode:
                        print(f"[DEBUG] Error in middle_finger hand detection: {e}")
                    continue
        
        # FALLBACK: Use pose detection - single hand up in center
        if 'pose' in results and results['pose'].pose_landmarks:
            try:
                landmarks = results['pose'].pose_landmarks.landmark
                left_wrist = landmarks[self.mp_pose.PoseLandmark.LEFT_WRIST]
                right_wrist = landmarks[self.mp_pose.PoseLandmark.RIGHT_WRIST]
                nose = landmarks[self.mp_pose.PoseLandmark.NOSE]
                left_shoulder = landmarks[self.mp_pose.PoseLandmark.LEFT_SHOULDER]
                right_shoulder = landmarks[self.mp_pose.PoseLandmark.RIGHT_SHOULDER]
                
                # One hand up in front of face
                left_hand_center = (abs(left_wrist.x - nose.x) < 0.1 and 
                                  left_wrist.y < nose.y - 0.05 and
                                  right_wrist.y > right_shoulder.y)
                
                right_hand_center = (abs(right_wrist.x - nose.x) < 0.1 and 
                                    right_wrist.y < nose.y - 0.05 and
                                    left_wrist.y > left_shoulder.y)
                
                if left_hand_center or right_hand_center:
                    if self.debug_mode:
                        print("[DEBUG] ✓ middle_finger detected (pose fallback)!")
                    return True
                    
            except Exception as e:
                if self.debug_mode:
                    print(f"[DEBUG] Error in middle_finger pose fallback: {e}")
        
        return False

    def _detect_shot_in_head_improved(self, results):
        """IMPROVED Shot in head detection - much easier"""
        if 'pose' not in results or not results['pose'].pose_landmarks:
            return False
        
        landmarks = results['pose'].pose_landmarks.landmark
        
        try:
            left_wrist = landmarks[self.mp_pose.PoseLandmark.LEFT_WRIST]
            right_wrist = landmarks[self.mp_pose.PoseLandmark.RIGHT_WRIST]
            left_ear = landmarks[self.mp_pose.PoseLandmark.LEFT_EAR]
            right_ear = landmarks[self.mp_pose.PoseLandmark.RIGHT_EAR]
            nose = landmarks[self.mp_pose.PoseLandmark.NOSE]
            left_shoulder = landmarks[self.mp_pose.PoseLandmark.LEFT_SHOULDER]
            right_shoulder = landmarks[self.mp_pose.PoseLandmark.RIGHT_SHOULDER]
            
            # Check visibility - relaxed
            required_landmarks = [left_wrist, right_wrist, nose]
            if any(lm.visibility < 0.3 for lm in required_landmarks):
                return False
            
            # RELAXED: Hand near head area (not just ears)
            # Check distance to head area (ears + nose + forehead area)
            head_points = [left_ear, right_ear, nose]
            
            # Left hand near head
            left_distances = []
            for point in head_points:
                if point.visibility > 0.3:
                    dist = ((left_wrist.x - point.x) ** 2 + (left_wrist.y - point.y) ** 2) ** 0.5
                    left_distances.append(dist)
            
            # Right hand near head  
            right_distances = []
            for point in head_points:
                if point.visibility > 0.3:
                    dist = ((right_wrist.x - point.x) ** 2 + (right_wrist.y - point.y) ** 2) ** 0.5
                    right_distances.append(dist)
            
            # RELAXED threshold
            threshold = 0.12  # More relaxed from 0.08
            
            left_near_head = left_distances and min(left_distances) < threshold
            right_near_head = right_distances and min(right_distances) < threshold
            
            # Also check if hand is above shoulder (pointing gesture)
            left_hand_up = left_wrist.y < left_shoulder.y + 0.1  # Relaxed
            right_hand_up = right_wrist.y < right_shoulder.y + 0.1  # Relaxed
            
            # Shot gesture: hand near head OR hand pointing upward near head level
            shot_gesture = (left_near_head and left_hand_up) or (right_near_head and right_hand_up)
            
            if self.debug_mode and shot_gesture:
                left_min = min(left_distances) if left_distances else 1.0
                right_min = min(right_distances) if right_distances else 1.0
                print(f"[DEBUG] ✓ shot_in_head detected! L:{left_min:.3f} R:{right_min:.3f}")
            
            return shot_gesture
            
        except Exception as e:
            if self.debug_mode:
                print(f"[DEBUG] Error in shot_in_head: {e}")
            return False

    def toggle_debug(self):
        """Toggle debug mode"""
        self.debug_mode = not self.debug_mode
        print(f"[DEBUG] Debug mode: {'ON' if self.debug_mode else 'OFF'}")

    def _debug_landmark_positions(self, landmarks):
        """Debug function to print landmark positions occasionally"""
        self._debug_frame_count += 1
        
        if self._debug_frame_count % 120 == 0:
            key_landmarks = {
                'LEFT_WRIST': self.mp_pose.PoseLandmark.LEFT_WRIST,
                'RIGHT_WRIST': self.mp_pose.PoseLandmark.RIGHT_WRIST,
                'NOSE': self.mp_pose.PoseLandmark.NOSE,
            }
            
            positions = {}
            for name, landmark_id in key_landmarks.items():
                landmark = landmarks[landmark_id]
                positions[name] = f"Y:{landmark.y:.3f} Vis:{landmark.visibility:.2f}"
            
            print(f"[DEBUG] Key positions: {positions}")


# Test function for your gestures
def test_detector():
    """Test function for your IMPROVED custom gestures"""
    # Your custom test config
    test_config = {
        "hands_up": {
            "gesture": {"type": "hands_up"},
            "video_path": "emotes/hands_up.mp4",
            "description": "Celebration - both hands up"
        },
        "hands_on_head": {
            "gesture": {"type": "hands_on_head"},
            "video_path": "emotes/hands_on_head.mp4",
            "description": "Facepalm - hands on head"
        },
        "violin_gesture": {
            "gesture": {"type": "violin_gesture"},
            "video_path": "emotes/saddest_violin.mp4",
            "description": "World's saddest violin - violin playing gesture"
        },
        "peace_out": {
            "gesture": {"type": "peace_out"},
            "video_path": "emotes/peace_out.mp4",
            "description": "Peace out - V sign with fingers (IMPROVED)"
        },
        "middle_finger": {
            "gesture": {"type": "middle_finger"},
            "video_path": "emotes/middle_finger.mp4",
            "description": "Middle finger - sassy response (IMPROVED)"
        },
        "shot_in_head": {
            "gesture": {"type": "shot_in_head"},
            "video_path": "emotes/shot_in_head.mp4",
            "description": "Shot in the head - hand to temple (IMPROVED)"
        }
    }
    
    detector = EmoteDetector(test_config, hold_time=0.8)  # Even faster
    cap = cv2.VideoCapture(0)
    
    if not cap.isOpened():
        print("[TEST] Error: Cannot open camera")
        return False
    
    print("🎭 Testing YOUR IMPROVED EmoteStream Gestures!")
    print("=" * 60)
    print("IMPROVED gestures with easier detection:")
    print("👋 hands_up        🤲 hands_on_head    🎻 violin_gesture")
    print("✌️ peace_out       🖕 middle_finger    🔫 shot_in_head")
    print("=" * 60)
    print("TIPS:")
    print("• peace_out: Just make V with fingers OR both hands up")
    print("• middle_finger: Make middle finger highest OR one hand up center")
    print("• shot_in_head: Hand near head/temple area")
    print("• Hold gestures for 0.8 seconds")
    print("=" * 60)
    print("Press 'd' to toggle debug, 'q' to quit")
    
    frame_count = 0
    detection_count = 0
    
    while True:
        ret, frame = cap.read()
        if not ret:
            break
        
        frame_count += 1
        frame = cv2.flip(frame, 1)  # Mirror effect
        
        try:
            results = detector.process_frame(frame)
            emote_detected, status = detector.detect_emote_with_status(results)
            
            # Draw landmarks
            detector.draw_pose_landmarks(frame, results)
            
            # Show status
            if status:
                # Progress bar
                bar_width = 300
                progress = int(status['progress'] * bar_width)
                cv2.rectangle(frame, (20, 50), (20 + bar_width, 70), (50, 50, 50), -1)
                cv2.rectangle(frame, (20, 50), (20 + progress, 70), (0, 255, 0), -1)
                cv2.putText(frame, status['text'], (20, 45), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)
            
            # Show detection
            if emote_detected:
                detection_count += 1
                print(f"[🎉] DETECTION #{detection_count}: {emote_detected['name'].upper()}")
                
                cv2.putText(frame, f"🎉 {emote_detected['name'].upper()}", (20, 100), 
                           cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 255, 0), 3)
            
            # Add UI
            cv2.putText(frame, "🎭 IMPROVED EmoteStream Test", (20, 25), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2)
            
            cv2.putText(frame, f"Detections: {detection_count}", (frame.shape[1] - 200, 25), 
                       cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)
            
            cv2.putText(frame, "IMPROVED: peace_out, middle_finger, shot_in_head", 
                       (20, frame.shape[0] - 20), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)
            
            cv2.imshow("🎭 IMPROVED EmoteStream Test", frame)
            
            key = cv2.waitKey(1) & 0xFF
            if key == ord('q'):
                break
            elif key == ord('d'):
                detector.toggle_debug()
            elif key == ord('h'):
                print("\n📖 IMPROVED GESTURE HELP:")
                print("✌️ Peace Out (IMPROVED):")
                print("   • Method 1: V sign with index + middle finger")
                print("   • Method 2: Both hands up and separated")
                print("🖕 Middle Finger (IMPROVED):")
                print("   • Method 1: Middle finger highest among all fingers")
                print("   • Method 2: One hand up in center of face")
                print("🔫 Shot in Head (IMPROVED):")
                print("   • Hand near head/temple area (relaxed distance)")
                print("   • Works with any part of head, not just ears")
                print("🎻 Violin (RELAXED):")
                print("   • One hand higher than the other + one to the side")
                print("💡 All gestures now have relaxed detection!")
        
        except Exception as e:
            print(f"[❌] Error: {e}")
            continue
    
    cap.release()
    cv2.destroyAllWindows()
    print(f"✅ IMPROVED Test completed! Total detections: {detection_count}")
    return True

if __name__ == "__main__":
    test_detector()