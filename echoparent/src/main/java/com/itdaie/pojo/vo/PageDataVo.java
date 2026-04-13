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
public class PageDataVo {

    private long total;
    private List records;
    public static <T> PageDataVo from(IPage<T> page) {
        if (page == null) {
            return new PageDataVo(0L, Collections.emptyList());
        }
        return new PageDataVo(
                page.getTotal(),
                page.getRecords()
        );
    }
}
