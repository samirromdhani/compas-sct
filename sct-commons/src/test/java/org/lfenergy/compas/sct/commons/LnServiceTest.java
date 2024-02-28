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
import org.lfenergy.compas.sct.commons.dto.SclReportItem;
import org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller;
import org.lfenergy.compas.sct.commons.util.ActiveStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.lfenergy.compas.sct.commons.testhelpers.DataTypeUtils.*;

class LnServiceTest {

    @Test
    void getAnylns_should_return_lns() {
        //Given
        SCL std = SclTestMarshaller.getSCLFromFile("/std/std_sample.std");
        TLDevice tlDevice = std.getIED().get(0).getAccessPoint().get(0).getServer().getLDevice().get(0);
        LnService lnService = new LnService();

        //When
        List<TAnyLN> tAnyLNS = lnService.getAnylns(tlDevice).toList();

        //Then
        assertThat(tAnyLNS)
                .hasSize(2)
                .extracting(TAnyLN::getLnType)
                .containsExactly("RTE_080BBB4D93E4E704CF69E8616CAF1A74_LLN0_V1.0.0", "RTE_8884DBCF760D916CCE3EE9D1846CE46F_LPAI_V1.0.0");
    }

    @Test
    void getDaiModStval_should_return_status() {
        //Given
        SCL std = SclTestMarshaller.getSCLFromFile("/std/std_sample.std");
        TLDevice tlDevice = std.getIED().get(0).getAccessPoint().get(0).getServer().getLDevice().get(0);
        LnService lnService = new LnService();

        //When
        Optional<ActiveStatus> daiModStval = lnService.getDaiModStval(tlDevice.getLN0());

        //Then
        assertThat(daiModStval).contains(ActiveStatus.ON);
    }

    @Test
    void getLnStatus_should_return_status() {
        //Given
        SCL std = SclTestMarshaller.getSCLFromFile("/std/std_sample.std");
        TLDevice tlDevice = std.getIED().get(0).getAccessPoint().get(0).getServer().getLDevice().get(0);
        LnService lnService = new LnService();

        //When
        ActiveStatus lnStatus = lnService.getLnStatus(tlDevice.getLN().get(0), tlDevice.getLN0());

        //Then
        assertThat(lnStatus).isEqualTo(ActiveStatus.ON);
    }

    @Test
    void getActiveLns_should_return_lns() {
        //Given
        SCL std = SclTestMarshaller.getSCLFromFile("/std/std_sample.std");
        TLDevice tlDevice = std.getIED().get(0).getAccessPoint().get(0).getServer().getLDevice().get(0);
        LnService lnService = new LnService();

        //When
        List<TAnyLN> tAnyLNS = lnService.getActiveLns(tlDevice).toList();

        //Then
        assertThat(tAnyLNS)
                .hasSize(2)
                .extracting(TAnyLN::getLnType, TAnyLN::getDesc)
                .containsExactly(Tuple.tuple("RTE_080BBB4D93E4E704CF69E8616CAF1A74_LLN0_V1.0.0", ""),
                        Tuple.tuple("RTE_8884DBCF760D916CCE3EE9D1846CE46F_LPAI_V1.0.0", ""));
    }

