package com.aitef.forum.model.dto;

import java.util.List;

public class Page<T> {
    private final List<T> items;
    private final int page;
    private final int size;
    private final long total;

    public Page(List<T> items, int page, int size, long total) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotal() {
        return total;
    }

    public int getTotalPages() {
        if (size <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(total * 1.0 / size));
    }

    public boolean isHasPrevious() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < getTotalPages();
    }
}
