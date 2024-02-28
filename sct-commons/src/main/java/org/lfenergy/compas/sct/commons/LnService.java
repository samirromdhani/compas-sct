// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import lombok.extern.slf4j.Slf4j;
import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.api.LNEditor;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;
import org.lfenergy.compas.sct.commons.util.ActiveStatus;

import java.util.*;
import java.util.stream.Stream;

import static org.lfenergy.compas.sct.commons.util.CommonConstants.MOD_DO_NAME;
import static org.lfenergy.compas.sct.commons.util.CommonConstants.STVAL_DA_NAME;
import static org.lfenergy.compas.sct.commons.util.SclConstructorHelper.newVal;

@Slf4j
public class LnService implements LNEditor {

    public Stream<TAnyLN> getAnylns(TLDevice tlDevice) {
        return Stream.concat(Stream.of(tlDevice.getLN0()), tlDevice.getLN().stream());
    }

    /**
     * The Lnode status depends on the LN0 status.
     * If Ln stVAl = null => we take the LN0 status
     * If Ln stVAl = OFF => the status is OFF
     * If Ln stVAl = ON => we take the LN0 status
     *
     * @param tAnyLN the Lnode whose the status is required
     * @param ln0    the LN0
     * @return the Lnode Status
     */
    public ActiveStatus getLnStatus(TAnyLN tAnyLN, LN0 ln0) {
        Optional<ActiveStatus> ln0Status = getDaiModStval(ln0);
        return getDaiModStval(tAnyLN).filter(ActiveStatus.OFF::equals).orElseGet(() -> ln0Status.orElse(ActiveStatus.OFF));
    }

    public Optional<ActiveStatus> getDaiModStval(TAnyLN tAnyLN) {
        return tAnyLN
                .getDOI()
                .stream()
                .filter(tdoi -> MOD_DO_NAME.equals(tdoi.getName()))
                .findFirst()
                .flatMap(tdoi -> tdoi.getSDIOrDAI()
                        .stream()
                        .filter(dai -> dai.getClass().equals(TDAI.class))
                        .map(TDAI.class::cast)
                        .filter(tdai -> STVAL_DA_NAME.equals(tdai.getName()))
                        .map(TDAI::getVal)
                        .flatMap(Collection::stream)
                        .findFirst()
                        .map(TVal::getValue))
                .map(ActiveStatus::fromValue);
    }

    public Stream<TAnyLN> getActiveLns(TLDevice tlDevice) {
        LN0 ln0 = tlDevice.getLN0();
        Stream<TLN> tlnStream = tlDevice.getLN()
                .stream()
                .filter(tln -> ActiveStatus.ON.equals(getLnStatus(tln, ln0)));
        Stream<LN0> ln0Stream = Stream.of(ln0).filter(ln02 -> getDaiModStval(ln02).map(ActiveStatus.ON::equals).orElse(false));
        return Stream.concat(ln0Stream, tlnStream);
    }

    @Override
    public boolean isDOAndDAInstanceExists(TAnyLN tAnyLN, DoTypeName doTypeName, DaTypeName daTypeName) {
        return tAnyLN.getDOI().stream().filter(doi -> doTypeName.getName().equals(doi.getName()))
                .findFirst()
                .map(doi -> {
                    LinkedList<String> structNamesList = new LinkedList<>(doTypeName.getStructNames());
                    structNamesList.addLast(daTypeName.getName());
                    daTypeName.getStructNames().forEach(structNamesList::addLast);
                    if(structNamesList.size() > 1) {
                        String firstSDIName = structNamesList.remove();
                        return doi.getSDIOrDAI().stream()
                                .filter(sdi -> sdi.getClass().equals(TSDI.class))
                                .map(TSDI.class::cast)
                                .filter(tsdi -> tsdi.getName().equals(firstSDIName))
                                .findFirst()
                                .map(intermediateSdi -> findSDIByStructName(intermediateSdi, structNamesList))
                                .stream()
                                .findFirst()
                                .flatMap(lastDsi -> {
                                    if (structNamesList.size() == 1) {
                                        return lastDsi.getSDIOrDAI().stream()
                                                .filter(dai -> dai.getClass().equals(TDAI.class))
                                                .map(TDAI.class::cast)
                                                .filter(dai -> dai.getName().equals(structNamesList.get(0)))
                                                .findFirst();
                                    }
                                    return Optional.empty();
                                })
                                .isPresent();
                    } else if(structNamesList.size() == 1){
                        return doi.getSDIOrDAI().stream()
                                .filter(unNaming -> unNaming.getClass().equals(TDAI.class))
                                .map(TDAI.class::cast)
                                .anyMatch(dai -> dai.getName().equals(structNamesList.get(0)));
                    }
                    return false;
                })
                .orElse(false);
    }

