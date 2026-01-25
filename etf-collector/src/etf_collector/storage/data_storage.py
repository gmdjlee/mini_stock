"""Data storage module for saving collected ETF data.

This module provides secure file storage with path traversal protection.
"""

import csv
import json
from dataclasses import asdict
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional

from ..collector.constituent import ConstituentStock, EtfConstituentSummary
from ..collector.etf_list import EtfInfo
from ..utils.helpers import now_iso
from ..utils.logger import log_info, log_err, log_warn
from ..utils.validators import validate_filename, validate_path, ALLOWED_EXTENSIONS

MODULE = "data_storage"


class StorageError(Exception):
    """Storage operation error."""

    def __init__(self, message: str, code: str = "STORAGE_ERROR"):
        super().__init__(message)
        self.code = code
        self.message = message


class OutputFormat(Enum):
    """Output file format."""

    CSV = "csv"
    JSON = "json"


class DataStorage:
    """Storage manager for ETF data with path traversal protection."""

    def __init__(self, output_dir: str = "./data"):
        """Initialize storage manager.

        Args:
            output_dir: Directory for output files

        Raises:
            StorageError: If output directory is invalid
        """
        # Validate output directory path
        is_valid, error_msg, resolved_path = validate_path(output_dir)
        if not is_valid:
            raise StorageError(f"Invalid output directory: {error_msg}", "INVALID_PATH")

        self.output_dir = resolved_path
        self._base_dir = str(resolved_path)  # Store for validation
        self.output_dir.mkdir(parents=True, exist_ok=True)
        log_info(MODULE, f"Initialized storage", {"output_dir": self._base_dir})

    def _validate_and_resolve_path(self, filename: str, extension: str) -> Path:
        """Validate filename and resolve full path securely.

        Args:
            filename: Base filename (without extension)
            extension: File extension (e.g., "json", "csv")

        Returns:
            Resolved file path

        Raises:
            StorageError: If filename is invalid
        """
        # Validate filename
        full_filename = f"{filename}.{extension}"
        is_valid, error_msg = validate_filename(full_filename, ALLOWED_EXTENSIONS)
        if not is_valid:
            raise StorageError(f"Invalid filename: {error_msg}", "INVALID_FILENAME")

        # Resolve and validate full path
        filepath = self.output_dir / full_filename
        is_valid, error_msg, resolved = validate_path(
            str(filepath),
            base_dir=self._base_dir,
            allowed_extensions=ALLOWED_EXTENSIONS,
        )
        if not is_valid:
            raise StorageError(f"Invalid path: {error_msg}", "PATH_TRAVERSAL")

        return resolved

    def save_etf_list(
        self,
        etfs: List[EtfInfo],
        filename: str = "etf_list",
        output_format: OutputFormat = OutputFormat.CSV,
    ) -> str:
        """Save ETF list to file.

        Args:
            etfs: List of EtfInfo objects
            filename: Output filename (without extension)
            output_format: Output format (CSV or JSON)

        Returns:
            Path to saved file

        Raises:
            StorageError: If path validation fails
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        ext = output_format.value

        # Validate and resolve path securely
        filepath = self._validate_and_resolve_path(f"{filename}_{timestamp}", ext)

        if output_format == OutputFormat.CSV:
            self._save_etf_list_csv(etfs, filepath)
        else:
            self._save_etf_list_json(etfs, filepath)

        log_info(MODULE, f"Saved ETF list to {filepath}", {"count": len(etfs)})
        return str(filepath)

    def _save_etf_list_csv(self, etfs: List[EtfInfo], filepath: Path) -> None:
        """Save ETF list as CSV."""
        fieldnames = [
            "etf_code",
            "etf_name",
            "etf_type",
            "listing_date",
            "tracking_index",
            "asset_class",
            "management_company",
            "total_assets",
            "collected_at",
        ]

        with open(filepath, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            for etf in etfs:
                writer.writerow(asdict(etf))

    def _save_etf_list_json(self, etfs: List[EtfInfo], filepath: Path) -> None:
        """Save ETF list as JSON."""
        data = {
            "collection_info": {
                "collected_at": now_iso(),
                "total_etfs": len(etfs),
            },
            "etfs": [asdict(etf) for etf in etfs],
        }

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    def save_constituents(
        self,
        constituents: List[ConstituentStock],
        filename: str = "constituents",
        output_format: OutputFormat = OutputFormat.CSV,
    ) -> str:
        """Save constituent stocks to file.

        Args:
            constituents: List of ConstituentStock objects
            filename: Output filename (without extension)
            output_format: Output format (CSV or JSON)

        Returns:
            Path to saved file

        Raises:
            StorageError: If path validation fails
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        ext = output_format.value

        # Validate and resolve path securely
        filepath = self._validate_and_resolve_path(f"{filename}_{timestamp}", ext)

        if output_format == OutputFormat.CSV:
            self._save_constituents_csv(constituents, filepath)
        else:
            self._save_constituents_json(constituents, filepath)

        log_info(MODULE, f"Saved constituents to {filepath}", {"count": len(constituents)})
        return str(filepath)

    def _save_constituents_csv(self, constituents: List[ConstituentStock], filepath: Path) -> None:
        """Save constituents as CSV."""
        fieldnames = [
            "etf_code",
            "etf_name",
            "stock_code",
            "stock_name",
            "current_price",
            "price_change",
            "price_change_sign",
            "price_change_rate",
            "volume",
            "trading_value",
            "market_cap",
            "weight",
            "evaluation_amount",
            "collected_at",
        ]

        with open(filepath, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            for stock in constituents:
                writer.writerow(asdict(stock))

    def _save_constituents_json(self, constituents: List[ConstituentStock], filepath: Path) -> None:
        """Save constituents as JSON."""
        data = {
            "collection_info": {
                "collected_at": now_iso(),
                "total_constituents": len(constituents),
            },
            "constituents": [asdict(stock) for stock in constituents],
        }

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    def save_full_report(
        self,
        summaries: List[EtfConstituentSummary],
        filename: str = "etf_report",
        output_format: OutputFormat = OutputFormat.JSON,
        filter_info: Optional[Dict[str, Any]] = None,
    ) -> str:
        """Save complete ETF report with nested constituents.

        Args:
            summaries: List of EtfConstituentSummary objects
            filename: Output filename (without extension)
            output_format: Output format (CSV or JSON)
            filter_info: Optional filter information to include

        Returns:
            Path to saved file

        Raises:
            StorageError: If path validation fails
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        ext = output_format.value

        # Validate and resolve path securely
        filepath = self._validate_and_resolve_path(f"{filename}_{timestamp}", ext)

        if output_format == OutputFormat.CSV:
            # For CSV, flatten the data
            all_constituents = []
            for summary in summaries:
                all_constituents.extend(summary.constituents)
            self._save_constituents_csv(all_constituents, filepath)
        else:
            self._save_full_report_json(summaries, filepath, filter_info)

        total_constituents = sum(len(s.constituents) for s in summaries)
        log_info(
            MODULE,
            f"Saved full report to {filepath}",
            {"etfs": len(summaries), "constituents": total_constituents},
        )
        return str(filepath)

    def _save_full_report_json(
        self,
        summaries: List[EtfConstituentSummary],
        filepath: Path,
        filter_info: Optional[Dict[str, Any]] = None,
    ) -> None:
        """Save full report as JSON."""
        total_constituents = sum(len(s.constituents) for s in summaries)

        data = {
            "collection_info": {
                "collected_at": now_iso(),
                "filter_applied": filter_info,
                "total_etfs": len(summaries),
                "total_constituents": total_constituents,
            },
            "etfs": [],
        }

        for summary in summaries:
            etf_data = {
                "etf_code": summary.etf_code,
                "etf_name": summary.etf_name,
                "current_price": summary.current_price,
                "price_change": summary.price_change,
                "price_change_rate": summary.price_change_rate,
                "nav": summary.nav,
                "total_assets": summary.total_assets,
                "cu_unit_count": summary.cu_unit_count,
                "constituent_count": summary.constituent_count,
                "actual_constituent_count": len(summary.constituents),
                "collected_at": summary.collected_at,
                "constituents": [asdict(c) for c in summary.constituents],
            }
            data["etfs"].append(etf_data)

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    def load_etf_list(self, filepath: str) -> List[Dict[str, Any]]:
        """Load ETF list from file.

        Args:
            filepath: Path to file

        Returns:
            List of ETF dictionaries

        Raises:
            StorageError: If path validation fails or file format is unsupported
        """
        # Validate path
        is_valid, error_msg, path = validate_path(filepath, allowed_extensions=ALLOWED_EXTENSIONS)
        if not is_valid:
            raise StorageError(f"Invalid file path: {error_msg}", "INVALID_PATH")

        if not path.exists():
            raise StorageError(f"File not found: {filepath}", "FILE_NOT_FOUND")

        if path.suffix == ".csv":
            return self._load_csv(path)
        elif path.suffix == ".json":
            data = self._load_json(path)
            return data.get("etfs", [])
        else:
            raise StorageError(f"Unsupported file format: {path.suffix}", "UNSUPPORTED_FORMAT")

    def _load_csv(self, filepath: Path) -> List[Dict[str, Any]]:
        """Load data from CSV file."""
        with open(filepath, "r", encoding="utf-8-sig") as f:
            reader = csv.DictReader(f)
            return list(reader)

    def _load_json(self, filepath: Path) -> Dict[str, Any]:
        """Load data from JSON file."""
        with open(filepath, "r", encoding="utf-8") as f:
            return json.load(f)
