"""
키움 REST API Python 클라이언트
==================================
Claude Code에서 활용하기 쉽게 구조화된 API 클라이언트

사용법:
    from kiwoom_api_client import KiwoomAPI
    
    api = KiwoomAPI(app_key="YOUR_APP_KEY", secret_key="YOUR_SECRET_KEY")
    api.get_token()
    
    # 주식 현재가 조회
    result = api.get_stock_price("005930")
"""

import requests
import json
from typing import Dict, Any, Optional
from dataclasses import dataclass
from enum import Enum
import os


class MarketType(Enum):
    """거래소 구분"""
    REAL = "real"      # 실전투자
    MOCK = "mock"      # 모의투자


@dataclass
class APIConfig:
    """API 설정"""
    REAL_DOMAIN = "https://api.kiwoom.com"
    MOCK_DOMAIN = "https://mockapi.kiwoom.com"
    WS_REAL_DOMAIN = "wss://api.kiwoom.com:10000"
    WS_MOCK_DOMAIN = "wss://mockapi.kiwoom.com:10000"


class KiwoomAPIError(Exception):
    """키움 API 에러"""
    def __init__(self, code: int, message: str):
        self.code = code
        self.message = message
        super().__init__(f"[{code}] {message}")


# ============================================================
# 오류 코드 정의
# ============================================================
ERROR_CODES = {
    1501: "API ID가 Null이거나 값이 없습니다",
    1504: "해당 URI에서는 지원하는 API ID가 아닙니다",
    1505: "해당 API ID는 존재하지 않습니다",
    1511: "필수 입력 값에 값이 존재하지 않습니다",
    1512: "Http header에 값이 설정되지 않았거나 읽을 수 없습니다",
    1513: "Http Header에 authorization 필드가 설정되어 있어야 합니다",
    1514: "입력으로 들어온 Http Header의 authorization 필드 형식이 맞지 않습니다",
    1515: "Http Header의 authorization 필드 내 Grant Type이 미리 정의된 형식이 아닙니다",
    1516: "Http Header의 authorization 필드 내 Token이 정의되어 있지 않습니다",
    1517: "입력 값 형식이 올바르지 않습니다",
    1687: "재귀 호출이 발생하여 API 호출을 제한합니다",
    1700: "허용된 요청 개수를 초과하였습니다",
    1901: "시장 코드값이 존재하지 않습니다",
    1902: "종목 정보가 없습니다",
    1999: "예기치 못한 에러가 발생했습니다",
    8001: "App Key와 Secret Key 검증에 실패했습니다",
    8002: "App Key와 Secret Key 검증에 실패했습니다",
    8003: "Access Token을 조회하는데 실패했습니다",
    8005: "Token이 유효하지 않습니다",
    8006: "Access Token을 생성하는데 실패했습니다",
    8009: "Access Token을 발급하는데 실패했습니다",
    8010: "Token을 발급받은 IP와 서비스를 요청한 IP가 동일하지 않습니다",
    8011: "grant_type이 들어오지 않았습니다",
    8012: "grant_type의 값이 맞지 않습니다",
    8015: "Access Token을 폐기하는데 실패했습니다",
    8016: "입력값에 Token이 들어오지 않았습니다",
    8020: "appkey 또는 secretkey가 들어오지 않았습니다",
    8030: "투자구분(실전/모의)이 달라서 Appkey를 사용할수가 없습니다",
    8031: "투자구분(실전/모의)이 달라서 Token를 사용할수가 없습니다",
    8040: "단말기 인증에 실패했습니다",
    8050: "지정단말기 인증에 실패했습니다",
    8103: "토큰 인증 또는 단말기인증에 실패했습니다",
}