    @Override
    public void updateOrCreateDOAndDAInstances(TAnyLN tAnyLN, DataAttributeRef dataAttributeRef) {
        createDoiSdiDaiChainIfNotExists(tAnyLN, dataAttributeRef.getDoName(), dataAttributeRef.getDaName())
                .ifPresent(tdai -> {
                    for (Map.Entry<Long, String> mapVal : dataAttributeRef.getDaName().getDaiValues().entrySet()) {
                        if (mapVal.getKey() != null && mapVal.getKey() != 0L) {
                            tdai.getVal().stream()
                                    .filter(tValElem -> tValElem.isSetSGroup() && mapVal.getKey().equals(tValElem.getSGroup()))
                                    .findFirst()
                                    .ifPresentOrElse(
                                            tVal -> tVal.setValue(mapVal.getValue()),
                                            () -> tdai.getVal().add(newVal(mapVal.getValue(), mapVal.getKey())));
                        } else {
                            tdai.getVal().stream().findFirst()
                                    .ifPresentOrElse(
                                            tVal -> tVal.setValue(mapVal.getValue()),
                                            () -> tdai.getVal().add(newVal(mapVal.getValue())));
                        }
            }
        });
    }

    @Override
    public List<SclReportItem> getDOAndDAInstances(TAnyLN tAnyLN, DataAttributeRef dataAttributeRef) {
        List<SclReportItem> sclReportItems = new ArrayList<>();
        return tAnyLN.getDOI().stream().filter(doi -> dataAttributeRef.getDoName().getName().equals(doi.getName()))
                .findFirst()
                .map(doi -> {
                    LinkedList<String> structNamesList = new LinkedList<>(dataAttributeRef.getDoName().getStructNames());
                    structNamesList.addLast(dataAttributeRef.getDaName().getName());
                    dataAttributeRef.getDaName().getStructNames().forEach(structNamesList::addLast);
                    if(structNamesList.size() > 1) {
                        String firstSDIName = structNamesList.remove();
                        return doi.getSDIOrDAI().stream()
                                .filter(sdi -> sdi.getClass().equals(TSDI.class))
                                .map(TSDI.class::cast)
                                .filter(sdi -> sdi.getName().equals(firstSDIName))
                                .findFirst()
                                .map(intermediateSdi -> findSDIByStructName(intermediateSdi, structNamesList))
                                .stream()
                                .findFirst()
                                .map(lastDsi -> {
                                    if (structNamesList.size() == 1) {
                                        lastDsi.getSDIOrDAI().stream()
                                                .filter(dai -> dai.getClass().equals(TDAI.class))
                                                .map(TDAI.class::cast)
                                                .filter(dai -> dai.getName().equals(structNamesList.get(0)))
                                                .findFirst()
                                                .ifPresentOrElse(tdai1 -> checkAndCompleteDataAttribute(tdai1, dataAttributeRef.getDaName()), ()-> {
                                                    log.warn("Missing DAI.name=({}) not found in SDI.name=({})",
                                                            structNamesList.get(0), lastDsi.getName());
                                                    sclReportItems.add(SclReportItem.error(null,
                                                            String.format("DAI.name=(%s) not found in SDI.name=(%s)",
                                                                    structNamesList.get(0), lastDsi.getName())));
                                                });
                                    } else {
                                        log.warn("Missing DataAttributes in SDI.name=({})", lastDsi.getName());
                                        sclReportItems.add(SclReportItem.error(null,
                                                String.format("missing DataAttribute in SDI.name=(%s)", lastDsi.getName())));
                                    }
                                    return sclReportItems;
                                })
                                .orElseGet(() -> {
                                    log.warn("Missing SubData Object Instance SDI or Data Attribute Instance DAI ({}) in DOI.name ({})",
                                            dataAttributeRef.getDoName().getStructNames(), doi.getName());
                                    sclReportItems.add(SclReportItem.error(null,
                                            String.format("Missing SubData Object Instance SDI or Data Attribute Instance DAI (%s) in DOI.name (%s)",
                                                    dataAttributeRef.getDoName().getStructNames(), doi.getName())));
                                    return sclReportItems;
                                });
                    } else if(structNamesList.size() == 1){
                        doi.getSDIOrDAI().stream()
                                .filter(unNaming -> unNaming.getClass().equals(TDAI.class))
                                .map(TDAI.class::cast)
                                .filter(dai -> dai.getName().equals(structNamesList.get(0)))
                                .findFirst()
                                .ifPresentOrElse(
                                        dai ->  checkAndCompleteDataAttribute(dai, dataAttributeRef.getDaName()),
                                        () -> {
                                            log.warn("Missing DAI.name=({}) not found in DOI.name=({})", structNamesList.get(0), doi.getName());
                                            sclReportItems.add(SclReportItem.error(null,
                                                    String.format("DAI.name=(%s) not found in DOI.name=(%s)", structNamesList.get(0), doi.getName())));
                                        });
                        return sclReportItems;
                    }
                    return sclReportItems;
                })
                .orElseGet(() -> {
                    log.warn("DOI.name=({}) not found in LN.type=({})", dataAttributeRef.getDoName(), tAnyLN.getLnType());
                    sclReportItems.add(SclReportItem.error(null,String.format("DOI.name=(%s) not found in LN.type=(%s)",
                            dataAttributeRef.getDoName(), tAnyLN.getLnType())));
                    return sclReportItems;
                });
    }

