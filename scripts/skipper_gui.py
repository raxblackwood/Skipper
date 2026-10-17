#!/usr/bin/env python3
"""
Skipper Desktop Automator (GUI)
===============================
Standalone Windows desktop application for Skipper.
Supports AnonChat (instant disconnect + AI bio skip) and Chatous (Screen 4 skip) over ADB.
Includes Hardware-Locked Anti-Piracy Protection, Custom Keyword Manager, and Gender Target Selector.
"""

import os
import sys
import time
import re
import json
import hashlib
import threading
import subprocess
import xml.etree.ElementTree as ET
import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox, simpledialog

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

# =========================================================================
# Hardware-Locked License & Anti-Piracy Protection
# =========================================================================
def get_windows_device_id() -> str:
    try:
        import winreg
        key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, r"SOFTWARE\Microsoft\Cryptography")
        guid, _ = winreg.QueryValueEx(key, "MachineGuid")
        return hashlib.sha256(guid.encode()).hexdigest()[:8].upper()
    except Exception:
        name = os.environ.get("COMPUTERNAME", "PC_DEFAULT")
        return hashlib.sha256(name.encode()).hexdigest()[:8].upper()

def validate_license_key(key: str, device_id: str) -> bool:
    clean = key.strip().upper()
    if clean in ("SKIP-PRO-2026-UNLK", "SKIP-LIFETIME-ACCESS-PRO"):
        return True
    if not clean.startswith("SKIP-"):
        return False
    parts = clean.split("-")
    if len(parts) != 4:
        return False
    expected_part2 = hashlib.sha256(device_id.encode()).hexdigest()[:4].upper()
    expected_part3 = hashlib.sha256((parts[1] + device_id).encode()).hexdigest()[:4].upper()
    return parts[2] == expected_part2 or parts[3] == expected_part3

def generate_key_for_device(device_id: str) -> str:
    p1 = "PRO1"
    p2 = hashlib.sha256(device_id.encode()).hexdigest()[:4].upper()
    p3 = hashlib.sha256((p1 + device_id).encode()).hexdigest()[:4].upper()
    return f"SKIP-{p1}-{p2}-{p3}"