# ============================================================
# API ID 상수 정의
# ============================================================
class APIID:
    """API ID 상수"""
    
    # OAuth 인증
    AUTH_TOKEN = "au10001"           # 접근토큰 발급
    AUTH_REVOKE = "au10002"          # 접근토큰 폐기
    
    # 종목정보
    STOCK_INFO = "ka10001"           # 주식기본정보요청
    STOCK_BROKER = "ka10002"         # 주식거래원요청
    STOCK_EXECUTION = "ka10003"      # 체결정보요청
    REAL_TIME_RANK = "ka00198"       # 실시간종목조회순위
    
    # 시세
    STOCK_QUOTE = "ka10004"          # 주식호가요청
    STOCK_OHLCV = "ka10005"          # 주식일주월시분요청
    STOCK_MINUTE = "ka10006"         # 주식시분요청
    STOCK_MARKET_INFO = "ka10007"    # 시세표성정보요청
    
    # 차트
    CHART_TICK = "ka10079"           # 주식틱차트조회요청
    CHART_MINUTE = "ka10080"         # 주식분봉차트조회요청
    CHART_DAILY = "ka10081"          # 주식일봉차트조회요청
    CHART_WEEKLY = "ka10082"         # 주식주봉차트조회요청
    CHART_MONTHLY = "ka10083"        # 주식월봉차트조회요청
    CHART_YEARLY = "ka10094"         # 주식년봉차트조회요청
    
    # 순위정보
    RANK_VOLUME = "ka10030"          # 당일거래량상위요청
    RANK_TRADE_AMOUNT = "ka10032"    # 거래대금상위요청
    RANK_FLUCTUATION = "ka10027"     # 전일대비등락률상위요청
    
    # 계좌
    ACCOUNT_BALANCE = "kt00004"      # 계좌평가현황요청
    ACCOUNT_DAILY_PL = "ka01690"     # 일별잔고수익률
    ACCOUNT_DEPOSIT = "kt00001"      # 예수금상세현황요청
    ACCOUNT_ORDER_STATUS = "kt00009" # 계좌별주문체결현황요청
    
    # 주문
    ORDER_BUY = "kt10000"            # 주식 매수주문
    ORDER_SELL = "kt10001"           # 주식 매도주문
    ORDER_MODIFY = "kt10002"         # 주식 정정주문
    ORDER_CANCEL = "kt10003"         # 주식 취소주문
    
    # 업종
    SECTOR_PRICE = "ka20001"         # 업종현재가요청
    SECTOR_STOCKS = "ka20002"        # 업종별주가요청
    SECTOR_ALL_INDEX = "ka20003"     # 전업종지수요청
    
    # ETF
    ETF_RETURN = "ka40001"           # ETF수익율요청
    ETF_INFO = "ka40002"             # ETF종목정보요청
    ETF_DAILY = "ka40003"            # ETF일별추이요청
    
    # 조건검색
    CONDITION_LIST = "ka10171"       # 조건검색 목록조회
    CONDITION_SEARCH = "ka10172"     # 조건검색 요청 일반
    CONDITION_REALTIME = "ka10173"   # 조건검색 요청 실시간
    
    # 실시간시세 (WebSocket)
    WS_ORDER_EXECUTION = "00"        # 주문체결
    WS_BALANCE = "04"                # 잔고
    WS_STOCK_QUOTE = "0A"            # 주식기세
    WS_STOCK_EXECUTION = "0B"        # 주식체결
    WS_STOCK_ORDERBOOK = "0D"        # 주식호가잔량
    WS_ETF_NAV = "0G"                # ETF NAV
    WS_SECTOR_INDEX = "0J"           # 업종지수
    WS_VI_ALERT = "1h"               # VI발동/해제


