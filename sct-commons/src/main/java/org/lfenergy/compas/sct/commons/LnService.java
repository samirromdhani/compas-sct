// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.api.LNEditor;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;
import org.lfenergy.compas.sct.commons.util.ActiveStatus;

import java.util.*;
import java.util.stream.Stream;

import static org.lfenergy.compas.sct.commons.util.CommonConstants.MOD_DO_NAME;
import static org.lfenergy.compas.sct.commons.util.CommonConstants.STVAL_DA_NAME;
import static org.lfenergy.compas.sct.commons.util.SclConstructorHelper.newVal;

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

    public boolean isDoObjectsInstanceAndDataAttributesInstanceExists(TAnyLN tAnyLN, DoTypeName doTypeName, DaTypeName daTypeName) {
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

    public void updateOrCreateDoObjectsAndDataAttributesInstances(TAnyLN tAnyLN, DataAttributeRef dataAttributeRef) {
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

    private TSDI findSDIByStructName(TSDI tsdi, List<String> structNames) {
        if(structNames.isEmpty()) return tsdi;
        return tsdi.getSDIOrDAI().stream()
                .filter(sdi -> sdi.getClass().equals(TSDI.class))
                .map(TSDI.class::cast)
                .filter(sdi -> sdi.getName().equals(structNames.get(0)))
                .findFirst()
                .map(sdi1 -> {
                    structNames.remove(0);
                    return findSDIByStructName(sdi1, structNames);
                })
                .orElse(tsdi);
    }

    private TSDI findOrCreateSDIByStructName(TSDI tsdi, List<String> structNames) {
        structNames.remove(0);
        if(structNames.isEmpty() || structNames.size() == 1) return tsdi;
        return findOrCreateSDIByStructName(findOrCreateSDIFromSDI(tsdi, structNames.get(0)), structNames);
    }

    private Optional<TDAI> createDoiSdiDaiChainIfNotExists(TAnyLN tAnyLN, DoTypeName doTypeName, DaTypeName daTypeName) {
        LinkedList<String> structNamesList = new LinkedList<>();
        structNamesList.addLast(doTypeName.getName());
        doTypeName.getStructNames().forEach(structNamesList::addLast);
        structNamesList.addLast(daTypeName.getName());
        daTypeName.getStructNames().forEach(structNamesList::addLast);

        String doiName = structNamesList.remove();
        TDOI doi = tAnyLN.getDOI().stream().filter(tdoi -> tdoi.getName().equals(doiName))
                .findFirst()
                .orElseGet(() -> {
                    TDOI tdoi = new TDOI();
                    tdoi.setName(doiName);
                    tAnyLN.getDOI().add(tdoi);
                    return tdoi;
                });
        if(structNamesList.size() > 1) {
            TSDI lastDsi = findOrCreateSDIByStructName(findOrCreateSDIFromDOI(doi, structNamesList.get(0)), structNamesList);
            if (structNamesList.size() == 1) {
                return Optional.of(findOrCreateDAIFromSDI(lastDsi, structNamesList.get(0)));
            }
        }
        else if(structNamesList.size() == 1){
            return Optional.of(doi.getSDIOrDAI().stream()
                    .filter(dai -> dai.getClass().equals(TDAI.class))
                    .map(TDAI.class::cast)
                    .filter(tdai -> tdai.getName().equals(structNamesList.get(0)))
                    .findFirst()
                    .orElseGet(() -> {
                        TDAI tdai = new TDAI();
                        tdai.setName(structNamesList.get(0));
                        doi.getSDIOrDAI().add(tdai);
                        return tdai;
                    }));
        }
        return Optional.empty();
    }

    private TDAI findOrCreateDAIFromSDI(TSDI sdi, String daiName) {
        return sdi.getSDIOrDAI().stream()
                .filter(unNaming -> unNaming.getClass().equals(TDAI.class))
                .map(TDAI.class::cast)
                .filter(tdai -> tdai.getName().equals(daiName))
                .findFirst()
                .orElseGet(() -> {
                    TDAI tdai = new TDAI();
                    tdai.setName(daiName);
                    sdi.getSDIOrDAI().add(tdai);
                    return tdai;
                });
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

}
