
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

    @Enumerated(EnumType.STRING)
    @Column(name = "call_source", nullable = false)
    private CallSource callSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type")
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_outcome")
    private CallOutcome callOutcome;

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
        this(dpc, contact, phoneNumber, callTimestamp, durationSeconds, verificationStatus, CallSource.APP, null);
    }

    public CallRecord(
            Dpc dpc,
            Contact contact,
            String phoneNumber,
            Instant callTimestamp,
            Integer durationSeconds,
            VerificationStatus verificationStatus,
            CallSource callSource,
            CallType callType) {
        this(dpc, contact, phoneNumber, callTimestamp, durationSeconds, verificationStatus,
                callSource, callType, null);
    }

    public CallRecord(
            Dpc dpc,
            Contact contact,
            String phoneNumber,
            Instant callTimestamp,
            Integer durationSeconds,
            VerificationStatus verificationStatus,
            CallSource callSource,
            CallType callType,
            CallOutcome callOutcome) {
        this.dpc = dpc;
        this.contact = contact;
        this.phoneNumber = phoneNumber;
        this.callTimestamp = callTimestamp;
        this.durationSeconds = durationSeconds;
        this.verificationStatus = verificationStatus;
        this.callSource = callSource;
        this.callType = callType;
        this.callOutcome = callOutcome;
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

    public void setContact(Contact contact) {
        this.contact = contact;
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

    public CallSource getCallSource() {
        return callSource;
    }

    public CallType getCallType() {
        return callType;
    }

    public CallOutcome getCallOutcome() {
        return callOutcome;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
