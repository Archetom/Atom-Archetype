package ${package}.api.dto.request;


import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author hanfeng
 */
@Data
@Accessors(chain = true)
public class QueryRequest {
    /**
     * 页码
     */
    protected Integer page = 1;

    /**
     * 每页显示数量
     */
    protected Integer size = 20;

    /**
     * 排序字段
     */
    protected String sortField;

    /**
     * 排序方向（ASC 或 DESC）
     */
    private String sortDirection;
}
