from __future__ import annotations

import csv
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


JAVA_EXTENSIONS = {'.java', '.jav', '.jsp', '.jspx'}
DOTNET_EXTENSIONS = {
    '.cs',
    '.csx',
    '.vb',
    '.fs',
    '.fsi',
    '.fsx',
    '.razor',
    '.cshtml',
    '.vbhtml',
}


@dataclass(frozen=True)
class ParsedCloc:
    files_total: int
    lines_total: int
    size_total: int
    java_files: int
    dotnet_files: int
    had_parse_failure: bool
    invalid_rows: int


def extract_insider_summary(results_directory: str | Path) -> dict[str, Any]:
    target = Path(results_directory)
    cloc_files: list[Path] = []
    had_parse_failures = False
    invalid_rows_total = 0
    files_total = 0
    lines_total = 0
    size_total = 0
    java_files = 0
    dotnet_files = 0

    try:
        entries = list(target.iterdir())
    except Exception:
        return _create_summary_payload(
            target,
            [],
            files_total=0,
            lines_total=0,
            size_total=0,
            java_files=0,
            dotnet_files=0,
            has_data_quality_issues=True,
        )

    cloc_files = [entry for entry in entries if entry.is_file() and entry.name.endswith('-cloc.csv')]

    for cloc_file in cloc_files:
        parsed = _parse_cloc_file(cloc_file)
        files_total += parsed.files_total
        lines_total += parsed.lines_total
        size_total += parsed.size_total
        java_files += parsed.java_files
        dotnet_files += parsed.dotnet_files
        invalid_rows_total += parsed.invalid_rows
        had_parse_failures = had_parse_failures or parsed.had_parse_failure

    has_data_quality_issues = had_parse_failures or invalid_rows_total > 0 or files_total == 0

    return _create_summary_payload(
        target,
        cloc_files,
        files_total=files_total,
        lines_total=lines_total,
        size_total=size_total,
        java_files=java_files,
        dotnet_files=dotnet_files,
        has_data_quality_issues=has_data_quality_issues,
    )


def _parse_cloc_file(file_path: Path) -> ParsedCloc:
    files_total = 0
    lines_total = 0
    size_total = 0
    java_files = 0
    dotnet_files = 0
    had_parse_failure = False
    invalid_rows = 0

    try:
        with file_path.open('r', encoding='utf-8', errors='replace', newline='') as handle:
            reader = csv.reader(handle)
            header = next(reader, None)
            if not _is_expected_header(header):
                return ParsedCloc(
                    files_total=0,
                    lines_total=0,
                    size_total=0,
                    java_files=0,
                    dotnet_files=0,
                    had_parse_failure=True,
                    invalid_rows=0,
                )

            for row in reader:
                if len(row) < 3:
                    invalid_rows += 1
                    continue

                file_path_value = row[0].strip()
                try:
                    file_lines = int(row[1])
                    file_size = int(row[2])
                except (TypeError, ValueError):
                    invalid_rows += 1
                    continue

                files_total += 1
                lines_total += file_lines
                size_total += file_size

                classification = _classify_language(file_path_value)
                if classification == 'java':
                    java_files += 1
                elif classification == 'dotnet':
                    dotnet_files += 1
    except Exception:
        had_parse_failure = True

    return ParsedCloc(
        files_total=files_total,
        lines_total=lines_total,
        size_total=size_total,
        java_files=java_files,
        dotnet_files=dotnet_files,
        had_parse_failure=had_parse_failure,
        invalid_rows=invalid_rows,
    )


def _is_expected_header(header: list[str] | None) -> bool:
    if not header or len(header) < 3:
        return False
    first = header[0].strip().lower()
    second = header[1].strip().lower()
    third = header[2].strip().lower()
    return first == 'file' and second == 'lines' and third == 'size'


def _classify_language(file_path_value: str) -> str:
    extension = Path(file_path_value).suffix.lower()
    if extension in JAVA_EXTENSIONS:
        return 'java'
    if extension in DOTNET_EXTENSIONS:
        return 'dotnet'
    return 'other'


def _create_summary_payload(
    results_directory: Path,
    cloc_files: list[Path],
    files_total: int,
    lines_total: int,
    size_total: int,
    java_files: int,
    dotnet_files: int,
    has_data_quality_issues: bool,
) -> dict[str, Any]:
    java_percent = _percent(java_files, files_total)
    dotnet_percent = _percent(dotnet_files, files_total)
    generated_at = _iso_now()

    status = _resolve_status(cloc_count=len(cloc_files), has_data_quality_issues=has_data_quality_issues)

    metadata = {
        'files.total': files_total,
        'lines.total': lines_total,
        'size.total': size_total,
        'cloc.files': len(cloc_files),
        'languages.java.files': java_files,
        'languages.java.percent': java_percent,
        'languages.dotnet.files': dotnet_files,
        'languages.dotnet.percent': dotnet_percent,
        'generated.at': generated_at,
    }

    markdown = '\n'.join(
        [
            '## Insider',
            '',
            f'- Status: {status}',
            f'- CLOC files: {len(cloc_files)}',
            f'- Total files: {files_total}',
            f'- Total lines: {lines_total}',
            f'- Total size: {size_total}',
            f'- Java footprint: {java_percent}% and {java_files} files',
            f'- .NET footprint: {dotnet_percent}% and {dotnet_files} files',
        ]
    )

    template_model = {
        'status': status,
        'statusClass': _to_status_class(status),
        'generatedAt': generated_at,
        'metrics': {
            'clocFiles': len(cloc_files),
            'filesTotal': files_total,
            'linesTotal': lines_total,
            'sizeTotal': size_total,
            'javaFiles': java_files,
            'javaPercent': java_percent,
            'dotnetFiles': dotnet_files,
            'dotnetPercent': dotnet_percent,
        },
    }

    return {
        'tool': 'insider',
        'status': status,
        'metadata': metadata,
        'markdown': markdown,
        'templateModel': template_model,
    }


def _percent(value: int, total: int) -> str:
    if total <= 0:
        return '0'
    percent = (value / total) * 100
    if percent.is_integer():
        return str(int(percent))
    return f'{percent:.2f}'.rstrip('0').rstrip('.')


def _resolve_status(cloc_count: int, has_data_quality_issues: bool) -> str:
    if cloc_count == 0:
        return 'failed'
    if has_data_quality_issues:
        return 'partial'
    return 'success'


def _iso_now() -> str:
    return datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')


def _to_status_class(status: str) -> str:
    if status == 'success':
        return 'status-success'
    if status == 'partial':
        return 'status-warning'
    if status == 'failed':
        return 'status-error'
    return 'status-unknown'
