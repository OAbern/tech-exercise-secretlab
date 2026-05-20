package com.secretlab.exercise.convertor;

import com.secretlab.exercise.common.JsonUtils;
import com.secretlab.exercise.model.KeyValue;
import com.secretlab.exercise.model.KeyValueHistory;
import com.secretlab.exercise.model.vo.KeyValuePairVO;
import com.secretlab.exercise.model.vo.KeyValueVO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** MapStruct mapper between {@link KeyValueHistory} entity and response VOs. */
@Mapper(imports = {JsonUtils.class})
public interface KeyValueConvertor {

  KeyValueConvertor INSTANCE = Mappers.getMapper(KeyValueConvertor.class);

  @Mapping(source = "storeKey", target = "key")
  @Mapping(source = "createdAt", target = "timestamp")
  @Mapping(target = "value", expression = "java(JsonUtils.parseJson(entry.getValue()))")
  KeyValueVO toVO(KeyValueHistory entry);

  @Mapping(source = "storeKey", target = "key")
  @Mapping(target = "value", expression = "java(JsonUtils.parseJson(entry.getValue()))")
  KeyValuePairVO toPairVO(KeyValue entry);

  List<KeyValuePairVO> toPairVOList(List<KeyValue> entries);
}
