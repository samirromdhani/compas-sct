// SPDX-FileCopyrightText: 2021 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.scl;

import org.lfenergy.compas.scl2007b4.model.SCL;
import org.lfenergy.compas.sct.commons.LdeviceService;
import org.lfenergy.compas.sct.commons.LnodeTypeService;
import org.lfenergy.compas.sct.commons.exception.ScdException;
import org.lfenergy.compas.sct.commons.scl.ln.LnKey;

import java.util.stream.Stream;


public class ObjectReferenceService {

    private final LdeviceService ldeviceService = new LdeviceService();
    private final LnodeTypeService lnodeTypeService = new LnodeTypeService();

    public boolean isValidObjRefValue(SCL scl, String iedName, String objRefValue) throws ScdException {
        ObjectReference objRef = new ObjectReference(objRefValue);
       return ldeviceService.findFilteredLDevice(scl, tlDevice ->
                       tlDevice.isSetLdName() && tlDevice.getLdName().equals(objRef.getLdName())
                               || tlDevice.isSetInst() && objRef.getLdName().equals(iedName+tlDevice.getInst()))
               .anyMatch(tlDevice -> Stream.concat(tlDevice.getLN().stream(), Stream.of(tlDevice.getLN0()))
                               .filter(anyLN -> new LnKey(anyLN).getLNodeName().equals(objRef.getLNodeName()))
                               .anyMatch(anyLN -> lnodeTypeService.getFilteredLnodeTypes(scl.getDataTypeTemplates(), tlNodeType ->
                                         tlNodeType.getId().equals(LnKey.createDataRef(anyLN).getLnType()))
                                       .anyMatch(tlNodeType -> {
                                           String dataAttribute = objRef.getDataAttributes();
                                           return anyLN.getDataSet().stream().anyMatch(tDataSet -> tDataSet.getName().equals(dataAttribute))
                                                   || anyLN.getReportControl().stream().anyMatch(rptCtl -> rptCtl.getName().equals(dataAttribute))
                                                   || lnodeTypeService.getDataAttributes(scl.getDataTypeTemplates(), tlNodeType, LnKey.createDataRef(anyLN))
                                                   .anyMatch(dataObjAndAttribute ->
                                                           dataObjAndAttribute.getDataAttributes().startsWith(dataAttribute));
                                       }))
               );
    }
}
