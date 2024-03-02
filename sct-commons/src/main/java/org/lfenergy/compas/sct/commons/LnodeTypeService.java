// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.lfenergy.compas.scl2007b4.model.TDataTypeTemplates;
import org.lfenergy.compas.scl2007b4.model.TLNodeType;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LnodeTypeService {

    private DoTypeService doTypeService = new DoTypeService();

    public Stream<TLNodeType> getLnodeTypes(TDataTypeTemplates tDataTypeTemplates) {
        return tDataTypeTemplates.getLNodeType().stream();
    }

    public Stream<TLNodeType> getFilteredLnodeTypes(TDataTypeTemplates tDataTypeTemplates, Predicate<TLNodeType> tlNodeTypePredicate) {
        return getLnodeTypes(tDataTypeTemplates).filter(tlNodeTypePredicate);
    }

    public Optional<TLNodeType> findLnodeType(TDataTypeTemplates tDataTypeTemplates, Predicate<TLNodeType> tlNodeTypePredicate) {
        return getFilteredLnodeTypes(tDataTypeTemplates, tlNodeTypePredicate).findFirst();
    }

    public List<DataAttributeRef> getDataAttributeRefs(TDataTypeTemplates dtt,
                                                       TLNodeType tlNodeType)  {

        DataAttributeRef dataRef = new DataAttributeRef();
        dataRef.setDoName(new DoTypeName());
        dataRef.setDaName(new DaTypeName());
        return tlNodeType.getDO().stream()
                .flatMap(tdo -> {
                    dataRef.setLnType(tlNodeType.getId());
                    if(tlNodeType.isSetLnClass()) dataRef.setLnClass(tlNodeType.getLnClass().get(0));
                    return doTypeService.findDoType(dtt, tdoType -> tdoType.getId().equals(tdo.getType()))
                            .stream().flatMap(tdoType -> {
                                dataRef.getDoName().setName(tdo.getName());
                                dataRef.getDoName().setCdc(tdoType.getCdc());
                                return doTypeService.getDataAttributeRefs(dtt, tdoType, dataRef).stream();
                            });
                })
                .toList();
    }
}
