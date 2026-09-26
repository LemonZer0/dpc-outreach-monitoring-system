
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "call_records")
public class CallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dpc_id", nullable = false)
    private Dpc dpc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private Instant callTimestamp;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus;

    protected CallRecord() {
        // Required by JPA
    }

    public CallRecord(
            Dpc dpc,
            Contact contact,
            String phoneNumber,
            Instant callTimestamp,
            Integer durationSeconds,
            VerificationStatus verificationStatus) {
        this.dpc = dpc;
        this.contact = contact;
        this.phoneNumber = phoneNumber;
        this.callTimestamp = callTimestamp;
        this.durationSeconds = durationSeconds;
        this.verificationStatus = verificationStatus;
    }

    public Long getId() {
        return id;
    }

    public Dpc getDpc() {
        return dpc;
    }

    public Contact getContact() {
        return contact;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Instant getCallTimestamp() {
        return callTimestamp;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
