package com.seniorlink.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;

@RestControllerAdvice(assignableTypes={OperacionesController.class,OperacionesLocalesController.class})
public class ErroresOperaciones {
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<Map<String,Object>> regla(ResponseStatusException e){
  int status=e.getStatusCode().value();
  String mensaje=status>=500?"No se pudo completar la operación.":e.getReason();
  if(mensaje==null||mensaje.isBlank())mensaje="No se pudo completar la operación.";
  return ResponseEntity.status(e.getStatusCode()).body(Map.of("status",status,"detail",mensaje));
 }
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,MissingServletRequestParameterException.class})
 public ResponseEntity<Map<String,Object>> formato(Exception e){
  return ResponseEntity.badRequest().body(Map.of("status",400,"detail","Revisa los campos obligatorios y el formato de los datos enviados."));
 }
 @ExceptionHandler(DataIntegrityViolationException.class)
 public ResponseEntity<Map<String,Object>> integridad(DataIntegrityViolationException e){
  return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status",409,"detail","La operación entra en conflicto con un registro existente o una restricción de datos. Actualiza la página y revisa los datos."));
 }
}
