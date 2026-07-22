import subprocess
import re

def get_and_parse_wifi():
    # Giả lập hoặc chạy lệnh thực tế
    cmd = f"iw dev wlan0 scan"
    try:
        raw_output = subprocess.check_output(cmd, shell=True, text=True, stderr=subprocess.DEVNULL)
    except subprocess.CalledProcessError:
        print("Error: Cannot run scan command. Turn on your Wi-Fi and try again.")
        return

    # 1. Sửa dùng nhóm non-capturing (?:...) để tránh split ra các phần tử rác (\n)
    # Tách dựa trên từ khoá BSS nhưng giữ lại toàn bộ block sạch
    bss_blocks = re.split(r'(?:^|\n)BSS\s+', raw_output)
    wifi_list = []
    
    for block in bss_blocks:
        if not block.strip():
            continue
            
        # Bỏ qua mạng đang kết nối
        if "-- associated" in block:
            continue
            
        # Xác định kiểu bảo mật & Lọc bỏ mạng Open
        wka = "Open"
        if "RSN:" in block:
            wka = "WPA2"
        elif "WPA:" in block:
            wka = "WPA"
        elif "WEP" in block:
            wka = "WEP"
            
        if wka == "Open":
            continue
            
        if "WPS:" in block:
            wka += " + WPS"
            
        # 2. SỬA REGEX BSSID: Lấy ngay 17 ký tự MAC ở đầu block 
        bssid_match = re.search(r'^([0-9a-fA-F:]{17})', block)
        if not bssid_match:
            continue
        bssid = bssid_match.group(1)
        
        # 3. Trích xuất SSID
        ssid_match = re.search(r'^\s+SSID:\s*(.*)', block, re.MULTILINE)
        ssid = ssid_match.group(1).strip() if ssid_match else "<Hide>"
        if not ssid: 
            ssid = "HIDE"
            
        # 4. Trích xuất Tín hiệu
        signal_match = re.search(r'^\s+signal:\s*([-\d.]+)\s*dBm', block, re.MULTILINE)
        if signal_match:
            signal_num = float(signal_match.group(1))
            signal_str = f"{signal_match.group(1)} dBm"
        else:
            signal_num = -999.0
            signal_str = "N/A"
        
        # 5. Xác định Tần số
        freq_match = re.search(r'^\s+freq:\s*(\d+)', block, re.MULTILINE)
        freq_val = int(freq_match.group(1)) if freq_match else 0
        band = "5GHz" if freq_val > 4000 else "2.4GHz"
            
        # 6. Xác định Chuẩn Wi-Fi
        if "HE capabilities" in block:
            wifi_standard = "Wi-Fi 6"
        elif "VHT capabilities" in block:
            wifi_standard = "Wi-Fi 5"
        elif "HT capabilities" in block:
            wifi_standard = "Wi-Fi 4"
        else:
            wifi_standard = "Legacy"

        wifi_list.append({
            'bssid': bssid,
            'ssid': ssid,
            'standard': wifi_standard,
            'band': band,
            'signal_str': signal_str,
            'signal_num': signal_num,
            'wka': wka
        })

    sorted_wifi = sorted(wifi_list, key=lambda x: x['signal_num'], reverse=True)

    for wifi in sorted_wifi:
        print(f"{wifi['bssid']}|{wifi['ssid']} ({wifi['standard']}, {wifi['band']}, {wifi['signal_str']}, {wifi['wka']})")

if __name__ == "__main__":
    get_and_parse_wifi()
