package ${package}.shared.util;

import io.github.archetom.common.result.Pager;

/**
 * @author hanfeng
 */
public class PageUtil {
    /**
     * Copies page metadata but intentionally omits the object list.
     * Unlike {@code Pager.map(Function)}, this method does not transform or retain elements.
     *
     * @param pager source pager
     * @return a pager with the same page metadata and no object list
     */
    public static <T, S> Pager<S> copy(Pager<T> pager) {
        Pager<S> newPager = new Pager<>();

        newPager.setPageNum(pager.getPageNum());
        newPager.setPageSize(pager.getPageSize());
        newPager.setTotalNum(pager.getTotalNum());
        newPager.setMeta(pager.getMeta());

        return newPager;
    }
}
