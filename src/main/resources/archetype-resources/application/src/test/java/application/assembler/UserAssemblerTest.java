package ${package}.application.assembler;

import ${package}.api.dto.response.UserPageResponse;
import ${package}.api.dto.response.UserResponse;
import ${package}.application.vo.UserVO;
import io.github.archetom.common.result.Pager;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserAssemblerTest {

    @Test
    void stripsInternalMetaFromThePublicPageResponse() {
        Pager<UserVO> voPager = new Pager<>();
        voPager.setPageNum(1);
        voPager.setPageSize(20);
        voPager.setTotalNum(37L);
        voPager.setObjectList(List.of());
        Map<String, Object> internalMeta = new HashMap<>();
        internalMeta.put("queryDigest", "internal-only");
        voPager.setMeta(internalMeta);

        Pager<UserResponse> responsePager = UserAssembler.INSTANCE.toResponsePager(voPager);

        assertEquals(37L, responsePager.getTotalNum());
        assertNull(responsePager.getMeta());

        UserPageResponse pageResponse = UserAssembler.INSTANCE.toPageResponse(voPager);
        assertEquals(1, pageResponse.getPageNum());
        assertEquals(20, pageResponse.getPageSize());
        assertEquals(37L, pageResponse.getTotalNum());
        assertEquals(List.of(), pageResponse.getObjectList());
    }

    @Test
    void rejectsUnknownTotalsAtThePublicPageBoundary() {
        Pager<UserVO> voPager = new Pager<>();
        voPager.setPageNum(1);
        voPager.setPageSize(20);
        voPager.setTotalNum(Pager.NO_TOTAL_NUM);
        voPager.setObjectList(List.of());

        assertThrows(IllegalStateException.class,
                () -> UserAssembler.INSTANCE.toResponsePager(voPager));
    }
}
