package com.scality.osis.model;

import java.util.List;

public interface Page<T> {

    List<T> getItems();

    void setItems(List<T> items);

    Page<T> pageInfo(PageInfo pageInfo);

    void setPageInfo(PageInfo pageInfo);
}
