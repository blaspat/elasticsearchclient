package org.blaspat.handler;

import org.blaspat.global.ClientStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

@Component
@RestControllerAdvice
public class ElasticsearchExceptionHandler  {
    @ExceptionHandler(CancellationException.class)
    public ResponseEntity<?> cancellationException(CancellationException e) {
        ClientStatus.setClientConnected(false);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "03");
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
