#!/usr/bin/env python3
"""
Android 앱 개발을 위한 Python 패키지 준비 스크립트.

사용법:
    python scripts/prepare_android.py <android_project_path>

예시:
    python scripts/prepare_android.py ../StockApp
    python scripts/prepare_android.py D:/Projects/StockApp
"""

import os
import shutil
import sys
from pathlib import Path


# 복사할 모듈 목록 (chart 제외)
MODULES_TO_COPY = [
    "__init__.py",
    "config.py",
    "core",
    "client",
    "stock",
    "indicator",
    "market",
    "search",
]

# 제외할 파일/폴더 패턴
EXCLUDE_PATTERNS = [
    "__pycache__",
    "*.pyc",
    ".pytest_cache",
    "chart",  # 차트 모듈 제외 (Android에서 Vico로 대체)
]


def should_exclude(path: Path) -> bool:
    """파일/폴더를 제외할지 확인."""
    name = path.name
    for pattern in EXCLUDE_PATTERNS:
        if pattern.startswith("*"):
            if name.endswith(pattern[1:]):
                return True
        elif name == pattern:
            return True
    return False


def copy_module(src: Path, dst: Path):
    """모듈 복사 (제외 패턴 적용)."""
    if should_exclude(src):
        print(f"  [SKIP] {src.name}")
        return

    if src.is_file():
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
        print(f"  [COPY] {src.name}")
    elif src.is_dir():
        for item in src.iterdir():
            if not should_exclude(item):
                copy_module(item, dst / item.name)


def create_android_init(dst_dir: Path):
    """Android용 __init__.py 생성."""
    init_content = '''"""Stock Analyzer - Android Version (without chart modules)."""

__version__ = "0.2.0-android"

from .config import Config
from .client.kiwoom import KiwoomClient
from .client.auth import AuthClient

__all__ = [
    "Config",
    "KiwoomClient",
    "AuthClient",
]
'''
    init_path = dst_dir / "__init__.py"
    init_path.write_text(init_content, encoding="utf-8")
    print(f"  [CREATE] __init__.py (Android version)")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    android_project_path = Path(sys.argv[1]).resolve()

    # 소스 경로
    script_dir = Path(__file__).parent
    src_dir = script_dir.parent / "stock-analyzer" / "src" / "stock_analyzer"

    # 대상 경로
    dst_dir = android_project_path / "app" / "src" / "main" / "python" / "stock_analyzer"

    print("=" * 60)
    print("Android용 Python 패키지 준비")
    print("=" * 60)
    print(f"\n소스: {src_dir}")
    print(f"대상: {dst_dir}\n")

    if not src_dir.exists():
        print(f"[ERROR] 소스 디렉토리를 찾을 수 없습니다: {src_dir}")
        sys.exit(1)

    # 기존 대상 폴더 삭제
    if dst_dir.exists():
        print(f"[INFO] 기존 폴더 삭제: {dst_dir}")
        shutil.rmtree(dst_dir)

    # 대상 폴더 생성
    dst_dir.mkdir(parents=True, exist_ok=True)

    print("\n모듈 복사 중...")
    print("-" * 40)

    for module in MODULES_TO_COPY:
        src_path = src_dir / module
        if src_path.exists():
            if src_path.is_file():
                if module == "__init__.py":
                    # Android용 __init__.py 생성
                    create_android_init(dst_dir)
                else:
                    shutil.copy2(src_path, dst_dir / module)
                    print(f"  [COPY] {module}")
            else:
                print(f"\n[DIR] {module}/")
                copy_module(src_path, dst_dir / module)
        else:
            print(f"  [WARN] 모듈 없음: {module}")

    print("\n" + "=" * 60)
    print("완료!")
    print("=" * 60)

    # 복사된 파일 수 계산
    file_count = sum(1 for _ in dst_dir.rglob("*.py"))
    print(f"\n복사된 Python 파일: {file_count}개")
    print(f"위치: {dst_dir}")

    print("\n다음 단계:")
    print("1. Android Studio에서 Gradle Sync 실행")
    print("2. build.gradle.kts에 Chaquopy 설정 확인")
    print("3. pip install 설정 (numpy, pandas, requests, python-dotenv)")
    print("\n주의: chart/ 모듈은 제외되었습니다. Vico Charts를 사용하세요.")


if __name__ == "__main__":
    main()
