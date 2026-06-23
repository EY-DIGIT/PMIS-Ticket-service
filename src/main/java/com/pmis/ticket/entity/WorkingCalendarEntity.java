package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pmis_sla_working_calendar")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkingCalendarEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    /** null = global default; set = tenant-specific override */
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** IANA timezone, e.g. "Asia/Kolkata", "UTC", "America/New_York" */
    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    /** Business day start hour (0-23), e.g. 9 = 09:00 */
    @Column(name = "work_day_start", nullable = false)
    private Integer workDayStart;

    /** Business day end hour (0-23), e.g. 18 = 18:00 */
    @Column(name = "work_day_end", nullable = false)
    private Integer workDayEnd;

    /** Comma-separated DayOfWeek names: "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY" */
    @Column(name = "work_days", nullable = false, length = 128)
    private String workDays;

    /** JSON array of holiday dates: ["2025-01-26","2025-08-15","2025-10-02"] */
    @Column(name = "holidays", columnDefinition = "text")
    private String holidays;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;
}
