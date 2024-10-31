package com.dema.prueba_2.services;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Servicio encargado de gestionar el guardado de archivos XML y la creación de archivos ZIP.
 *
 * @since 29 de octubre de 2024
 * @autor Juan Alejandro Londoño Lopez
 */
@Service
public class FilesService {

    private static final String FILE_PATH = "/resources/files/";

    /**
     * Guarda un archivo XML en el sistema de archivos con un nombre de archivo único.
     *
     * @param inputStream Flujo de entrada del archivo XML a guardar.
     * @return Nombre del archivo guardado.
     * @throws IOException Si ocurre un error al guardar el archivo o el contenido está vacío.
     */
    public String saveXmlFile(InputStream inputStream) throws IOException {
        String fileName = UUID.randomUUID().toString() + ".xml";
        Path targetPath = Paths.get(FILE_PATH, fileName);

        Files.createDirectories(targetPath.getParent());
        byte[] content = inputStream.readAllBytes();

        if (content.length == 0) {
            throw new IOException("El contenido del XML está vacío");
        }

        Files.write(targetPath, content);
        return fileName;
    }

    /**
     * Guarda un archivo ZIP en el sistema de archivos con un nombre de archivo único.
     *
     * @param inputStream Flujo de entrada del archivo ZIP a guardar.
     * @return Nombre del archivo ZIP guardado.
     * @throws IOException Si ocurre un error al guardar el archivo.
     */
    public String saveZipFile(InputStream inputStream) throws IOException {
        String fileName = UUID.randomUUID().toString() + ".zip";
        Path targetPath = Paths.get(FILE_PATH, fileName);

        Files.createDirectories(targetPath.getParent());
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    /**
     * Crea un archivo ZIP que contiene un archivo XML.
     *
     * @param xmlInputStream Flujo de entrada del archivo XML a agregar al ZIP.
     * @param xmlFileName    Nombre del archivo XML dentro del ZIP (sin la extensión).
     * @return Flujo de entrada del archivo ZIP creado.
     * @throws IOException Si ocurre un error al crear el archivo ZIP.
     */
    public InputStream createZipWithXml(InputStream xmlInputStream, String xmlFileName) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream)) {
            ZipEntry zipEntry = new ZipEntry(xmlFileName + ".xml");
            zipOut.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = xmlInputStream.read(buffer)) > 0) {
                zipOut.write(buffer, 0, length);
            }
            zipOut.closeEntry();
        }

        return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    }
}
