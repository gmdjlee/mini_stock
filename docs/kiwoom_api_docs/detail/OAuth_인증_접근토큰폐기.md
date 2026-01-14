# OAuth 인증 > 접근토큰폐기

이 문서는 접근토큰폐기 관련 API 1개를 포함합니다.

## API 목록

| API ID | API 명 | URL | 모의투자 | WebSocket |
|--------|--------|-----|----------|-----------|
| au10002 | 접근토큰폐기 | /oauth2/revoke | ✓ |  |

---

## 접근토큰폐기

**API ID**: `au10002`

### 기본 정보

- Method: `POST`
- URL: `/oauth2/revoke`
- 모의투자: 지원

### Request Parameters

```python
params = {
    "appkey": "",  # Required - 앱키
    "secretkey": "",  # Required - 시크릿키
    "token": "",  # Required - 접근토큰
}
```

### Request Example

```json
{"appkey": "AxserEsdcredca.....","secretkey": "SEefdcwcforehDre2fdvc....","token": "WQJCwyqInphKnR3bSRtB9NE1lv..."}
```

---
