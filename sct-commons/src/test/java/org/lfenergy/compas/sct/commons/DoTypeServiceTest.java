// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;
import org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.lfenergy.compas.sct.commons.scl.dtt.DataTypeTemplateTestUtils.*;

class DoTypeServiceTest {

    @Test
    void getDoTypes() {
        //Given
        SCL std = SclTestMarshaller.getSCLFromFile("/std/std_sample.std");
        TDataTypeTemplates dataTypeTemplates = std.getDataTypeTemplates();
        DoTypeService doTypeService = new DoTypeService();

        //When
        List<TDOType> tdoTypes = doTypeService.getDoTypes(dataTypeTemplates).toList();

        //Then
        assertThat(tdoTypes)
                .hasSize(47);
    }

    @Test
    void getFilteredDoTypes() {
        //Given
        SCL std = SclTestMarshaller.getSCLFromFile("/std/std_sample.std");
        TDataTypeTemplates dataTypeTemplates = std.getDataTypeTemplates();
        DoTypeService doTypeService = new DoTypeService();

        //When
        List<TDOType> tdoTypes =
                doTypeService.getFilteredDoTypes(dataTypeTemplates, tdoType -> TPredefinedCDCEnum.DPL.equals(tdoType.getCdc())).toList();

        //Then
        assertThat(tdoTypes)
                .hasSize(2)
                .extracting(TDOType::getCdc, TDOType::getId)
                .containsExactly(Tuple.tuple(TPredefinedCDCEnum.DPL, "RTE_X_X_X_48BA5C40D0913654FA5291A28C0D9716_DPL_V1.0.0"),
                        Tuple.tuple(TPredefinedCDCEnum.DPL, "RTE_X_X_X_D93000A2D6F9B026504B48576A914DA3_DPL_V1.0.0"));

    }

    @Test
    void findDoType() {
        //Given
        SCL std = SclTestMarshaller.getSCLFromFile("/std/std_sample.std");
        TDataTypeTemplates dataTypeTemplates = std.getDataTypeTemplates();
        DoTypeService doTypeService = new DoTypeService();

        //When
        TDOType tdoType = doTypeService.findDoType(dataTypeTemplates, tdoType1 -> TPredefinedCDCEnum.DPL.equals(tdoType1.getCdc())).orElseThrow();

        //Then
        assertThat(tdoType)
                .extracting(TDOType::getCdc, TDOType::getId)
                .containsExactly(TPredefinedCDCEnum.DPL, "RTE_X_X_X_48BA5C40D0913654FA5291A28C0D9716_DPL_V1.0.0");
    }

    @Test
    void getDataAttributeRefs_should_return_expected_dataReference() {
        //Given
        TDataTypeTemplates dtt = initDttFromFile("/dtt-test-schema-conf/scd_dtt_do_sdo_da_bda_test.xml");
        TDOType tdoType = dtt.getDOType().stream().filter(tdoType1 -> tdoType1.getId().equals("DOType0"))
                        .findFirst().get();
        String DO_NAME = "FirstDoName";

        DataAttributeRef dataRef = new DataAttributeRef();
        dataRef.setLnType("LNodeType0");
        DoTypeName doTypeName = new DoTypeName(DO_NAME);
        dataRef.setDoName(doTypeName);
        DaTypeName daTypeName = new DaTypeName();
        dataRef.setDaName(daTypeName);

        //When
        DoTypeService doTypeService = new DoTypeService();
        List<DataAttributeRef> result = doTypeService.getDataAttributeRefs(dtt, tdoType, dataRef);
        //Then
        assertThat(result).hasSize(8).extracting(
                        DataAttributeRef::getDoRef, DataAttributeRef::getSdoNames,
                        DataAttributeRef::getDaRef, DataAttributeRef::getBdaNames, DataAttributeRef::getBType, DataAttributeRef::getType)
                .containsExactlyInAnyOrder(
                        Tuple.tuple(DO_NAME, List.of(),
                                "sampleDaName1", List.of(), TPredefinedBasicTypeEnum.BOOLEAN, null),
                        Tuple.tuple(DO_NAME+".sdoName1", List.of("sdoName1"),
                                "sampleDaName21", List.of(), TPredefinedBasicTypeEnum.BOOLEAN, null),
                        Tuple.tuple(DO_NAME+".sdoName1.sdoName21", List.of("sdoName1", "sdoName21"),
                                "sampleDaName31", List.of(), TPredefinedBasicTypeEnum.BOOLEAN, null),
                        Tuple.tuple(DO_NAME+".sdoName1.sdoName21.sdoName31", List.of("sdoName1", "sdoName21", "sdoName31"),
                                "sampleDaName41", List.of(), TPredefinedBasicTypeEnum.BOOLEAN, null),
                        Tuple.tuple(DO_NAME+".sdoName2", List.of("sdoName2"),
                                "sampleDaName11", List.of(), TPredefinedBasicTypeEnum.BOOLEAN, null),
                        Tuple.tuple(DO_NAME+".sdoName2", List.of("sdoName2"),
                                "structDaName1.sampleBdaName1", List.of("sampleBdaName1"), TPredefinedBasicTypeEnum.BOOLEAN, null),
                        Tuple.tuple(DO_NAME+".sdoName2", List.of("sdoName2"),
                                "structDaName1.structBdaName1.sampleBdaName21", List.of("structBdaName1", "sampleBdaName21"), TPredefinedBasicTypeEnum.BOOLEAN, null),
                        Tuple.tuple(DO_NAME+".sdoName2", List.of("sdoName2"),
                                "structDaName1.structBdaName1.enumBdaName22", List.of("structBdaName1", "enumBdaName22"), TPredefinedBasicTypeEnum.ENUM, "EnumType1")
                );
    }

}
