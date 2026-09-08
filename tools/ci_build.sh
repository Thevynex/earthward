#!/usr/bin/env bash
set -uo pipefail
mkdir -p ci-output
./gradlew --no-daemon --console=plain --max-workers=2 --stacktrace clean contractTest build 2>&1 | tee ci-output/build.log
statuses=("${PIPESTATUS[@]}")
status=${statuses[0]}
if [[ "$status" -eq 0 && "${statuses[1]}" -ne 0 ]]; then status=${statuses[1]}; fi
if [[ "$status" -ne 0 ]]; then
  python3 - <<'PY'
from pathlib import Path
import re
import xml.etree.ElementTree as ET
text = Path('ci-output/build.log').read_text(encoding='utf-8', errors='replace')
start = text.find('* What went wrong:')
if start >= 0:
    message = text[start:]
    end = message.find('* Try:')
    if end >= 0:
        message = message[:end]
else:
    message = '\n'.join(text.splitlines()[-35:])
for report in sorted(Path('build/test-results/test').glob('TEST-*.xml')):
    try:
        root = ET.parse(report).getroot()
        for case in root.iter('testcase'):
            for failure in list(case.findall('failure')) + list(case.findall('error')):
                message += '\nTEST ' + case.get('classname', '') + '.' + case.get('name', '')
                message += '\n' + failure.get('message', '') + '\n' + (failure.text or '')[:2200]
    except (ET.ParseError, OSError):
        message += '\nUnreadable JUnit report: ' + report.name
message = re.sub(r'(github_pat_|gh[pousr]_)[A-Za-z0-9_]+', '[REDACTED]', message)
message = re.sub(r'(?i)(authorization|password|token|secret)(\s*[:=]\s*)\S+', r'\1\2[REDACTED]', message)
message = message.strip()[:5000] or 'Gradle failed without a readable error summary.'
message = message.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
print('::error title=Gradle failure details::' + message)
PY
fi
exit "$status"
