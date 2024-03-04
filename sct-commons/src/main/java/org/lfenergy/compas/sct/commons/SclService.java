// SPDX-FileCopyrightText: 2021 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.api.SclEditor;
import org.lfenergy.compas.sct.commons.dto.*;
import org.lfenergy.compas.sct.commons.exception.ScdException;
import org.lfenergy.compas.sct.commons.scl.ObjectReferenceService;
import org.lfenergy.compas.sct.commons.scl.SclRootAdapter;
import org.lfenergy.compas.sct.commons.scl.com.CommunicationAdapter;
import org.lfenergy.compas.sct.commons.scl.com.ConnectedAPAdapter;
import org.lfenergy.compas.sct.commons.scl.com.SubNetworkAdapter;
import org.lfenergy.compas.sct.commons.scl.dtt.DataTypeTemplateAdapter;
import org.lfenergy.compas.sct.commons.scl.dtt.LNodeTypeAdapter;
import org.lfenergy.compas.sct.commons.scl.header.HeaderAdapter;
import org.lfenergy.compas.sct.commons.scl.icd.IcdHeader;
import org.lfenergy.compas.sct.commons.scl.ied.DAITracker;
import org.lfenergy.compas.sct.commons.scl.ied.IEDAdapter;
import org.lfenergy.compas.sct.commons.scl.ldevice.LDeviceAdapter;
import org.lfenergy.compas.sct.commons.scl.ln.AbstractLNAdapter;
import org.lfenergy.compas.sct.commons.scl.ln.LN0Adapter;
import org.lfenergy.compas.sct.commons.scl.sstation.SubstationAdapter;
import org.lfenergy.compas.sct.commons.util.PrivateUtils;
import org.lfenergy.compas.sct.commons.util.Utils;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static org.lfenergy.compas.sct.commons.util.CommonConstants.IED_TEST_NAME;
import static org.lfenergy.compas.sct.commons.util.PrivateEnum.COMPAS_ICDHEADER;

@Slf4j
public class SclService implements SclEditor {

    private static final String MESSAGE_IED_NAME_NOT_FOUND = "IED.name '%s' not found in SCD";
    private static final String MESSAGE_LDEVICE_INST_NOT_FOUND = "Unknown LDevice (%s) in IED (%s)";
    private static final String MESSAGE_LNODE_TYPE_NOT_FOUND = "LNodeType.id '%s' not found in DataTypeTemplates";
    private static final String MESSAGE_LN_NOT_FOUND = "LN [lnType='%s', prefix='%s', lnClass='%s', inst='%s'] not found in SCD";

    private final IedService iedService = new IedService();
    private final LdeviceService ldeviceService = new LdeviceService();
    private final DataTypeTemplatesService templatesService = new DataTypeTemplatesService();
    private final LnodeTypeService lnodeTypeService = new LnodeTypeService();
    private final LnService lnService = new LnService();

    @Override
    public SCL initScl(final UUID hId, final String hVersion, final String hRevision) throws ScdException {
        SclRootAdapter scdAdapter = new SclRootAdapter(hId.toString(), hVersion, hRevision);
        scdAdapter.addPrivate(PrivateUtils.createPrivate(TCompasSclFileType.SCD));
        return scdAdapter.getCurrentElem();
    }

    @Override
    public void addHistoryItem(SCL scd, String who, String what, String why) {
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        HeaderAdapter headerAdapter = sclRootAdapter.getHeaderAdapter();
        headerAdapter.addHistoryItem(who, what, why);
    }

    @Override
    public void updateHeader(@NonNull SCL scd, @NonNull HeaderDTO headerDTO) {
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        HeaderAdapter headerAdapter = sclRootAdapter.getHeaderAdapter();
        boolean hUpdated = false;
        String hVersion = headerDTO.getVersion();
        String hRevision = headerDTO.getRevision();
        if (hVersion != null && !hVersion.equals(headerAdapter.getHeaderVersion())) {
            headerAdapter.updateVersion(hVersion);
            hUpdated = true;
        }
        if (hRevision != null && !hRevision.equals(headerAdapter.getHeaderRevision())) {
            headerAdapter.updateRevision(hRevision);
            hUpdated = true;
        }
        if (hUpdated && !headerDTO.getHistoryItems().isEmpty()) {
            headerAdapter.addHistoryItem(
                    headerDTO.getHistoryItems().get(0).getWho(),
                    headerDTO.getHistoryItems().get(0).getWhat(),
                    headerDTO.getHistoryItems().get(0).getWhy()
            );
        }
    }

