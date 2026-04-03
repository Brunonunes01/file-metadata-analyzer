package com.metascan.exception;

import com.metascan.dto.AntivirusScanStatus;
import com.metascan.dto.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PUBLIC_PROCESSING_ERROR = "Ocorreu um erro durante o processamento.";
    private static final String PUBLIC_PROCESSING_TITLE = "Erro ao processar arquivo";

    @ExceptionHandler(AntivirusThreatDetectedException.class)
    public ResponseEntity<ErrorResponseDto> handleAntivirusThreatDetected(AntivirusThreatDetectedException ex) {
        log.warn("Arquivo bloqueado por antivirus.");
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(AntivirusScanException.class)
    public ResponseEntity<ErrorResponseDto> handleAntivirusScanException(AntivirusScanException ex) {
        log.warn("Falha de scanner antivirus. status={}", ex.getStatus());
        return buildProcessingError(resolveAntivirusScanStatus(ex.getStatus()));
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
        log.warn("Requisicao multipart invalida: {}", ex.getClass().getSimpleName());
        return buildError(HttpStatus.BAD_REQUEST, "Requisicao multipart invalida.");
    }

    @ExceptionHandler(MetadataExtractionException.class)
    public ResponseEntity<ErrorResponseDto> handleMetadataExtractionException(MetadataExtractionException ex) {
        log.warn("Erro de processamento de metadata: {}", ex.getClass().getSimpleName());
        return buildProcessingError(HttpStatus.UNPROCESSABLE_ENTITY);
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
        log.error("Erro interno inesperado: {}", ex.getClass().getSimpleName());
        return buildProcessingError(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponseDto> buildError(HttpStatus status, String message) {
        return buildError(status, status.getReasonPhrase(), message);
    }

    private ResponseEntity<ErrorResponseDto> buildError(HttpStatus status, String error, String message) {
        ErrorResponseDto response = new ErrorResponseDto(
                status.value(),
                error,
                message,
                Instant.now()
        );
        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<ErrorResponseDto> buildProcessingError(HttpStatus status) {
        return buildError(status, PUBLIC_PROCESSING_TITLE, PUBLIC_PROCESSING_ERROR);
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
