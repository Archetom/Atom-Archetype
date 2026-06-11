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
     * code
     */
    protected Integer page = 1;

    /**
     *
     */
    protected Integer size = 20;

    /**
     * field
     */
    protected String sortField;

    /**
     * (ASC or DESC)
     */
    private String sortDirection;
}
