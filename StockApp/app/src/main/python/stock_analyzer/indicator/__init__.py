"""Technical indicators.

This module provides technical analysis indicators:
- trend: Trend Signal (MA, CMF, Fear/Greed)
- elder: Elder Impulse System (EMA13, MACD)
- demark: DeMark TD Sequential Setup
- oscillator: Market Cap & Supply/Demand Oscillator (MACD Style)
"""

from . import demark, elder, oscillator, trend

__all__ = ["trend", "elder", "demark", "oscillator"]
