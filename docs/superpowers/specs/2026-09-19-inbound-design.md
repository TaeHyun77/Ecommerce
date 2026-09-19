# 입고(inbound) 설계

## 요약

product-service 안에 `inbound` 패키지를 두고 입고를 처리한다. Kafka는 쓰지 않는다.
입고 예정서는 인터페이스 테이블에 원본을 저장한 뒤 스케줄러가 비동기로 입고 예정을 만들고, 상품 미등록은 DB 기반으로 재시도한다.
재고는 입고 확정의 양품 수량만큼만 원자적으로 늘리며, 입고 예정 비관적 락과 입고 확정 번호로 중복 반영을 막는다.

## 결정 사항과 근거

| 결정 | 근거 |
|---|---|
| product-service 내부 처리, Kafka 미사용 | 같은 서비스 안에서는 아웃박스 폴러가 이미 있어 Kafka가 경유지만 늘린다. Kafka는 서비스 간 연동(주문/결제/배송)에 도입한다 |
| 인터페이스 테이블 1개 + JSON 원본 | 형식 오류는 수신 시 동기로 400 응답, 비동기 재시도는 기반 정보 미동기화에만 사용 |
| 한 SKU라도 미등록이면 예정서 전체 대기 | 예정서 1건 = 입고 예정 1건으로 상태를 단순하게 유지 |
| 공급사는 코드만 저장 | 재시도 시나리오는 상품 미등록만으로 재현 가능 |
| 고정 간격 재시도, 설정값 분리 | 데모용 짧은 기본값 (10초 폴링, 1분 간격, 최대 10회) |
| 예정서 1건당 입고 확정 1회 | 분할 입고 제외 — 확정 테이블 없이 입고 예정에 확정 번호와 수량 기록 |
| 입고 확정 시 비관적 락 | 동시 중복 확정 시 두 번째 요청이 대기 후 COMPLETED를 보고 멱등 응답 |

## 테이블

- `inbound_interface`: asn_no(유니크), payload(JSON), status(PENDING/DONE/FAILED), retry_count, next_retry_at, fail_reason, received_at
- `inbound_expectation`: asn_no(유니크), supplier_code, expected_arrival_at, status(EXPECTED/COMPLETED), receipt_no(유니크), completed_at
- `inbound_expectation_line`: expectation_id, product_id, product_code, expected_quantity, accepted_quantity, rejected_quantity

## 흐름

1. **수신** `POST /api/partner/inbound-expectations` — `@Valid` 형식 검증, 중복 SKU 라인 400, PENDING 저장 후 202
   - 같은 asn_no 재수신: 기존 건이 FAILED면 정정본으로 보고 원본 교체 후 PENDING, 그 외(PENDING/DONE)는 저장 없이 성공
2. **스케줄러** — `PENDING AND next_retry_at <= now`를 최대 100건 조회해 건별 트랜잭션으로 처리
   - 모든 SKU 존재: 입고 예정(EXPECTED) 생성 + 인터페이스 DONE (같은 트랜잭션)
   - SKU 미등록: retry_count 증가, next_retry_at 연기, 최대 초과 시 FAILED
   - 역직렬화 실패: 즉시 FAILED
   - 예상하지 못한 예외: 별도 트랜잭션에서 재시도 횟수 증가 (무한 반복 방지)
3. **입고 확정** `POST /internal/inbound-receipts` — 한 트랜잭션
   - 입고 예정 `SELECT ... FOR UPDATE` 조회 (없으면 404)
   - COMPLETED: 같은 확정 번호면 204(멱등), 다르면 409
   - 예정서에 없는 SKU 400, 양품+불량 > 예정 수량 400, 요청에 없는 SKU는 0/0
   - 양품 > 0인 라인만 `UPDATE inventory SET quantity = quantity + ?` (갱신 0건이면 예외로 롤백)
   - COMPLETED 기록 후 204

## 실패 건 복구

FAILED는 재시도로 해결되지 않아 사람이 조치해야 하는 상태다. 담당자는 `GET /api/admin/inbound-interfaces?status=FAILED`로 실패 사유(누락 상품코드 전체)를 확인하고, 원인에 따라 복구한다.

- 상품 등록 지연: 담당자가 상품을 등록한 뒤 `POST /api/admin/inbound-interfaces/{asnNo}/retry` 호출 — FAILED만 허용(아니면 409), 재시도 횟수 0으로 초기화, 실패 사유는 성공 시까지 유지
- 공급사 상품코드 오기: 상품을 등록하면 가짜 상품이 생기므로 등록하지 않고, 공급사가 같은 asn_no로 정정본을 재전송

## 테스트

- 단위: InboundInterface, InboundExpectation 도메인 규칙 / 처리기, 입고 확정 서비스(Mockito) / 컨트롤러 슬라이스
- 통합(Testcontainers MySQL): 같은 입고 확정 2건을 동시에 보내도 재고는 한 번만 증가
- 수동(`http/inbound.http`): 정상 흐름, 재시도 후 성공(SKU-0005), 멱등성, 재시도 소진

## 범위 제외

예정서 수정·취소, 분할 입고, 공급사 검증, 로트·소비기한, 회송, 입고 상태 조회 API, 다중 인스턴스(`SKIP LOCKED`), 동시 중복 수신의 409 처리
