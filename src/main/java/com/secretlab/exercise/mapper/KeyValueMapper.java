package com.secretlab.exercise.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.secretlab.exercise.common.JsonUtils;
import com.secretlab.exercise.model.KeyValue;
import com.secretlab.exercise.model.KeyValueHistory;
import com.secretlab.exercise.model.vo.KeyValuePairVO;
import com.secretlab.exercise.model.vo.KeyValueVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * MapStruct mapper between {@link KeyValueHistory} entity and response VOs.
 */
@Mapper
public interface KeyValueMapper {

    KeyValueMapper INSTANCE = Mappers.getMapper(KeyValueMapper.class);

    @Mapping(source = "storeKey", target = "key")
    @Mapping(source = "createdAt", target = "timestamp")
    @Mapping(target = "value", expression = "java(parseValue(entry.getValue()))")
    KeyValueVO toVO(KeyValueHistory entry);

    @Mapping(source = "storeKey", target = "key")
    @Mapping(target = "value", expression = "java(parseValue(entry.getValue()))")
    KeyValuePairVO toPairVO(KeyValue entry);

    List<KeyValuePairVO> toPairVOList(List<KeyValue> entries);

    default JsonNode parseValue(String json) {
        return JsonUtils.parseJson(json);
    }
}
