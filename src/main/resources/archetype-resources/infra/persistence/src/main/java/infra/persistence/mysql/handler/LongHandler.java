package ${package}.infra.persistence.mysql.handler;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.List;

/**
 * 解析 Json 强制使用 Long 类型
 *
 * @author hanfeng
 */
public class LongHandler extends JacksonTypeHandler {

    public LongHandler(Class<?> type) {
        super(type);
    }

    @Override
    public Object parse(String json) {
        try {
            return getObjectMapper().readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
