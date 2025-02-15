package cn.gotom.commons.data;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.gotom.commons.Note;

@Note("删除关联表注解")
@Target({ ElementType.TYPE })
@Retention(RUNTIME)
public @interface LinkDelete {
	
	@Note("对应实体类")
	Class<?> value();
	
	@Note("对应字段")
	String column();
}
