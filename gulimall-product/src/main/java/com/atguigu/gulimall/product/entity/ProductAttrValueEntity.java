package com.atguigu.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * spu????ֵ
 * 
 * @author yuke
 * @email 627617510@gmail.com
 * @date 2020-08-23 16:18:07
 */
@Data
@TableName("pms_product_attr_value")
public class ProductAttrValueEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * $column.comments
	 */
	@TableId
	private Long id;
	/**
	 * $column.comments
	 */
	private Long spuId;
	/**
	 * $column.comments
	 */
	private Long attrId;
	/**
	 * $column.comments
	 */
	private String attrName;
	/**
	 * $column.comments
	 */
	private String attrValue;
	/**
	 * $column.comments
	 */
	private Integer attrSort;
	/**
	 * $column.comments
	 */
	private Integer quickShow;

}
