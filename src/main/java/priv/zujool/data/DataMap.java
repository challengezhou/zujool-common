package priv.zujool.data;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 方便的组装数据体map的方法
 *
 * @author zujool  At 2020/4/28 17:03
 **/
@Getter
public class DataMap<T> {

    private Map<String, T> data;


    public static <T> DataMap<T> of(String key, T value) {
        DataMap<T> holder = new DataMap<>();
        if (null == holder.data) {
            holder.data = new HashMap<>(4);
        }
        holder.data.put(key, value);
        return holder;
    }

    public DataMap<T> append(String key, T value) {
        data.put(key, value);
        return this;
    }

    public Map<String, T> toMap() {
        return data;
    }

    public static String nullToNullString(Object o) {
        return (null == o) ? null : o.toString();
    }

}
