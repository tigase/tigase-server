package tigase.kernel.beans;

import java.lang.annotation.*;

/**
 * Defines name of bean.<br/>
 * This annotation is not required, but each bean must be named! Instead of
 * using annotation, name of bean may be defined during registration.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Bean {

	/**
	 * Name of bean.
	 * 
	 * @return name of bean.
	 */
	String name();
}
