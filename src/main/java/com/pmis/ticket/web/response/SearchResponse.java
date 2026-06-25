package com.pmis.ticket.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {
    private int  totalCount;   // total matching records (before pagination)
    private int  page;         // current page (0-based)
    private int  size;         // page size requested
    private int  totalPages;   // totalCount / size (rounded up)
    private List<TicketResponse> tickets;
}
