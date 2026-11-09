import subprocess

out = subprocess.check_output(
    ['C:\\Users\\ADMIN\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe', 'shell', 'cat', '/data/anr/trace_00'],
    text=True,
    errors='replace'
)

blocks = out.split('\n\n')
for b in blocks:
    lines = b.strip().splitlines()
    if lines and lines[0].startswith('"'):
        header = lines[0]
        stack = [l for l in lines[1:] if l.strip().startswith('at ')]
        if stack:
            print(f"=== {header} ===")
            for s in stack:
                print(f"    {s.strip()}")
