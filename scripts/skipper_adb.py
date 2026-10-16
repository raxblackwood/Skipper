#!/usr/bin/env python3
"""
Skipper ADB Companion Script
============================
Automates partner skipping for AnonChat over Android Debug Bridge (ADB).
Implements the exact same trigger logic and loophole safeguards as the native Skipper Android app.

Requirements:
- Android phone with USB Debugging or Wireless Debugging enabled
- 'adb' installed or in PATH

Usage:
  python skipper_adb.py [--instant-only] [--interval 0.5]
"""

import subprocess
import xml.etree.ElementTree as ET
import time
import re
import sys
import argparse

def run_adb(cmd: list) -> str:
    full_cmd = ["adb"] + cmd
    res = subprocess.run(full_cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    return res.stdout.strip()

def parse_bounds(bounds_str: str):
    # bounds="[x1,y1][x2,y2]"
    match = re.findall(r"\[(\d+),(\d+)\]", bounds_str)
    if len(match) == 2:
        x1, y1 = int(match[0][0]), int(match[0][1])
        x2, y2 = int(match[1][0]), int(match[1][1])
        return x1, y1, x2, y2
    return None

def get_screen_dimensions():
    output = run_adb(["shell", "wm", "size"])
    match = re.search(r"(\d+)x(\d+)", output)
    if match:
        return int(match.group(1)), int(match.group(2))
    return 1080, 2400

def dump_ui():
    run_adb(["shell", "uiautomator", "dump", "/data/local/tmp/window_dump.xml"])
    xml_content = run_adb(["shell", "cat", "/data/local/tmp/window_dump.xml"])
    try:
        return ET.fromstring(xml_content)
    except Exception:
        return None

def evaluate_and_skip(instant_only=True, screen_h=2400):
    root = dump_ui()
    if root is None:
        return False

    all_nodes = list(root.iter())

    # Safeguard 1: Active Chat Input Guard
    # If EditText is present, user is actively typing / chatting -> DO NOT TRIGGER
    for node in all_nodes:
        cls = node.attrib.get("class", "")
        if "EditText" in cls:
            return False

    # Safeguard 2: Disconnect Confirmation Guard
    # Screen must contain "Chat was ended by your partner"
    chat_ended_found = False
    for node in all_nodes:
        text = node.attrib.get("text", "")
        if "Chat was ended by your partner" in text or "Chat was ended" in text:
            chat_ended_found = True
            break

    if not chat_ended_found:
        return False

    # Safeguard 3: Screen 4 (Instant Skip) vs Screen 3
    if instant_only:
        has_profile_card = False
        has_timestamps = False

        for node in all_nodes:
            text = node.attrib.get("text", "")
            if "About partner" in text or "Chat starts" in text or "Interests" in text:
                has_profile_card = True
            if "Seen" in text or re.search(r"\b\d{1,2}:\d{2}\s*(AM|PM)?\b", text, re.I):
                has_timestamps = True

        # In Screen 4, profile card exists and no conversation timestamps exist
        if not has_profile_card or has_timestamps:
            print("[SAFEGUARD] Chat ended, but conversation occurred (Screen 3). Instant-only is enabled.")
            return False

    # Safeguard 4: Locate TRUE Action Button "New partner" at bottom
    target_click = None
    for node in all_nodes:
        text = node.attrib.get("text", "")
        if "New partner" in text:
            bounds = parse_bounds(node.attrib.get("bounds", ""))
            if bounds:
                x1, y1, x2, y2 = bounds
                # Action button is strictly at the bottom (Y > 55% of screen)
                # Header title is at the top (Y < 20% of screen)
                if y1 > (screen_h * 0.55):
                    target_click = ((x1 + x2) // 2, (y1 + y2) // 2)
                    break

    if target_click:
        cx, cy = target_click
        print(f"[TRIGGER SUCCESS] Instant skip detected on profile! Tapping 'New partner' at ({cx}, {cy})...")
        run_adb(["shell", "input", "tap", str(cx), str(cy)])
        return True
    else:
        print("[SAFEGUARD] 'New partner' button not in valid bottom action region.")
        return False

def main():
    parser = argparse.ArgumentParser(description="Skipper ADB Automator for AnonChat")
    parser.add_argument("--instant-only", action="store_true", default=True, help="Only trigger on instant skips (Screen 4)")
    parser.add_argument("--all-skips", action="store_false", dest="instant_only", help="Trigger on all disconnects (Screen 3 and 4)")
    parser.add_argument("--interval", type=float, default=0.7, help="Poll interval in seconds")
    parser.add_argument("--cooldown", type=float, default=2.5, help="Cooldown after click in seconds")
    args = parser.parse_args()

    print("==================================================")
    print("           SKIPPER ADB AUTOMATION ENGINE          ")
    print("==================================================")
    print(f"Mode: {'Instant Skips Only (Screen 4)' if args.instant_only else 'All Skips (Screen 3 & 4)'}")
    print(f"Poll Interval: {args.interval}s | Cooldown: {args.cooldown}s")
    print("Checking device connection...")

    devices = run_adb(["devices"])
    if "device" not in devices.splitlines()[-1]:
        print("Warning: No ADB device detected. Please connect phone via USB with USB Debugging enabled.")
    else:
        print("Device connected successfully!")

    w, h = get_screen_dimensions()
    print(f"Screen resolution detected: {w}x{h}")
    print("Monitoring screen triggers... (Press Ctrl+C to stop)")

    try:
        while True:
            did_click = evaluate_and_skip(instant_only=args.instant_only, screen_h=h)
            if did_click:
                time.sleep(args.cooldown)
            else:
                time.sleep(args.interval)
    except KeyboardInterrupt:
        print("\nSkipper stopped by user.")

if __name__ == "__main__":
    main()
