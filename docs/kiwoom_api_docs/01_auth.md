# 키움 REST API - OAuth 인증

## 개요
이 문서는 키움 REST API의 OAuth 인증 관련 API를 정리한 것입니다.

## 접근토큰발급

### 접근토큰 발급 (au10001)

#### 기본 정보

- **API ID**: `au10001`
- **Method**: `POST`
- **URL**: `/oauth2/token`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| grant_type | grant_type | Y |
| appkey | 앱키 | Y |
| secretkey | 시크릿키 | Y |
| expires_dt | 만료일 | Y |
| token_type | 토큰타입 | Y |
| token | 접근토큰 | Y |

#### Request Example

```json
{"grant_type": "client_credentials","appkey": "AxserEsdcredca.....","secretkey": "SEefdcwcforehDre2fdvc...."}
```

---

## 접근토큰폐기

### 접근토큰폐기 (au10002)

#### 기본 정보

- **API ID**: `au10002`
- **Method**: `POST`
- **URL**: `/oauth2/revoke`
- **모의투자 지원**: 예

#### Request Body

| 파라미터 | 설명 | 필수 |
|----------|------|------|
| appkey | 앱키 | Y |
| secretkey | 시크릿키 | Y |
| token | 접근토큰 | Y |

#### Request Example

```json
{"appkey": "AxserEsdcredca.....","secretkey": "SEefdcwcforehDre2fdvc....","token": "WQJCwyqInphKnR3bSRtB9NE1lv..."}
```

---
