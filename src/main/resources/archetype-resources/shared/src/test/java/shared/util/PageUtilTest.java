package ${package}.shared.util;

import io.github.archetom.common.result.Pager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PageUtilTest {

    @Test
    void copiesMetadataWithoutObjectList() {
        Map<String, Object> meta = Map.of("cursor", "next-page");
        Pager<String> source = new Pager<>(List.of("first"), 25, 3, Pager.NO_TOTAL_NUM, meta);

        Pager<Long> copied = PageUtil.copy(source);

        assertEquals(3L, copied.getPageNum());
        assertEquals(25L, copied.getPageSize());
        assertEquals(Pager.NO_TOTAL_NUM, copied.getTotalNum());
        assertSame(meta, copied.getMeta());
        assertNull(copied.getObjectList());
    }
}
