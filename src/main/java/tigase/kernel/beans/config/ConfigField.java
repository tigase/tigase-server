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

}
