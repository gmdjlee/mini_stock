# OAuth 인증 > 접근토큰발급

이 문서는 접근토큰발급 관련 API 1개를 포함합니다.

## API 목록

| API ID | API 명 | URL | 모의투자 | WebSocket |
|--------|--------|-----|----------|-----------|
| au10001 | 접근토큰 발급 | /oauth2/token | ✓ |  |

---

## 접근토큰 발급

**API ID**: `au10001`

### 기본 정보

- Method: `POST`
- URL: `/oauth2/token`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "grant_type": "",  # Required - grant_type
    "appkey": "",  # Required - 앱키
    "secretkey": "",  # Required - 시크릿키
    "expires_dt": "",  # Required - 만료일
    "token_type": "",  # Required - 토큰타입
    "token": "",  # Required - 접근토큰
}
```

### Request Example

```json
{"grant_type": "client_credentials","appkey": "AxserEsdcredca.....","secretkey": "SEefdcwcforehDre2fdvc...."}
```

---