    private void checkAndCompleteDataAttribute(TDAI tdai, DaTypeName daTypeName) {
        daTypeName.addDaiValues(tdai.getVal());
        if (daTypeName.getFc() == TFCEnum.SG || daTypeName.getFc() == TFCEnum.SE) {
            boolean isGroup = hasSgGroup(tdai);
            if (isGroup) {
                daTypeName.setValImport((!tdai.isSetValImport() || tdai.isValImport())
                        // TODO && iedHasConfSG()
                );
            } else {
                daTypeName.setValImport(false);
                // TODO
                log.warn("Inconsistency in the SCD file - DAI ?? with fc=?? must have a sGroup attribute");
            }
        } else if (tdai.isSetValImport()) {
            daTypeName.setValImport(tdai.isValImport());
        }
    }

    private boolean hasSgGroup(TDAI tdai) {
        return tdai.getVal().stream().anyMatch(tVal -> tVal.isSetSGroup() && tVal.getSGroup() > 0);
    }

    // TODO
    public boolean iedHasConfSG(TIED tied, TLDevice tlDevice) {
        TAccessPoint accessPoint = tied.getAccessPoint().stream()
                .filter(tAccessPoint ->
                        (tAccessPoint.getServer() != null) &&
                                tAccessPoint.getServer().getLDevice().stream()
                                        .anyMatch(tlDevice1 -> tlDevice1.getInst().equals(tlDevice.getInst()))

                )
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                String.format("LD (%s) is unknown in %s", tlDevice.getInst(), tlDevice.getLdName())
                        )
                );

        TServices srv = accessPoint.getServices();
        return srv != null && srv.getSettingGroups() != null && srv.getSettingGroups().getConfSG() != null;
    }


    private Optional<TDAI> createDoiSdiDaiChainIfNotExists(TAnyLN tAnyLN, DoTypeName doTypeName, DaTypeName daTypeName) {
        LinkedList<String> structInstances = new LinkedList<>(doTypeName.getStructNames());
        structInstances.addLast(daTypeName.getName());
        daTypeName.getStructNames().forEach(structInstances::addLast);
        TDOI doi = tAnyLN.getDOI().stream().filter(doi1 -> doi1.getName().equals(doTypeName.getName()))
                .findFirst()
                .orElseGet(()-> {
                    TDOI newDOI = new TDOI();
                    newDOI.setName(doTypeName.getName());
                    tAnyLN.getDOI().add(newDOI);
                    return newDOI;
                });
        if(structInstances.size() > 1){
            TSDI firstSDI = findOrCreateSDIFromDOI(doi, structInstances.getFirst());
            TSDI lastSDI = findOrCreateSDIByStructName(firstSDI, structInstances);
            if(structInstances.size() == 1){
                TDAI newDAI = new TDAI();
                newDAI.setName(structInstances.getFirst());
                lastSDI.getSDIOrDAI().add(newDAI);
                return Optional.of(newDAI);
            }
        } else if(structInstances.size() == 1){
            TDAI newDAI = new TDAI();
            newDAI.setName(structInstances.getFirst());
            doi.getSDIOrDAI().add(newDAI);
            return Optional.of(newDAI);
        }
        return Optional.empty();
    }

    private TSDI findSDIByStructName(TSDI tsdi, LinkedList<String> sdiNames) {
        if(sdiNames.isEmpty()) return tsdi;
        return tsdi.getSDIOrDAI().stream()
                .filter(sdi -> sdi.getClass().equals(TSDI.class))
                .map(TSDI.class::cast)
                .filter(sdi -> sdi.getName().equals(sdiNames.getFirst()))
                .findFirst()
                .map(sdi1 -> {
                    sdiNames.remove();
                    return findSDIByStructName(sdi1, sdiNames);
                })
                .orElse(tsdi);
    }

    private TSDI findOrCreateSDIFromDOI(TDOI doi, String sdiName) {
        return doi.getSDIOrDAI().stream()
                .filter(sdi -> sdi.getClass().equals(TSDI.class))
                .map(TSDI.class::cast)
                .filter(tsdi -> tsdi.getName().equals(sdiName))
                .findFirst()
                .orElseGet(() -> {
                    TSDI tsdi = new TSDI();
                    tsdi.setName(sdiName);
                    doi.getSDIOrDAI().add(tsdi);
                    return tsdi;
                });
    }

    private TSDI findOrCreateSDIFromSDI(TSDI sdi, String sdiName) {
        return sdi.getSDIOrDAI().stream()
                .filter(unNaming -> unNaming.getClass().equals(TSDI.class))
                .map(TSDI.class::cast)
                .filter(tsdi -> tsdi.getName().equals(sdiName))
                .findFirst()
                .orElseGet(() -> {
                    TSDI tsdi = new TSDI();
                    tsdi.setName(sdiName);
                    sdi.getSDIOrDAI().add(tsdi);
                    return tsdi;
                });
    }

    /**
     *
     * @param sdi TSDI
     * @param structName start with doi name
     * @return already existing TSDI or newly created TSDI from given TSDI
     */
    private TSDI findOrCreateSDIByStructName(TSDI sdi, LinkedList<String> structName) {
        structName.remove();
        if(structName.isEmpty() || structName.size() == 1) return sdi;
        return findOrCreateSDIByStructName(findOrCreateSDIFromSDI(sdi, structName.getFirst()), structName);
    }



}
