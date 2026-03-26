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
    extension_to_technology, filename_to_technology = _load_language_maps()
    technology_breakdown = _build_technology_breakdown(
        cloc_files,
        extension_to_technology,
        filename_to_technology,
    )

    java_percent = _percent(java_files, files_total)
    dotnet_percent = _percent(dotnet_files, files_total)
    size_total_formatted = _format_size(size_total)
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
            f'- CLOC files: {len(cloc_files)}',
            f'- Total files: {files_total}',
            f'- Total lines: {lines_total}',
            f'- Total size: {size_total_formatted}',
            '',
            '### Technology Breakdown',
            '',
            '| Technology | Files | Lines |',
            '| --- | ---: | ---: |',
            *[
                f"| {row['name']} | {row['files']} | {row['lines']} |"
                for row in technology_breakdown
            ],
        ]
    )

    template_model = {
        'generatedAt': generated_at,
        'technologyBreakdown': technology_breakdown,
        'metrics': {
            'clocFiles': len(cloc_files),
            'filesTotal': files_total,
            'linesTotal': lines_total,
            'sizeTotal': size_total,
            'sizeTotalFormatted': size_total_formatted,
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


def _build_technology_breakdown(
    cloc_files: list[Path],
    extension_to_technology: dict[str, str],
    filename_to_technology: dict[str, str],
) -> list[dict[str, Any]]:
    technology_aggregates: dict[str, dict[str, int]] = {}

    for cloc_file in cloc_files:
        try:
            with cloc_file.open('r', encoding='utf-8', errors='replace', newline='') as handle:
                reader = csv.reader(handle)
                header = next(reader, None)
                if not _is_expected_header(header):
                    continue

                for row in reader:
                    if len(row) < 3:
                        continue

                    file_path_value = row[0].strip().strip('"')
                    try:
                        file_lines = int(row[1])
                    except (TypeError, ValueError):
                        continue

                    technology = _detect_technology(
                        file_path_value,
                        extension_to_technology,
                        filename_to_technology,
                    )

                    if technology not in technology_aggregates:
                        technology_aggregates[technology] = {'files': 0, 'lines': 0}

                    technology_aggregates[technology]['files'] += 1
                    technology_aggregates[technology]['lines'] += file_lines
        except Exception:
            continue

    rows = [
        {'name': tech, 'files': values['files'], 'lines': values['lines']}
        for tech, values in technology_aggregates.items()
    ]

    rows.sort(key=lambda row: (-row['lines'], -row['files'], row['name'].lower()))

    return rows


def _detect_technology(
    file_path_value: str,
    extension_to_technology: dict[str, str],
    filename_to_technology: dict[str, str],
) -> str:
    normalized_path = file_path_value.replace('\\', '/')
    file_name = Path(normalized_path).name.lower()

    if file_name in filename_to_technology:
        return filename_to_technology[file_name]

    extension = Path(normalized_path).suffix.lower()
    if extension in extension_to_technology:
        return extension_to_technology[extension]

    return 'Other'


def _load_language_maps() -> tuple[dict[str, str], dict[str, str]]:
    language_file = _resolve_languages_file()
    if language_file is None:
        return {}, {}

    try:
        lines = language_file.read_text(encoding='utf-8', errors='replace').splitlines()
    except Exception:
        return {}, {}

    extension_to_technology: dict[str, str] = {}
    filename_to_technology: dict[str, str] = {}
    current_language: str | None = None
    current_section: str | None = None

    for raw_line in lines:
        line = raw_line.rstrip('\n')
        stripped = line.strip()

        if not stripped or stripped == '---' or stripped.startswith('#'):
            continue

        if _is_language_key_line(line):
            current_language = _normalize_language_name(stripped[:-1].strip())
            current_section = None
            continue

        if current_language is None:
            continue

        if stripped == 'extensions:':
            current_section = 'extensions'
            continue

        if stripped == 'filenames:':
            current_section = 'filenames'
            continue

        if stripped.endswith(':') and not stripped.startswith('- '):
            current_section = None
            continue

        if not stripped.startswith('- ') or current_section is None:
            continue

        value = _strip_optional_quotes(stripped[2:].strip()).lower()
        if not value:
            continue

        if current_section == 'extensions':
            extension_to_technology.setdefault(value, current_language)
            continue

        if current_section == 'filenames':
            filename_to_technology.setdefault(value, current_language)

    return extension_to_technology, filename_to_technology


def _resolve_languages_file() -> Path | None:
    current = Path(__file__).resolve().parent
    candidates = [
        current / 'languages.yml',
        current.parent / 'languages.yml',
    ]

    for candidate in candidates:
        if candidate.exists():
            return candidate

    return None


def _is_language_key_line(line: str) -> bool:
    return not line.startswith(' ') and line.strip().endswith(':')


def _normalize_language_name(raw_name: str) -> str:
    return _strip_optional_quotes(raw_name)


def _strip_optional_quotes(value: str) -> str:
    if len(value) >= 2 and value[0] == value[-1] and value[0] in {'"', "'"}:
        return value[1:-1]
    return value


def _percent(value: int, total: int) -> str:
    if total <= 0:
        return '0'
    percent = (value / total) * 100
    if percent.is_integer():
        return str(int(percent))
    return f'{percent:.2f}'.rstrip('0').rstrip('.')


def _format_size(size_in_bytes: int) -> str:
    units = ['B', 'KB', 'MB', 'GB', 'TB']
    value = float(size_in_bytes)
    unit_index = 0

    while value >= 1024 and unit_index < len(units) - 1:
        value /= 1024
        unit_index += 1

    if unit_index == 0:
        return f'{int(value)} {units[unit_index]}'

    return f'{value:.1f} {units[unit_index]}'


def _resolve_status(cloc_count: int, has_data_quality_issues: bool) -> str:
    if cloc_count == 0:
        return 'failed'
    if has_data_quality_issues:
        return 'partial'
    return 'success'


def _iso_now() -> str:
    return datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S UTC')
