package com.example.mcpquery

import org.springaicommunity.mcp.annotation.McpTool
import org.springaicommunity.mcp.annotation.McpToolParam
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * SQL 쿼리 실행 MCP 도구.
 *
 * Claude Code 등 AI 에이전트가 데이터베이스 쿼리를 실행할 수 있도록 도구를 제공합니다.
 */
@Component
class QueryMcpTools(
    private val jdbcTemplate: JdbcTemplate,
) {

    @McpTool(
        name = "select_query",
        description = """
            SQL SELECT 쿼리를 실행하고 결과를 마크다운 테이블 형식으로 반환합니다.

            파라미터:
            - query: 실행할 SQL SELECT 문

            반환값:
            - 성공: 마크다운 테이블 형식의 쿼리 결과
            - 실패: "QUERY_FAILED: {에러 메시지}" 형식의 에러 문자열

            예시:
            - "SELECT * FROM member LIMIT 10"
            - "SELECT id, name FROM team WHERE season = '024'"
        """,
    )
    fun selectQuery(
        @McpToolParam(description = "실행할 SQL SELECT 문") query: String,
    ): String {
        val trimmedQuery = query.trim()

        if (!trimmedQuery.uppercase().startsWith("SELECT")) {
            return "QUERY_FAILED: SELECT 문만 허용됩니다."
        }

        return runCatching {
            val results = jdbcTemplate.queryForList(trimmedQuery)

            if (results.isEmpty()) {
                return@runCatching "결과 없음 (0행)"
            }

            toMarkdownTable(results)
        }.getOrElse { e ->
            "QUERY_FAILED: ${e.message}"
        }
    }

    @McpTool(
        name = "explain_query",
        description = """
            SQL 쿼리의 실행 계획을 분석합니다. 인덱스 사용 여부와 쿼리 최적화에 활용합니다.

            파라미터:
            - query: 분석할 SQL SELECT 문

            반환값:
            - 성공: 실행 계획 마크다운 테이블
            - 실패: "EXPLAIN_FAILED: {에러 메시지}"

            주요 컬럼 해석:
            - type: ALL(풀스캔), index, range, ref, eq_ref, const (뒤로 갈수록 좋음)
            - key: 사용된 인덱스 (NULL이면 인덱스 미사용)
            - rows: 예상 스캔 행 수
            - Extra: Using index(커버링), Using filesort(정렬), Using temporary(임시테이블)
        """,
    )
    fun explainQuery(
        @McpToolParam(description = "분석할 SQL SELECT 문") query: String,
    ): String {
        val trimmedQuery = query.trim()

        if (!trimmedQuery.uppercase().startsWith("SELECT")) {
            return "EXPLAIN_FAILED: SELECT 문만 허용됩니다."
        }

        return runCatching {
            val results = jdbcTemplate.queryForList("EXPLAIN $trimmedQuery")

            if (results.isEmpty()) {
                return@runCatching "실행 계획 없음"
            }

            toMarkdownTable(results)
        }.getOrElse { e ->
            "EXPLAIN_FAILED: ${e.message}"
        }
    }

    @McpTool(
        name = "explain_analyze_query",
        description = """
            SQL 쿼리를 실제 실행하여 실행 계획과 실측 시간을 분석합니다. (MySQL 8.0.18+)

            파라미터:
            - query: 분석할 SQL SELECT 문

            반환값:
            - 성공: 트리 형태의 실행 계획 (실제 실행 시간, 행 수 포함)
            - 실패: "EXPLAIN_ANALYZE_FAILED: {에러 메시지}"

            주의: 실제로 쿼리가 실행되므로 무거운 쿼리는 주의하세요.
        """,
    )
    fun analyzeQuery(
        @McpToolParam(description = "분석할 SQL SELECT 문") query: String,
    ): String {
        val trimmedQuery = query.trim()

        if (!trimmedQuery.uppercase().startsWith("SELECT")) {
            return "EXPLAIN_ANALYZE_FAILED: SELECT 문만 허용됩니다."
        }

        return runCatching {
            val results = jdbcTemplate.queryForList("EXPLAIN ANALYZE $trimmedQuery")

            if (results.isEmpty()) {
                return@runCatching "실행 계획 없음"
            }

            results.joinToString("\n") { row ->
                row.values.firstOrNull()?.toString() ?: ""
            }
        }.getOrElse { e ->
            "EXPLAIN_ANALYZE_FAILED: ${e.message}"
        }
    }

    @McpTool(
        name = "show_query",
        description = """
            MySQL SHOW 명령어를 실행하고 결과를 마크다운 테이블 형식으로 반환합니다.

            파라미터:
            - query: 실행할 SHOW 문

            반환값:
            - 성공: 마크다운 테이블 형식의 결과
            - 실패: "SHOW_FAILED: {에러 메시지}" 형식의 에러 문자열

            예시:
            - "SHOW TABLES"
            - "SHOW CREATE TABLE member"
            - "SHOW INDEX FROM member"
            - "SHOW COLUMNS FROM team"
            - "SHOW TABLE STATUS LIKE 'member'"
        """,
    )
    fun showQuery(
        @McpToolParam(description = "실행할 SHOW 문") query: String,
    ): String {
        val trimmedQuery = query.trim()

        if (!trimmedQuery.uppercase().startsWith("SHOW")) {
            return "SHOW_FAILED: SHOW 문만 허용됩니다."
        }

        return runCatching {
            val results = jdbcTemplate.queryForList(trimmedQuery)

            if (results.isEmpty()) {
                return@runCatching "결과 없음 (0행)"
            }

            toMarkdownTable(results)
        }.getOrElse { e ->
            "SHOW_FAILED: ${e.message}"
        }
    }

    private fun toMarkdownTable(results: List<Map<String, Any?>>): String {
        val columns = results.first().keys.toList()

        val sb = StringBuilder()

        // 헤더
        sb.append("| ").append(columns.joinToString(" | ")).append(" |\n")

        // 구분선
        sb.append("|").append(columns.joinToString("|") { "---" }).append("|\n")

        // 데이터 행
        results.forEach { row ->
            sb.append("| ")
            sb.append(columns.joinToString(" | ") { col -> row[col]?.toString() ?: "NULL" })
            sb.append(" |\n")
        }

        sb.append("\n(${results.size}행)")

        return sb.toString()
    }
}
