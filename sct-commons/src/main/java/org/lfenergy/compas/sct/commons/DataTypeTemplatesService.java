// SPDX-FileCopyrightText: 2024 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons;

import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DaTypeName;
import org.lfenergy.compas.sct.commons.dto.DataAttributeRef;
import org.lfenergy.compas.sct.commons.dto.DoTypeName;
import org.lfenergy.compas.sct.commons.dto.SclReportItem;
import org.lfenergy.compas.sct.commons.exception.ScdException;

import java.util.*;
import java.util.function.Predicate;

import static org.lfenergy.compas.sct.commons.util.CommonConstants.MOD_DO_NAME;
import static org.lfenergy.compas.sct.commons.util.CommonConstants.STVAL_DA_NAME;

public class DataTypeTemplatesService {

    final LnodeTypeService lnodeTypeService = new LnodeTypeService();
    final DoTypeService doTypeService = new DoTypeService();
    final DaTypeService daTypeService = new DaTypeService();
    final DoService doService = new DoService();

    private Optional<TDOType> getDOTypeBySdoName(TDataTypeTemplates dtt, TDOType tdoType, String sdoName) {
        return tdoType.getSDOOrDA().stream()
                .filter(unNaming -> unNaming.getClass().equals(TSDO.class))
                .map(TSDO.class::cast)
                .filter(sdo -> sdo.getName().equals(sdoName))
                .map(tsdo1 -> doTypeService.findDoType(dtt, tdoType1 -> tdoType1.getId().equals(tsdo1.getType()))
                        .orElseThrow())
                .findFirst();
    }


    private Optional<TDAType> getDATypeByDaName(TDataTypeTemplates dtt, TDOType tdoType, String daName) {
        return tdoType.getSDOOrDA().stream()
                .filter(unNaming -> unNaming.getClass().equals(TDA.class))
                .map(TDA.class::cast)
                .filter(da -> da.getName().equals(daName))
                .map(tda -> {
                    if(tda.getBType() == TPredefinedBasicTypeEnum.STRUCT) {
                        return daTypeService.findDaType(dtt, tdaType -> tdaType.getId().equals(tda.getType()))
                                .orElseThrow(() -> new ScdException("daName = "+daName + " not exist in datype id = "+tdoType.getId()));
                    } else {
                        return new TDAType();
                    }
                })
                .findFirst();
    }

    private Optional<TDAType> getDATypeByBdaName(TDataTypeTemplates dtt, TDAType tdaType, String bdaName) {
        if(tdaType == null) return Optional.empty();
        return tdaType.getBDA().stream()
                .filter(unNaming -> unNaming.getClass().equals(TBDA.class))
                .map(TBDA.class::cast)
                .filter(bda -> bda.getName().equals(bdaName))
                .map(tbda ->  {
                    if(tbda.getBType() == TPredefinedBasicTypeEnum.STRUCT) {
                        return daTypeService.findDaType(dtt, tdaType2 -> tdaType2.getId().equals(tbda.getType()))
                                .orElseThrow(() -> new ScdException("bdaName = "+tbda + " not exist in datype id = "+tdaType.getId()));
                    }
                    return tdaType;
                })
                .findFirst();
    }

    private Optional<TDA> findDa(TDOType tdoType, Predicate<TDA> daPredicate) {
        return tdoType.getSDOOrDA().stream()
                .filter(unNaming -> unNaming.getClass().equals(TDA.class))
                .map(TDA.class::cast)
                .filter(daPredicate)
                .findFirst();
    }


    private Optional<TBDA> findBda(TDAType tdaType, Predicate<TBDA> bdaPredicate) {
        return tdaType.getBDA().stream()
                .filter(bdaPredicate)
                .findFirst();
    }

    /**
     * verify if DO(name=Mod)/DA(name=stVal) exists in DataTypeTemplate
     * @param dtt TDataTypeTemplates where Data object and Data attribute exists
     * @param lNodeTypeId LNode Type ID where Data object exists
     *  DataTypeTemplates model :
     * <DataTypeTemplates>
     *     <LNodeType lnClass="LNodeTypeClass" id="LNodeTypeID">
     *         <DO name="Mod" type="DOModTypeID" ../>
     *     </LNodeType>
     *     ...
     *     <DOType cdc="DOTypeCDC" id="DOModTypeID">
     *         <DA name="stVal" ../>
     *     </DOType>
     * </DataTypeTemplates>
     * @return true if the Data Object (Mod) and Data attribute (stVal) present, false otherwise
     */
    public boolean isDoModAndDaStValExist(TDataTypeTemplates dtt, String lNodeTypeId) {
        return lnodeTypeService.findLnodeType(dtt, lNodeType -> lNodeType.getId().equals(lNodeTypeId))
                .map(lNodeType -> doService.findDo(lNodeType, tdo -> tdo.getName().equals(MOD_DO_NAME))
                        .map(tdo -> doTypeService.findDoType(dtt, doType -> doType.getId().equals(tdo.getType()))
                                .map(doType -> doType.getSDOOrDA().stream()
                                        .filter(unNaming -> unNaming.getClass().equals(TDA.class))
                                        .map(TDA.class::cast)
                                        .anyMatch(tda -> tda.getName().equals(STVAL_DA_NAME)))
                                .orElse(false))
                        .orElse(false))
                .orElse(false);
    }

