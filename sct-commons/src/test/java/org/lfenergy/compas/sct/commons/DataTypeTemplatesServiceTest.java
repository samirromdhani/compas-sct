// SPDX-FileCopyrightText: 2024 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.Test;
import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;
import org.lfenergy.compas.sct.commons.scl.dtt.DataTypeTemplateAdapter;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.lfenergy.compas.sct.commons.scl.dtt.DataTypeTemplateTestUtils.SCD_DTT_DO_SDO_DA_BDA;
import static org.lfenergy.compas.sct.commons.scl.dtt.DataTypeTemplateTestUtils.initDttAdapterFromFile;

class DataTypeTemplatesServiceTest {

    @Test
    void isDoModAndDaStValExist_when_LNodeType_not_exist_should_return_false() {
        //Given
        SCL scl = new SCL();
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        scl.setDataTypeTemplates(dtt);
        //When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        boolean result = dataTypeTemplatesService.isDoModAndDaStValExist(dtt, "lnodeTypeId");
        //Then
        assertThat(result).isFalse();
    }


    @Test
    void isDoModAndDaStValExist_when_Do_not_exist_should_return_false() {
        //Given
        SCL scl = new SCL();
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        dtt.getLNodeType().add(tlNodeType);
        scl.setDataTypeTemplates(dtt);
        //When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        boolean result = dataTypeTemplatesService.isDoModAndDaStValExist(dtt, "lnodeTypeId");
        //Then
        assertThat(result).isFalse();
    }

    @Test
    void isDoModAndDaStValExist_when_DoType_not_exist_should_return_false() {
        //Given
        SCL scl = new SCL();
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        TDO tdo = new TDO();
        tdo.setType("doTypeId");
        tdo.setName("Mod");
        tlNodeType.getDO().add(tdo);
        dtt.getLNodeType().add(tlNodeType);
        //When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        boolean result = dataTypeTemplatesService.isDoModAndDaStValExist(dtt, "lnodeTypeId");
        //Then
        assertThat(result).isFalse();
    }


    @Test
    void isDoModAndDaStValExist_when_Da_Mod_not_exist_should_return_false() {
        //Given
        SCL scl = new SCL();
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        TDO tdo = new TDO();
        tdo.setType("doTypeId");
        tdo.setName("Mod");
        tlNodeType.getDO().add(tdo);
        dtt.getLNodeType().add(tlNodeType);
        TDOType tdoType = new TDOType();
        tdoType.setId("doTypeId");
        dtt.getDOType().add(tdoType);
        scl.setDataTypeTemplates(dtt);
        //When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        boolean result = dataTypeTemplatesService.isDoModAndDaStValExist(dtt, "lnodeTypeId");
        //Then
        assertThat(result).isFalse();
    }


    @Test
    void isDoModAndDaStValExist_when_Da_stVal_not_found_should_return_false() {
        //Given
        SCL scl = new SCL();
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        TDO tdo = new TDO();
        tdo.setType("doTypeId");
        tdo.setName("Mod");
        tlNodeType.getDO().add(tdo);
        dtt.getLNodeType().add(tlNodeType);
        TDOType tdoType = new TDOType();
        tdoType.setId("doTypeId");
        TDA tda = new TDA();
        tda.setName("daName");
        tdoType.getSDOOrDA().add(tda);
        dtt.getDOType().add(tdoType);
        scl.setDataTypeTemplates(dtt);
        //When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        boolean result = dataTypeTemplatesService.isDoModAndDaStValExist(dtt, "lnodeTypeId");
        //Then
        assertThat(result).isFalse();
    }


    @Test
    void isDoModAndDaStValExist_when_DO_Mod_And_DA_stVal_exist_return_true() {
        //Given
        SCL scl = new SCL();
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        TDO tdo = new TDO();
        tdo.setType("doTypeId");
        tdo.setName("Mod");
        tlNodeType.getDO().add(tdo);
        dtt.getLNodeType().add(tlNodeType);
        TDOType tdoType = new TDOType();
        tdoType.setId("doTypeId");
        TDA tda = new TDA();
        tda.setName("stVal");
        tdoType.getSDOOrDA().add(tda);
        dtt.getDOType().add(tdoType);
        scl.setDataTypeTemplates(dtt);
        //When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        boolean result = dataTypeTemplatesService.isDoModAndDaStValExist(dtt, "lnodeTypeId");
        //Then
        assertThat(result).isTrue();
    }


    @Test
    void isDataAttributeExist2_should_find_DO_SDO_DA_and_BDA() {
        // Given
        DataTypeTemplateAdapter dttAdapter = initDttAdapterFromFile(SCD_DTT_DO_SDO_DA_BDA);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();

        DataAttributeRef dataAttributeRefs = dataTypeTemplatesService.isDoObjectsAndDataAttributesExists2(
                dttAdapter.getCurrentElem(), "LN1", "Do1.sdo1.sdo2.da2.bda1.bda2");
        // Then
        assertThatCode(() -> dataTypeTemplatesService.isDoObjectsAndDataAttributesExists2(
                dttAdapter.getCurrentElem(), "LN1", "Do1.sdo1.sdo2.da2.bda1.bda2"))
                .doesNotThrowAnyException();

        assertThat(dataAttributeRefs.getDoRef()).isEqualTo("Do1.sdo1.sdo2");
        assertThat(dataAttributeRefs.getDaRef()).isEqualTo("da2.bda1.bda2");
        Assertions.assertThat(dataAttributeRefs).extracting(DataAttributeRef::getDoRef, DataAttributeRef::getDaRef)
                .containsExactly("Do1.sdo1.sdo2", "da2.bda1.bda2");
        Assertions.assertThat(dataAttributeRefs.getDoName().getCdc()).isEqualTo(TPredefinedCDCEnum.WYE);
        Assertions.assertThat(dataAttributeRefs.getDaName()).extracting(DaTypeName::getBType, DaTypeName::getFc)
                .containsExactly(TPredefinedBasicTypeEnum.ENUM, TFCEnum.ST);
    }


