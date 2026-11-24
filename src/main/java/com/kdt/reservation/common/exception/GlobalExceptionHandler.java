package com.kdt.reservation.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 컨트롤러에서 발생한 예외를 명세의 에러 응답으로 바꾼다
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MSG_REQUIRED = "필수 값입니다.";
    private static final String MSG_BOOLEAN_EXPECTED = "true 또는 false로 입력해 주세요.";
    private static final String MSG_INVALID_FORMAT = "형식이 올바르지 않습니다.";

    // 서비스가 던진 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException err) {
        ErrorCode errorCode = err.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    // 컨트롤러 메서드 파라미터의 제약 위반
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException err) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ParameterValidationResult result : err.getParameterValidationResults()) {
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError f
                        ? f.getField()
                        : result.getMethodParameter().getParameterName();
                fieldErrors.putIfAbsent(field, error.getDefaultMessage());
            }
        }
        return badRequest(fieldErrors);
    }

    // @Valid 요청 본문의 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException err) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : err.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return badRequest(fieldErrors);
    }

    // @Validated 컨트롤러의 파라미터 제약 위반
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException err) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : err.getConstraintViolations()) {
            String name = "";
            for (Path.Node node : violation.getPropertyPath()) {
                name = node.getName();
            }
            fieldErrors.putIfAbsent(name, violation.getMessage());
        }
        return badRequest(fieldErrors);
    }

    // 필수 쿼리 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException err) {
        return badRequest(Map.of(err.getParameterName(), MSG_REQUIRED));
    }

    // 경로·쿼리 값의 타입 변환 실패
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException err) {
        return badRequest(Map.of(err.getName(), MSG_INVALID_FORMAT));
    }

    // 요청 본문 JSON의 문법 오류와 필드 타입 불일치
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException err) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (findCause(err, StreamReadException.class) == null) {
            JacksonException cause = findCause(err, JacksonException.class);
            if (cause != null && !cause.getPath().isEmpty()) {
                String field = toFieldPath(cause.getPath());
                if (!field.isEmpty()) {
                    fieldErrors.putIfAbsent(field, typeMismatchMessage(cause));
                }
            }
        }
        return badRequest(fieldErrors);
    }

    // 필드별 오류를 400 INVALID_INPUT 응답으로 만든다
    private static ResponseEntity<ErrorResponse> badRequest(Map<String, String> fieldErrors) {
        List<ErrorResponse.FieldError> errors = fieldErrors.entrySet().stream()
                .map(entry -> new ErrorResponse.FieldError(entry.getKey(), entry.getValue()))
                .toList();
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT, errors));
    }

    // 본문 필드의 타입 불일치 안내 문구를 고른다
    private static String typeMismatchMessage(JacksonException cause) {
        if (cause instanceof MismatchedInputException mismatched) {
            Class<?> target = mismatched.getTargetType();
            if (target == Boolean.class || target == boolean.class) {
                return MSG_BOOLEAN_EXPECTED;
            }
        }
        return MSG_INVALID_FORMAT;
    }

    // 예외의 원인 체인에서 주어진 타입을 찾는다
    private static <T extends Throwable> T findCause(Throwable root, Class<T> type) {
        for (Throwable t = root; t != null && t.getCause() != t; t = t.getCause()) {
            if (type.isInstance(t)) return type.cast(t);
        }
        return null;
    }

    // Jackson 경로를 items[0].name 형태의 필드 경로로 바꾼다
    private static String toFieldPath(List<JacksonException.Reference> path) {
        StringBuilder sb = new StringBuilder();
        for (JacksonException.Reference ref : path) {
            if (ref.getPropertyName() != null) {
                if (!sb.isEmpty()) sb.append('.');
                sb.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }
}
