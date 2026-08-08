package ${package}.api.dto.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/** Typed page of tenant-visible users. */
@Data
@Accessors(chain = true)
public class UserPageResponse {
    private int pageNum;
    private int pageSize;
    private long totalNum;
    private List<UserResponse> objectList;
}
