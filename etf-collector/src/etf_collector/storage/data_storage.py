"""Data storage module for saving collected ETF data."""

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
from ..utils.logger import log_info, log_err

MODULE = "data_storage"


class OutputFormat(Enum):
    """Output file format."""

    CSV = "csv"
    JSON = "json"


class DataStorage:
    """Storage manager for ETF data."""

    def __init__(self, output_dir: str = "./data"):
        """Initialize storage manager.

        Args:
            output_dir: Directory for output files
        """
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)

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
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        ext = output_format.value
        filepath = self.output_dir / f"{filename}_{timestamp}.{ext}"

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
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        ext = output_format.value
        filepath = self.output_dir / f"{filename}_{timestamp}.{ext}"

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
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        ext = output_format.value
        filepath = self.output_dir / f"{filename}_{timestamp}.{ext}"

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
        """
        path = Path(filepath)

        if path.suffix == ".csv":
            return self._load_csv(path)
        elif path.suffix == ".json":
            data = self._load_json(path)
            return data.get("etfs", [])
        else:
            raise ValueError(f"Unsupported file format: {path.suffix}")

    def _load_csv(self, filepath: Path) -> List[Dict[str, Any]]:
        """Load data from CSV file."""
        with open(filepath, "r", encoding="utf-8-sig") as f:
            reader = csv.DictReader(f)
            return list(reader)

    def _load_json(self, filepath: Path) -> Dict[str, Any]:
        """Load data from JSON file."""
        with open(filepath, "r", encoding="utf-8") as f:
            return json.load(f)
