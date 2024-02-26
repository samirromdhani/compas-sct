// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;
import org.lfenergy.compas.sct.commons.exception.ScdException;
import org.lfenergy.compas.sct.commons.util.ActiveStatus;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.lfenergy.compas.sct.commons.util.CommonConstants.MOD_DO_NAME;
import static org.lfenergy.compas.sct.commons.util.CommonConstants.STVAL_DA_NAME;

public class LnService {

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

    private Optional<TDOI> findDOI(TAnyLN tAnyLN, Predicate<TDOI> doiPredicate) {
        return tAnyLN.getDOI().stream().filter(doiPredicate)
                .findFirst();
    }

    private Optional<TDAI> findDAI(TDOI doi, Predicate<TDAI> daiPredicate) {
        return doi.getSDIOrDAI().stream()
                .filter(unNaming -> unNaming.getClass().equals(TDAI.class))
                .map(TDAI.class::cast)
                .filter(daiPredicate)
                .findFirst();
    }

    private Optional<TSDI> findSDI(TDOI doi, String sdiName) {
        return doi.getSDIOrDAI().stream()
                .filter(unNaming -> unNaming.getClass().equals(TSDI.class))
                .map(TSDI.class::cast)
                .filter(tsdi -> tsdi.getName().equals(sdiName))
                .findFirst();
    }

    private Optional<TSDI> findSDI(TSDI sdi, String sdiName) {
        return sdi.getSDIOrDAI().stream()
                .filter(unNaming -> unNaming.getClass().equals(TSDI.class))
                .map(TSDI.class::cast)
                .filter(tsdi -> tsdi.getName().equals(sdiName))
                .findFirst();
    }

    private Optional<TDAI> findDAI(TSDI sdi, Predicate<TDAI> daiPredicate) {
        return sdi.getSDIOrDAI().stream()
                .filter(unNaming -> unNaming.getClass().equals(TDAI.class))
                .map(TDAI.class::cast)
                .filter(daiPredicate)
                .findFirst();
    }


    public Boolean getDAI(TAnyLN tAnyLN, DataAttributeRef dataAttributeRef) {
        LinkedList<String> dataRefList = new LinkedList<>(Arrays.asList(
                dataAttributeRef.getDoName().toStringWithoutInst().split("\\.")));
        String doiName = dataRefList.remove();
        List<String> sdiAccumulator = new ArrayList<>();
        Optional<TDOI> optDoi = findDOI(tAnyLN, tdoi -> doiName.equals(tdoi.getName()));
        if(optDoi.isPresent()){
            System.out.println(":: DOI FOUND :: "+optDoi.get().getName());
            String sdiOrDaiName = dataRefList.remove();
            findSDI(optDoi.get(), sdiOrDaiName)
                    .map(tsdi -> {
                        System.out.println(":: FIRST SDI FOUND :: "+tsdi.getName());
                        return dataRefList.stream().reduce(tsdi, (lastSdi, name) -> {
                            Optional<TSDI> optSdi = findSDI(tsdi, name);
                            if (optSdi.isPresent()) {
                                System.out.println(":: SECOND SDI FOUND :: "+optSdi.get().getName());
                                sdiAccumulator.add(optSdi.get().getName());
                                return optSdi.get();
                            } else {
                                Optional<TDAI> optDai = findDAI(lastSdi, tdai -> tdai.getName().equals(name));
                                if(optDai.isPresent()) {
                                    System.out.println(":: DAI FOUND :: "+optDai.get().getName());
                                }
                            }
                            return lastSdi;
                        }, (doi1, doi2) -> {
                            throw new ScdException("This reduction cannot be parallel");
                        });
                    });
        }
        return false;
    }

}
