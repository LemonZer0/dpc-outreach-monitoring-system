
package com.dpcoutreach.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "outreach_statuses")
public class OutreachStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_call_record_id")
    private CallRecord latestCallRecord;

    @Column(nullable = false)
    private Instant updatedAt;

    protected OutreachStatus() {}

    public OutreachStatus(
            Company company,
            String status,
            CallRecord latestCallRecord,
            Instant updatedAt) {
        this.company = company;
        this.status = status;
        this.latestCallRecord = latestCallRecord;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public CallRecord getLatestCallRecord() {
        return latestCallRecord;
    }

    public void setLatestCallRecord(CallRecord latestCallRecord) {
        this.latestCallRecord = latestCallRecord;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
