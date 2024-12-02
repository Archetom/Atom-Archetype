package ${package}.infra.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public static <T, S> Pager<S> toPager(Page<T> page) {
        Pager<S> pager = new Pager<>();

        pager.setPageNum(page.getPages());
        pager.setPageSize(page.getCurrent());
        pager.setTotalNum((Math.max(page.getTotal(), 0L)));

        return pager;
    }
}
