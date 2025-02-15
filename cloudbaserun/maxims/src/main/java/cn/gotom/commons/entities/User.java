package cn.gotom.commons.entities;

import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cn.gotom.commons.data.Forbid;
import cn.gotom.commons.data.LinkDelete;
import cn.gotom.commons.data.LinkDeletes;
import cn.gotom.commons.data.SQLDelete;
import cn.gotom.commons.utils.ValidatorUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table("base_user")
@javax.persistence.Table(//
		name = "base_user", //
		indexes = { //
				@Index(unique = true, columnList = "account", name = "UK_USER_USERNAME"),
				@Index(unique = true, columnList = "mobile", name = "UK_USER_MOBILE"),
				@Index(unique = true, columnList = "email", name = "UK_USER_EMAIL"),
				@Index(unique = true, columnList = "rfid", name = "UK_USER_RFID"), //
				@Index(unique = true, columnList = "tokenId", name = "UK_USER_TOKENID"), //
//				@Index(unique = false, columnList = "created", name = "IDX_USER_CREATED"), //
				@Index(unique = false, columnList = "expiresAt", name = "IDX_USER_EXPIRESAT"), //
		})
@Forbid
@ApiModel("用户信息")
@SQLDelete
@LinkDeletes({ //
		@LinkDelete(value = StructureUser.class, column = "user_id"), //
		@LinkDelete(value = UserRole.class, column = "user_id"), //
		@LinkDelete(value = UserTenant.class, column = "user_id"), //
})
public class User extends cn.gotom.commons.model.Token {

	private static final long serialVersionUID = -7066074229094495156L;
	private static final String ADMIN = "admin";
	public static final String DEFAULT_PASSWORD = "naste123456";
	private static final User SUPERADMIN = admin();

	private static final User EMPTY = empty();

	public static User admin() {
		if (SUPERADMIN != null) {
			return SUPERADMIN;
		}
		User user = new User();
		user.setSuperAdmin(true);
		user.setAccount(ADMIN);
//		user.setId(User.nextId());
//		user.setPassword(DEFAULT_PASSWORD);
//		user.setName("超级管理员");
//		user.setMemo("系统初始化的超级管理员角色,请不要删除");
		return user;

	}

	public static User empty() {
		if (EMPTY != null) {
			return EMPTY;
		}
		User user = new User();
		user.setSuperAdmin(false);
		user.setAccount("");
		user.setPassword("");
		user.setName("");
		user.setAuthenticated(false);
		user.setAuthorPatterns(Collections.emptyList());
		return user;
	}

	@ApiModelProperty(value = "手机号码", notes = "找回密码，重置密码")
	@Column(length = 32)
	@Pattern(regexp = ValidatorUtils.MOBILE, message = "手机不合法")
	private String mobile;

	@ApiModelProperty(value = "邮箱地址", notes = "找回密码，重置密码")
	@Size(min = 5, max = 50, message = "邮箱长度需要在50个字符以内")
	@Pattern(regexp = ValidatorUtils.EMAIL, message = "邮箱不合法")
	@Column(length = 128)
	private String email;

	@ApiModelProperty(value = "IC卡")
	@Column
	private String rfid;

	@ApiModelProperty(value = "帐户状态(0-已注册 1-已激活 2-已锁定)")
	private Integer state;

	@ApiModelProperty(value = "用户姓名")
	@Column(length = 64)
	@NotBlank(message = "用户姓名不可以为空")
	@Size(min = 2, max = 25, message = "用户姓名长度需要在25个字符以内")
	private String name;

	@ApiModelProperty(value = "头像地址")
	@Column(length = 100)
	private String icon;

	@ApiModelProperty(value = "用户类型(1-WEB端用户 2-APP用户)")
	private Integer userType;

	@ApiModelProperty(value = "身份：0-系统运维人员 1-用户/客户 10-预付费-户主")
	@Column
	private Integer identity;

	@ApiModelProperty(value = "备注")
	@Column()
	private String memo;

	@ApiModelProperty(value = "申请加入")
	@Enumerated(EnumType.STRING)
	@ReadOnlyProperty
	private transient UserTenantJoin joined;

	@ApiModelProperty(value = "当前租户")
	@Transient
	@ReadOnlyProperty
	private transient Tenant currentTenant;

	@ApiModelProperty(value = "用户权限", hidden = true)
	@Transient
	@JsonIgnore
	private transient List<String> authorPatterns;

	@ApiModelProperty(value = "角色", notes = "用户已经拥有的角色")
	@Transient
	private transient List<Role> roleList;

	@ApiModelProperty(value = "组织", notes = "用户拥有的组织")
	@Transient
	private transient List<Structure> structureList;

	@ApiModelProperty(value = "角色ID", notes = "用户已经拥有的角色ID")
	@Transient
	private transient List<String> roleIdList;

	@ApiModelProperty(value = "组织ID", notes = "用户拥有的组织")
	@Transient
	private transient List<String> structureIdList;

	@ApiModelProperty(value = "用户角色", hidden = true)
	@ReadOnlyProperty
	private transient String role;

	@ApiModelProperty(value = "所在组织", hidden = true)
	@ReadOnlyProperty
	private transient String structure;

	@Column(updatable = false)
	@Override
	public String getTenantId() {
		return super.getTenantId();
	}

	public String getFullname() {
		return this.name;
	}
}
