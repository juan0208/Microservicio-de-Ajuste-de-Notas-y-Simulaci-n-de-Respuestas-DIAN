package com.dema.prueba_2.services;

import com.dema.prueba_2.models.responses.DIANResponse;
import com.dema.prueba_2.models.responses.ServerResponse;
import com.dema.prueba_2.utils.enums.CopyTags;
import com.dema.prueba_2.utils.enums.RequiredTags;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Servicio encargado de realizar el ajuste de notas en archivos XML,
 * incluyendo la creación y envío de un archivo ZIP al servicio de la DIAN.
 * También gestiona la estructura de XML y su validación.
 *
 * @since 29 de octubre de 2024
 * @autor Juan Alejandro Londoño Lopez
 */


@Service
public class NoteAdjustmentService {

    @Autowired
    private NoteTypeService noteTypeService;
    @Autowired
    private FilesService filesService;

    /**
     * Realiza el ajuste de la nota en el archivo XML, aplicando modificaciones específicas y generando un archivo ZIP,
     * que luego es enviado al servicio DIAN para su validación.
     *
     * @param xmlInputStream Flujo de entrada del archivo XML.
     * @param noteType       Tipo de la nota.
     * @return Respuesta del servidor con el ID de la nota procesada.
     */

    public ServerResponse adjustNote(InputStream xmlInputStream, int noteType) {
        try {
            String originalXmlContent = inputStreamToString(xmlInputStream);

            if (originalXmlContent.isEmpty()) {
                throw new IllegalArgumentException("El XML original está vacío");
            }

            String newRootTagName = noteTypeService.getLineType(noteType);
            String modifiedXmlContent = replaceInvoiceTag(originalXmlContent, newRootTagName);

            EnumSet<CopyTags> tagsToCopy = EnumSet.of(CopyTags.ID, CopyTags.UUID, CopyTags.IssueDate);

            String finalXmlContent = createBillingReference(modifiedXmlContent, tagsToCopy);

            finalXmlContent = copyAndPasteTags(finalXmlContent, EnumSet.allOf(RequiredTags.class));

            byte[] zip = filesService.createZipWithXml(new ByteArrayInputStream(finalXmlContent.getBytes(StandardCharsets.UTF_8)), "note").readAllBytes();

            ResponseEntity<DIANResponse> response = sendZipToController2(new ByteArrayInputStream(zip), noteType);

            String id = extractIdFromXml(finalXmlContent);

            System.out.println("response");
            System.out.println(response);

            if (response.getBody().getDianResponse()) {
                // Guardar el XML modificado
                System.out.println("response.getBody().dianResponse Guadando archivo");
                filesService.saveXmlFile(new ByteArrayInputStream(finalXmlContent.getBytes(StandardCharsets.UTF_8)));
                filesService.saveZipFile(new ByteArrayInputStream(zip));

                return new ServerResponse(id);
            } else {
                return new ServerResponse(0L);
            }


        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error adjusting note: " + e.getMessage(), e);
        }
    }

    /**
     * Reemplaza la etiqueta raíz del XML por una nueva según el tipo de nota.
     *
     * @param xmlContent     Contenido XML original.
     * @param newRootTagName Nuevo nombre para la etiqueta raíz.
     * @return Contenido XML con la nueva etiqueta raíz.
     */

    private String replaceInvoiceTag(String xmlContent, String newRootTagName)
            throws ParserConfigurationException, TransformerException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document originalDocument = builder.parse(new InputSource(new StringReader(xmlContent)));

        Document newDocument = builder.newDocument();
        Element newRoot = newDocument.createElement(newRootTagName);

