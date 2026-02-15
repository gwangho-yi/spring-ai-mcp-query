# Spring AI MCP Query Tools

AI 에이전트(Claude Code 등)가 MySQL 데이터베이스에 안전하게 쿼리를 실행할 수 있도록 MCP(Model Context Protocol) 서버를 제공합니다.

## 제공 도구

| 도구 | 설명 |
|------|------|
| `select_query` | SELECT 쿼리 실행, 결과를 마크다운 테이블로 반환 |
| `explain_query` | EXPLAIN 실행 계획 분석 (인덱스 사용 여부 확인) |
| `explain_analyze_query` | EXPLAIN ANALYZE 실측 분석 (MySQL 8.0.18+, 실제 실행) |
| `show_query` | SHOW 명령어 실행 (테이블 목록, 컬럼 정보 등) |

**안전 장치**: SELECT/SHOW 문만 허용되며, INSERT/UPDATE/DELETE 등 데이터 변경 쿼리는 차단됩니다.

## 기술 스택

- Kotlin 1.9.25, JDK 17
- Spring Boot 3.5.x
- Spring AI 1.1.2 (MCP Server WebMVC)
- mcp-annotations 0.8.0

## 빠른 시작

### 1. 설정

`src/main/resources/application-local.yml`에 DB 연결 정보를 설정합니다:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: root
    password: your_password
```

### 2. 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

서버가 시작되면 SSE 엔드포인트가 활성화됩니다:
- SSE: `http://localhost:8080/sse`
- Message: `http://localhost:8080/mcp/messages`

## Claude Code 연결

### 프로젝트별 설정 (`.mcp.json`)

프로젝트 루트에 `.mcp.json` 파일을 생성합니다:

```json
{
  "mcpServers": {
    "mcp-query": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

### 글로벌 설정 (`~/.claude.json`)

모든 프로젝트에서 사용하려면 `~/.claude.json`에 추가합니다:

```json
{
  "mcpServers": {
    "mcp-query": {
      "type": "sse",
      "url": "http://localhost:8080/sse"
    }
  }
}
```

## 사용 예시

Claude Code에서 MCP 서버에 연결되면 다음과 같이 사용할 수 있습니다:

```
사용자: member 테이블에서 최근 가입한 회원 5명을 조회해줘
Claude: select_query("SELECT * FROM member ORDER BY created_at DESC LIMIT 5")

사용자: team 테이블의 인덱스 상태를 확인해줘
Claude: show_query("SHOW INDEX FROM team")

사용자: 이 쿼리의 실행 계획을 분석해줘
Claude: explain_query("SELECT * FROM member WHERE username = 'test'")
```

## 보안 주의사항

- **로컬 개발 전용**: 이 서버는 로컬 개발 환경에서만 사용하세요.
- **읽기 전용**: SELECT/SHOW만 허용되지만, DB 계정 자체도 읽기 전용 권한만 부여하는 것을 권장합니다.
- **네트워크 노출 금지**: 외부 네트워크에 노출하지 마세요.
- **민감 데이터 주의**: 개인정보가 포함된 테이블 조회 시 주의하세요.

## 라이선스

MIT
