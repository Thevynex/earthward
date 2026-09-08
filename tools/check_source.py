"""Dokuz statik kontrol; Java derleme veya çalışma zamanı testi değildir."""
from pathlib import Path
import re
import tomllib

root = Path(__file__).resolve().parents[1]
metadata = tomllib.loads((root / 'src/main/resources/META-INF/neoforge.mods.toml').read_text(encoding='utf-8'))
build = (root / 'build.gradle').read_text(encoding='utf-8')
entry = (root / 'src/main/java/io/github/thevynex/earthward/Earthward.java').read_text(encoding='utf-8')
tests = (root / 'src/test/java/io/github/thevynex/earthward/selection/SpawnSelectionContractTest.java').read_text(encoding='utf-8')
checks = [
    metadata['mods'][0]['modId'] == 'earthward',
    metadata['mods'][0]['version'] == '0.1.0-dev.1',
    {d['modId']: d['versionRange'] for d in metadata['dependencies']['earthward']} == {'neoforge': '[21.1.249]', 'minecraft': '[1.21.1]'},
    "version '2.0.146'" in build,
    "gradle.gradleVersion != '9.2.1'" in build,
    bool(re.fullmatch('[a-z][a-z0-9_]{1,63}', metadata['mods'][0]['modId'])),
    'MOD_ID = "earthward"' in entry,
    len(re.findall(r'^        (?:accept|reject)\(', tests, re.M)) == 15,
    'Copyright (c) 2023 NeoForged project' in (root / 'TEMPLATE_LICENSE.txt').read_text(encoding='utf-8'),
]
for number, passed in enumerate(checks, start=1):
    if not passed:
        raise SystemExit(f'FAIL: static check {number}')
print('PASS: 9 static checks. Java compilation, Java contracts and Minecraft NOT tested.')