# ============================================================
# URL 매핑
# ============================================================
URL_MAP = {
    # OAuth
    APIID.AUTH_TOKEN: "/oauth2/token",
    APIID.AUTH_REVOKE: "/oauth2/revoke",
    
    # 종목정보/시세
    APIID.STOCK_INFO: "/api/dostk/stkinfo",
    APIID.STOCK_BROKER: "/api/dostk/stkinfo",
    APIID.STOCK_EXECUTION: "/api/dostk/stkinfo",
    APIID.STOCK_QUOTE: "/api/dostk/mrkt",
    APIID.STOCK_OHLCV: "/api/dostk/mrkt",
    APIID.STOCK_MINUTE: "/api/dostk/mrkt",
    APIID.STOCK_MARKET_INFO: "/api/dostk/mrkt",
    APIID.REAL_TIME_RANK: "/api/dostk/stkinfo",
    
    # 차트
    APIID.CHART_TICK: "/api/dostk/chart",
    APIID.CHART_MINUTE: "/api/dostk/chart",
    APIID.CHART_DAILY: "/api/dostk/chart",
    APIID.CHART_WEEKLY: "/api/dostk/chart",
    APIID.CHART_MONTHLY: "/api/dostk/chart",
    APIID.CHART_YEARLY: "/api/dostk/chart",
    
    # 순위
    APIID.RANK_VOLUME: "/api/dostk/ranking",
    APIID.RANK_TRADE_AMOUNT: "/api/dostk/ranking",
    APIID.RANK_FLUCTUATION: "/api/dostk/ranking",
    
    # 계좌
    APIID.ACCOUNT_BALANCE: "/api/dostk/acnt",
    APIID.ACCOUNT_DAILY_PL: "/api/dostk/acnt",
    APIID.ACCOUNT_DEPOSIT: "/api/dostk/acnt",
    APIID.ACCOUNT_ORDER_STATUS: "/api/dostk/acnt",
    
    # 주문
    APIID.ORDER_BUY: "/api/dostk/order",
    APIID.ORDER_SELL: "/api/dostk/order",
    APIID.ORDER_MODIFY: "/api/dostk/order",
    APIID.ORDER_CANCEL: "/api/dostk/order",
    
    # 업종
    APIID.SECTOR_PRICE: "/api/dostk/sector",
    APIID.SECTOR_STOCKS: "/api/dostk/sector",
    APIID.SECTOR_ALL_INDEX: "/api/dostk/sector",
    
    # ETF
    APIID.ETF_RETURN: "/api/dostk/etf",
    APIID.ETF_INFO: "/api/dostk/etf",
    APIID.ETF_DAILY: "/api/dostk/etf",
    
    # 조건검색
    APIID.CONDITION_LIST: "/api/dostk/cond",
    APIID.CONDITION_SEARCH: "/api/dostk/cond",
    APIID.CONDITION_REALTIME: "/api/dostk/cond",
}


