package com.pmis.ticket.service;

import com.pmis.ticket.web.response.TicketDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    List<TicketDocumentResponse> uploadDocuments(String ticketUuid,
                                                  List<MultipartFile> files,
                                                  String uploadedByUuid,
                                                  String uploadedByName);

    List<TicketDocumentResponse> getDocuments(String ticketUuid);

    void deleteDocument(String docUuid);
}