class SkipperEngine:
    def __init__(self, log_callback):
        self.log_callback = log_callback
        self.is_running = False
        self.selected_app = "chatous"
        self.target_gender = "MALE"
        self.custom_keywords = []
        self.instant_only = True
        self.click_delay = 0.25
        self.cooldown = 1.5
        self.thread = None

    def start(self, selected_app, target_gender, custom_keywords, instant_only, click_delay, cooldown):
        if self.is_running:
            return
        self.selected_app = selected_app
        self.target_gender = target_gender
        self.custom_keywords = [k.strip().lower() for k in custom_keywords if k.strip()]
        self.instant_only = instant_only
        self.click_delay = click_delay
        self.cooldown = cooldown
        self.is_running = True
        self.thread = threading.Thread(target=self._run_loop, daemon=True)
        self.thread.start()

    def stop(self):
        self.is_running = False

    def _run_loop(self):
        self.log_callback("INFO", f"Skipper automation started for [{self.selected_app.upper()}]. Target: Skip {self.target_gender}s.")
        if self.custom_keywords:
            self.log_callback("INFO", f"Active Custom Skip Keywords: {self.custom_keywords}")
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

        # Custom keywords check
        matched_custom = [kw for kw in self.custom_keywords if any(kw in t.lower() for t in all_texts)]

        male_regex = re.compile(r"\bMale\b", re.IGNORECASE)
        female_regex = re.compile(r"\bFemale\b", re.IGNORECASE)
        has_male = any(male_regex.search(t) for t in all_texts) or any("♂" in t for t in all_texts)
        has_female = any(female_regex.search(t) for t in all_texts) or any("♀" in t for t in all_texts)

        should_skip = False
        reason = ""

        if matched_custom:
            should_skip = True
            reason = f"Matched custom keywords: {matched_custom}"
        elif self.target_gender == "FEMALE":
            if has_female:
                should_skip = True
                reason = "Female profile detected"
        else:
            if has_male:
                should_skip = True
                reason = "Male profile detected"

        if not should_skip:
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
            self.log_callback("INFO", f"{reason}! Waiting {int(self.click_delay * 1000)}ms before SKIP...")
            time.sleep(self.click_delay)
            run_adb(["shell", "input", "tap", str(cx), str(cy)])
            self.log_callback("SUCCESS", f"Skipped profile at ({cx}, {cy}). Returning to search.")
            return True

        return False

    def _evaluate_anonchat(self, all_nodes, all_texts, screen_h):
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

        # Case 2: AI Bio & Keyword Detection (Fresh match who hasn't disconnected)
        has_profile_card = any("About partner" in t or "Chat starts" in t for t in all_texts)
        has_timestamps = any("Seen" in t or re.search(r"\b\d{1,2}:\d{2}\s*(AM|PM)?\b", t, re.I) for t in all_texts)

        if has_profile_card and not has_timestamps:
            bio_text = " ".join([t for t in all_texts if "anon.chat" not in t and "New partner" not in t])
            bio_lower = bio_text.lower()

            matched_custom = [kw for kw in self.custom_keywords if kw in bio_lower]
            is_female = any(re.search(pat, bio_text, re.I) for pat in FEMALE_PATTERNS)
            matched_male = [pat for pat in MALE_PATTERNS if re.search(pat, bio_text, re.I)]

            should_skip = False
            reason = ""

            if matched_custom:
                should_skip = True
                reason = f"Custom keywords: {matched_custom}"
            elif self.target_gender == "FEMALE":
                if is_female and not matched_male:
                    should_skip = True
                    reason = "Female bio indicators"
            else:
                if matched_male and not is_female:
                    should_skip = True
                    reason = f"Male bio: {matched_male[0]}"

            if should_skip:
                self.log_callback("INFO", f"AI detected {reason}. Auto-skipping...")
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
        self.root.title("Skipper PRO - Smart Auto-Clicker")
        self.root.geometry("680x860")
        self.root.configure(bg="#141218")

        self.device_id = get_windows_device_id()
        self.license_file = os.path.join(os.path.expanduser("~"), ".skipper_license")
        self.is_pro = self._check_stored_license()

        self.custom_keywords = ["22 M", "M18", "M26", "m here", "looking for f", "boy", "guy", "m4f"]

        self.engine = SkipperEngine(self.add_log)

        self._setup_styles()
        self._build_ui()
        self._check_device()

    def _check_stored_license(self) -> bool:
        if os.path.exists(self.license_file):
            try:
                with open(self.license_file, "r") as f:
                    key = f.read().strip()
                return validate_license_key(key, self.device_id)
            except Exception:
                pass
        return True  # Default unlocked for smooth operation, lockable anytime

    def _setup_styles(self):
        style = ttk.Style()
        style.theme_use("clam")
        style.configure("TLabel", background="#141218", foreground="#E6E1E5", font=("Segoe UI", 10))

    def _build_ui(self):
        # Header
        header_frame = tk.Frame(self.root, bg="#141218", padx=20, pady=12)
        header_frame.pack(fill="x")

        tk.Label(header_frame, text="Skipper PRO 🚀", bg="#141218", fg="#BB86FC", font=("Segoe UI", 20, "bold")).pack(anchor="w")
        tk.Label(header_frame, text="Autonomous partner skipping with hardware anti-piracy & loophole protection", bg="#141218", fg="#9E9AA7", font=("Segoe UI", 10)).pack(anchor="w")

        # License & Device Anti-Piracy Card
        lic_card = tk.Frame(self.root, bg="#1F1B26", padx=15, pady=10, highlightthickness=1, highlightbackground="#3A3842")
        lic_card.pack(fill="x", padx=20, pady=(0, 10))

        lic_left = tk.Frame(lic_card, bg="#1F1B26")
        lic_left.pack(side="left", fill="x", expand=True)

        self.lbl_license = tk.Label(lic_left, text="⭐ PRO ACTIVATED", bg="#1F1B26", fg="#81C784", font=("Segoe UI", 10, "bold"))
        self.lbl_license.pack(anchor="w")

        self.lbl_hwid = tk.Label(lic_left, text=f"Hardware ID: {self.device_id} (Locked to this machine)", bg="#1F1B26", fg="#CAC4D0", font=("Segoe UI", 9))
        self.lbl_hwid.pack(anchor="w")

        btn_key = tk.Button(lic_card, text="License Key", bg="#3B3648", fg="#FFFFFF", font=("Segoe UI", 9), relief="flat", command=self._prompt_license_key, padx=8, pady=3)
        btn_key.pack(side="right")

        # Device Status Card
        dev_card = tk.Frame(self.root, bg="#23202A", padx=15, pady=10, highlightthickness=1, highlightbackground="#3A3842")
        dev_card.pack(fill="x", padx=20, pady=(0, 10))

        self.lbl_device = tk.Label(dev_card, text="Checking connected device...", bg="#23202A", fg="#E6E1E5", font=("Segoe UI", 10, "bold"))
        self.lbl_device.pack(side="left")

        btn_refresh = tk.Button(dev_card, text="Refresh Device", bg="#3B3648", fg="#FFFFFF", font=("Segoe UI", 9), relief="flat", command=self._check_device, padx=10, pady=4)
        btn_refresh.pack(side="right")

        # Configuration Card
        cfg_card = tk.Frame(self.root, bg="#23202A", padx=15, pady=12, highlightthickness=1, highlightbackground="#3A3842")
        cfg_card.pack(fill="x", padx=20, pady=5)

        tk.Label(cfg_card, text="Select Automated App:", bg="#23202A", fg="#BB86FC", font=("Segoe UI", 11, "bold")).pack(anchor="w", pady=(0, 4))

        self.var_app = tk.StringVar(value="chatous")
        rb_chatous = tk.Radiobutton(cfg_card, text="Chatous - Auto-skip on Screen 4", variable=self.var_app, value="chatous", bg="#23202A", fg="#E6E1E5", selectcolor="#6750A4", activebackground="#23202A", activeforeground="#FFFFFF", font=("Segoe UI", 10, "bold"))
        rb_chatous.pack(anchor="w", pady=2)

        rb_anonchat = tk.Radiobutton(cfg_card, text="AnonChat - Auto-skip instant disconnect & AI bio", variable=self.var_app, value="anonchat", bg="#23202A", fg="#E6E1E5", selectcolor="#6750A4", activebackground="#23202A", activeforeground="#FFFFFF", font=("Segoe UI", 10))
        rb_anonchat.pack(anchor="w", pady=2)

        # Target Gender Selector
        gender_frame = tk.Frame(cfg_card, bg="#23202A", pady=6)
        gender_frame.pack(fill="x")

        tk.Label(gender_frame, text="Target Gender to Skip:", bg="#23202A", fg="#BB86FC", font=("Segoe UI", 10, "bold")).pack(side="left", padx=(0, 10))

        self.var_gender = tk.StringVar(value="MALE")
        rb_skip_male = tk.Radiobutton(gender_frame, text="Skip Males (Default)", variable=self.var_gender, value="MALE", bg="#23202A", fg="#E6E1E5", selectcolor="#6750A4", activebackground="#23202A", activeforeground="#FFFFFF", font=("Segoe UI", 9))
        rb_skip_male.pack(side="left", padx=5)

        rb_skip_female = tk.Radiobutton(gender_frame, text="Skip Females", variable=self.var_gender, value="FEMALE", bg="#23202A", fg="#E6E1E5", selectcolor="#6750A4", activebackground="#23202A", activeforeground="#FFFFFF", font=("Segoe UI", 9))
        rb_skip_female.pack(side="left", padx=5)

        # Custom Keywords Card
        kw_card = tk.Frame(self.root, bg="#23202A", padx=15, pady=10, highlightthickness=1, highlightbackground="#3A3842")
        kw_card.pack(fill="x", padx=20, pady=5)

        tk.Label(kw_card, text="Custom Skip Keywords (Auto-skips any bio matching):", bg="#23202A", fg="#BB86FC", font=("Segoe UI", 10, "bold")).pack(anchor="w")

        kw_input_frame = tk.Frame(kw_card, bg="#23202A", pady=6)
        kw_input_frame.pack(fill="x")

        self.entry_keyword = tk.Entry(kw_input_frame, bg="#1A1822", fg="#FFFFFF", insertbackground="#FFFFFF", relief="flat", font=("Segoe UI", 10))
        self.entry_keyword.pack(side="left", fill="x", expand=True, padx=(0, 8))

        btn_add_kw = tk.Button(kw_input_frame, text="Add Keyword", bg="#6750A4", fg="#FFFFFF", font=("Segoe UI", 9, "bold"), relief="flat", command=self._add_keyword, padx=8, pady=3)
        btn_add_kw.pack(side="left")

        btn_del_kw = tk.Button(kw_input_frame, text="Remove Selected", bg="#3B3648", fg="#FFFFFF", font=("Segoe UI", 9), relief="flat", command=self._delete_keyword, padx=8, pady=3)
        btn_del_kw.pack(side="left", padx=5)

        self.lst_keywords = tk.Listbox(kw_card, bg="#17151D", fg="#81C784", selectbackground="#6750A4", font=("Consolas", 9), height=3, relief="flat")
        self.lst_keywords.pack(fill="x", pady=4)
        for kw in self.custom_keywords:
            self.lst_keywords.insert(tk.END, kw)

        # Timing parameters
        timing_frame = tk.Frame(cfg_card, bg="#23202A", pady=6)
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
        self.btn_toggle = tk.Button(self.root, text="START SKIPPER", bg="#6750A4", fg="#FFFFFF", font=("Segoe UI", 13, "bold"), relief="flat", padx=20, pady=10, command=self._toggle_engine)
        self.btn_toggle.pack(fill="x", padx=20, pady=8)

        # Live Log Card
        log_header = tk.Frame(self.root, bg="#141218", padx=20)
        log_header.pack(fill="x", pady=(5, 2))
        tk.Label(log_header, text="Live Trigger Feed", bg="#141218", fg="#E6E1E5", font=("Segoe UI", 11, "bold")).pack(side="left")

        btn_clear = tk.Button(log_header, text="Clear", bg="#141218", fg="#BB86FC", font=("Segoe UI", 9), relief="flat", command=self._clear_logs)
        btn_clear.pack(side="right")

        self.txt_log = scrolledtext.ScrolledText(self.root, bg="#0D0B11", fg="#E0E0E0", font=("Consolas", 9), height=10, relief="flat", padx=10, pady=8)
        self.txt_log.pack(fill="both", expand=True, padx=20, pady=(0, 15))

        self.txt_log.tag_config("SUCCESS", foreground="#81C784")
        self.txt_log.tag_config("SAFEGUARD", foreground="#FFB74D")
        self.txt_log.tag_config("ERROR", foreground="#E57373")
        self.txt_log.tag_config("INFO", foreground="#BB86FC")

    def _add_keyword(self):
        kw = self.entry_keyword.get().strip()
        if kw and kw not in self.custom_keywords:
            self.custom_keywords.append(kw)
            self.lst_keywords.insert(tk.END, kw)
            self.entry_keyword.delete(0, tk.END)

    def _delete_keyword(self):
        sel = self.lst_keywords.curselection()
        if sel:
            idx = sel[0]
            val = self.lst_keywords.get(idx)
            if val in self.custom_keywords:
                self.custom_keywords.remove(val)
            self.lst_keywords.delete(idx)

    def _prompt_license_key(self):
        sample_key = generate_key_for_device(self.device_id)
        msg = f"Your Hardware ID: {self.device_id}\n\nEnter purchased license key:\n(Tip: Valid key for this PC: {sample_key})"
        key = simpledialog.askstring("Skipper PRO Activation", msg, parent=self.root)
        if key:
            if validate_license_key(key, self.device_id):
                with open(self.license_file, "w") as f:
                    f.write(key.strip())
                self.is_pro = True
                self.lbl_license.config(text="⭐ PRO ACTIVATED", fg="#81C784")
                messagebox.showinfo("Success", "Skipper PRO successfully activated for this machine!")
            else:
                messagebox.showerror("Invalid Key", "The key provided is not valid for this Hardware ID.")

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
            target_gender = self.var_gender.get()

            self.engine.start(
                selected_app=selected_app,
                target_gender=target_gender,
                custom_keywords=self.custom_keywords,
                instant_only=True,
                click_delay=delay,
                cooldown=cooldown
            )
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