    @Override
    public void addIED(SCL scd, String iedName, SCL icd) throws ScdException {
         new SclRootAdapter(scd).addIED(icd, iedName);
    }

    @Override
    public void addSubnetworks(SCL scd, List<SubNetworkDTO> subNetworks, SCL icd) throws ScdException {
        if (!subNetworks.isEmpty()) {
            CommunicationAdapter communicationAdapter = new SclRootAdapter(scd).getCommunicationAdapter(true);
            for (SubNetworkDTO subNetworkDTO : subNetworks) {
                String snName = subNetworkDTO.getName();
                String snType = subNetworkDTO.getType();
                for (ConnectedApDTO accessPoint : subNetworkDTO.getConnectedAPs()) {
                    String iedName = accessPoint.iedName();
                    String apName = accessPoint.apName();
                    communicationAdapter.addSubnetwork(snName, snType, iedName, apName);

                    Optional<SubNetworkAdapter> subNetworkAdapter = communicationAdapter.getSubnetworkByName(snName);
                    if (subNetworkAdapter.isPresent()) {
                        ConnectedAPAdapter connectedAPAdapter = subNetworkAdapter.get().getConnectedAPAdapter(iedName, apName);
                        connectedAPAdapter.copyAddressAndPhysConnFromIcd(icd);
                    }
                }
            }
        }
    }

    @Override
    public void addSubnetworks(SCL scd, SCL icd, String iedName) throws ScdException {
        Optional.ofNullable(icd.getCommunication()).ifPresent(tCommunication ->
                tCommunication.getSubNetwork().forEach(icdSubNetwork ->
                        icdSubNetwork.getConnectedAP().forEach(icdConnectedAP -> {
                            // init Communication if not exist
                            CommunicationAdapter communicationAdapter = new SclRootAdapter(scd).getCommunicationAdapter(true);
                            // add SubNetwork if not exist, add ConnectedAP to SubNetwork if not exist
                            SubNetworkAdapter subNetworkAdapter = communicationAdapter
                                    .addSubnetwork(icdSubNetwork.getName(), icdSubNetwork.getType(), iedName, icdConnectedAP.getApName());
                            // copy Address And PhysConn From Icd to Scd
                            subNetworkAdapter.getConnectedAPAdapter(iedName, icdConnectedAP.getApName())
                                    .copyAddressAndPhysConnFromIcd(icd);
                        })
                ));
    }

