package ${package}.infra.persistence.mysql.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.archetom.common.result.Pager;

/**
 * @author hanfeng
 */
public class PageUtil {
    /**
     * Mybatis Page -> Pager
     *
     * @param page Page<T>
     */
    public static <T, S> Pager<S> toPager(IPage<T> page) {
        Pager<S> pager = new Pager<>();

        pager.setPageNum(page.getCurrent());
        pager.setPageSize(page.getSize());
        pager.setTotalNum(Math.max(page.getTotal(), 0L));

        return pager;
    }
}
