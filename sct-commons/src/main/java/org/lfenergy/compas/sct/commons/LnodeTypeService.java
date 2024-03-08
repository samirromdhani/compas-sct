// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.lfenergy.compas.scl2007b4.model.TDO;
import org.lfenergy.compas.scl2007b4.model.TDataTypeTemplates;
import org.lfenergy.compas.scl2007b4.model.TLNodeType;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.scl.dtt.DataTypeTemplateAdapter;
import org.lfenergy.compas.sct.commons.scl.dtt.LNodeTypeAdapter;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LnodeTypeService {

    private final DoTypeService doTypeService = new DoTypeService();

    public Stream<TLNodeType> getLnodeTypes(TDataTypeTemplates tDataTypeTemplates) {
        return tDataTypeTemplates.getLNodeType().stream();
    }

    public Stream<TLNodeType> getFilteredLnodeTypes(TDataTypeTemplates tDataTypeTemplates, Predicate<TLNodeType> tlNodeTypePredicate) {
        return getLnodeTypes(tDataTypeTemplates).filter(tlNodeTypePredicate);
    }

    public Optional<TLNodeType> findLnodeType(TDataTypeTemplates tDataTypeTemplates, Predicate<TLNodeType> tlNodeTypePredicate) {
        return getFilteredLnodeTypes(tDataTypeTemplates, tlNodeTypePredicate).findFirst();
    }

    public Stream<DataAttributeRef> getAllDOAndDA(TDataTypeTemplates dtt, TLNodeType tlNodeType, DataAttributeRef dataRef) {
        return tlNodeType.getDO().stream()
                .flatMap(tdo -> {
                    dataRef.setLnType(tlNodeType.getId());
                    if (tlNodeType.isSetLnClass() && !tlNodeType.getLnClass().isEmpty())
                        dataRef.setLnClass(tlNodeType.getLnClass().get(0));
                    dataRef.getDoName().setName(tdo.getName());
                    return doTypeService.findDoType(dtt, tdoType -> tdoType.getId().equals(tdo.getType()))
                            .stream().flatMap(tdoType -> {
                                dataRef.getDoName().setCdc(tdoType.getCdc());
                                return doTypeService.getAllSDOAndDA(dtt, tdoType, dataRef).stream();
                            });
                });
    }


    public Stream<DataAttributeRef> getFilteredDOAndDA(TDataTypeTemplates dtt, DataAttributeRef dataRef) {
        return getFilteredLnodeTypes(dtt, tlNodeType -> tlNodeType.getId().equals(dataRef.getLnType()))
                .flatMap(tlNodeType -> tlNodeType.getDO().stream()
                        .flatMap(tdo -> {
                            dataRef.getDoName().setName(tdo.getName());
                            return doTypeService.findDoType(dtt, tdoType -> tdoType.getId().equals(tdo.getType()))
                                    .stream().flatMap(tdoType -> {
                                        dataRef.getDoName().setCdc(tdoType.getCdc());
                                        return doTypeService.getAllSDOAndDA(dtt, tdoType, dataRef).stream();
                                    });
                        }));

    }

    protected Map<String, String> importLNodeType(String iedName, TDataTypeTemplates dtt) {
        Map<String, String> pairOldAndNewId = new HashMap<>();
        getLnodeTypes(dtt)
                .forEach(prvLNodeType -> {
                    String oldId = prvLNodeType.getId();
                    String newId = prvLNodeType.getId();
                    Optional<TLNodeType> opRcvLNodeType = findLnodeType(dtt, tlNodeType -> tlNodeType.getId().equals(oldId));
                    boolean isImportable = opRcvLNodeType.isEmpty() || !hasSameContentAs(opRcvLNodeType.get());
                    if (isImportable && opRcvLNodeType.isPresent()) {
                        // same ID, different content
                        // rename enumType Id
                        newId = generateDttId(iedName,prvLNodeType.getId());
                        prvLNodeType.setId(newId);
                    }
                    if (isImportable) {
                        //import this LNodeType
                        dtt.getLNodeType().add(prvLNodeType);
                        if (!Objects.equals(oldId, newId)) {
                            pairOldAndNewId.put(oldId, newId);
                        }
                    }
                });
        return pairOldAndNewId;
    }

    protected String generateDttId(String iedName,String dttId){
        final int MAX_LENGTH = 255;
        String str = iedName + "_" + dttId;
        return str.length() <= MAX_LENGTH ? str : str.substring(0,MAX_LENGTH);
    }

    public boolean hasSameContentAs(TLNodeType tlNodeType) {

//        if (!DataTypeTemplateAdapter.hasSamePrivates(currentElem, tlNodeType)) {
//            return false;
//        }
//
//        if (Objects.equals(
//                currentElem.getLnClass().toArray(new String[0]),
//                tlNodeType.getLnClass().toArray(new String[0])
//        ) || !Objects.equals(currentElem.getIedType(), tlNodeType.getIedType())) {
//            return false;
//        }
//
//        List<TDO> thisTDOs = currentElem.getDO();
//        List<TDO> inTDOs = tlNodeType.getDO();
//        if (thisTDOs.size() != inTDOs.size()) {
//            return false;
//        }
//        for (int i = 0; i < inTDOs.size(); i++) {
//            // the order in which DOs appears matter
//            TDO inTDO = inTDOs.get(i);
//            TDO thisTDO = thisTDOs.get(i);
//            if (!thisTDO.getType().equals(inTDO.getType())
//                    || !thisTDO.getName().equals(inTDO.getName())
//                    || thisTDO.isTransient() != inTDO.isTransient()
//                    || !Objects.equals(thisTDO.getAccessControl(), inTDO.getAccessControl())) {
//                return false;
//            }
//        }
        return true;
    }





}
