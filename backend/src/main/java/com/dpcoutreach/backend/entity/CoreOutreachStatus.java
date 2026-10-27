package com.dpcoutreach.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "core_outreach_statuses")
public class CoreOutreachStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoreOutreachStatusValue status;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CoreOutreachStatus() {}

    public CoreOutreachStatus(Company company, CoreOutreachStatusValue status, Instant updatedAt) {
        this.company = company;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public Company getCompany() {
        return company;
    }

    public CoreOutreachStatusValue getStatus() {
        return status;
    }

    public void setStatus(CoreOutreachStatusValue status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