    @Test
    void isDoObjectsAndDataAttributesExists_test0() {
        // Given
        DataTypeTemplateAdapter dttAdapter = initDttAdapterFromFile(SCD_DTT_DO_SDO_DA_BDA);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDoObjectsAndDataAttributesExists(
                dttAdapter.getCurrentElem(), "LN1", "Do1.sdo1.sdo2.da2.bda1.bda2");
        // Then
        AssertionsForInterfaceTypes.assertThat(sclReportItems).isEqualTo(List.of());
    }


    @Test
    void isDoObjectsAndDataAttributesExists_when_LNodeType_not_exist() {
        // Given
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        dtt.getLNodeType().add(tlNodeType);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDoObjectsAndDataAttributesExists(dtt,
                "lnodeTypeId2", "Do1.sdo1.sdo2.da2.bda1.bda2");
        // Then
        AssertionsForInterfaceTypes.assertThat(sclReportItems)
                .hasSize(1)
                .extracting(SclReportItem::message)
                .containsExactly("No Data Attribute found with this reference Do1.sdo1.sdo2.da2.bda1.bda2 for LNodeType.id (lnodeTypeId2)");
    }

    @Test
    void isDoObjectsAndDataAttributesExists_when_DO_not_exist() {
        // Given
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        TDO tdo = new TDO();
        tdo.setType("doTypeId");
        tdo.setName("Mods");
        tlNodeType.getDO().add(tdo);
        dtt.getLNodeType().add(tlNodeType);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDoObjectsAndDataAttributesExists(dtt,
                "lnodeTypeId", "Mod.stVal");
        // Then
        AssertionsForInterfaceTypes.assertThat(sclReportItems)
                .hasSize(1)
                .extracting(SclReportItem::message)
                .containsExactly("Unknown DO.name (Mod) in DOType.id (lnodeTypeId)");
    }


    @Test
    void isDoObjectsAndDataAttributesExists_when_DOType_not_exist() {
        // Given
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        TDO tdo = new TDO();
        tdo.setType("doTypeId");
        tdo.setName("Mod");
        tlNodeType.getDO().add(tdo);
        dtt.getLNodeType().add(tlNodeType);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDoObjectsAndDataAttributesExists(dtt,
                "lnodeTypeId", "Mod.stVal");
        // Then
        AssertionsForInterfaceTypes.assertThat(sclReportItems)
                .hasSize(1)
                .extracting(SclReportItem::message)
                .containsExactly("DOType.id (doTypeId) for DO.name (Mod) not found in DataTypeTemplates");
    }

    @Test
    void isDoObjectsAndDataAttributesExists_when_DA_not_exist() {
        // Given
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        TDO tdo = new TDO();
        tdo.setType("doTypeId");
        tdo.setName("Mod");
        tlNodeType.getDO().add(tdo);
        dtt.getLNodeType().add(tlNodeType);
        TDOType tdoType = new TDOType();
        tdoType.setId("doTypeId");
        TDA tda = new TDA();
        tda.setName("daName");
        tdoType.getSDOOrDA().add(tda);
        dtt.getDOType().add(tdoType);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDoObjectsAndDataAttributesExists(dtt,
                "lnodeTypeId", "Mod.stVal");
        // Then
        AssertionsForInterfaceTypes.assertThat(sclReportItems)
                .hasSize(1)
                .extracting(SclReportItem::message)
                .containsExactly("Unknown Data Attribute DA (stVal) in DOType.id (doTypeId)");
    }


    @Test
    void isDoObjectsAndDataAttributesExists_when_DO_DA_exist() {
        // Given
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        TDO tdo = new TDO();
        tdo.setType("doTypeId");
        tdo.setName("Mod");
        tlNodeType.getDO().add(tdo);
        dtt.getLNodeType().add(tlNodeType);
        TDOType tdoType = new TDOType();
        tdoType.setId("doTypeId");
        TDA tda = new TDA();
        tda.setName("stVal");
        tdoType.getSDOOrDA().add(tda);
        dtt.getDOType().add(tdoType);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDoObjectsAndDataAttributesExists(dtt,
                "lnodeTypeId", "Mod.stVal");
        // Then
        AssertionsForInterfaceTypes.assertThat(sclReportItems).isEmpty();
    }


    @Test
    void isDoObjectsAndDataAttributesExists_test01() {
        // Given
        DataTypeTemplateAdapter dttAdapter = initDttAdapterFromFile(SCD_DTT_DO_SDO_DA_BDA);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDoObjectsAndDataAttributesExists(
                dttAdapter.getCurrentElem(), "LN1", "Do1.sdo1.sdo2.da2.bda1.bda2");
        // Then
        AssertionsForInterfaceTypes.assertThat(sclReportItems).isEqualTo(List.of());
    }
}