    @Test
    void isDOAndDAInstanceExists_should_return_true_when_DO_and_DA_instances_exists() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        TAnyLN tAnyLN = scd.getIED().stream()
                .filter(tied -> tied.getName().equals("IED_NAME")).findFirst().get()
                .getAccessPoint()
                .get(0)
                .getServer()
                .getLDevice().stream()
                .filter(tlDevice -> tlDevice.getInst().equals("LD_INS1")).findFirst()
                .get()
                .getLN0();
        DoTypeName doTypeName = new DoTypeName("Do.sdo1.d");
        DaTypeName daTypeName = new DaTypeName("antRef.bda1.bda2.bda3");
        //When
        LnService lnService = new LnService();
        boolean exist = lnService.isDOAndDAInstanceExists(tAnyLN, doTypeName, daTypeName);
        //Then
        assertThat(exist).isTrue();
    }


    @Test
    void isDOAndDAInstanceExists_should_return_false_when_DO_and_DA_instances_not_exists() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        TAnyLN tAnyLN = scd.getIED().stream()
                .filter(tied -> tied.getName().equals("IED_NAME")).findFirst().get()
                .getAccessPoint()
                .get(0)
                .getServer()
                .getLDevice().stream()
                .filter(tlDevice -> tlDevice.getInst().equals("LD_INS1")).findFirst()
                .get()
                .getLN0();
        DoTypeName doTypeName = new DoTypeName("Do.sdo1.d");
        DaTypeName daTypeName = new DaTypeName("antRef.unknown.bda2.bda3");
        //When
        LnService lnService = new LnService();
        boolean exist = lnService.isDOAndDAInstanceExists(tAnyLN, doTypeName, daTypeName);
        //Then
        assertThat(exist).isFalse();
    }


    private TAnyLN initDOAndDAInstances(List<String> doInstances, List<String> daInstances){
        TLN0 tln0 = new TLN0();
//        TDOI
//        tln0.getDOI().add()
//        doInstances.g
        return tln0;
    }

    @Test
    void getDOAndDAInstances_should_return_when_ADF() {
        //Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        TAnyLN tAnyLN = scd.getIED().stream()
                .filter(tied -> tied.getName().equals("IED_NAME")).findFirst().get()
                .getAccessPoint()
                .get(0)
                .getServer()
                .getLDevice().stream()
                .filter(tlDevice -> tlDevice.getInst().equals("LD_INS1")).findFirst()
                .get()
                .getLN0();
        DoTypeName doTypeName = new DoTypeName("Do.sdo1.d");
        doTypeName.setCdc(TPredefinedCDCEnum.WYE);
        DaTypeName daTypeName = new DaTypeName("antRef.bda1.bda2.bda3");
        daTypeName.setFc(TFCEnum.ST);
        daTypeName.setBType(TPredefinedBasicTypeEnum.ENUM);
        daTypeName.setValImport(true);

        DataAttributeRef dataAttributeRef = new DataAttributeRef();
        dataAttributeRef.setDoName(doTypeName);
        dataAttributeRef.setDaName(daTypeName);

        assertThat(daTypeName.isValImport()).isEqualTo(true);
        assertThat(daTypeName.isUpdatable()).isEqualTo(true);
        //When
        LnService lnService = new LnService();
//        assertThatCode(() -> lnService.getDOAndDAInstances(tAnyLN, dataAttributeRef))
//                .doesNotThrowAnyException();
        List<SclReportItem> sclReportItems = lnService.getDOAndDAInstances(tAnyLN, dataAttributeRef);
        //Then
        assertThat(sclReportItems).isEmpty();
        assertThat(dataAttributeRef.getDoRef()).isEqualTo("Do.sdo1.d");
        assertThat(dataAttributeRef.getDaRef()).isEqualTo("antRef.bda1.bda2.bda3");
        assertThat(dataAttributeRef.getDaName().isValImport()).isEqualTo(false);
        assertThat(dataAttributeRef.getDaName().isUpdatable()).isEqualTo(false);
        assertThat(dataAttributeRef.getDoName())
                .usingRecursiveComparison()
                .isEqualTo(doTypeName);
        assertThat(dataAttributeRef.getDaName())
                .usingRecursiveComparison()
                .ignoringFields("valImport","daiValues")
                .isEqualTo(daTypeName);
    }

    @Test
    void updateOrCreateDoObjectsAndDataAttributesInstances_should_create_given_DO_and_DA_instances_when_no_struct_and_with_settingGroup() {
        //Given
        TAnyLN tAnyLN = new LN0();

        DoTypeName doTypeName = new DoTypeName("Mod");
        DaTypeName daTypeName = new DaTypeName("stVal");
        daTypeName.getDaiValues().put(1L, "new value");
        daTypeName.getDaiValues().put(2L, "new value 2");
        DataAttributeRef dataAttributeRef = createDataAttributeRef(doTypeName, daTypeName);

        //When
        LnService lnService = new LnService();
        lnService.updateOrCreateDOAndDAInstances(tAnyLN, dataAttributeRef);
        //Then
        assertThat(tAnyLN.getDOI()).hasSize(1);
        assertThat(tAnyLN.getDOI().get(0).getName()).isEqualTo("Mod");
        assertThat(tAnyLN.getDOI().get(0).getSDIOrDAI()).hasSize(1);
        assertThat(((TDAI)tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getName()).isEqualTo("stVal");
        assertThat(((TDAI)tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getVal()).hasSize(2);
        assertThat(((TDAI)tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getVal().get(0).getSGroup()).isEqualTo(1L);
        assertThat(((TDAI)tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getVal().get(0).getValue()).isEqualTo("new value");
        assertThat(((TDAI)tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getVal().get(1).getSGroup()).isEqualTo(2L);
        assertThat(((TDAI)tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getVal().get(1).getValue()).isEqualTo("new value 2");
    }


    @Test
    void updateOrCreateDoObjectsAndDataAttributesInstances_should_create_given_DO_and_DA_instances_when_struct_and_without_settingGroup() {
        //Given
        TAnyLN tAnyLN = new LN0();
        DoTypeName doTypeName = new DoTypeName("Do.sdo1.d");
        DaTypeName daTypeName = new DaTypeName("antRef.bda1");
        daTypeName.getDaiValues().put(0L, "new value");
        DataAttributeRef dataAttributeRef = createDataAttributeRef(doTypeName, daTypeName);

        //When
        LnService lnService = new LnService();
        lnService.updateOrCreateDOAndDAInstances(tAnyLN, dataAttributeRef);

        //Then
        assertThat(tAnyLN.getDOI()).hasSize(1);
        assertThat(tAnyLN.getDOI().get(0).getName()).isEqualTo("Do");
        assertThat(tAnyLN.getDOI().get(0).getSDIOrDAI()).hasSize(1);
        assertThat((( TSDI )tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getName()).isEqualTo("sdo1");
        assertThat((( TSDI )tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getSDIOrDAI()).hasSize(1);
        assertThat((( TSDI )(( TSDI )tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getSDIOrDAI().get(0)).getName()).isEqualTo("d");
        assertThat((( TSDI )(( TSDI )tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getSDIOrDAI().get(0)).getSDIOrDAI()).hasSize(1);

        assertThat((( TSDI )(( TSDI )(( TSDI )tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getSDIOrDAI().get(0))
                .getSDIOrDAI().get(0)).getName()).isEqualTo("antRef");
        assertThat((( TSDI )(( TSDI )(( TSDI )tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getSDIOrDAI().get(0))
                .getSDIOrDAI().get(0)).getSDIOrDAI()).hasSize(1);

        assertThat((( TDAI )(( TSDI )(( TSDI )(( TSDI )tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getSDIOrDAI().get(0))
                .getSDIOrDAI().get(0)).getSDIOrDAI().get(0)).getName()).isEqualTo("bda1");
        assertThat((( TDAI )(( TSDI )(( TSDI )(( TSDI )tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getSDIOrDAI().get(0))
                .getSDIOrDAI().get(0)).getSDIOrDAI().get(0)).getVal()).hasSize(1);
        assertThat((( TDAI )(( TSDI )(( TSDI )(( TSDI )tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getSDIOrDAI().get(0))
                .getSDIOrDAI().get(0)).getSDIOrDAI().get(0)).getVal().get(0).isSetSGroup()).isFalse();
        assertThat((( TDAI )(( TSDI )(( TSDI )(( TSDI )tAnyLN.getDOI().get(0).getSDIOrDAI().get(0)).getSDIOrDAI().get(0))
                .getSDIOrDAI().get(0)).getSDIOrDAI().get(0)).getVal().get(0).getValue()).isEqualTo("new value");
    }


}