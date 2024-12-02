package ${package}.shared.util;

import io.github.archetom.common.result.Pager;

/**
 * @author hanfeng
 */
public class PageUtil {
    /**
     * 复制基础页码信息
     *
     * @param pager Pager
     */
    public static <T, S> Pager<S> copy(Pager<T> pager) {
        Pager<S> newPager = new Pager<>();

        newPager.setPageNum(pager.getPageNum());
        newPager.setPageSize(pager.getPageSize());
        newPager.setTotalNum((Math.max(pager.getTotalNum(), 0L)));

        return newPager;
    }
}
