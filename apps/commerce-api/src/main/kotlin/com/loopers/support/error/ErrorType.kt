package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    /** 범용 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase, "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.reasonPhrase, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.reasonPhrase, "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.reasonPhrase, "이미 존재하는 리소스입니다."),

    /** 회원 도메인 에러 */
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "E001", "이미 존재하는 로그인 ID입니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "E002", "비밀번호 형식이 올바르지 않습니다."),
    INVALID_LOGIN_ID_FORMAT(HttpStatus.BAD_REQUEST, "E003", "로그인 ID는 영문과 숫자만 허용합니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "E004", "이메일 형식이 올바르지 않습니다."),
    PASSWORD_CONTAINS_BIRTHDATE(HttpStatus.BAD_REQUEST, "E005", "비밀번호에 생년월일을 포함할 수 없습니다."),
    SAME_PASSWORD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "E006", "현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "E007", "회원을 찾을 수 없습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "E008", "인증에 실패했습니다."),
    INVALID_NAME_FORMAT(HttpStatus.BAD_REQUEST, "E009", "이름 형식이 올바르지 않습니다."),
    INVALID_BIRTHDATE_FORMAT(HttpStatus.BAD_REQUEST, "E010", "생년월일 형식이 올바르지 않습니다."),
}
