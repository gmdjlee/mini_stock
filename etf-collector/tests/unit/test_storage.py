"""Tests for data storage module."""

import json
import tempfile
from pathlib import Path

import pytest

from etf_collector.storage.data_storage import DataStorage, OutputFormat, StorageError
from etf_collector.collector.etf_list import EtfInfo
from etf_collector.collector.constituent import ConstituentStock, EtfConstituentSummary


class TestOutputFormat:
    """Tests for OutputFormat enum."""

    def test_csv_format(self):
        """Test CSV format value."""
        assert OutputFormat.CSV.value == "csv"

    def test_json_format(self):
        """Test JSON format value."""
        assert OutputFormat.JSON.value == "json"


class TestDataStorage:
    """Tests for DataStorage class."""

    def setup_method(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.mkdtemp()
        self.storage = DataStorage(self.temp_dir)

    def test_init_creates_directory(self):
        """Test that init creates output directory."""
        new_dir = Path(self.temp_dir) / "new_dir"
        storage = DataStorage(str(new_dir))
        assert new_dir.exists()

    def test_save_etf_list_csv(self, sample_etf_infos):
        """Test saving ETF list as CSV."""
        filepath = self.storage.save_etf_list(
            sample_etf_infos,
            filename="test_etf_list",
            output_format=OutputFormat.CSV,
        )

        assert Path(filepath).exists()
        assert filepath.endswith(".csv")

        # Check content
        with open(filepath, "r", encoding="utf-8-sig") as f:
            content = f.read()
            assert "etf_code" in content
            assert "069500" in content
            assert "KODEX 200" in content

    def test_save_etf_list_json(self, sample_etf_infos):
        """Test saving ETF list as JSON."""
        filepath = self.storage.save_etf_list(
            sample_etf_infos,
            filename="test_etf_list",
            output_format=OutputFormat.JSON,
        )

        assert Path(filepath).exists()
        assert filepath.endswith(".json")

        # Check content
        with open(filepath, "r", encoding="utf-8") as f:
            data = json.load(f)
            assert "collection_info" in data
            assert "etfs" in data
            assert len(data["etfs"]) == len(sample_etf_infos)

    def test_save_constituents_csv(self):
        """Test saving constituents as CSV."""
        constituents = [
            ConstituentStock(
                etf_code="069500",
                etf_name="KODEX 200",
                stock_code="005930",
                stock_name="삼성전자",
                current_price=71500,
                price_change=500,
                price_change_sign="2",
                price_change_rate=0.70,
                volume=15000000,
                trading_value=1072500000000,
                market_cap=427000000000000,
                weight=31.25,
                evaluation_amount=15625000000,
            ),
        ]

        filepath = self.storage.save_constituents(
            constituents,
            filename="test_constituents",
            output_format=OutputFormat.CSV,
        )

        assert Path(filepath).exists()

        with open(filepath, "r", encoding="utf-8-sig") as f:
            content = f.read()
            assert "stock_code" in content
            assert "005930" in content
            assert "삼성전자" in content

    def test_save_constituents_json(self):
        """Test saving constituents as JSON."""
        constituents = [
            ConstituentStock(
                etf_code="069500",
                etf_name="KODEX 200",
                stock_code="005930",
                stock_name="삼성전자",
                current_price=71500,
                price_change=500,
                price_change_sign="2",
                price_change_rate=0.70,
                volume=15000000,
                trading_value=1072500000000,
                market_cap=427000000000000,
                weight=31.25,
                evaluation_amount=15625000000,
            ),
        ]

        filepath = self.storage.save_constituents(
            constituents,
            filename="test_constituents",
            output_format=OutputFormat.JSON,
        )

        assert Path(filepath).exists()

        with open(filepath, "r", encoding="utf-8") as f:
            data = json.load(f)
            assert "constituents" in data
            assert len(data["constituents"]) == 1

    def test_save_full_report_json(self):
        """Test saving full report as JSON."""
        constituents = [
            ConstituentStock(
                etf_code="069500",
                etf_name="KODEX 200",
                stock_code="005930",
                stock_name="삼성전자",
                current_price=71500,
                price_change=500,
                price_change_sign="2",
                price_change_rate=0.70,
                volume=15000000,
                trading_value=1072500000000,
                market_cap=427000000000000,
                weight=31.25,
                evaluation_amount=15625000000,
            ),
        ]

        summary = EtfConstituentSummary(
            etf_code="069500",
            etf_name="KODEX 200",
            current_price=35250,
            price_change=500,
            price_change_rate=1.44,
            nav=35248.50,
            total_assets=58234500000000,
            cu_unit_count=50000,
            constituent_count=200,
            constituents=constituents,
        )

        filepath = self.storage.save_full_report(
            [summary],
            filename="test_report",
            output_format=OutputFormat.JSON,
            filter_info={"active_only": True},
        )

        assert Path(filepath).exists()

        with open(filepath, "r", encoding="utf-8") as f:
            data = json.load(f)
            assert "collection_info" in data
            assert data["collection_info"]["filter_applied"]["active_only"] is True
            assert "etfs" in data
            assert len(data["etfs"]) == 1
            assert data["etfs"][0]["etf_code"] == "069500"
            assert len(data["etfs"][0]["constituents"]) == 1

    def test_save_full_report_csv(self):
        """Test saving full report as CSV (flattened)."""
        constituents = [
            ConstituentStock(
                etf_code="069500",
                etf_name="KODEX 200",
                stock_code="005930",
                stock_name="삼성전자",
                current_price=71500,
                price_change=500,
                price_change_sign="2",
                price_change_rate=0.70,
                volume=15000000,
                trading_value=1072500000000,
                market_cap=427000000000000,
                weight=31.25,
                evaluation_amount=15625000000,
            ),
        ]

        summary = EtfConstituentSummary(
            etf_code="069500",
            etf_name="KODEX 200",
            current_price=35250,
            price_change=500,
            price_change_rate=1.44,
            nav=35248.50,
            total_assets=58234500000000,
            cu_unit_count=50000,
            constituent_count=200,
            constituents=constituents,
        )

        filepath = self.storage.save_full_report(
            [summary],
            filename="test_report",
            output_format=OutputFormat.CSV,
        )

        assert Path(filepath).exists()

        with open(filepath, "r", encoding="utf-8-sig") as f:
            content = f.read()
            # CSV should contain constituent data
            assert "005930" in content
            assert "삼성전자" in content

    def test_load_etf_list_csv(self, sample_etf_infos):
        """Test loading ETF list from CSV."""
        # Save first
        filepath = self.storage.save_etf_list(
            sample_etf_infos,
            filename="test_load",
            output_format=OutputFormat.CSV,
        )

        # Load back
        loaded = self.storage.load_etf_list(filepath)

        assert len(loaded) == len(sample_etf_infos)
        assert loaded[0]["etf_code"] == "069500"

    def test_load_etf_list_json(self, sample_etf_infos):
        """Test loading ETF list from JSON."""
        # Save first
        filepath = self.storage.save_etf_list(
            sample_etf_infos,
            filename="test_load",
            output_format=OutputFormat.JSON,
        )

        # Load back
        loaded = self.storage.load_etf_list(filepath)

        assert len(loaded) == len(sample_etf_infos)
        assert loaded[0]["etf_code"] == "069500"

    def test_load_unsupported_format(self):
        """Test loading unsupported file format raises error."""
        with pytest.raises(StorageError) as exc_info:
            self.storage.load_etf_list("/path/to/file.xyz")

        assert "Unsupported" in str(exc_info.value) or exc_info.value.code == "INVALID_PATH"
