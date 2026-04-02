package com.metascan.exception;

import com.metascan.dto.AntivirusScanStatus;
import com.metascan.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AntivirusThreatDetectedException.class)
    public ResponseEntity<ErrorResponseDto> handleAntivirusThreatDetected(AntivirusThreatDetectedException ex) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(AntivirusScanException.class)
    public ResponseEntity<ErrorResponseDto> handleAntivirusScanException(AntivirusScanException ex) {
        return buildError(resolveAntivirusScanStatus(ex.getStatus()), ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(BadRequestException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParam(MissingServletRequestParameterException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Parametro obrigatorio ausente: " + ex.getParameterName());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingPart(MissingServletRequestPartException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Parte obrigatoria ausente: " + ex.getRequestPartName());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponseDto> handleMultipartException(MultipartException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Requisicao multipart invalida.");
    }

    @ExceptionHandler(MetadataExtractionException.class)
    public ResponseEntity<ErrorResponseDto> handleMetadataExtractionException(MetadataExtractionException ex) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "Metodo HTTP nao suportado para este endpoint.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "Arquivo excede o tamanho maximo permitido para upload.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Parametro invalido na requisicao.";
        }
        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.");
    }

    private ResponseEntity<ErrorResponseDto> buildError(HttpStatus status, String message) {
        ErrorResponseDto response = new ErrorResponseDto(
                status.value(),
                status.getReasonPhrase(),
                message,
                Instant.now()
        );
        return ResponseEntity.status(status).body(response);
    }

    private HttpStatus resolveAntivirusScanStatus(AntivirusScanStatus status) {
        if (status == null) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

        return switch (status) {
            case NOT_INSTALLED, TIMEOUT, SCAN_ERROR -> HttpStatus.SERVICE_UNAVAILABLE;
            case INFECTED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case CLEAN -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
