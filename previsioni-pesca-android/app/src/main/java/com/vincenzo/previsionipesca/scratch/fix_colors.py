import re

filepath = r"c:\Antigravity\meteopesca\previsioni-pesca-android\app\src\main\java\com\vincenzo\previsionipesca\MainActivity.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Replace Color.white with Color.White, Color.black with Color.Black, Color.cyan with Color.Cyan
content = content.replace("Color.white", "Color.White")
content = content.replace("Color.black", "Color.Black")
content = content.replace("Color.cyan", "Color.Cyan")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("Replacement complete!")
