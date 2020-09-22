package com.atguigu.gulimall.member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ??Ա
 *
 * @author yuke
 * @email 627617510@gmail.com
 * @date 2020-08-23 17:46:43
 */
@Data
@TableName("ums_member")
public class MemberEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * $column.comments
	 */
	@TableId
	private Long id;
	/**
	 * $column.comments
	 */
	private Long levelId;
	/**
	 * $column.comments
	 */
	private String username;
	/**
	 * $column.comments
	 */
	private String password;
	/**
	 * $column.comments
	 */
	private String nickname;
	/**
	 * $column.comments
	 */
	private String mobile;
	/**
	 * $column.comments
	 */
	private String email;
	/**
	 * $column.comments
	 */
	private String header;
	/**
	 * $column.comments
	 */
	private Integer gender;
	/**
	 * $column.comments
	 */
	private Date birth;
	/**
	 * $column.comments
	 */
	private String city;
	/**
	 * $column.comments
	 */
	private String job;
	/**
	 * $column.comments
	 */
	private String sign;
	/**
	 * $column.comments
	 */
	private Integer sourceType;
	/**
	 * $column.comments
	 */
	private Integer integration;
	/**
	 * $column.comments
	 */
	private Integer growth;
	/**
	 * $column.comments
	 */
	private Integer status;
	/**
	 * $column.comments
	 */
	private Date createTime;

	/**
	 * 以下三个地段是微博账号需要的字段
	 */
	private String socialUid;

	private String accessToken;

	private Long expiresIn;

}
