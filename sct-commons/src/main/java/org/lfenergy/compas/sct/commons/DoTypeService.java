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
        for (TUnNaming tUnNaming : tdoType.getSDOOrDA()) {
            // clear struct data objects before processing each unNaming object
            dataRef.getDoName().getStructNames().clear();
            dataRef.getDaName().setType(null);
            if (tUnNaming.getClass() == TDA.class) {
                TDA tda = (TDA) tUnNaming;
                dataRef.getDaName().setName(tda.getName());
                if(tda.isSetFc()) dataRef.getDaName().setFc(tda.getFc());
                if(tda.isSetType()) dataRef.getDaName().setType(tda.getType());
                if(tda.isSetBType()) dataRef.getDaName().setBType(tda.getBType());
                if(tda.isSetValImport()) dataRef.getDaName().setValImport(tda.isValImport());

                // STRUCT type (BType=STRUCT) refer to BDA, otherwise it is DA
                if(tda.isSetType() && tda.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)) {
                    DataAttributeRef newDataObjectRef = DataAttributeRef.copyFrom(dataRef);
                    newDataObjectRef.getDaName().getStructNames().add(tda.getName());
                    daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(tda.getType()))
                            .ifPresent(nextDaType -> result.addAll(getDataAttributesFromBDA(dtt, nextDaType, newDataObjectRef)));

                } else {
                    DataAttributeRef newDataObjectRef = DataAttributeRef.copyFrom(dataRef);
                    result.add(newDataObjectRef);
                }
            } else {
                TSDO tsdo = (TSDO) tUnNaming;
                findDoType(dtt, tdoType1 -> tdoType1.getId().equals(tsdo.getType()))
                        .ifPresent(nextDoType -> {
                            DataAttributeRef newDataObjectRef = DataAttributeRef.copyFrom(dataRef);
                            newDataObjectRef.getDoName().getStructNames().add(tsdo.getName());
                            if(nextDoType.isSetCdc()) newDataObjectRef.getDoName().setCdc(nextDoType.getCdc());
                            result.addAll(getDataAttributeRefsFromSDO(dtt, nextDoType, newDataObjectRef));
                        });
            }
        }
        return result;
    }

    private List<DataAttributeRef> getDataAttributeRefsFromSDO(TDataTypeTemplates dtt, TDOType tdoType, DataAttributeRef dataObjectRef) {
        List<DataAttributeRef> result = new ArrayList<>();
        // DA -> BDA -> BDA..
        tdoType.getSDOOrDA().stream().filter(tUnNaming -> tUnNaming.getClass().equals(TDA.class)).map(TDA.class::cast).toList()
                .forEach(subDa -> {
                    dataObjectRef.getDaName().setName(subDa.getName());
                    if(subDa.isSetFc()) dataObjectRef.getDaName().setFc(subDa.getFc());
                    if(subDa.isSetType()) dataObjectRef.getDaName().setType(subDa.getType());
                    if(subDa.isSetBType()) dataObjectRef.getDaName().setBType(subDa.getBType());
                    if(subDa.isSetValImport()) dataObjectRef.getDaName().setValImport(subDa.isValImport());
                    DataAttributeRef newDataAttributeRef = DataAttributeRef.copyFrom(dataObjectRef);

                    // STRUCT type (BType=STRUCT) refer to BDA, otherwise it is DA
                    if(subDa.isSetType() && subDa.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)){
                        daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(subDa.getType()))
                                .ifPresent(nextDaType -> result.addAll(getDataAttributesFromBDA(dtt, nextDaType, newDataAttributeRef)));
                    } else
                        result.add(newDataAttributeRef);
                });
        // SDO -> SDO -> SDO..
        tdoType.getSDOOrDA().stream().filter(tUnNaming -> tUnNaming.getClass().equals(TSDO.class)).map(TSDO.class::cast)
                .forEach(subSdo -> {
                    if(subSdo.isSetType()){
                        DataAttributeRef newDataAttributeRef = DataAttributeRef.copyFrom(dataObjectRef);
                        newDataAttributeRef.getDoName().getStructNames().add(subSdo.getName());
                        findDoType(dtt, tdoType1 -> tdoType1.getId().equals(subSdo.getType()))
                                .ifPresent(nextDoType -> {
                                    if(nextDoType.isSetCdc()) newDataAttributeRef.getDoName().setCdc(nextDoType.getCdc());
                                    result.addAll(getDataAttributeRefsFromSDO(dtt, nextDoType, newDataAttributeRef));
                                });
                    }
                });
        return result;
    }

    private List<DataAttributeRef> getDataAttributesFromBDA(TDataTypeTemplates dtt, TDAType tdaType1, DataAttributeRef dataAttributeRef) {
        List<DataAttributeRef> result = new ArrayList<>();
        // BDA -> BDA -> BDA..
        tdaType1.getBDA().forEach(bda -> {
            dataAttributeRef.getDaName().setType(null);
            if(bda.isSetType()) dataAttributeRef.getDaName().setType(bda.getType());
            if(bda.isSetBType()) dataAttributeRef.getDaName().setBType(bda.getBType());
            if(bda.isSetValImport()) dataAttributeRef.getDaName().setValImport(bda.isValImport());

            // STRUCT type (BType=STRUCT) refer to complex BDA object, otherwise it is kind of DA object
            if(bda.isSetType() && bda.isSetType() && bda.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)){
                DataAttributeRef newDataAttributeRef = DataAttributeRef.copyFrom(dataAttributeRef);
                newDataAttributeRef.getDaName().getStructNames().add(bda.getName());
                daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(bda.getType()))
                        .ifPresent(nextDaType -> result.addAll(getDataAttributesFromBDA(dtt, nextDaType, newDataAttributeRef)));
            } else {
                dataAttributeRef.getDaName().getStructNames().add(bda.getName());
                result.add(dataAttributeRef);
            }
        });
        return result;
    }
}