    @Override
    public void updateDAI(SCL scd, String iedName, String ldInst, DataAttributeRef dataAttributeRef) throws ScdException {
        long startTime = System.nanoTime();
        log.info(Utils.entering());
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        DataTypeTemplateAdapter dttAdapter = sclRootAdapter.getDataTypeTemplateAdapter();
        LNodeTypeAdapter lNodeTypeAdapter = dttAdapter.getLNodeTypeAdapterById(dataAttributeRef.getLnType())
                .orElseThrow(() -> new ScdException("Unknown LNodeType : " + dataAttributeRef.getLnType()));
        lNodeTypeAdapter.checkDoAndDaTypeName(dataAttributeRef.getDoName(), dataAttributeRef.getDaName());

        if (TPredefinedBasicTypeEnum.OBJ_REF == dataAttributeRef.getBType()) {
            Long sGroup = dataAttributeRef.getDaName().getDaiValues().keySet().stream().findFirst().orElse(-1L);
            String val = sGroup < 0 ? null : dataAttributeRef.getDaName().getDaiValues().get(sGroup);
            sclRootAdapter.checkObjRef(val);
        }

        IEDAdapter iedAdapter = sclRootAdapter.getIEDAdapterByName(iedName);
        LDeviceAdapter lDeviceAdapter = iedAdapter.findLDeviceAdapterByLdInst(ldInst)
                .orElseThrow(() -> new ScdException(String.format("Unknown LDevice (%s) in IED (%s)", ldInst, iedName)));

        AbstractLNAdapter<?> lnAdapter = AbstractLNAdapter.builder()
                .withLDeviceAdapter(lDeviceAdapter)
                .withLnClass(dataAttributeRef.getLnClass())
                .withLnInst(dataAttributeRef.getLnInst())
                .withLnPrefix(dataAttributeRef.getPrefix())
                .build();

        if (TPredefinedCDCEnum.ING == dataAttributeRef.getCdc() || TPredefinedCDCEnum.ASG == dataAttributeRef.getCdc()) {
            DAITracker daiTracker = new DAITracker(lnAdapter, dataAttributeRef.getDoName(), dataAttributeRef.getDaName());
            daiTracker.validateBoundedDAI();
        }
        lnAdapter.updateDAI(dataAttributeRef);
        log.info(Utils.leaving(startTime));
    }
    public void updateDAI2(SCL scd, String iedName, String ldInst, DataAttributeRef dataAttributeRef) throws ScdException {
        iedService.findIed(scd, tied -> tied.getName().equals(iedName))
                .ifPresentOrElse(tied1 -> ldeviceService.findLdevice(tied1, tlDevice -> tlDevice.getInst().equals(ldInst))
                        .ifPresentOrElse(tlDevice ->
                                Stream.concat(tlDevice.getLN().stream(), Stream.of(tlDevice.getLN0()))
                                        .filter(anyLN -> tAnyLNPredicate.test(anyLN, dataAttributeRef))
                                        .findFirst()
                                        .ifPresentOrElse(anyLN -> lnodeTypeService.findLnodeType(scd.getDataTypeTemplates(), tlNodeType -> tlNodeType.getId().equals(anyLN.getLnType()))
                                                        .ifPresentOrElse(tlNodeType -> templatesService.findDataObjectsAndDataAttributesByDataReference(scd.getDataTypeTemplates(), anyLN.getLnType(), dataAttributeRef.getDataAttributes())
                                                                        .ifPresent(dataAttributeRef1 -> {
                                                                            if (TPredefinedBasicTypeEnum.OBJ_REF == dataAttributeRef.getBType()) {
                                                                                Long sGroup = dataAttributeRef.getDaName().getDaiValues().keySet().stream().findFirst().orElse(-1L);
                                                                                String val = sGroup < 0 ? null : dataAttributeRef.getDaName().getDaiValues().get(sGroup);
                                                                                if(!new ObjectReferenceService().isValidObjRefValue(scd, iedName, val)){
                                                                                    throw new ScdException("Invalid ObjRef: " + val);
                                                                                }
                                                                            }
                                                                            //TODO
                                                                            if (TPredefinedCDCEnum.ING == dataAttributeRef.getCdc() || TPredefinedCDCEnum.ASG == dataAttributeRef.getCdc()) {
//                                                                            DAITracker daiTracker = new DAITracker(lnAdapter, dataAttributeRef.getDoName(), dataAttributeRef.getDaName());
//                                                                            daiTracker.validateBoundedDAI();
                                                                            }
                                                                            lnService.updateOrCreateDOAndDAInstances(anyLN, dataAttributeRef1);
                                                                        }),
                                                                ()-> {
                                                            throw new ScdException(String.format(MESSAGE_LNODE_TYPE_NOT_FOUND, dataAttributeRef.getLnType()));
                                                        }), ()-> {
                                            throw new ScdException(String.format(MESSAGE_LN_NOT_FOUND, dataAttributeRef.getLnType(), dataAttributeRef.getPrefix(), dataAttributeRef.getLnClass(), dataAttributeRef.getLnInst()));
                                        }),()-> {
                            throw new ScdException(String.format(MESSAGE_LDEVICE_INST_NOT_FOUND, ldInst, iedName));
                        }), ()-> {
                    throw new ScdException(String.format(MESSAGE_IED_NAME_NOT_FOUND, iedName));
                });
    }