class KiwoomAPI:
    """키움 REST API 클라이언트"""
    
    def __init__(
        self, 
        app_key: str, 
        secret_key: str, 
        market_type: MarketType = MarketType.REAL
    ):
        """
        Args:
            app_key: 앱키
            secret_key: 시크릿키
            market_type: 시장 구분 (실전/모의)
        """
        self.app_key = app_key
        self.secret_key = secret_key
        self.market_type = market_type
        self.token: Optional[str] = None
        self.token_expires: Optional[str] = None
        
        if market_type == MarketType.REAL:
            self.base_url = APIConfig.REAL_DOMAIN
            self.ws_url = APIConfig.WS_REAL_DOMAIN
        else:
            self.base_url = APIConfig.MOCK_DOMAIN
            self.ws_url = APIConfig.WS_MOCK_DOMAIN
    
    def _get_headers(self, api_id: str, cont_yn: str = "N", next_key: str = "") -> Dict:
        """공통 헤더 생성"""
        headers = {
            "Content-Type": "application/json;charset=UTF-8",
            "api-id": api_id,
        }
        
        if self.token:
            headers["authorization"] = f"Bearer {self.token}"
        
        if cont_yn == "Y":
            headers["cont-yn"] = cont_yn
            headers["next-key"] = next_key
        
        return headers
    
    def _request(
        self, 
        api_id: str, 
        body: Dict, 
        url: Optional[str] = None,
        cont_yn: str = "N",
        next_key: str = ""
    ) -> Dict:
        """API 요청 실행"""
        
        if url is None:
            url = URL_MAP.get(api_id, "/api/dostk/stkinfo")
        
        full_url = f"{self.base_url}{url}"
        headers = self._get_headers(api_id, cont_yn, next_key)
        
        response = requests.post(full_url, headers=headers, json=body)
        result = response.json()
        
        # 에러 체크
        if result.get("return_code", 0) != 0:
            code = result.get("return_code", -1)
            msg = result.get("return_msg", ERROR_CODES.get(code, "Unknown error"))
            raise KiwoomAPIError(code, msg)
        
        return result
    
    # ========================================
    # 인증 관련
    # ========================================
    
    def get_token(self) -> str:
        """
        접근토큰 발급 (au10001)
        
        Returns:
            발급된 토큰
        """
        body = {
            "grant_type": "client_credentials",
            "appkey": self.app_key,
            "secretkey": self.secret_key
        }
        
        result = self._request(
            APIID.AUTH_TOKEN, 
            body, 
            url="/oauth2/token"
        )
        
        self.token = result.get("token")
        self.token_expires = result.get("expires_dt")
        
        return self.token
    
    def revoke_token(self) -> bool:
        """
        접근토큰 폐기 (au10002)
        
        Returns:
            성공 여부
        """
        body = {
            "appkey": self.app_key,
            "secretkey": self.secret_key,
            "token": self.token
        }
        
        result = self._request(
            APIID.AUTH_REVOKE, 
            body, 
            url="/oauth2/revoke"
        )
        
        self.token = None
        return True
    
    # ========================================
    # 시세 조회
    # ========================================
    
    def get_stock_quote(self, stock_code: str) -> Dict:
        """
        주식호가요청 (ka10004)
        
        Args:
            stock_code: 종목코드 (6자리)
            
        Returns:
            호가 정보
        """
        body = {
            "stk_cd": stock_code
        }
        return self._request(APIID.STOCK_QUOTE, body)
    
    def get_stock_info(self, stock_code: str) -> Dict:
        """
        주식기본정보요청 (ka10001)
        
        Args:
            stock_code: 종목코드 (6자리)
            
        Returns:
            종목 기본 정보
        """
        body = {
            "stk_cd": stock_code
        }
        return self._request(APIID.STOCK_INFO, body)
    
    def get_stock_ohlcv(
        self, 
        stock_code: str, 
        period_type: str = "D",
        adj_price: str = "1"
    ) -> Dict:
        """
        주식일주월시분요청 (ka10005)
        
        Args:
            stock_code: 종목코드
            period_type: 기간구분 (D:일, W:주, M:월)
            adj_price: 수정주가여부 (0:미적용, 1:적용)
            
        Returns:
            OHLCV 데이터
        """
        body = {
            "stk_cd": stock_code,
            "base_dt": "",
            "upd_stkpc_tp": adj_price
        }
        return self._request(APIID.STOCK_OHLCV, body)
    
    # ========================================
    # 차트
    # ========================================
    
    def get_chart_daily(
        self, 
        stock_code: str,
        start_date: str = "",
        end_date: str = "",
        adj_price: str = "1"
    ) -> Dict:
        """
        주식일봉차트조회요청 (ka10081)
        
        Args:
            stock_code: 종목코드
            start_date: 시작일자 (YYYYMMDD)
            end_date: 종료일자 (YYYYMMDD)
            adj_price: 수정주가여부
            
        Returns:
            일봉 차트 데이터
        """
        body = {
            "stk_cd": stock_code,
            "base_dt": start_date,
            "upd_stkpc_tp": adj_price
        }
        return self._request(APIID.CHART_DAILY, body)
    
    def get_chart_minute(
        self, 
        stock_code: str,
        tick_range: str = "1"
    ) -> Dict:
        """
        주식분봉차트조회요청 (ka10080)
        
        Args:
            stock_code: 종목코드
            tick_range: 틱범위 (1, 3, 5, 10, 15, 30, 45, 60)
            
        Returns:
            분봉 차트 데이터
        """
        body = {
            "stk_cd": stock_code,
            "tick_rng": tick_range
        }
        return self._request(APIID.CHART_MINUTE, body)
    
    # ========================================
    # 순위 조회
    # ========================================
    
    def get_volume_rank(self, market: str = "0") -> Dict:
        """
        당일거래량상위요청 (ka10030)
        
        Args:
            market: 시장구분 (0:전체, 1:코스피, 2:코스닥)
            
        Returns:
            거래량 상위 종목
        """
        body = {
            "mrkt_tp": market
        }
        return self._request(APIID.RANK_VOLUME, body)
    
    def get_fluctuation_rank(self, market: str = "0", sort_type: str = "1") -> Dict:
        """
        전일대비등락률상위요청 (ka10027)
        
        Args:
            market: 시장구분 (0:전체, 1:코스피, 2:코스닥)
            sort_type: 정렬구분 (1:상승률, 2:하락률)
            
        Returns:
            등락률 상위 종목
        """
        body = {
            "mrkt_tp": market,
            "sort_tp": sort_type
        }
        return self._request(APIID.RANK_FLUCTUATION, body)
    
    # ========================================
    # 계좌
    # ========================================
    
    def get_account_balance(self) -> Dict:
        """
        계좌평가현황요청 (kt00004)
        
        Returns:
            계좌 평가 현황
        """
        body = {}
        return self._request(APIID.ACCOUNT_BALANCE, body)
    
    def get_deposit(self) -> Dict:
        """
        예수금상세현황요청 (kt00001)
        
        Returns:
            예수금 상세 현황
        """
        body = {}
        return self._request(APIID.ACCOUNT_DEPOSIT, body)
    
    # ========================================
    # 주문
    # ========================================
    
    def order_buy(
        self, 
        stock_code: str, 
        qty: int, 
        price: int = 0,
        order_type: str = "00"
    ) -> Dict:
        """
        주식 매수주문 (kt10000)
        
        Args:
            stock_code: 종목코드
            qty: 주문수량
            price: 주문가격 (시장가 주문시 0)
            order_type: 주문유형 (00:지정가, 03:시장가)
            
        Returns:
            주문 결과
        """
        body = {
            "stk_cd": stock_code,
            "ord_qty": str(qty),
            "ord_uv": str(price),
            "ord_tp": order_type
        }
        return self._request(APIID.ORDER_BUY, body)
    
    def order_sell(
        self, 
        stock_code: str, 
        qty: int, 
        price: int = 0,
        order_type: str = "00"
    ) -> Dict:
        """
        주식 매도주문 (kt10001)
        
        Args:
            stock_code: 종목코드
            qty: 주문수량
            price: 주문가격 (시장가 주문시 0)
            order_type: 주문유형 (00:지정가, 03:시장가)
            
        Returns:
            주문 결과
        """
        body = {
            "stk_cd": stock_code,
            "ord_qty": str(qty),
            "ord_uv": str(price),
            "ord_tp": order_type
        }
        return self._request(APIID.ORDER_SELL, body)
    
    # ========================================
    # 업종
    # ========================================
    
    def get_sector_price(self, sector_code: str) -> Dict:
        """
        업종현재가요청 (ka20001)
        
        Args:
            sector_code: 업종코드
            
        Returns:
            업종 현재가 정보
        """
        body = {
            "upjong_cd": sector_code
        }
        return self._request(APIID.SECTOR_PRICE, body)
    
    # ========================================
    # 조건검색
    # ========================================
    
    def get_condition_list(self) -> Dict:
        """
        조건검색 목록조회 (ka10171)
        
        Returns:
            저장된 조건검색 목록
        """
        body = {}
        return self._request(APIID.CONDITION_LIST, body)
    
    def search_by_condition(self, cond_idx: str, cond_name: str) -> Dict:
        """
        조건검색 요청 일반 (ka10172)
        
        Args:
            cond_idx: 조건식 인덱스
            cond_name: 조건식명
            
        Returns:
            조건검색 결과
        """
        body = {
            "cond_idx": cond_idx,
            "cond_nm": cond_name
        }
        return self._request(APIID.CONDITION_SEARCH, body)


