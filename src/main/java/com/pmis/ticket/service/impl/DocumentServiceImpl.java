package com.pmis.ticket.service.impl;

import com.pmis.ticket.entity.TicketDocumentEntity;
import com.pmis.ticket.repository.TicketDocumentRepository;
import com.pmis.ticket.service.DocumentService;
import com.pmis.ticket.web.response.TicketDocumentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final TicketDocumentRepository documentRepo;

    @Value("${app.document.store-path}")
    private String storePath;

    @Override
    public List<TicketDocumentResponse> uploadDocuments(String ticketUuid,
                                                         List<MultipartFile> files,
                                                         String uploadedByUuid,
                                                         String uploadedByName) {
        List<TicketDocumentResponse> responses = new ArrayList<>();
        Path ticketDir = Paths.get(storePath, ticketUuid);

        try {
            Files.createDirectories(ticketDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + ticketDir, e);
        }

        for (MultipartFile file : files) {
            String docUuid     = UUID.randomUUID().toString();
            String originalName = sanitize(file.getOriginalFilename());
            String storedName   = docUuid + "_" + originalName;
            Path   filePath     = ticketDir.resolve(storedName);

            try {
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("Failed to save file {} for ticket {}: {}", originalName, ticketUuid, e.getMessage());
                throw new RuntimeException("Failed to save file: " + originalName, e);
            }

            long now = System.currentTimeMillis();
            TicketDocumentEntity entity = TicketDocumentEntity.builder()
                    .uuid(docUuid)
                    .ticketUuid(ticketUuid)
                    .originalName(originalName)
                    .storedName(storedName)
                    .filePath(filePath.toAbsolutePath().toString())
                    .mimeType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .uploadedByUuid(uploadedByUuid)
                    .uploadedByName(uploadedByName)
                    .uploadedAt(now)
                    .build();

            documentRepo.save(entity);
            responses.add(toResponse(entity));
            log.info("Document uploaded: {} -> {}", originalName, filePath);
        }

        return responses;
    }

    @Override
    public List<TicketDocumentResponse> getDocuments(String ticketUuid) {
        return documentRepo.findByTicketUuidOrderByUploadedAtDesc(ticketUuid)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public void deleteDocument(String docUuid) {
        TicketDocumentEntity entity = documentRepo.findById(docUuid)
                .orElseThrow(() -> new NoSuchElementException("Document not found: " + docUuid));

        // Delete physical file
        try {
            Path filePath = Paths.get(entity.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.warn("Could not delete physical file for document {}: {}", docUuid, e.getMessage());
        }

        documentRepo.delete(entity);
    }

    // =========================================================
    // Helpers
    // =========================================================

    private TicketDocumentResponse toResponse(TicketDocumentEntity e) {
        return TicketDocumentResponse.builder()
                .uuid(e.getUuid())
                .ticketUuid(e.getTicketUuid())
                .originalName(e.getOriginalName())
                .storedName(e.getStoredName())
                .filePath(e.getFilePath())
                .mimeType(e.getMimeType())
                .sizeBytes(e.getSizeBytes())
                .uploadedByUuid(e.getUploadedByUuid())
                .uploadedByName(e.getUploadedByName())
                .uploadedAt(e.getUploadedAt())
                .build();
    }

    /** Strip path traversal characters and whitespace from filename. */
    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) return "unnamed";
        return Paths.get(filename).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}
