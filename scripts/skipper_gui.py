#!/usr/bin/env python3
"""
Skipper Desktop Automator (GUI)
===============================
Standalone Windows desktop application for Skipper.
Supports both AnonChat (instant disconnect auto-skip) and Chatous (male profile auto-skip) over ADB.
"""

import os
import sys
import time
import re
import threading
import subprocess
import xml.etree.ElementTree as ET
import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox

DEFAULT_ADB_PATHS = [
    "adb",
    r"C:\Android\Sdk\platform-tools\adb.exe",
    os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"),
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "adb.exe"),
    os.path.join(os.path.dirname(sys.executable), "adb.exe"),
]

def find_adb():
    for path in DEFAULT_ADB_PATHS:
        try:
            res = subprocess.run([path, "version"], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            if res.returncode == 0:
                return path
        except Exception:
            continue
    return "adb"

ADB_BIN = find_adb()

def run_adb(cmd: list) -> str:
    global ADB_BIN
    try:
        startupinfo = None
        if os.name == 'nt':
            startupinfo = subprocess.STARTUPINFO()
            startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        full_cmd = [ADB_BIN] + cmd
        res = subprocess.run(
            full_cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            startupinfo=startupinfo,
            timeout=10
        )
        return res.stdout.strip()
    except Exception:
        return ""

def parse_bounds(bounds_str: str):
    match = re.findall(r"\[(\d+),(\d+)\]", bounds_str)
    if len(match) == 2:
        return int(match[0][0]), int(match[0][1]), int(match[1][0]), int(match[1][1])
    return None

def get_screen_dimensions():
    output = run_adb(["shell", "wm", "size"])
    match = re.search(r"(\d+)x(\d+)", output)
    if match:
        return int(match.group(1)), int(match.group(2))
    return 1080, 2400

class SkipperEngine:
    def __init__(self, log_callback):
        self.log_callback = log_callback
        self.is_running = False
        self.selected_app = "chatous"
        self.instant_only = True
        self.click_delay = 0.25
        self.cooldown = 1.5
        self.thread = None

    def start(self, selected_app, instant_only, click_delay, cooldown):
        if self.is_running:
            return
        self.selected_app = selected_app
        self.instant_only = instant_only
        self.click_delay = click_delay
        self.cooldown = cooldown
        self.is_running = True
        self.thread = threading.Thread(target=self._run_loop, daemon=True)
        self.thread.start()

    def stop(self):
        self.is_running = False

    def _run_loop(self):
        self.log_callback("INFO", f"Skipper automation started for [{self.selected_app.upper()}].")
        w, h = get_screen_dimensions()
        self.log_callback("INFO", f"Device resolution: {w}x{h}")

        while self.is_running:
            try:
                clicked = self._evaluate_and_skip(screen_h=h)
                if clicked:
                    time.sleep(self.cooldown)
                else:
                    time.sleep(0.5)
            except Exception as e:
                self.log_callback("ERROR", f"Loop exception: {str(e)}")
                time.sleep(1.0)

        self.log_callback("INFO", "Skipper automation stopped.")

    def _evaluate_and_skip(self, screen_h=2400):
        run_adb(["shell", "uiautomator", "dump", "/data/local/tmp/window_dump.xml"])
        xml_content = run_adb(["shell", "cat", "/data/local/tmp/window_dump.xml"])
        if not xml_content or "<hierarchy" not in xml_content:
            return False

        try:
            root = ET.fromstring(xml_content)
        except Exception:
            return False

        all_nodes = list(root.iter())
        all_texts = [n.attrib.get("text", "") for n in all_nodes if n.attrib.get("text")]

        if self.selected_app == "chatous":
            return self._evaluate_chatous(all_nodes, all_texts, screen_h)
        else:
            return self._evaluate_anonchat(all_nodes, all_texts, screen_h)

    def _evaluate_chatous(self, all_nodes, all_texts, screen_h):
        # Safeguards against Screen 1 (Home), Screen 2 (Setup), Screen 3 (Searching)
        if any("Featured Users" in t for t in all_texts) or any("START CHAT" in t for t in all_texts):
            return False
        if any("STOP WAITING" in t for t in all_texts) or any("WAITING FOR PERSON" in t for t in all_texts):
            return False

        # Screen 4 Verification: "Let's Chat" & "SKIP"
        has_lets_chat = any("Let's Chat" in t or "Let’s Chat" in t for t in all_texts)
        has_skip = any("SKIP" == t.strip().upper() for t in all_texts)

        if not (has_lets_chat and has_skip):
            return False

        # Gender check: Male
        male_regex = re.compile(r"\bMale\b", re.IGNORECASE)
        has_male_word = any(male_regex.search(t) for t in all_texts)
        has_male_symbol = any("♂" in t for t in all_texts)

        if not (has_male_word or has_male_symbol):
            return False

        # Locate SKIP button
        target_click = None
        for node in all_nodes:
            text = node.attrib.get("text", "").strip()
            if text.upper() == "SKIP":
                bounds = parse_bounds(node.attrib.get("bounds", ""))
                if bounds:
                    x1, y1, x2, y2 = bounds
                    if y1 > (screen_h * 0.65):
                        target_click = ((x1 + x2) // 2, (y1 + y2) // 2)
                        break

        if target_click:
            cx, cy = target_click
            self.log_callback("INFO", f"Male profile matched! Waiting {int(self.click_delay * 1000)}ms before SKIP...")
            time.sleep(self.click_delay)
            run_adb(["shell", "input", "tap", str(cx), str(cy)])
            self.log_callback("SUCCESS", f"Skipped male profile at ({cx}, {cy}). Returning to search.")
            return True

        return False

    def _evaluate_anonchat(self, all_nodes, all_texts, screen_h):
        # AI Male Bio Patterns
        MALE_PATTERNS = [
            r"\b\d{1,2}\s*[mM]\b",
            r"\b[mM]\s*\d{1,2}\b",
            r"\b[mM]\s*[/|-]\s*\d{1,2}\b",
            r"\b\d{1,2}\s*[/|-]\s*[mM]\b",
            r"[\[\(]\s*[mM]\s*[\]\)]",
            r"\b[mM]\s+(ind|india|delhi|mumbai|bangalore|blr|pune|hyd|kolkata|chennai|noida|gurgaon)\b",
            r"\b[mM](ale)?\s+here\b",
            r"\b(guy|boy|dude)\s+here\b",
            r"\b(male|boy|guy|dude|man)\b",
            r"\bhe\s*/\s*him\b",
            r"\b(m4f|m\s*4\s*f|m\s+for\s+f)\b",
            r"\b(looking\s+for|want|need)\s+(a\s+)?(f|female|girl|woman|gf)\b"
        ]
        FEMALE_PATTERNS = [
            r"\b\d{1,2}\s*[fF]\b",
            r"\b[fF]\s*\d{1,2}\b",
            r"\b(female|girl|woman|lady)\b",
            r"\bshe\s*/\s*her\b"
        ]

        # Case 1: Partner already disconnected
        if any("Chat was ended" in t for t in all_texts):
            has_timestamps = any("Seen" in t or re.search(r"\b\d{1,2}:\d{2}\s*(AM|PM)?\b", t, re.I) for t in all_texts)
            if self.instant_only and has_timestamps:
                return False

            for node in all_nodes:
                text = node.attrib.get("text", "")
                if "New partner" in text:
                    bounds = parse_bounds(node.attrib.get("bounds", ""))
                    if bounds and bounds[1] > (screen_h * 0.55):
                        cx = (bounds[0] + bounds[2]) // 2
                        cy = (bounds[1] + bounds[3]) // 2
                        self.log_callback("INFO", "Partner skipped! Clicking 'New partner'...")
                        time.sleep(self.click_delay)
                        run_adb(["shell", "input", "tap", str(cx), str(cy)])
                        self.log_callback("SUCCESS", "Skipped to next partner search.")
                        return True

        # Case 2: AI Male-Bio Detection (Fresh match who hasn't disconnected)
        has_profile_card = any("About partner" in t or "Chat starts" in t for t in all_texts)
        has_timestamps = any("Seen" in t or re.search(r"\b\d{1,2}:\d{2}\s*(AM|PM)?\b", t, re.I) for t in all_texts)

        if has_profile_card and not has_timestamps:
            bio_text = " ".join([t for t in all_texts if "anon.chat" not in t and "New partner" not in t])

            is_female = any(re.search(pat, bio_text, re.I) for pat in FEMALE_PATTERNS)
            matched_male = [pat for pat in MALE_PATTERNS if re.search(pat, bio_text, re.I)]

            if matched_male and not is_female:
                self.log_callback("INFO", f"AI detected Male bio: {matched_male[0]}. Auto-skipping...")
                time.sleep(self.click_delay)

                # Step A: Check if 'New partner' button is visible
                for node in all_nodes:
                    if "New partner" in node.attrib.get("text", ""):
                        bounds = parse_bounds(node.attrib.get("bounds", ""))
                        if bounds and bounds[1] > (screen_h * 0.55):
                            cx = (bounds[0] + bounds[2]) // 2
                            cy = (bounds[1] + bounds[3]) // 2
                            run_adb(["shell", "input", "tap", str(cx), str(cy)])
                            self.log_callback("SUCCESS", "Clicked 'New partner'.")
                            return True

                # Step B: Check if 'End chat' button is visible
                for node in all_nodes:
                    if "End chat" in node.attrib.get("text", ""):
                        bounds = parse_bounds(node.attrib.get("bounds", ""))
                        if bounds:
                            cx = (bounds[0] + bounds[2]) // 2
                            cy = (bounds[1] + bounds[3]) // 2
                            run_adb(["shell", "input", "tap", str(cx), str(cy)])
                            self.log_callback("INFO", "Clicked 'End chat'.")
                            return True

                # Step C: Tap top-right action icon (!) or Android back button
                for node in all_nodes:
                    bounds = parse_bounds(node.attrib.get("bounds", ""))
                    if bounds:
                        x1, y1, x2, y2 = bounds
                        if y1 < (screen_h * 0.15) and x2 > 800:
                            cx = (x1 + x2) // 2
                            cy = (y1 + y2) // 2
                            run_adb(["shell", "input", "tap", str(cx), str(cy)])
                            self.log_callback("INFO", "Tapped top action menu to end chat.")
                            return True

                run_adb(["shell", "input", "keyevent", "4"])
                return True

        return False


class SkipperAppUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Skipper - Smart Auto-Clicker")
        self.root.geometry("640x760")
        self.root.configure(bg="#141218")

        self.engine = SkipperEngine(self.add_log)

        self._setup_styles()
        self._build_ui()
        self._check_device()

    def _setup_styles(self):
        style = ttk.Style()
        style.theme_use("clam")
        style.configure("TLabel", background="#141218", foreground="#E6E1E5", font=("Segoe UI", 10))

    def _build_ui(self):
        # Header
        header_frame = tk.Frame(self.root, bg="#141218", padx=20, pady=15)
        header_frame.pack(fill="x")

        tk.Label(header_frame, text="Skipper 🚀", bg="#141218", fg="#BB86FC", font=("Segoe UI", 20, "bold")).pack(anchor="w")
        tk.Label(header_frame, text="Autonomous partner skipping with loophole protection", bg="#141218", fg="#9E9AA7", font=("Segoe UI", 10)).pack(anchor="w")

        # Device Status Card
        dev_card = tk.Frame(self.root, bg="#23202A", padx=15, pady=12, highlightthickness=1, highlightbackground="#3A3842")
        dev_card.pack(fill="x", padx=20, pady=(0, 10))

        self.lbl_device = tk.Label(dev_card, text="Checking connected device...", bg="#23202A", fg="#E6E1E5", font=("Segoe UI", 10, "bold"))
        self.lbl_device.pack(side="left")

        btn_refresh = tk.Button(dev_card, text="Refresh Device", bg="#3B3648", fg="#FFFFFF", font=("Segoe UI", 9), relief="flat", command=self._check_device, padx=10, pady=4)
        btn_refresh.pack(side="right")

        # Configuration Card
        cfg_card = tk.Frame(self.root, bg="#23202A", padx=15, pady=15, highlightthickness=1, highlightbackground="#3A3842")
        cfg_card.pack(fill="x", padx=20, pady=10)

        tk.Label(cfg_card, text="Select Automated App:", bg="#23202A", fg="#BB86FC", font=("Segoe UI", 11, "bold")).pack(anchor="w", pady=(0, 8))

        self.var_app = tk.StringVar(value="chatous")
        rb_chatous = tk.Radiobutton(cfg_card, text="Chatous - Auto-skip Male profiles on Screen 4", variable=self.var_app, value="chatous", bg="#23202A", fg="#E6E1E5", selectcolor="#6750A4", activebackground="#23202A", activeforeground="#FFFFFF", font=("Segoe UI", 10, "bold"))
        rb_chatous.pack(anchor="w", pady=3)

        rb_anonchat = tk.Radiobutton(cfg_card, text="AnonChat - Auto-skip instant disconnect on profile", variable=self.var_app, value="anonchat", bg="#23202A", fg="#E6E1E5", selectcolor="#6750A4", activebackground="#23202A", activeforeground="#FFFFFF", font=("Segoe UI", 10))
        rb_anonchat.pack(anchor="w", pady=3)

        # Timing parameters
        timing_frame = tk.Frame(cfg_card, bg="#23202A", pady=10)
        timing_frame.pack(fill="x")

        tk.Label(timing_frame, text="Click Delay (ms):", bg="#23202A", fg="#CAC4D0", font=("Segoe UI", 9)).grid(row=0, column=0, sticky="w", padx=5)
        self.entry_delay = tk.Entry(timing_frame, width=8, bg="#1A1822", fg="#FFFFFF", insertbackground="#FFFFFF", relief="flat")
        self.entry_delay.insert(0, "250")
        self.entry_delay.grid(row=0, column=1, padx=5)

        tk.Label(timing_frame, text="Cooldown (ms):", bg="#23202A", fg="#CAC4D0", font=("Segoe UI", 9)).grid(row=0, column=2, sticky="w", padx=(15, 5))
        self.entry_cooldown = tk.Entry(timing_frame, width=8, bg="#1A1822", fg="#FFFFFF", insertbackground="#FFFFFF", relief="flat")
        self.entry_cooldown.insert(0, "1500")
        self.entry_cooldown.grid(row=0, column=3, padx=5)

        # Action Button
        self.btn_toggle = tk.Button(self.root, text="START SKIPPER", bg="#6750A4", fg="#FFFFFF", font=("Segoe UI", 13, "bold"), relief="flat", padx=20, pady=12, command=self._toggle_engine)
        self.btn_toggle.pack(fill="x", padx=20, pady=10)

        # Live Log Card
        log_header = tk.Frame(self.root, bg="#141218", padx=20)
        log_header.pack(fill="x", pady=(10, 5))
        tk.Label(log_header, text="Live Trigger Feed", bg="#141218", fg="#E6E1E5", font=("Segoe UI", 11, "bold")).pack(side="left")

        btn_clear = tk.Button(log_header, text="Clear", bg="#141218", fg="#BB86FC", font=("Segoe UI", 9), relief="flat", command=self._clear_logs)
        btn_clear.pack(side="right")

        self.txt_log = scrolledtext.ScrolledText(self.root, bg="#0D0B11", fg="#E0E0E0", font=("Consolas", 9), height=14, relief="flat", padx=10, pady=8)
        self.txt_log.pack(fill="both", expand=True, padx=20, pady=(0, 20))

        self.txt_log.tag_config("SUCCESS", foreground="#81C784")
        self.txt_log.tag_config("SAFEGUARD", foreground="#FFB74D")
        self.txt_log.tag_config("ERROR", foreground="#E57373")
        self.txt_log.tag_config("INFO", foreground="#BB86FC")

    def _check_device(self):
        devices = run_adb(["devices"]).strip().splitlines()
        found = False
        device_name = "No device detected"
        for line in devices[1:]:
            parts = line.split()
            if len(parts) >= 2 and parts[1] == "device":
                model = run_adb(["-s", parts[0], "shell", "getprop", "ro.product.model"]).strip()
                device_name = f"Connected: {parts[0]} ({model if model else 'Android'})"
                found = True
                break

        if found:
            self.lbl_device.config(text=f"🟢 {device_name}", fg="#81C784")
        else:
            self.lbl_device.config(text="🔴 No device connected (Plug in USB & enable USB Debugging)", fg="#E57373")

    def _toggle_engine(self):
        if not self.engine.is_running:
            try:
                delay = float(self.entry_delay.get()) / 1000.0
            except ValueError:
                delay = 0.25
            try:
                cooldown = float(self.entry_cooldown.get()) / 1000.0
            except ValueError:
                cooldown = 1.5

            selected_app = self.var_app.get()

            self.engine.start(selected_app=selected_app, instant_only=True, click_delay=delay, cooldown=cooldown)
            self.btn_toggle.config(text="STOP SKIPPER", bg="#B3261E")
        else:
            self.engine.stop()
            self.btn_toggle.config(text="START SKIPPER", bg="#6750A4")

    def add_log(self, level, message):
        timestamp = time.strftime("%H:%M:%S")
        def _append():
            self.txt_log.insert(tk.END, f"[{timestamp}] ", "")
            self.txt_log.insert(tk.END, f"[{level}] ", level)
            self.txt_log.insert(tk.END, f"{message}\n", "")
            self.txt_log.see(tk.END)
        self.root.after(0, _append)

    def _clear_logs(self):
        self.txt_log.delete("1.0", tk.END)

if __name__ == "__main__":
    root = tk.Tk()
    app = SkipperAppUI(root)
    root.mainloop()
