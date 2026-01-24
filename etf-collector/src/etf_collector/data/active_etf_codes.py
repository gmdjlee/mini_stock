"""Predefined list of Korean Active ETF codes.

This list is used because KIS API's search-info endpoint (CTPF1604R)
does not support bulk ETF listing with empty PDNO parameter.

The list should be updated periodically as new active ETFs are listed.

Note: This project uses KIS API (Korea Investment & Securities),
NOT Kiwoom API. The two are different API providers.

Data source: KRX (Korea Exchange) - https://data.krx.co.kr/
Last updated: 2026-01-24
"""

from typing import Dict, List, Tuple

# Active ETF codes with their names
# Format: (etf_code, etf_name)
# Note: Names are for reference only, actual names are fetched from API
ACTIVE_ETF_CODES: List[Tuple[str, str]] = [
    # KODEX (삼성자산운용) Active ETFs
    ("278530", "KODEX 200액티브"),
    ("400590", "KODEX K-미래차액티브"),
    ("401400", "KODEX K-신재생에너지액티브"),
    ("401410", "KODEX K-2차전지액티브"),
    ("401420", "KODEX K-배터리 산업체인액티브"),
    ("411060", "KODEX 미국나스닥100TR액티브"),
    ("461500", "KODEX 미국배당다우존스액티브"),
    ("473460", "KODEX 미국S&P500TR액티브"),
    ("486450", "KODEX AI반도체핵심장비액티브"),
    ("497550", "KODEX 미국성장커버드콜액티브"),
    # TIGER (미래에셋자산운용) Active ETFs
    ("329200", "TIGER AI코리아그로스액티브"),
    ("411570", "TIGER 미국나스닥100TR액티브"),
    ("458730", "TIGER 미국배당다우존스액티브"),
    ("465330", "TIGER 미국S&P500TR액티브"),
    ("475720", "TIGER AI반도체핵심장비액티브"),
    ("486290", "TIGER 미국필라델피아반도체TR액티브"),
    # ACE (한국투자신탁운용) Active ETFs
    ("409820", "ACE 미국나스닥100TR액티브"),
    ("441680", "ACE 미국배당다우존스액티브"),
    ("461290", "ACE 미국S&P500TR액티브"),
    ("487230", "ACE AI반도체핵심장비액티브"),
    # RISE (KB자산운용) Active ETFs
    ("419890", "RISE 단기채권ESG액티브"),
    ("429760", "RISE 200TR액티브"),
    ("437080", "RISE 미국S&P500액티브"),
    ("441800", "RISE 미국나스닥100액티브"),
    ("449450", "RISE AI반도체소부장액티브"),
    # KIWOOM (키움투자자산운용) Active ETFs
    ("449780", "KIWOOM 200TR액티브"),
    ("456600", "KIWOOM 미국S&P500액티브"),
    # HANARO (NH-Amundi자산운용) Active ETFs
    ("418660", "HANARO 200TR액티브"),
    ("459580", "HANARO 글로벌반도체액티브"),
    # ARIRANG (한화자산운용) Active ETFs
    ("437870", "ARIRANG 200TR액티브"),
    ("461920", "ARIRANG 미국나스닥100액티브"),
    # SOL (신한자산운용) Active ETFs
    ("438330", "SOL 200TR액티브"),
    ("455030", "SOL 미국배당다우존스액티브"),
    # WOORI (우리자산운용) Active ETFs
    ("449170", "WOORI 200TR액티브"),
    # 1Q (하나자산운용) Active ETFs
    ("454860", "1Q K-바이오액티브"),
    ("475090", "1Q 미국나스닥100미국채혼합50액티브"),
    # KoAct (삼성액티브자산운용) Active ETFs
    ("470600", "KoAct 미국나스닥채권혼합50액티브"),
    # 채권 액티브 ETFs (Bond Active ETFs)
    ("411420", "KODEX 국고채3년액티브"),
    ("423160", "TIGER 국고채3년액티브"),
    ("433330", "ACE 국고채3년액티브"),
    ("447770", "RISE 국고채3년액티브"),
    ("461460", "KODEX 종합채권(AA-이상)액티브"),
    ("473750", "TIGER 종합채권(AA-이상)액티브"),
]


def get_active_etf_codes() -> List[str]:
    """Get list of active ETF codes.

    Returns:
        List of ETF ticker codes
    """
    return [code for code, _ in ACTIVE_ETF_CODES]


def get_active_etf_dict() -> Dict[str, str]:
    """Get dictionary of active ETF codes to names.

    Returns:
        Dict mapping ETF code to ETF name
    """
    return {code: name for code, name in ACTIVE_ETF_CODES}
