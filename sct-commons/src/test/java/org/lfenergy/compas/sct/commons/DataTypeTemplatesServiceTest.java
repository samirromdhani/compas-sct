// SPDX-FileCopyrightText: 2024 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.junit.jupiter.api.Test;
import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;
import org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.lfenergy.compas.sct.commons.scl.dtt.DataTypeTemplateTestUtils.*;

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
    void isDataObjectsAndDataAttributesExists_should_find_DO_SDO_DA_and_BDA() {
        // Given
        String SCD_DTT_DO_SDO_DA_BDA = "/dtt-test-schema-conf/scd_dtt_do_sdo_da_bda.xml";
        TDataTypeTemplates dtt = initDttFromFile(SCD_DTT_DO_SDO_DA_BDA);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        DataAttributeRef dataAttributeRefs = dataTypeTemplatesService.getDataObjectsAndDataAttributes(
                dtt, "LN1", "Do1.sdo1.sdo2.da2.bda1.bda2");
        // Then
        assertThatCode(() -> dataTypeTemplatesService.isDataObjectsAndDataAttributesExists(
                dtt, "LN1", "Do1.sdo1.sdo2.da2.bda1.bda2"))
                .doesNotThrowAnyException();

        assertThat(dataAttributeRefs.getDoRef()).isEqualTo("Do1.sdo1.sdo2");
        assertThat(dataAttributeRefs.getDaRef()).isEqualTo("da2.bda1.bda2");
        assertThat(dataAttributeRefs).extracting(DataAttributeRef::getDoRef, DataAttributeRef::getDaRef)
                .containsExactly("Do1.sdo1.sdo2", "da2.bda1.bda2");
        assertThat(dataAttributeRefs.getDoName().getCdc()).isEqualTo(TPredefinedCDCEnum.WYE);
        assertThat(dataAttributeRefs.getDaName()).extracting(DaTypeName::getBType, DaTypeName::getFc)
                .containsExactly(TPredefinedBasicTypeEnum.ENUM, TFCEnum.ST);
    }


    @Test
    void isDataObjectsAndDataAttributesExists_should_find_DO_SDO_DA_and_BDA_test2() {
        // Given
//        DataTypeTemplateAdapter dttAdapter = initDttAdapterFromFile(SCD_DTT_DO_SDO_DA_BDA);
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        TDataTypeTemplates dtt = scd.getDataTypeTemplates();
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        String dataRef = "Do.sdo1.d.antRef.bda1.bda2.bda3";
        DataAttributeRef dataAttributeRefs = dataTypeTemplatesService.getDataObjectsAndDataAttributes(
                dtt, "LNO1", dataRef);
        // Then
        assertThatCode(() -> dataTypeTemplatesService.getDataObjectsAndDataAttributes(
                dtt, "LNO1", dataRef))
                .doesNotThrowAnyException();

        assertThat(dataAttributeRefs.getDoRef()).isEqualTo("Do.sdo1.d");
        assertThat(dataAttributeRefs.getDaRef()).isEqualTo("antRef.bda1.bda2.bda3");
        assertThat(dataAttributeRefs).extracting(DataAttributeRef::getDoRef, DataAttributeRef::getDaRef)
                .containsExactly("Do.sdo1.d", "antRef.bda1.bda2.bda3");
        assertThat(dataAttributeRefs.getDoName().getCdc()).isEqualTo(TPredefinedCDCEnum.WYE);
        assertThat(dataAttributeRefs.getDaName()).extracting(DaTypeName::getBType, DaTypeName::getFc)
                .containsExactly(TPredefinedBasicTypeEnum.ENUM, TFCEnum.ST);
        assertThat(dataAttributeRefs.getDaName().isValImport()).isEqualTo(true);
        assertThat(dataAttributeRefs.getDaName().isUpdatable()).isEqualTo(true);
    }


    @Test
    void isDoObjectsAndDataAttributesExists_when_LNodeType_not_exist_should_return_error_report_item() {
        // Given
        TDataTypeTemplates dtt = new TDataTypeTemplates();
        TLNodeType tlNodeType = new TLNodeType();
        tlNodeType.setId("lnodeTypeId");
        dtt.getLNodeType().add(tlNodeType);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDataObjectsAndDataAttributesExists(dtt,
                "lnodeTypeId2", "Do1.sdo1.sdo2.da2.bda1.bda2");
        // Then
        assertThat(sclReportItems)
                .hasSize(1)
                .extracting(SclReportItem::message)
                .containsExactly("No Data Attribute found with this reference Do1.sdo1.sdo2.da2.bda1.bda2 for LNodeType.id (lnodeTypeId2)");
    }

    @Test
    void isDoObjectsAndDataAttributesExists_when_DO_not_exist_should_return_error_report_item() {
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
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDataObjectsAndDataAttributesExists(dtt,
                "lnodeTypeId", "Mod.stVal");
        // Then
        assertThat(sclReportItems)
                .hasSize(1)
                .extracting(SclReportItem::message)
                .containsExactly("Unknown DO.name (Mod) in DOType.id (lnodeTypeId)");
    }


    @Test
    void isDoObjectsAndDataAttributesExists_when_DOType_not_exist_should_return_error_report_item() {
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
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDataObjectsAndDataAttributesExists(dtt,
                "lnodeTypeId", "Mod.stVal");
        // Then
        assertThat(sclReportItems)
                .hasSize(1)
                .extracting(SclReportItem::message)
                .containsExactly("DOType.id (doTypeId) for DO.name (Mod) not found in DataTypeTemplates");
    }

    @Test
    void isDoObjectsAndDataAttributesExists_when_DA_not_exist_should_return_error_report_item() {
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
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDataObjectsAndDataAttributesExists(dtt,
                "lnodeTypeId", "Mod.stVal");
        // Then
        assertThat(sclReportItems)
                .hasSize(1)
                .extracting(SclReportItem::message)
                .containsExactly("Unknown Sub Data Object SDO or Data Attribute DA (stVal) in DOType.id (doTypeId)");
    }


    @Test
    void isDoObjectsAndDataAttributesExists_when_DO_DA_exist_should_return_empty_report() {
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
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDataObjectsAndDataAttributesExists(dtt,
                "lnodeTypeId", "Mod.stVal");
        // Then
        assertThat(sclReportItems).isEmpty();
    }

    @Test
    void isDoObjectsAndDataAttributesExists_should_return_empty_report() {
        // Given
        String SCD_DTT_DO_SDO_DA_BDA = "/dtt-test-schema-conf/scd_dtt_do_sdo_da_bda.xml";
        TDataTypeTemplates dtt = initDttFromFile(SCD_DTT_DO_SDO_DA_BDA);
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.isDataObjectsAndDataAttributesExists(
                dtt, "LN1", "Do1.sdo1.sdo2.da2.bda1.bda2");
        // Then
        assertThat(sclReportItems).isEmpty();
    }

    @Test
    void verifyDataObjectsAndDataAttributes_should_return_empty_report() {
        // Given
        String SCD_DTT_DO_SDO_DA_BDA = "/dtt-test-schema-conf/scd_dtt_do_sdo_da_bda.xml";
        TDataTypeTemplates dtt = initDttFromFile(SCD_DTT_DO_SDO_DA_BDA);
        DoTypeName doTypeName = new DoTypeName("Do1.sdo1.sdo2");
        DaTypeName daTypeName = new DaTypeName("da2.bda1.bda2");
        // When
        DataTypeTemplatesService dataTypeTemplatesService = new DataTypeTemplatesService();
        List<SclReportItem> sclReportItems = dataTypeTemplatesService.verifyDataObjectsAndDataAttributes(
                dtt, "LN1", doTypeName, daTypeName);
        // Then
        assertThat(sclReportItems).isEmpty();
    }

    @Test
    void getAllDataObjectsAndDataAttributes_should_return_all_dataReference() {
        // Given
        String SCD_DTT_DO_SDO_DA_BDA = "/dtt-test-schema-conf/scd_dtt_do_sdo_da_bda_test.xml";
        TDataTypeTemplates dtt = initDttFromFile(SCD_DTT_DO_SDO_DA_BDA);
        // When
        DataTypeTemplatesService dttService = new DataTypeTemplatesService();
        List<DataAttributeRef> dataRef = dttService.getAllDataObjectsAndDataAttributes(dtt);
        // Then
        assertThat(dataRef).hasSize(6);
    }

}
