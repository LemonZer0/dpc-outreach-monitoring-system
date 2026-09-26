package com.dpcoutreach.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumberNormalizerTest {

    @Test
    void removesCommonFormattingCharacters() {
        assertEquals("919876543210", PhoneNumberNormalizer.normalize("+91 (98765) 432-10"));
    }

    @Test
    void rejectsLettersAndOtherUnsupportedCharacters() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.normalize("98765abc"));
    }

    @Test
    void rejectsBlankPhoneNumbers() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNumberNormalizer.normalize("   "));
    }
}
