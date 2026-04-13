package com.itdaie.pojo.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageData {

    private long total;
    private List records;

    public static <T> PageData from(IPage<T> page) {
        if (page == null) {
            return new PageData(0L, Collections.emptyList());
        }
        return new PageData(
                page.getTotal(),
                page.getRecords()
        );
    }
}
