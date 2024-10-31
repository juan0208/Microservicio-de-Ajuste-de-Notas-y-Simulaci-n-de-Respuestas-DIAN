package com.dema.prueba_2.utils.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeración que representa los tipos de notas para clasificación en el sistema,
 * asociando cada tipo de nota con un código específico.
 * Incluye métodos para obtener el tipo de nota según su código.
 *
 * @since 29 de octubre de 2024
 * @autor Juan Alejandro Londoño Lopez
 */

public enum LineType {
    CreditNote(21),
    DebitNote(22);

    private final int noteType;


    /**
     * Constructor para inicializar la constante con el código del tipo de nota.
     *
     * @param noteType Código numérico que representa el tipo de nota.
     */

    LineType(int noteType) {
        this.noteType = noteType;
    }

    private static final Map<Integer, LineType> BY_NOTE_TYPE = new HashMap<>();

    static {
        for (LineType lineType : values()) {
            BY_NOTE_TYPE.put(lineType.noteType, lineType);
        }
    }

    /**
     * Obtiene el tipo de nota correspondiente al código proporcionado.
     *
     * @param noteType Código del tipo de nota.
     * @return El tipo de nota asociado o null si no se encuentra.
     */

    public static LineType forNoteType(int noteType) {
        return BY_NOTE_TYPE.getOrDefault(noteType, null);
    }
}