# ============================================================
# 유틸리티 함수
# ============================================================

def load_api_spec(filepath: str = "api_spec.json") -> Dict:
    """API 스펙 JSON 파일 로드"""
    with open(filepath, 'r', encoding='utf-8') as f:
        return json.load(f)


def get_api_info(api_id: str, spec: Dict = None) -> Dict:
    """특정 API ID의 정보 조회"""
    if spec is None:
        spec = load_api_spec()
    return spec.get('apis', {}).get(api_id, {})


def search_apis(keyword: str, spec: Dict = None) -> list:
    """키워드로 API 검색"""
    if spec is None:
        spec = load_api_spec()
    
    results = []
    for api_id, api_info in spec.get('apis', {}).items():
        if keyword.lower() in api_info.get('name', '').lower() or \
           keyword.lower() in api_info.get('subcategory', '').lower():
            results.append({
                'api_id': api_id,
                **api_info
            })
    return results


if __name__ == "__main__":
    # 사용 예시
    print("키움 REST API 클라이언트")
    print("=" * 50)
    print("\n사용법:")
    print("""
    from kiwoom_api_client import KiwoomAPI, MarketType
    
    # 실전투자
    api = KiwoomAPI(app_key="...", secret_key="...")
    
    # 모의투자
    api = KiwoomAPI(app_key="...", secret_key="...", market_type=MarketType.MOCK)
    
    # 토큰 발급
    api.get_token()
    
    # 주식 호가 조회
    result = api.get_stock_quote("005930")
    print(result)
    """)
