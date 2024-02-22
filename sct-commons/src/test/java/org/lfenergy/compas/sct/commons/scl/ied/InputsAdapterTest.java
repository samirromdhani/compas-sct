// SPDX-FileCopyrightText: 2022 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.scl.ied;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.FcdaForDataSetsCreation;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;
import org.lfenergy.compas.sct.commons.scl.SclRootAdapter;
import org.lfenergy.compas.sct.commons.scl.ldevice.LDeviceAdapter;
import org.lfenergy.compas.sct.commons.scl.ln.AbstractLNAdapter;
import org.lfenergy.compas.sct.commons.scl.ln.LN0Adapter;
import org.lfenergy.compas.sct.commons.testhelpers.FCDARecord;
import org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller;
import org.lfenergy.compas.sct.commons.util.CsvUtils;
import org.opentest4j.AssertionFailedError;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;
import static org.lfenergy.compas.sct.commons.testhelpers.SclHelper.*;

class InputsAdapterTest {

    private Set<FcdaForDataSetsCreation> allowedFcdas;

    @BeforeEach
    void init() {
        allowedFcdas = new HashSet<>(CsvUtils.parseRows("FcdaCandidates.csv", StandardCharsets.UTF_8, FcdaForDataSetsCreation.class));
    }

    @Test
    @Tag("issue-321")
    void constructor_should_succeed() {
        // Given
        TInputs tInputs = new TInputs();
        LN0 ln0 = new LN0();
        ln0.setInputs(tInputs);
        // When
        LN0Adapter ln0Adapter = new LN0Adapter(null, ln0);
        // When && Then
        assertThatNoException().isThrownBy(() -> new InputsAdapter(ln0Adapter, tInputs));
    }

    @Test
    void elementXPath_should_return_expected_xpath_value() {
        // Given
        TInputs tInputs = new TInputs();
        InputsAdapter inputsAdapter = new InputsAdapter(null, tInputs);
        // When
        String result = inputsAdapter.elementXPath();
        // Then
        assertThat(result).isEqualTo("Inputs");
    }

