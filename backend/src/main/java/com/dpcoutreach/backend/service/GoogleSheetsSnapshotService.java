package com.dpcoutreach.backend.service;

import com.dpcoutreach.backend.entity.CallRecord;
import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.CoreOutreachStatus;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.entity.OutreachStatus;
import com.dpcoutreach.backend.repository.CallRecordRepository;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.ContactRepository;
import com.dpcoutreach.backend.repository.CoreOutreachStatusRepository;
import com.dpcoutreach.backend.repository.DpcRepository;
import com.dpcoutreach.backend.repository.OutreachStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleSheetsSnapshotService {

    private final CallRecordRepository callRecordRepository;
    private final DpcRepository dpcRepository;
    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;
    private final OutreachStatusRepository outreachStatusRepository;
    private final CoreOutreachStatusRepository coreOutreachStatusRepository;

    public GoogleSheetsSnapshotService(
            CallRecordRepository callRecordRepository,
            DpcRepository dpcRepository,
            ContactRepository contactRepository,
            CompanyRepository companyRepository,
            OutreachStatusRepository outreachStatusRepository,
            CoreOutreachStatusRepository coreOutreachStatusRepository) {
        this.callRecordRepository = callRecordRepository;
        this.dpcRepository = dpcRepository;
        this.contactRepository = contactRepository;
        this.companyRepository = companyRepository;
        this.outreachStatusRepository = outreachStatusRepository;
        this.coreOutreachStatusRepository = coreOutreachStatusRepository;
    }

    @Transactional(readOnly = true)
    public List<SheetSnapshot> createSnapshot() {
        List<SheetSnapshot> sheets = new ArrayList<>();

        sheets.add(new SheetSnapshot(
                "Call records",
                List.of("ID", "DPC ID", "DPC", "Contact ID", "Contact", "Company ID", "Company",
                        "Phone number", "Call timestamp", "Duration seconds", "Verification status",
                        "Call source", "Call type", "Reported outcome"),
                callRecordRepository.findAllByOrderByCallTimestampDesc().stream()
                        .map(this::callRecordRow)
                        .toList()));

        sheets.add(new SheetSnapshot(
                "DPCs",
                List.of("ID", "Name"),
                dpcRepository.findAllByOrderByNameAsc().stream()
                        .map(dpc -> List.of(value(dpc.getId()), value(dpc.getName())))
                        .toList()));

        sheets.add(new SheetSnapshot(
                "Contacts",
                List.of("ID", "Name", "Phone number", "Company ID", "Company"),
                contactRepository.findAllWithCompanyOrderByCompanyNameAndName().stream()
                        .map(this::contactRow)
                        .toList()));

        sheets.add(new SheetSnapshot(
                "Companies",
                List.of("ID", "Name"),
                companyRepository.findAllByOrderByNameAsc().stream()
                        .map(company -> List.of(value(company.getId()), value(company.getName())))
                        .toList()));

        sheets.add(new SheetSnapshot(
                "DPC outreach status",
                List.of("Company ID", "Company", "Status", "Latest call record ID", "Updated at"),
                outreachStatusRepository.findAllByOrderByUpdatedAtDesc().stream()
                        .map(this::outreachStatusRow)
                        .toList()));

        sheets.add(new SheetSnapshot(
                "CORE outreach decisions",
                List.of("Company ID", "Company", "Decision", "Updated at"),
                coreOutreachStatusRepository.findAllByOrderByUpdatedAtDesc().stream()
                        .map(this::coreOutreachStatusRow)
                        .toList()));

        return List.copyOf(sheets);
    }

    private List<String> callRecordRow(CallRecord call) {
        Contact contact = call.getContact();
        Company company = contact == null ? null : contact.getCompany();
        return List.of(
                value(call.getId()),
                value(call.getDpc().getId()),
                value(call.getDpc().getName()),
                contact == null ? "" : value(contact.getId()),
                contact == null ? "" : value(contact.getName()),
                company == null ? "" : value(company.getId()),
                company == null ? "" : value(company.getName()),
                value(call.getPhoneNumber()),
                value(call.getCallTimestamp()),
                value(call.getDurationSeconds()),
                value(call.getVerificationStatus()),
                value(call.getCallSource()),
                value(call.getCallType()),
                value(call.getCallOutcome()));
    }

    private List<String> contactRow(Contact contact) {
        return List.of(
                value(contact.getId()),
                value(contact.getName()),
                value(contact.getPhoneNumber()),
                value(contact.getCompany().getId()),
                value(contact.getCompany().getName()));
    }

    private List<String> outreachStatusRow(OutreachStatus status) {
        return List.of(
                value(status.getCompany().getId()),
                value(status.getCompany().getName()),
                value(status.getStatus()),
                status.getLatestCallRecord() == null ? "" : value(status.getLatestCallRecord().getId()),
                value(status.getUpdatedAt()));
    }

    private List<String> coreOutreachStatusRow(CoreOutreachStatus status) {
        return List.of(
                value(status.getCompany().getId()),
                value(status.getCompany().getName()),
                value(status.getStatus()),
                value(status.getUpdatedAt()));
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    public record SheetSnapshot(String title, List<String> headers, List<List<String>> rows) {
        public SheetSnapshot {
            headers = List.copyOf(headers);
            rows = rows.stream().map(List::copyOf).toList();
        }
    }
}
