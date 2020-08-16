package priv.zujool.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;

import java.text.SimpleDateFormat;

/**
 * @author zujool  At 2020/5/14 10:26
 **/
public class JacksonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //序列化的时候序列对象的所有属性 
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        //反序列化的时候如果多了其他属性,不抛出异常
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //如果是空对象的时候,不抛异常
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        //属性为null的转换
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //取消时间的转化格式,默认是时间戳,可以取消,同时需要设置要表现的时间格式
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    @SneakyThrows
    public static <T> T parseObject(ObjectMapper mapper, String str, Class<T> tClass) {
        if (null == mapper) {
            mapper = OBJECT_MAPPER;
        }
        return mapper.readValue(str, tClass);
    }

    @SneakyThrows
    public static <T> T parseObject(ObjectMapper mapper, String str, TypeReference<T> valueTypeReference) {
        if (null == mapper) {
            mapper = OBJECT_MAPPER;
        }
        return mapper.readValue(str, valueTypeReference);
    }

    @SneakyThrows
    public static <T> T parseObject(String str, TypeReference<T> valueTypeReference) {
        return parseObject(null, str, valueTypeReference);
    }

    @SneakyThrows
    public static <T> T parseObject(String str, Class<T> tClass) {
        return parseObject(null, str, tClass);
    }

    @SneakyThrows
    public static String writeValueAsString(ObjectMapper mapper, Object object) {
        if (null == mapper) {
            mapper = OBJECT_MAPPER;
        }
        if (null == object) {
            throw new NullPointerException("param can not be null");
        }
        return mapper.writeValueAsString(object);
    }

    @SneakyThrows
    public static String writeValueAsString(Object object) {
        return writeValueAsString(null, object);
    }

}