    public List<SclReportItem> isDoObjectsAndDataAttributesExists(TDataTypeTemplates dtt, String lNodeTypeId, String dataRef) {
        List<SclReportItem> sclReportItems = new ArrayList<>();
        LinkedList<String> dataRefList = new LinkedList<>(Arrays.asList(dataRef.split("\\.")));
        if (dataRefList.size() < 2) {
            sclReportItems.add(SclReportItem.error("", "Invalid data reference %s. At least DO name and DA name are required".formatted(dataRef)));
        }
        String doName = dataRefList.remove();
        return lnodeTypeService.findLnodeType(dtt, lNodeType -> lNodeTypeId.equals(lNodeType.getId()))
                .map(lNodeType -> doService.findDo(lNodeType, tdo -> tdo.getName().equals(doName))
                        .map(tdo -> doTypeService.findDoType(dtt, doType -> doType.getId().equals(tdo.getType()))
                                .map(tdoType -> {
                                    List<String> sdoAccumulator = new ArrayList<>();
                                    dataRefList.stream()
                                            .reduce(tdoType, (lastDoType, name) -> {
                                                Optional<TDOType> optSdo = getDOTypeBySdoName(dtt, lastDoType, name);
                                                if (optSdo.isPresent()) {
                                                    sdoAccumulator.add(optSdo.get().getId());
                                                    return optSdo.get();
                                                } else {
                                                    LinkedList<String> daRefList = (LinkedList<String>) dataRefList.clone();
                                                    getDATypeByDaName(dtt, lastDoType, name)
                                                            .ifPresentOrElse(tdaType -> daRefList.stream().skip(sdoAccumulator.size() + 1L)
                                                                            .reduce(tdaType, (lastDaType, bdaOrDaName) -> {
                                                                                Optional<TDAType> optBda = getDATypeByBdaName(dtt, lastDaType, bdaOrDaName);
                                                                                if (lastDaType != null && optBda.isPresent()) {
                                                                                    return optBda.get();
                                                                                } else if(lastDaType != null) {
                                                                                    sclReportItems.add(SclReportItem.warning("", String.format("Unknown BDA.name (%s) in DAType.id (%s)", bdaOrDaName, lastDaType.getId())));
                                                                                }
                                                                                return null;
                                                                            }, (daType1, daType2) -> {
                                                                                throw new ScdException("This reduction cannot be parallel");
                                                                            }),
                                                                    () -> {
                                                                        List<String> subDataAttributes = daRefList.stream().skip(sdoAccumulator.size()).toList();
                                                                        String firstSubDataAttribute = subDataAttributes.get(0);
                                                                        String lastSubDataAttribute = subDataAttributes.get(subDataAttributes.size() - 1);
                                                                        if(firstSubDataAttribute.equals(name)){
                                                                            if(firstSubDataAttribute.equals(lastSubDataAttribute)){
                                                                                sclReportItems.add(SclReportItem.error("", String.format("Unknown Data Attribute DA (%s) in DOType.id (%s)", name, lastDoType.getId())));
                                                                            } else
                                                                                sclReportItems.add(SclReportItem.error("", String.format("Unknown Sub Data Object SDO or Data Attribute DA (%s) in DOType.id (%s)", name, lastDoType.getId())));
                                                                        }
                                                                    });
                                                }
                                                return lastDoType;
                                            }, (doType1, doType2) -> {
                                                throw new ScdException("This reduction cannot be parallel");
                                            });
                                    return sclReportItems;
                                })
                                .orElseGet(() -> {
                                    sclReportItems.add(SclReportItem.error("", String.format("DOType.id (%s) for DO.name (%s) not found in DataTypeTemplates", tdo.getType(), tdo.getName())));
                                    return sclReportItems;
                                }))
                        .orElseGet(() -> {
                            sclReportItems.add(SclReportItem.error("", String.format("Unknown DO.name (%s) in DOType.id (%s)", doName, lNodeTypeId)));
                            return sclReportItems;
                        }))
                .orElseGet(() -> {
                    sclReportItems.add(SclReportItem.error("", "No Data Attribute found with this reference %s for LNodeType.id (%s)".formatted(dataRef, lNodeTypeId)));
                    return sclReportItems;
                });
    }


