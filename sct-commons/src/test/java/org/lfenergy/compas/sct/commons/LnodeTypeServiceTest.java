// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lfenergy.compas.sct.commons.scl.dtt.DataTypeTemplateTestUtils.*;

@Disabled
class LnodeTypeServiceTest {

    @Test
    void getLnodeTypes() {
        //Given
        SCL std = SclTestMarshaller.getSCLFromFile("/std/std_sample.std");
        TDataTypeTemplates dataTypeTemplates = std.getDataTypeTemplates();
        LnodeTypeService lnodeTypeService = new LnodeTypeService();

        //When
        List<TLNodeType> tlnodeTypes = lnodeTypeService.getLnodeTypes(dataTypeTemplates).toList();

        //Then
        assertThat(tlnodeTypes)
                .hasSize(15)
                .flatExtracting(TLNodeType::getLnClass)
                .containsExactly("LPAI",
                        "LLN0",
                        "LLN0",
                        "TCTR",
                        "LSET",
                        "LCCH",
                        "LPCP",
                        "LGOS",
                        "LTMS",
                        "XSWI",
                        "GAPC",
                        "TVTR",
                        "LSYN",
                        "XSWI",
                        "LSET");
    }

    @Test
    void getFilteredLnodeTypes() {
        //Given
        SCL std = SclTestMarshaller.getSCLFromFile("/std/std_sample.std");
        TDataTypeTemplates dataTypeTemplates = std.getDataTypeTemplates();
        LnodeTypeService lnodeTypeService = new LnodeTypeService();

        //When
        List<TLNodeType> tlnodeTypes =
                lnodeTypeService.getFilteredLnodeTypes(dataTypeTemplates, tlNodeType -> tlNodeType.getLnClass().contains("LPAI")).toList();

        //Then
        assertThat(tlnodeTypes)
                .hasSize(1)
                .extracting(TLNodeType::getLnClass, TLNodeType::getId)
                .containsExactly(Tuple.tuple(List.of("LPAI"), "RTE_8884DBCF760D916CCE3EE9D1846CE46F_LPAI_V1.0.0"));
    }

    @Test
    void findLnodeType() {
        //Given
        SCL std = SclTestMarshaller.getSCLFromFile("/std/std_sample.std");
        TDataTypeTemplates dataTypeTemplates = std.getDataTypeTemplates();
        LnodeTypeService lnodeTypeService = new LnodeTypeService();

        //When
        TLNodeType tlnodeType =
                lnodeTypeService.findLnodeType(dataTypeTemplates, tlNodeType -> tlNodeType.getLnClass().contains("LPAI")).orElseThrow();

        //Then
        assertThat(tlnodeType)
                .extracting(TLNodeType::getLnClass, TLNodeType::getId)
                .containsExactly(List.of("LPAI"), "RTE_8884DBCF760D916CCE3EE9D1846CE46F_LPAI_V1.0.0");
    }

    @Test
    void getDataAttributeRefs_should_return_expected_dataReference() {
        //Given
        String SCD_DTT_DO_SDO_DA_BDA = "/dtt-test-schema-conf/scd_dtt_do_sdo_da_bda_test.xml";
        TDataTypeTemplates dtt = initDttFromFile(SCD_DTT_DO_SDO_DA_BDA);

        LnodeTypeService lnodeTypeService = new LnodeTypeService();
        TLNodeType tdoType = lnodeTypeService.findLnodeType(dtt, tlNodeType -> tlNodeType.getId()
                .equals("LN1")).get();
        //When
        List<DataAttributeRef> list = lnodeTypeService.getDataAttributeRefs(dtt, tdoType);
        //Then
        assertThat(list).hasSize(8);
        assertThat(list.stream().map(DataAttributeRef::getDoRef))
                .containsExactly(
                        "Do1.unused",
                        "Do1.unused.otherSdo",
                        "Do1.unused.otherSdo.otherSdo2",
                        "Do1.sdo2",
                        "Do1.sdo2",
                        "Do1.sdo2",
                        "Do1.sdo2",
                        "Do1");
        assertThat(list.stream().map(DataAttributeRef::getDaRef))
                .containsExactly(
                        "unused",
                        "unused",
                        "danameForotherSdo2",
                        "da1",
                        "da2.bda1sample",
                        "da2.bda1Struct.bda2sample",
                        "da2.bda1Struct.bda2Enum",
                        "daname");
    }

    @Test
    void getDataAttributeRefs_should_return_all_dai() {
        // given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scl-srv-import-ieds/ied_1_test.xml");
        TDataTypeTemplates dtt = scd.getDataTypeTemplates();

        // when
        LnodeTypeService lnodeTypeService = new LnodeTypeService();
        TLNodeType tdoType = lnodeTypeService.findLnodeType(dtt, tlNodeType -> tlNodeType.getId()
                .equals("LN2")).get();
        //When
        List<DataAttributeRef> list = lnodeTypeService.getDataAttributeRefs(dtt, tdoType);
        //Then
        assertThat(list).hasSize(1622);
    }
}
