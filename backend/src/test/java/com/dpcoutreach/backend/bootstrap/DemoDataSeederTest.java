package com.dpcoutreach.backend.bootstrap;

import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.ContactRepository;
import com.dpcoutreach.backend.repository.DpcRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoDataSeederTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final ContactRepository contactRepository = mock(ContactRepository.class);
    private final DpcRepository dpcRepository = mock(DpcRepository.class);

    @Test
    void createsDemoDataWhenItDoesNotAlreadyExist() {
        when(dpcRepository.findFirstByName("Demo DPC")).thenReturn(Optional.empty());
        when(companyRepository.findFirstByName("Demo Company")).thenReturn(Optional.empty());
        when(contactRepository.findFirstByPhoneNumberAndCompanyId("919876543210", null))
                .thenReturn(Optional.empty());
        when(dpcRepository.save(any(Dpc.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new DemoDataSeeder(companyRepository, contactRepository, dpcRepository, "+91 (98765) 43210").run();

        verify(dpcRepository).save(any(Dpc.class));
        verify(companyRepository).save(any(Company.class));
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void reusesExistingDemoDataWithoutSavingDuplicates() {
        Company company = new Company("Demo Company");
        Dpc dpc = new Dpc("Demo DPC", "local-demo");
        Contact contact = new Contact("Demo Contact", "919876543210", company);
        when(dpcRepository.findFirstByName("Demo DPC")).thenReturn(Optional.of(dpc));
        when(companyRepository.findFirstByName("Demo Company")).thenReturn(Optional.of(company));
        when(contactRepository.findFirstByPhoneNumberAndCompanyId("919876543210", null))
                .thenReturn(Optional.of(contact));

        new DemoDataSeeder(companyRepository, contactRepository, dpcRepository, "919876543210").run();

        verify(dpcRepository, never()).save(any(Dpc.class));
        verify(companyRepository, never()).save(any(Company.class));
        verify(contactRepository, never()).save(any(Contact.class));
    }
}
