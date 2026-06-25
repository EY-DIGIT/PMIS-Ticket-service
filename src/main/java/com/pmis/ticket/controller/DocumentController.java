package com.pmis.ticket.controller;

import com.pmis.ticket.service.DocumentService;
import com.pmis.ticket.web.response.TicketDocumentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Ticket Documents", description = "Upload and manage ticket file attachments stored on local disk")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/{ticketUuid}/documents",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload one or more files for a ticket",
               description = "Files are stored at the configured local path under a sub-folder named by ticketUuid.")
    public ResponseEntity<List<TicketDocumentResponse>> upload(
            @PathVariable String ticketUuid,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(value = "uploadedByUuid", required = false, defaultValue = "SYSTEM") String uploadedByUuid,
            @RequestParam(value = "uploadedByName", required = false, defaultValue = "System") String uploadedByName) {

        return ResponseEntity.status(201)
                .body(documentService.uploadDocuments(ticketUuid, files, uploadedByUuid, uploadedByName));
    }

    @GetMapping("/{ticketUuid}/documents")
    @Operation(summary = "List all documents attached to a ticket")
    public ResponseEntity<List<TicketDocumentResponse>> list(@PathVariable String ticketUuid) {
        return ResponseEntity.ok(documentService.getDocuments(ticketUuid));
    }

    @DeleteMapping("/documents/{docUuid}")
    @Operation(summary = "Delete a document (removes DB record and physical file)")
    public ResponseEntity<Void> delete(@PathVariable String docUuid) {
        documentService.deleteDocument(docUuid);
        return ResponseEntity.noContent().build();
    }
}