    public DataAttributeRef isDoObjectsAndDataAttributesExists2(TDataTypeTemplates dtt, String lNodeTypeId, String dataRef) {
        DataAttributeRef dataAttributeRef = new DataAttributeRef();
        LinkedList<String> dataRefList = new LinkedList<>(Arrays.asList(dataRef.split("\\.")));
        if (dataRefList.size() < 2) {
            throw new ScdException("Invalid data reference %s. At least DO name and DA name are required".formatted(dataRef));
        }
        String doName = dataRefList.remove();
        return lnodeTypeService.findLnodeType(dtt, lNodeType -> lNodeTypeId.equals(lNodeType.getId()))
                .map(lNodeType -> doService.findDo(lNodeType, tdo -> tdo.getName().equals(doName))
                        .map(tdo -> doTypeService.findDoType(dtt, doType -> doType.getId().equals(tdo.getType()))
                                .map(tdoType -> {
                                    List<String> sdoAccumulator = new ArrayList<>();
                                    List<String> daAndBdaAccumulator = new ArrayList<>();
                                    DaTypeName daTypeName = new DaTypeName();
                                    dataRefList.stream()
                                            .reduce(tdoType, (lastDoType, name) -> {
                                                Optional<TDOType> optSdo = getDOTypeBySdoName(dtt, lastDoType, name);
                                                if (optSdo.isPresent()) {
                                                    sdoAccumulator.add(name);
                                                    //DO:: Setter -- Mapper
                                                    DoTypeName doTypeName = new DoTypeName(doName);
                                                    doTypeName.setCdc(tdoType.getCdc());
                                                    doTypeName.getStructNames().addAll(sdoAccumulator);
                                                    dataAttributeRef.setDoName(doTypeName);
                                                    return optSdo.get();
                                                } else {
                                                    LinkedList<String> daRefList = (LinkedList<String>) dataRefList.clone();
                                                    getDATypeByDaName(dtt, lastDoType, name)
                                                            .ifPresentOrElse(tdaType -> {
                                                                daAndBdaAccumulator.add(name);
                                                                //DA::Mapper
                                                                TDA tda = findDa(lastDoType, tda1 -> tda1.getName().equals(name)).orElseThrow(() -> new ScdException("da should be present"));
                                                                daTypeName.setFc(tda.getFc());
                                                                if(tda.getBType() != TPredefinedBasicTypeEnum.STRUCT || tda.getBType() != TPredefinedBasicTypeEnum.ENUM) {
                                                                    daTypeName.setBType(tda.getBType());
                                                                    daTypeName.setType(tda.getType());
                                                                    daTypeName.setValImport(tda.isValImport());
                                                                    dataAttributeRef.setDaiValues(tda.getVal());
                                                                }
                                                                daRefList.stream().skip(sdoAccumulator.size() + 1L)
                                                                        .reduce(tdaType, (lastDaType, bdaOrDaName) -> {
                                                                            Optional<TDAType> optBda = getDATypeByBdaName(dtt, lastDaType, bdaOrDaName);
                                                                            if (lastDaType != null && optBda.isPresent()) {
                                                                                daAndBdaAccumulator.add(bdaOrDaName);
                                                                                //DA/BDA::Mapper
                                                                                Optional<TBDA> tbdaOptional = findBda(lastDaType, tbda1 -> tbda1.getName().equals(bdaOrDaName));
                                                                                if(tbdaOptional.isPresent() && tbdaOptional.get().getBType() != TPredefinedBasicTypeEnum.STRUCT) {
                                                                                    TBDA tbda = tbdaOptional.get();
                                                                                    daTypeName.setBType(tbda.getBType());
                                                                                    daTypeName.setType(tbda.getType());
                                                                                    daTypeName.setValImport(tbda.isValImport());
                                                                                    dataAttributeRef.setDaiValues(tbda.getVal());
                                                                                }
                                                                                return optBda.get();
                                                                            } else if (lastDaType != null) {
                                                                                throw new ScdException(String.format("Unknown BDA.name (%s) in DAType.id (%s)", bdaOrDaName, lastDaType.getId()));
                                                                            }
                                                                            return null;
                                                                        }, (daType1, daType2) -> {
                                                                            throw new ScdException("This reduction cannot be parallel");
                                                                        });
                                                            }, () -> {
                                                                if(daRefList.stream().skip(sdoAccumulator.size()).toList().get(0).equals(name)){
                                                                    throw new ScdException(String.format("Unknown Sub Data Object SDO or Data Attribute DA (%s) in DOType.id (%s)", name, lastDoType.getId()));
                                                                }
                                                            });
                                                    daTypeName.setName(String.join(".", daAndBdaAccumulator));
                                                    dataAttributeRef.setDaName(daTypeName);
                                                }
                                                return lastDoType;
                                            }, (doType1, doType2) -> {
                                                throw new ScdException("This reduction cannot be parallel");
                                            });
                                    return dataAttributeRef;
                                })
                                .orElseThrow(() -> new ScdException( String.format("DOType.id (%s) for DO.name (%s) not found in DataTypeTemplates", tdo.getType(), tdo.getName()))))
                        .orElseThrow(() -> new ScdException(String.format("Unknown DO.name (%s) in DOType.id (%s)", doName, lNodeTypeId))))
                .orElseThrow(() -> new ScdException("No Data Attribute found with this reference %s for LNodeType.id (%s)".formatted(dataRef, lNodeTypeId)));
    }
}
