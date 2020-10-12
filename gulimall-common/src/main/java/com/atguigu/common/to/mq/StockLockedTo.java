package com.atguigu.common.to.mq;

import lombok.Data;

import java.util.List;

@Data
public class StockLockedTo {
    /**
     * 库存工作单id
     */
    private Long id;

    /**
     * 工作单所有详情id
     */
    private StockDetailTo detail;
}
