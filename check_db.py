import subprocess

cmds = [
    ".tables",
    "SELECT count(*) FROM snapped_apps;",
    "SELECT count(*) FROM domain_rules;",
    "SELECT count(*) FROM dns_configuration;",
    "SELECT count(*) FROM dns_schedule;",
    "SELECT count(*) FROM app_category_overrides;",
]

for cmd in cmds:
    res = subprocess.run(
        ['C:\\Users\\ADMIN\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe', 'shell', f"sqlite3 /data/user/0/com.coconut/databases/coconut_snap.db '{cmd}'"],
        capture_output=True,
        text=True
    )
    print(f"=== {cmd} ===")
    print(res.stdout.strip())

