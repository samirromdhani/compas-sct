package org.lfenergy.compas.sct.commons.scl;

import org.junit.jupiter.api.Test;
import org.lfenergy.compas.scl2007b4.model.SCL;
import org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ObjectReferenceServiceTest {

    @Test
    void testMatches0() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        // When Then
        boolean isValid = assertDoesNotThrow(
                () -> new ObjectReferenceService().isValidObjRefValue(scd,"IED_NAME", "IED_NAMELD_INS3/LLN0.Do.da2"));
        assertThat(isValid).isTrue();
    }

}