    @Test
    void updateAllSourceDataSetsAndControlBlocks_should_report_Target_Ied_missing_Private_compasBay_errors() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-extref-create-dataset-and-controlblocks/scd_create_dataset_and_controlblocks_ied_errors.xml");
        InputsAdapter inputsAdapter = findInputs(scd, "IED_NAME1", "LD_INST11");
        // When
        List<SclReportItem> sclReportItems = inputsAdapter.updateAllSourceDataSetsAndControlBlocks(allowedFcdas);
        // Then
        assertThat(sclReportItems).containsExactly(
            SclReportItem.error("/SCL/IED[@name=\"IED_NAME1\"]",
                "IED is missing Private/compas:Bay@UUID attribute")
        );
    }

    @Test
    void updateAllSourceDataSetsAndControlBlocks_should_report_Source_Ied_missing_Private_compasBay_errors() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-extref-create-dataset-and-controlblocks/scd_create_dataset_and_controlblocks_ied_errors.xml");
        InputsAdapter inputsAdapter = findInputs(scd, "IED_NAME3", "LD_INST31");
        // When
        List<SclReportItem> sclReportItems = inputsAdapter.updateAllSourceDataSetsAndControlBlocks(allowedFcdas);
        // Then
        assertThat(sclReportItems).containsExactly(
            SclReportItem.error("/SCL/IED[@name=\"IED_NAME3\"]/AccessPoint/Server/LDevice[@inst=\"LD_INST31\"]/LN0/Inputs/ExtRef[@desc=\"Source IED is " +
                    "missing compas:Bay @UUID\"]",
                "Source IED is missing Private/compas:Bay@UUID attribute")
        );
    }

    @Test
    void updateAllSourceDataSetsAndControlBlocks_should_report_ExtRef_attribute_missing() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-extref-create-dataset-and-controlblocks/scd_create_dataset_and_controlblocks_extref_errors.xml");
        InputsAdapter inputsAdapter = findInputs(scd, "IED_NAME1", "LD_INST11");
        // When
        List<SclReportItem> sclReportItems = inputsAdapter.updateAllSourceDataSetsAndControlBlocks(allowedFcdas);
        // Then
        assertThat(sclReportItems).containsExactlyInAnyOrder(
            SclReportItem.error("/SCL/IED[@name=\"IED_NAME1\"]/AccessPoint/Server/LDevice[@inst=\"LD_INST11\"]/LN0/Inputs/" +
                    "ExtRef[@desc=\"ExtRef is missing ServiceType attribute\"]",
                "The signal ExtRef is missing ServiceType attribute"),
            SclReportItem.error("/SCL/IED[@name=\"IED_NAME1\"]/AccessPoint/Server/LDevice[@inst=\"LD_INST11\"]/LN0/Inputs/" +
                    "ExtRef[@desc=\"ExtRef is ServiceType Poll\"]",
                "The signal ExtRef ServiceType attribute is unexpected : POLL"),
            SclReportItem.error("/SCL/IED[@name=\"IED_NAME1\"]/AccessPoint/Server/LDevice[@inst=\"LD_INST11\"]/LN0/Inputs/" +
                    "ExtRef[@desc=\"ExtRef is ServiceType Report with malformed desc attribute\"]",
                "ExtRef.serviceType=Report but ExtRef.desc attribute is malformed")
        );
    }

    @Test
    void updateAllSourceDataSetsAndControlBlocks_should_succeed() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-extref-create-dataset-and-controlblocks/scd_create_dataset_and_controlblocks_success.xml");
        InputsAdapter inputsAdapter = findInputs(scd, "IED_NAME1", "LD_INST11");
        // When
        List<SclReportItem> sclReportItems = inputsAdapter.updateAllSourceDataSetsAndControlBlocks(allowedFcdas);
        // Then
        assertThat(sclReportItems).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideCreateFCDA")
    void updateAllSourceDataSetsAndControlBlocks_should_create_dataset_and_fcda_for_valid_extRef(String extRefDesc, String dataSetPath,
                                                                                                 List<FCDARecord> expectedFcda) {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-extref-create-dataset-and-controlblocks/scd_create_dataset_and_controlblocks_success.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        InputsAdapter inputsAdapter = keepOnlyThisExtRef(sclRootAdapter, extRefDesc);
        String[] splitPath = dataSetPath.split("/");
        final int IED_NAME_PART = 0;
        final int LDEVICE_INST_PART = 1;
        final int DATASET_NAME_PART = 2;
        String expectedSourceIedName = splitPath[IED_NAME_PART];
        String expectedSourceLDeviceInst = splitPath[LDEVICE_INST_PART];
        String expectedDataSetName = splitPath[DATASET_NAME_PART];
        // When
        List<SclReportItem> sclReportItems = inputsAdapter.updateAllSourceDataSetsAndControlBlocks(allowedFcdas);
        // Then
        assertThat(sclReportItems).isEmpty();
        DataSetAdapter dataSet = findDataSet(scd, expectedSourceIedName, expectedSourceLDeviceInst, expectedDataSetName);
        assertThat(dataSet.getCurrentElem().getFCDA())
            .extracting(TFCDA::getLdInst)
            .containsOnly(expectedSourceLDeviceInst);

        assertThat(dataSet.getCurrentElem().getFCDA())
            .map(FCDARecord::toFCDARecord)
            .containsExactly(expectedFcda.toArray(new FCDARecord[]{}));
    }

    public static Stream<Arguments> provideCreateFCDA() {
        return Stream.of(
            Arguments.of(named("should include signal internal to a Bay",
                    "test bay internal"),
                "IED_NAME2/LD_INST21/DS_LD_INST21_GSI",
                List.of(new FCDARecord("LD_INST21", "ANCR", "1", "", "DoName", "daNameST", TFCEnum.ST))),
            Arguments.of(named("should include signal external to a Bay",
                    "test bay external"),
                "IED_NAME3/LD_INST31/DS_LD_INST31_GSE",
                List.of(new FCDARecord("LD_INST31", "ANCR", "1", "", "DoName", "daNameST", TFCEnum.ST))),
            Arguments.of(named("keep source DA with fc = ST",
                    "test daName ST"),
                "IED_NAME2/LD_INST21/DS_LD_INST21_GSI",
                List.of(new FCDARecord("LD_INST21", "ANCR", "1", "", "DoName", "daNameST", TFCEnum.ST))),
            Arguments.of(named("keep source DA with fc = MX",
                    "test daName MX"),
                "IED_NAME2/LD_INST21/DS_LD_INST21_GMI",
                List.of(new FCDARecord("LD_INST21", "ANCR", "1", "", "DoName", "daNameMX", TFCEnum.MX))),
            Arguments.of(named("for GOOSE, should keep only valid fcda candidates",
                    "test ServiceType is GOOSE, no daName and DO contains ST and MX, but only ST is FCDA candidate"),
                "IED_NAME2/LD_INST21/DS_LD_INST21_GSI",
                List.of(new FCDARecord("LD_INST21", "ANCR", "1", "", "OtherDoName", "daNameST", TFCEnum.ST))),
            Arguments.of(named("for SMV, should keep only valid fcda candidates",
                    "test ServiceType is SMV, no daName and DO contains ST and MX, but only ST is FCDA candidate"),
                "IED_NAME2/LD_INST21/DS_LD_INST21_SVI",
                List.of(new FCDARecord("LD_INST21", "ANCR", "1", "", "OtherDoName", "daNameST", TFCEnum.ST))),
            Arguments.of(named("for Report, should get source daName from ExtRef.desc to deduce FC ST",
                    "test ServiceType is Report_daReportST_1"),
                "IED_NAME2/LD_INST21/DS_LD_INST21_DQCI",
                List.of(new FCDARecord("LD_INST21", "ANCR", "1", "", "DoName", null, TFCEnum.ST))),
            Arguments.of(named("for Report, should get source daName from ExtRef.desc to deduce FC MX",
                    "test ServiceType is Report_daReportMX_1"),
                "IED_NAME2/LD_INST21/DS_LD_INST21_CYCI",
                List.of(new FCDARecord("LD_INST21", "ANCR", "1", "", "DoName", null, TFCEnum.MX))),
            Arguments.of(named("should ignore instance number when checking FCDA Candidate file",
                    "test no daName and doName with instance number"),
                "IED_NAME2/LD_INST21/DS_LD_INST21_GSI",
                List.of(new FCDARecord("LD_INST21", "ANCR", "1", "", "DoWithInst1", "daNameST", TFCEnum.ST))),
            Arguments.of(named("should ignore instance number when checking FCDA Candidate file (DO with SDO)",
                    "test no daName and doName with instance number and SDO"),
                "IED_NAME2/LD_INST21/DS_LD_INST21_GSI",
                List.of(new FCDARecord("LD_INST21", "ANCR", "1", "", "DoWithInst2.subDo", "daNameST", TFCEnum.ST))),
            Arguments.of(named("hould include UNTESTED FlowStatus",
                    "test include compas:Flow.FlowStatus UNTESTED"),
                "IED_NAME2/LD_INST21/DS_LD_INST21_GSI",
                List.of(new FCDARecord("LD_INST21", "ANCR", "1", "", "DoName", "daNameST", TFCEnum.ST)))
        );
    }

    @ParameterizedTest
    @MethodSource("provideDoNotCreateFCDA")
    void updateAllSourceDataSetsAndControlBlocks_when_no_valid_source_Da_found_should_not_create_FCDA(String extRefDesc) {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-extref-create-dataset-and-controlblocks/scd_create_dataset_and_controlblocks_success.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        InputsAdapter inputsAdapter = keepOnlyThisExtRef(sclRootAdapter, extRefDesc);
        // When
        List<SclReportItem> sclReportItems = inputsAdapter.updateAllSourceDataSetsAndControlBlocks(allowedFcdas);
        // Then
        assertThat(sclReportItems).isEmpty();
        assertThat(sclRootAdapter.streamIEDAdapters()
            .flatMap(IEDAdapter::streamLDeviceAdapters)
            .filter(LDeviceAdapter::hasLN0)
            .map(LDeviceAdapter::getLN0Adapter))
            .allMatch(ln0Adapter -> !ln0Adapter.getCurrentElem().isSetDataSet());
    }

    public static Stream<Arguments> provideDoNotCreateFCDA() {
        return Stream.of(
            Arguments.of(named("should not create FCDA for source Da different from MX and ST",
                "test daName BL")),
            Arguments.of(named("should not create FCDA for extref with a binding internal to the IED",
                "test ignore internal binding")),
            Arguments.of(named("should not create FCDA for extref with missing binding attributes",
                "test ignore missing bindings attributes")),
            Arguments.of(named("should not create FCDA for ExtRef with compas:Flow.FlowStatus INACTIVE",
                "test ignore when compas:Flow.FlowStatus is neither ACTIVE nor UNTESTED"))
        );
    }

    @Test
    void updateAllSourceDataSetsAndControlBlocks_when_AccessPoint_does_not_have_dataset_creation_capability_should_report_error() {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile("/scd-extref-create-dataset-and-controlblocks/scd_create_dataset_and_controlblocks_success.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        InputsAdapter inputsAdapter = keepOnlyThisExtRef(sclRootAdapter, "test bay internal");
        TExtRef extRef = inputsAdapter.getCurrentElem().getExtRef().get(0);
        LDeviceAdapter sourceLDevice = findLDevice(sclRootAdapter.getCurrentElem(), extRef.getIedName(), extRef.getLdInst());
        sourceLDevice.getAccessPoint().setServices(new TServices());
        // When
        List<SclReportItem> sclReportItems = inputsAdapter.updateAllSourceDataSetsAndControlBlocks(allowedFcdas);
        // Then
        assertThat(sclReportItems).hasSize(1)
            .first().extracting(SclReportItem::message).asString()
            .startsWith("Could not create DataSet or ControlBlock for this ExtRef : IED/AccessPoint does not have capability to create DataSet of type GSE");
    }

    private static InputsAdapter keepOnlyThisExtRef(SclRootAdapter sclRootAdapter, String extRefDesc) {
        InputsAdapter foundInputsAdapter = sclRootAdapter.streamIEDAdapters()
            .flatMap(IEDAdapter::streamLDeviceAdapters)
            .filter(LDeviceAdapter::hasLN0)
                .map(LDeviceAdapter::getLN0Adapter)
                .filter(AbstractLNAdapter::hasInputs)
                .map(LN0Adapter::getInputsAdapter)
                .filter(inputsAdapter ->
                        inputsAdapter.getCurrentElem().getExtRef().stream().map(TExtRef::getDesc).anyMatch(extRefDesc::equals))
                .findFirst()
                .orElseThrow(() -> new AssertionFailedError("ExtRef not found: " + extRefDesc));
        foundInputsAdapter.getCurrentElem().getExtRef().removeIf(Predicate.not(extref -> extRefDesc.equals(extref.getDesc())));
        return foundInputsAdapter;
    }

    @Test
    void filterDuplicatedExtRefs_should_remove_duplicated_extrefs() {
        // Given
        TExtRef tExtRefLnClass = createExtRefExample("CB_Name1", TServiceType.GOOSE);
        tExtRefLnClass.getSrcLNClass().add(TLLN0Enum.LLN_0.value());
        TExtRef tExtRef = createExtRefExample("CB_Name1", TServiceType.GOOSE);
        List<TExtRef> tExtRefList = List.of(tExtRef, tExtRefLnClass, createExtRefExample("CB", TServiceType.GOOSE),
                createExtRefExample("CB", TServiceType.GOOSE));
        TInputs tInputs = new TInputs();
        tInputs.getExtRef().addAll(tExtRefList);
        InputsAdapter inputsAdapter = new InputsAdapter(null, tInputs);
        // When
        List<TExtRef> result = inputsAdapter.filterDuplicatedExtRefs();
        // Then
        assertThat(result).hasSizeLessThan(tExtRefList.size())
                .hasSize(2);
    }

    @Test
    void filterDuplicatedExtRefs_should_not_remove_not_duplicated_extrefs() {
        // Given
        TExtRef tExtRefIedName = createExtRefExample("CB_1", TServiceType.GOOSE);
        tExtRefIedName.setIedName("IED_XXX");
        TExtRef tExtRefLdInst = createExtRefExample("CB_1", TServiceType.GOOSE);
        tExtRefLdInst.setSrcLDInst("LD_XXX");
        TExtRef tExtRefLnInst = createExtRefExample("CB_1", TServiceType.GOOSE);
        tExtRefLnInst.setSrcLNInst("X");
        TExtRef tExtRefPrefix = createExtRefExample("CB_1", TServiceType.GOOSE);
        tExtRefPrefix.setSrcPrefix("X");
        List<TExtRef> tExtRefList = List.of(tExtRefIedName, tExtRefLdInst, tExtRefLnInst, tExtRefPrefix,
                createExtRefExample("CB_1", TServiceType.GOOSE), createExtRefExample("CB_1", TServiceType.SMV));
        TInputs tInputs = new TInputs();
        tInputs.getExtRef().addAll(tExtRefList);
        InputsAdapter inputsAdapter = new InputsAdapter(null, tInputs);
        // When
        List<TExtRef> result = inputsAdapter.filterDuplicatedExtRefs();
        // Then
        assertThat(result).hasSameSizeAs(tExtRefList)
                .hasSize(6);
    }

    @Test
    void updateAllExtRefIedNames_when_DOI_Mod_and_DAI_stVal_notExists_should_not_produce_error() {
        // Given
        SCL scl = SclTestMarshaller.getSCLFromFile("/scd-refresh-lnode/Test_Missing_ModstVal_In_LN0_when_binding.scd");
        InputsAdapter inputsAdapter = findInputs(scl, "IedName1", "LDSUIED");
        // When
        List<SclReportItem> sclReportItems = inputsAdapter.updateAllExtRefIedNames(Map.of());
        // Then
        assertThat(sclReportItems).isEmpty();
    }

}
