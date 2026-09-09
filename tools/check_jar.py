"""Paket kontrolü; Minecraft içinde yükleme testi değildir."""
from pathlib import Path
import hashlib
import struct
import tomllib
import zipfile

jar = Path('build/libs/earthward-0.1.0-dev.1.jar')
with zipfile.ZipFile(jar) as archive:
    if archive.testzip() is not None:
        raise SystemExit('FAIL: corrupt ZIP entry')
    names = set(archive.namelist())
    required = {'META-INF/neoforge.mods.toml', 'io/github/thevynex/earthward/Earthward.class', 'LICENSE', 'TEMPLATE_LICENSE.txt', 'THIRD_PARTY_NOTICES.md'}
    if not required <= names:
        raise SystemExit(f'FAIL: missing entries: {sorted(required - names)}')
    metadata = tomllib.loads(archive.read('META-INF/neoforge.mods.toml').decode('utf-8'))
    if metadata['mods'][0]['modId'] != 'earthward' or metadata['mods'][0]['version'] != '0.1.0-dev.1':
        raise SystemExit('FAIL: mod identity/version mismatch')
    dependencies = {item['modId']: item['versionRange'] for item in metadata['dependencies']['earthward']}
    if dependencies != {'neoforge': '[21.1.249]', 'minecraft': '[1.21.1]'}:
        raise SystemExit('FAIL: dependency version mismatch')
    classes = [name for name in names if name.endswith('.class')]
    for name in classes:
        data = archive.read(name)
        if data[:4] != b'\xca\xfe\xba\xbe' or struct.unpack('>H', data[6:8])[0] != 65:
            raise SystemExit(f'FAIL: expected Java 21 bytecode: {name}')
        if not name.startswith('io/github/thevynex/earthward/'):
            raise SystemExit(f'FAIL: unexpected bundled class: {name}')
        if name.endswith('ContractTest.class'):
            raise SystemExit('FAIL: test class bundled in mod')
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
jar.with_suffix('.jar.sha256').write_text(f'{digest}  {jar.name}\n', encoding='utf-8')
print(f'PASS: JAR structure, metadata, notices and Java 21 bytecode; SHA-256 {digest}')
print('Minecraft mod loading and gameplay NOT tested.')
