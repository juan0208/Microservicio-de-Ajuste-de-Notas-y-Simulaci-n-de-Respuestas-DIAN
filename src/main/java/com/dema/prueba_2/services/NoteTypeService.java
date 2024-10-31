package com.dema.prueba_2.services;

import com.dema.prueba_2.utils.enums.LineType;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de obtener el tipo de línea correspondiente a un tipo de nota.
 * Proporciona una funcionalidad para traducir un tipo de nota en su representación de línea.
 *
 * @since 29 de octubre de 2024
 * @autor Juan Alejandro Londoño Lopez
 */

@Service
public class NoteTypeService {

    /**
     * Obtiene el nombre del tipo de línea correspondiente a un tipo de nota específico.
     *
     * @param noteType Tipo de nota que se desea traducir a su representación de línea.
     * @return El nombre del tipo de línea correspondiente al tipo de nota.
     * @throws IllegalArgumentException Si el tipo de nota proporcionado no es válido.
     */

    public String getLineType(int noteType) {
        LineType lineType = LineType.forNoteType(noteType);
        if (lineType != null) {
            return lineType.name();
        } else {
            throw new IllegalArgumentException("NoteType no válido: " + noteType);
        }
    }
}
