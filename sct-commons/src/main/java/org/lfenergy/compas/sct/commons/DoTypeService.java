// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DoTypeService {

    final DaTypeService daTypeService =  new DaTypeService();

    public Stream<TDOType> getDoTypes(TDataTypeTemplates tDataTypeTemplates) {
        return tDataTypeTemplates.getDOType().stream();
    }

    public Stream<TDOType> getFilteredDoTypes(TDataTypeTemplates tDataTypeTemplates, Predicate<TDOType> tdoTypePredicate) {
        return getDoTypes(tDataTypeTemplates).filter(tdoTypePredicate);
    }

    public Optional<TDOType> findDoType(TDataTypeTemplates tDataTypeTemplates, Predicate<TDOType> tdoTypePredicate) {
        return getFilteredDoTypes(tDataTypeTemplates, tdoTypePredicate).findFirst();
    }

    public List<DataAttributeRef> getDataAttributeRefs(TDataTypeTemplates dtt, TDOType tdoType, DataAttributeRef dataRef) {
        List<DataAttributeRef> result = new ArrayList<>();
        // DA -> BDA -> BDA -> BDA..
        tdoType.getSDOOrDA().stream().filter(tUnNaming -> tUnNaming.getClass().equals(TDA.class)).map(TDA.class::cast).toList()
                .forEach(tda -> {
                    DataAttributeRef newDataObjectRef = DataAttributeRef.copyFrom(dataRef);
                    newDataObjectRef.getDaName().setName(tda.getName());
                    if(tda.isSetFc()) newDataObjectRef.getDaName().setFc(tda.getFc());
                    if(tda.isSetValImport()) newDataObjectRef.getDaName().setValImport(tda.isValImport());

                    // STRUCT type (BType=STRUCT) refer to BDA, otherwise it is DA
                    if(tda.isSetBType() && tda.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)) {
                        daTypeService.findDaType(dtt, tdaType -> tda.isSetType() && tdaType.getId().equals(tda.getType()))
                                .ifPresent(nextDaType -> result.addAll(getDataAttributesFromBDA(dtt, nextDaType, newDataObjectRef)));
                    } else {
                        if(tda.isSetType()) newDataObjectRef.getDaName().setType(tda.getType());
                        if(tda.isSetBType()) newDataObjectRef.getDaName().setBType(tda.getBType());

                        result.add(newDataObjectRef);
                    }
                });
        // SDO -> SDO -> SDO..
        tdoType.getSDOOrDA().stream().filter(tUnNaming -> tUnNaming.getClass().equals(TSDO.class)).map(TSDO.class::cast)
                .forEach(tsdo -> findDoType(dtt, tdoType1 -> tsdo.isSetType() && tdoType1.getId().equals(tsdo.getType()))
                        .ifPresent(nextDoType -> {
                            DataAttributeRef newDataAttributeRef = DataAttributeRef.copyFrom(dataRef);
                            newDataAttributeRef.getDoName().getStructNames().add(tsdo.getName());
                            if(nextDoType.isSetCdc()) newDataAttributeRef.getDoName().setCdc(nextDoType.getCdc());

                            result.addAll(getDataAttributeRefs(dtt, nextDoType, newDataAttributeRef));
                        }));
        return result;
    }

    private List<DataAttributeRef> getDataAttributesFromBDA(TDataTypeTemplates dtt, TDAType tdaType1, DataAttributeRef dataAttributeRef) {
        List<DataAttributeRef> result = new ArrayList<>();
        // BDA -> BDA -> BDA..
        tdaType1.getBDA().forEach(bda -> {
            DataAttributeRef newDataAttributeRef = DataAttributeRef.copyFrom(dataAttributeRef);
            newDataAttributeRef.getDaName().getStructNames().add(bda.getName());
            if(bda.isSetValImport()) newDataAttributeRef.getDaName().setValImport(bda.isValImport());

            // STRUCT type (BType=STRUCT) refer to complex BDA object, otherwise it is kind of DA object
            if(bda.isSetType() && bda.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)){
                daTypeService.findDaType(dtt, tdaType -> bda.isSetType() && tdaType.getId().equals(bda.getType()))
                        .ifPresent(nextDaType -> result.addAll(getDataAttributesFromBDA(dtt, nextDaType, newDataAttributeRef)));
            } else {
                if(bda.isSetType()) newDataAttributeRef.getDaName().setType(bda.getType());
                if(bda.isSetBType()) newDataAttributeRef.getDaName().setBType(bda.getBType());

                result.add(newDataAttributeRef);
            }
        });
        return result;
    }
}
