// SPDX-FileCopyrightText: 2023 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DataTypeName;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DoTypeService {

    final SDOOrDAService sdoOrDAService = new SDOOrDAService();
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

    public List<DataAttributeRef> getDataAttributeRefs(TDataTypeTemplates dtt,
                                                       TDOType tdoType,
                                                       DataAttributeRef dataRef) {
        List<DataAttributeRef> result = new ArrayList<>();
        for (TUnNaming tUnNaming : tdoType.getSDOOrDA()) {
            dataRef.getDoName().getStructNames().clear();
            if (tUnNaming.getClass() == TDA.class) {
                TDA tda = (TDA) tUnNaming;
                dataRef.getDaName().setName(tda.getName());
                if(tda.isSetType()) dataRef.getDaName().setType(tda.getType());
                if(tda.isSetBType()) dataRef.getDaName().setBType(tda.getBType());
                DataAttributeRef currentDataRef = new DataAttributeRef();
                currentDataRef.setDoName(dataRef.getDoName());
                currentDataRef.setDaName(dataRef.getDaName());
                if(tda.isSetType() && tda.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)) {
                    daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(tda.getType()))
                            .ifPresent(tdaType1 -> result.addAll(getDataAttributeRefsRECBDAAndComplete(dtt, tdaType1, currentDataRef)));

                } else {
                    result.add(currentDataRef);
                }
            } else {
                TSDO tsdo = (TSDO) tUnNaming;
                dataRef.getDoName().getStructNames().add(tsdo.getName());
                DataAttributeRef currentDataRef = new DataAttributeRef();
                currentDataRef.setDoName(dataRef.getDoName());
                currentDataRef.setDaName(dataRef.getDaName());
                List<DataAttributeRef> dataRefsSDO = findDoType(dtt, tdoType1 -> tdoType1.getId().equals(tsdo.getType()))
                        .stream()
                        .flatMap(tdoType2 -> getDataAttributeRefsRECSDOAndComplete(dtt, tdoType2, currentDataRef).stream())
                        .toList();
                result.addAll(dataRefsSDO);
            }
        }
        return result;
    }

    private List<DataAttributeRef>  getDataAttributeRefsRECSDOAndComplete(TDataTypeTemplates dtt, TDOType tdoType, DataAttributeRef deepDataRef) {
        List<DataAttributeRef> result = new ArrayList<>();

        // DA/BDA
        List<TDA> tdas = tdoType.getSDOOrDA().stream().filter(tUnNaming -> tUnNaming.getClass().equals(TDA.class))
                .map(TDA.class::cast).toList();
        tdas.forEach(subDa -> {
            if(subDa.isSetType() && subDa.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)){
                daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(subDa.getType()))
                        .ifPresent(tdaType1 -> {
                            DataAttributeRef currentDataRef = new DataAttributeRef();
                            currentDataRef.setDoName(deepDataRef.getDoName());
                            currentDataRef.setDaName(deepDataRef.getDaName());
                            currentDataRef.getDaName().setName(subDa.getName());
                            result.addAll(getDataAttributeRefsRECBDAAndComplete(dtt, tdaType1, currentDataRef));
                        });
            } else {
                deepDataRef.getDaName().setName(subDa.getName());
                result.add(deepDataRef);
            }
        });
        // DO/SDO
        List<TSDO> tsdos = tdoType.getSDOOrDA().stream().filter(tUnNaming -> tUnNaming.getClass().equals(TSDO.class))
                .map(TSDO.class::cast).toList();
        tsdos.forEach(subSdo -> {
            deepDataRef.getDoName().getStructNames().add(subSdo.getName());
            if(subSdo.isSetType()){
                findDoType(dtt, tdoType1 -> tdoType1.getId().equals(subSdo.getType()))
                        .ifPresent(tdoType2 -> getDataAttributeRefsRECSDOAndComplete(dtt, tdoType2, deepDataRef));
            }
        });
        return result;
    }

    private List<DataAttributeRef> getDataAttributeRefsRECBDAAndComplete(TDataTypeTemplates dtt, TDAType tdaType1,
                                                                         DataAttributeRef deepDaRef) {
        List<DataAttributeRef> result = new ArrayList<>();
        tdaType1.getBDA().forEach(bda -> {
            if(bda.isSetType()) deepDaRef.getDaName().setType(bda.getType());
            if(bda.getBType().equals(TPredefinedBasicTypeEnum.STRUCT)){
                DataAttributeRef currentDataRef = new DataAttributeRef();
                currentDataRef.setDoName(deepDaRef.getDoName());
                currentDataRef.setDaName(deepDaRef.getDaName());
                currentDataRef.getDaName().getStructNames().add(bda.getName());
                daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(bda.getType()))
                        .ifPresent(tdaType2 -> result.addAll(getDataAttributeRefsRECBDAAndComplete(dtt, tdaType2, currentDataRef)));
            } else {
                DataAttributeRef currentDataRef = new DataAttributeRef();
                currentDataRef.setDoName(deepDaRef.getDoName());
                currentDataRef.setDaName(deepDaRef.getDaName());
                currentDataRef.getDaName().getStructNames().add(bda.getName());
                result.add(currentDataRef);
            }
        });
        return result;
    }


}
