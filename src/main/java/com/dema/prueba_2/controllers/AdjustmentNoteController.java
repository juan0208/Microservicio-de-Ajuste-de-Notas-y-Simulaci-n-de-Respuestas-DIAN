package com.dema.prueba_2.controllers;

import com.dema.prueba_2.models.responses.ServerResponse;
import com.dema.prueba_2.services.NoteAdjustmentService;
import com.dema.prueba_2.services.NoteTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Controlador REST para procesar notas de ajuste.
 * Proporciona un endpoint para cargar y procesar archivos XML según el tipo de nota especificado.
 *
 * @since 29 de octubre de 2024
 * @autor Juan Alejandro Londoño Lopez
 */

@RestController
@RequestMapping("/api/note")
public class AdjustmentNoteController {

    @Autowired
    private NoteAdjustmentService noteAdjustmentService;

    /**
     * Endpoint para procesar una nota de ajuste.
     * Valida el tipo de nota y el archivo proporcionado, luego lo envía al servicio de ajuste.
     *
     * @param noteType Tipo de nota de ajuste (debe ser 21 o 22).
     * @param file Archivo XML a procesar.
     * @return Respuesta con el estado del procesamiento.
     */

    @PostMapping("/process")
    public ResponseEntity<ServerResponse> processNote(
            @RequestParam("noteType") int noteType,
            @RequestParam("file") MultipartFile file) {

        if (noteType != 21 && noteType != 22) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerResponse(0L));
        }

        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ServerResponse(0L));
        }

        try {
            byte[] bytes = file.getBytes();
            InputStream inputStream = new ByteArrayInputStream(bytes);

            ServerResponse response = noteAdjustmentService.adjustNote(inputStream, noteType);

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ServerResponse(0L));
        }
    }

}