        Element originalRoot = originalDocument.getDocumentElement();
        NamedNodeMap attributes = originalRoot.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            newRoot.setAttribute(attr.getNodeName(), attr.getNodeValue());
        }

        NodeList childNodes = originalRoot.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            newRoot.appendChild(newDocument.importNode(child, true));
        }

        newDocument.appendChild(newRoot);

        return documentToString(newDocument);
    }

    /**
     * Crea la referencia de facturación en el XML agregando elementos específicos.
     *
     * @param xmlContent Contenido XML donde se añadirá la referencia de facturación.
     * @param tagsToCopy Conjunto de etiquetas a copiar para la referencia.
     * @return XML con la referencia de facturación añadida.
     */

    private String createBillingReference(String xmlContent, EnumSet<CopyTags> tagsToCopy)
            throws ParserConfigurationException, TransformerException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

        Element root = document.getDocumentElement();
        Element billingReference = document.createElement("BillingReference");

        for (CopyTags tag : tagsToCopy) {
            NodeList nodes = document.getElementsByTagName(tag.getValue());
            if (nodes.getLength() > 0) {
                Node node = nodes.item(0);
                Element newElement = document.createElement(node.getNodeName());
                newElement.setTextContent(node.getTextContent());
                billingReference.appendChild(newElement);
            }
        }

        root.appendChild(billingReference);

        return documentToString(document);
    }

    /**
     * Copia y pega etiquetas requeridas en el XML.
     *
     * @param xmlContent Contenido XML donde se pegarán las etiquetas.
     * @param tagsToCopy Conjunto de etiquetas requeridas.
     * @return XML con las etiquetas copiadas y pegadas.
     */


    private String copyAndPasteTags(String xmlContent, EnumSet<RequiredTags> tagsToCopy)
            throws ParserConfigurationException, TransformerException, SAXException, IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

        Element root = document.getDocumentElement();

        for (RequiredTags tag : tagsToCopy) {
            NodeList nodes = root.getElementsByTagName(tag.getTagName());
            if (nodes.getLength() > 0) {
                Node nodeToCopy = nodes.item(0);
                Node copiedNode = document.importNode(nodeToCopy, true);

                if (nodeToCopy.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) nodeToCopy;
                    String nodeContent = element.getTextContent();
                }

                root.appendChild(copiedNode);
            } else {
                System.out.println("No se encontró el nodo: " + tag.getTagName());
            }
        }

        return documentToString(document);
    }

    /**
     * Extrae el ID del XML para su posterior uso.
     *
     * @param xmlContent Contenido XML.
     * @return ID extraído o null si no se encuentra.
     */

    private String extractIdFromXml(String xmlContent)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

        NodeList idNodes = document.getElementsByTagName("cbc:ID");
        if (idNodes.getLength() > 0) {
            Node idNode = idNodes.item(0);
            return idNode.getTextContent();
        } else {
            return null;
        }
    }

    /**
     * Convierte un Document XML en una cadena de texto.
     *
     * @param document Documento XML.
     * @return Representación en String del XML.
     */

    private String documentToString(Document document) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        String xmlString = writer.toString();

        return xmlString;
    }

    /**
     * Convierte un InputStream a un String.
     *
     * @param inputStream InputStream a convertir.
     * @return Representación en String del contenido.
     */

    private String inputStreamToString(InputStream inputStream) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
            return sb.toString();
        }
    }

    /**
     * Envía el archivo ZIP con el XML ajustado al servicio DIAN para su validación.
     *
     * @param zipInputStream Flujo de entrada del archivo ZIP.
     * @param noteType       Tipo de nota a enviar como parámetro.
     * @return ResponseEntity con la respuesta de DIAN.
     * @throws IOException Si ocurre un error al procesar o enviar el archivo ZIP.
     */


    private ResponseEntity<DIANResponse> sendZipToController3(InputStream zipInputStream, int noteType) throws IOException {
        Path tempFile = Files.createTempFile("tempZip", ".zip");
        Files.copy(zipInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        zipInputStream.close(); // Cerrar InputStream después de crear el archivo temporal

        HttpResponse<String> response;
        try {
            response = Unirest.post("http://localhost:9090/api/process-zip-note")
                    .header("Content-Type", "multipart/form-data")
                    .field("file", tempFile.toFile(), "application/zip")
                    .field("noteType", String.valueOf(noteType))
                    .asString();
        } catch (UnirestException e) {
            throw new RuntimeException("Error al enviar la solicitud: " + e.getMessage(), e);
        } finally {
            Files.deleteIfExists(tempFile);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        DIANResponse dianResponse;
        dianResponse = objectMapper.readValue(response.getBody(), DIANResponse.class);

        return ResponseEntity
                .status(HttpStatus.valueOf(response.getStatus()))
                .body(dianResponse);
    }

    /**
     * Envía un archivo ZIP con el XML ajustado al servicio DIAN para su validación mediante una solicitud HTTP POST.
     * El archivo ZIP se envía como parte de una solicitud multipart, utilizando la biblioteca Unirest para el manejo de la comunicación.
     *
     * @param zipInputStream Flujo de entrada del archivo ZIP a enviar.
     * @param noteType       Tipo de nota, usado como parámetro adicional en la solicitud.
     * @return ResponseEntity que contiene la respuesta del servicio DIAN, incluyendo el estado HTTP y el cuerpo de la respuesta.
     * @throws IOException Si ocurre un error al procesar o enviar el archivo ZIP.
     */



    private ResponseEntity<DIANResponse> sendZipToController(InputStream zipInputStream, int noteType) throws IOException {

        byte[] zipBytes = zipInputStream.readAllBytes();

        Path tempFile = Files.createTempFile("tempZip", ".zip");
        Files.copy(zipInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        String boundary = UUID.randomUUID().toString();

        HttpResponse<String> response;
        try {
            response = Unirest.post("http://localhost:9090/api/process-zip-note")
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .field("file",tempFile.toFile())
                    //.field("file", zipBytes,ContentType.APPLICATION_OCTET_STREAM, "note.zip")
                    //.field("file", zipInputStream, ContentType.APPLICATION_OCTET_STREAM, "note.zip") // Especificar el tipo de contenido del archivo
                    .field("noteType", String.valueOf(noteType))
                    .asString();
        } catch (UnirestException e) {
            throw new RuntimeException("Error al enviar la solicitud: " + e.getMessage(), e);
        } finally {
            Files.deleteIfExists(tempFile);
        }

        System.out.println("Status: " + response.getStatus());
        System.out.println("Headers: " + response.getHeaders());
        System.out.println("Body: " + response.getBody());

        ObjectMapper objectMapper = new ObjectMapper();
        DIANResponse dianResponse;
        dianResponse = objectMapper.readValue(response.getBody(), DIANResponse.class);

        System.out.println("Dian response: " + dianResponse.getDianResponse());


        return ResponseEntity
                .status(HttpStatus.valueOf(response.getStatus()))
                .body(dianResponse);
    }

    /**
     * Envía el archivo ZIP con el XML ajustado al servicio DIAN para su validación.
     *
     * @param zipInputStream Flujo de entrada del archivo ZIP.
     * @param noteType       Tipo de nota a enviar como parámetro.
     * @return ResponseEntity con la respuesta de DIAN.
     * @throws IOException Si ocurre un error al procesar o enviar el archivo ZIP.
     */

    private ResponseEntity<DIANResponse> sendZipToController2(InputStream zipInputStream, int noteType) throws IOException {


        byte[] zipBytes = zipInputStream.readAllBytes();

        if (zipBytes.length == 0) {
            System.out.println("Peso 0");
            throw new IOException("El archivo ZIP está vacío");
        }

        Path tempFile = Files.createTempFile("tempZip", ".zip");
        Files.copy(zipInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        HttpEntity entity = MultipartEntityBuilder.create()
                .addBinaryBody("file", zipBytes, ContentType.APPLICATION_OCTET_STREAM, "temp.zip")
                .addTextBody("noteType", String.valueOf(noteType), ContentType.TEXT_PLAIN)
                .build();

        HttpPost post = new HttpPost("http://localhost:9090/api/process-zip-note");
        post.setEntity(entity);

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            CloseableHttpResponse response = client.execute(post);

            String responseBody = EntityUtils.toString(response.getEntity());

            DIANResponse dianResponse = new ObjectMapper().readValue(responseBody, DIANResponse.class);

            System.out.println("Response: " + response);
            System.out.println("Response body: " + responseBody);
            System.out.println("Dian Response: " + dianResponse.getDianResponse());

            return ResponseEntity
                    .status(HttpStatus.valueOf(response.getStatusLine().getStatusCode()))
                    .body(dianResponse);
        }
    }

}