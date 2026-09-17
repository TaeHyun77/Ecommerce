# member-service 소셜 로그인(OAuth2) + JWT 인증 설계

- 작성일: 2026-09-17
- 대상 모듈: `member-service`
- 상태: 승인됨 (구현 계획 수립 단계로 진행)

## 1. 배경 및 목표

`member-service`에는 아직 회원 관련 엔티티/로직이 없다. 실서비스 운영을 목표로 하는
이 프로젝트에서, 카카오/구글 소셜 로그인만으로 회원가입·로그인을 처리하고 자체 발급한
JWT로 인증을 유지하는 구조를 구축한다.

현재는 API Gateway가 없어 각 서비스(`member-service`, `product-service` 등)가 요청을
직접 받는다. 다만 향후 Gateway 도입이 예정되어 있으므로, 인증 검증 로직이 특정 지점에
묶이지 않고 Gateway 도입 시 검증 위치만 옮기면 되는 stateless 구조를 전제로 한다.

## 2. 요구사항 요약

- 소셜 로그인만 지원 (자체 이메일/비밀번호 회원가입 없음)
- 지원 제공자: 카카오, 구글
- 클라이언트: 웹(SPA/서버사이드)
- Refresh Token은 Redis에 저장하여 즉시 무효화(로그아웃, 탈취 대응)가 가능해야 함
- 실서비스 운영 목표 — 보안(토큰 탈취 대응)과 장애 대응(재발급 실패 처리 등)을 고려

## 3. 검토한 접근 방식과 선택 이유

| 방식 | 요약 | 결론 |
|---|---|---|
| **A. Spring Security OAuth2 Client + Authorization Code Flow** | 표준 OAuth2 로그인 흐름을 Spring Security로 처리, 성공 후 자체 JWT 발급 | **채택** |
| B. 프론트 SDK 연동 + ID Token 검증 | 프론트에서 소셜 SDK로 로그인 후 id_token을 백엔드로 전달, 백엔드가 서명 검증 | 미채택 — Spring Security 표준 지원 밖이라 제공자별 검증 로직을 직접 구현해야 하고, 인증 로직 일부가 프론트로 분산됨 |
| C. 세션 기반 인증(Spring Session + Redis) | OAuth는 로그인 수단으로만 사용, 이후 세션 쿠키로 인증 | 미채택 — 서비스 간 세션 공유가 필요해 향후 Gateway 도입/MSA 확장 방향과 어긋나고, Redis 기반 Refresh Token 관리와 역할이 중복됨 |

A안을 채택한 이유: Client Secret이 서버에만 존재해 보안적으로 안전하고, Spring Security
표준 기능을 활용해 구현 리스크가 낮으며, stateless JWT 검증 구조가 향후 Gateway 도입과
자연스럽게 맞물린다.

## 4. 회원 도메인 모델

```
Member
- id (PK)
- provider (Enum: KAKAO, GOOGLE)
- providerId (소셜 제공자가 내려주는 고유 식별자)
- email
- nickname
- profileImageUrl
- role (Enum: USER, ADMIN)
- BaseTimeEntity 상속 (createdAt, updatedAt)
```

제약: `(provider, providerId)` 복합 유니크 → 동일 소셜 계정 중복 가입 방지.

소셜 로그인만 지원하므로 별도의 회원가입 완료 절차 없이, 최초 로그인 시점에 즉시
회원으로 저장한다(추가 정보 입력 단계 없음).

## 5. 인증 흐름 (로그인)

1. 프론트 → `GET /oauth2/authorization/{provider}` (Spring Security 기본 제공 엔드포인트) →
   카카오/구글 로그인 페이지로 리다이렉트
2. 로그인 성공 → 등록된 redirect-uri(`/login/oauth2/code/{provider}`)로 콜백
3. Spring Security가 토큰 교환 및 사용자 정보 조회 후 `CustomOAuth2UserService.loadUser()` 호출
4. `(provider, providerId)`로 기존 회원 조회 → 없으면 신규 저장, 있으면 조회
5. `OAuth2SuccessHandler`에서 자체 Access/Refresh JWT 발급
6. Access Token은 프론트로 전달, Refresh Token은 **httpOnly + Secure 쿠키**로 전달

> **가정**: Refresh Token을 httpOnly 쿠키에 담는 것을 기본값으로 한다. XSS로 인한 탈취를
> 막기 위한 표준적인 선택이며, 사용자 승인하에 확정되었다.

## 6. JWT 구성

- **Access Token**: 만료 30분, payload에 `memberId`, `role` 포함. HMAC-SHA256 서명(공유
  시크릿)으로 각 서비스가 직접 검증하는 stateless 구조. 향후 Gateway 도입 시 검증 지점만
  Gateway로 옮기면 되므로 현재 구조와 충돌하지 않는다.
- **Refresh Token**: 만료 14일, JWT로 발급하되 Redis에 `refresh:{memberId} → refreshToken`
  형태로 저장해 서버 측 무효화 근거로 사용한다.

## 7. 토큰 재발급 (Rotation)

`POST /api/auth/reissue`

1. 쿠키의 Refresh Token 수신 → 서명/만료 검증
2. Redis에 저장된 값과 일치 확인
3. 불일치 시 탈취 의심으로 해당 memberId의 Redis 키 즉시 삭제(강제 재로그인 유도)
4. 일치하면 Access + Refresh 모두 재발급(Refresh Rotation), Redis 값 갱신

## 8. 로그아웃

`POST /api/auth/logout`: Redis에서 `refresh:{memberId}` 삭제 + Refresh Token 쿠키 만료.
Access Token은 만료가 짧아(30분) 별도 블랙리스트 없이 자연 만료로 처리한다. 실서비스
확장 단계에서 필요성이 확인되면 블랙리스트 도입을 검토한다.

## 9. 예외 처리

기존 컨벤션(단일 커스텀 예외 + ErrorCode enum + `@ControllerAdvice`)을 따른다.

- `AuthException(RuntimeException)` + `AuthErrorCode`: `INVALID_TOKEN`, `EXPIRED_TOKEN`,
  `REFRESH_TOKEN_NOT_FOUND`, `REFRESH_TOKEN_MISMATCH` 등
- 토큰 없음/만료/서명 불일치 → 401 Unauthorized
- 권한 부족(role 기반) → 403 Forbidden

## 10. 패키지/계층 구조

```
member/
  domain/         Member, Provider, Role
  repository/     MemberRepository
security/
  CustomOAuth2UserService
  OAuth2SuccessHandler
  JwtProvider
  JwtAuthenticationFilter
auth/
  AuthController   (재발급, 로그아웃)
  AuthService
  AuthException, AuthErrorCode
```

Controller-Service-Repository 책임 분리, 생성자 주입 원칙을 그대로 따른다.

## 11. 테스트 전략

- `Member` 엔티티 단위 테스트 (기존 `ProductTest`, `DiscountTest`와 동일한 패턴 — 생성자/
  비즈니스 메서드 검증)
- `JwtProvider` 단위 테스트 (토큰 생성/파싱/만료 검증)
- `AuthService` 재발급/로그아웃 로직 테스트 (Redis는 테스트용 embedded 또는 Mock 사용)

## 12. 범위 밖(Out of Scope)

- 자체 이메일/비밀번호 회원가입
- API Gateway 도입 자체 (향후 별도 설계)
- Access Token 블랙리스트 (필요성이 확인되면 후속 설계)