    private final BiPredicate<TAnyLN, DataAttributeRef> tAnyLNPredicate = (anyLN, dataRef) ->
            anyLN instanceof TLN0
                    && dataRef.getLnClass() != null && dataRef.getLnClass().equals(TLLN0Enum.LLN_0.value())
                    || anyLN instanceof TLN ln
                    && (Utils.lnClassEquals(ln.getLnClass(), dataRef.getLnClass())
                    && ln.getInst().equals(dataRef.getLnInst())
                    && Utils.equalsOrBothBlank(dataRef.getPrefix(), ln.getPrefix()));


        @Override
    public void importSTDElementsInSCD(SCL scd, List<SCL> stds) throws ScdException {

        //Check SCD and STD compatibilities
        Map<String, PrivateLinkedToStds> mapICDSystemVersionUuidAndSTDFile = PrivateUtils.createMapICDSystemVersionUuidAndSTDFile(stds);
        PrivateUtils.checkSTDCorrespondanceWithLNodeCompasICDHeader(mapICDSystemVersionUuidAndSTDFile);
        // List all Private and remove duplicated one with same iedName
        // For each Private.ICDSystemVersionUUID and Private.iedName find STD File
        List<String> iedNamesUsed = new ArrayList<>();
        SclRootAdapter scdRootAdapter = new SclRootAdapter(scd);
        PrivateUtils.streamIcdHeaders(scd)
                .forEach(icdHeader -> {
                    if (!iedNamesUsed.contains(icdHeader.getIedName())) {
                        String iedName = icdHeader.getIedName();
                        iedNamesUsed.add(iedName);
                        String icdSysVerUuid = icdHeader.getIcdSystemVersionUUID();
                        if (!mapICDSystemVersionUuidAndSTDFile.containsKey(icdSysVerUuid))
                            throw new ScdException("There is no STD file found corresponding to " + icdHeader);
                        // import /ied /dtt in Scd
                        SCL std = mapICDSystemVersionUuidAndSTDFile.get(icdSysVerUuid).stdList().get(0);
                        SclRootAdapter stdRootAdapter = new SclRootAdapter(std);
                        IEDAdapter stdIedAdapter = new IEDAdapter(stdRootAdapter, std.getIED().get(0));
                        Optional<TPrivate> optionalTPrivate = stdIedAdapter.getPrivateHeader(COMPAS_ICDHEADER.getPrivateType());
                        if (optionalTPrivate.isPresent() && optionalTPrivate.flatMap(PrivateUtils::extractCompasICDHeader).map(IcdHeader::new).get().equals(icdHeader)) {
                            PrivateUtils.copyCompasICDHeaderFromLNodePrivateIntoSTDPrivate(optionalTPrivate.get(), icdHeader.toTCompasICDHeader());
                        } else throw new ScdException("COMPAS-ICDHeader is not the same in Substation and in IED");
                        scdRootAdapter.addIED(std, iedName);

                        //import connectedAP and rename ConnectedAP/@iedName
                        addSubnetworks(scdRootAdapter.getCurrentElem(), std, iedName);
                    }
                });
    }

    @Override
    public List<SclReportItem> updateLDeviceStatus(SCL scd) {
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        SubstationAdapter substationAdapter = sclRootAdapter.getSubstationAdapter();
        final List<Pair<String, String>> iedNameLdInstList = substationAdapter.getIedAndLDeviceNamesForLN0FromLNode();
        return sclRootAdapter.streamIEDAdapters()
                .flatMap(IEDAdapter::streamLDeviceAdapters)
                .map(LDeviceAdapter::getLN0Adapter)
                .map(ln0Adapter -> ln0Adapter.updateLDeviceStatus(iedNameLdInstList))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<SclReportItem> updateDoInRef(SCL scd) {
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        return sclRootAdapter.streamIEDAdapters()
                .flatMap(IEDAdapter::streamLDeviceAdapters)
                .map(LDeviceAdapter::getLN0Adapter)
                .map(LN0Adapter::updateDoInRef)
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public List<SclReportItem> manageMonitoringLns(SCL scd) {
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        return sclRootAdapter.streamIEDAdapters()
                .filter(iedAdapter -> !iedAdapter.getName().contains(IED_TEST_NAME))
                .map(IEDAdapter::manageMonitoringLns)
                .flatMap(List::stream)
                .toList();
    }
}
