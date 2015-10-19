package tigase.kernel.beans.config;

import java.lang.annotation.*;

/**
 * Annotation to define configurable field.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ConfigField {

	/**
	 * Description of field. May be used in all human readable forms.
	 * 
	 * @return description of field.
	 */
	String desc();

	/**
	 * Makes alias of "component root level" property in config file.
	 * <p>
	 * Not only {@code component/bean/property=value} will be used but also
	 * {@code component/alias=value}.
	 * </p>
	 * 
	 * @return alias of config field.
	 */
	String alias() default "";

}
