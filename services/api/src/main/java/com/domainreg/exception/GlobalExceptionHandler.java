package com.domainreg.exception;

import com.domainreg.dto.ErrorResponse;
import com.domainreg.service.AuthService.AuthException;
import com.domainreg.service.OrderService.OrderException;
import com.domainreg.service.PaymentService.PaymentException;
import com.domainreg.service.PlatformDomainService.PlatformDomainException;
import com.domainreg.service.BoardService.BoardException;
import com.domainreg.service.TermsService.TermsException;
import com.domainreg.core.service.PaidGateEnforcer.PaymentRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException e) {
        HttpStatus status = switch (e.getCode()) {
            case "INVALID_USERNAME", "USERNAME_EXISTS", "INVALID_EMAIL_DOMAIN", "EMAIL_EXISTS", "INVALID_TOKEN" -> HttpStatus.BAD_REQUEST;
            case "EMAIL_NOT_VERIFIED" -> HttpStatus.FORBIDDEN;
            case "ACCOUNT_LOCKED" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.UNAUTHORIZED;
        };
        return ResponseEntity.status(status)
            .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(PaymentRequiredException.class)
    public ResponseEntity<ErrorResponse> handlePaymentRequired(PaymentRequiredException e) {
        return ResponseEntity.status(402)
            .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("INVALID_STATE", e.getMessage()));
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ErrorResponse> handleOrder(OrderException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePayment(PaymentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(PlatformDomainException.class)
    public ResponseEntity<ErrorResponse> handlePlatformDomain(PlatformDomainException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(TermsException.class)
    public ResponseEntity<ErrorResponse> handleTerms(TermsException e) {
        HttpStatus status = switch (e.getCode()) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INVALID_STATE" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
            .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(BoardException.class)
    public ResponseEntity<ErrorResponse> handleBoard(BoardException e) {
        HttpStatus status = switch (e.getCode()) {
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "FORBIDDEN", "INVALID_PASSWORD" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
            .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        var errors = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .toList();
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_FAILED", String.join(", ", errors)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("Unhandled exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }
}
