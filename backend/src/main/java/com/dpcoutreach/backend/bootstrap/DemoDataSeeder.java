package com.dpcoutreach.backend.bootstrap;

import com.dpcoutreach.backend.entity.Company;
import com.dpcoutreach.backend.entity.Contact;
import com.dpcoutreach.backend.entity.Dpc;
import com.dpcoutreach.backend.repository.CompanyRepository;
import com.dpcoutreach.backend.repository.ContactRepository;
import com.dpcoutreach.backend.repository.DpcRepository;
import com.dpcoutreach.backend.service.PhoneNumberNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_DPC_NAME = "Demo DPC";
    private static final String DEMO_DPC_ACCOUNT = "local-demo";
    private static final String DEMO_COMPANY_NAME = "Demo Company";
    private static final String DEMO_CONTACT_NAME = "Demo Contact";

    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final DpcRepository dpcRepository;
    private final String configuredContactPhone;

    public DemoDataSeeder(
            CompanyRepository companyRepository,
            ContactRepository contactRepository,
            DpcRepository dpcRepository,
            @Value("${DEMO_CONTACT_PHONE}") String configuredContactPhone) {
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
        this.dpcRepository = dpcRepository;
        this.configuredContactPhone = configuredContactPhone;
    }

    @Override
    public void run(String... args) {
        String phoneNumber = PhoneNumberNormalizer.normalize(configuredContactPhone);
        Dpc dpc = dpcRepository.findFirstByName(DEMO_DPC_NAME)
                .orElseGet(() -> dpcRepository.save(new Dpc(DEMO_DPC_NAME, DEMO_DPC_ACCOUNT)));
        Company company = companyRepository.findFirstByName(DEMO_COMPANY_NAME)
                .orElseGet(() -> companyRepository.save(new Company(DEMO_COMPANY_NAME)));
        Contact contact = contactRepository.findFirstByPhoneNumberAndCompanyId(phoneNumber, company.getId())
                .orElseGet(() -> contactRepository.save(new Contact(DEMO_CONTACT_NAME, phoneNumber, company)));

        LOGGER.info("Demo data ready: dpcId={}, companyId={}, contactId={}",
                dpc.getId(), company.getId(), contact.getId());
    }
}
