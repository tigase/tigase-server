package tigase.kernel.beans;

import java.lang.annotation.*;

/**
 * This annotation marks field in class that Kernel should inject dependency
 * here.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Inject {

	class EMPTY {
		private EMPTY() {
		}
	}

	/**
	 * Name of bean to be injected (optional).
	 * 
	 * @return name of bean.
	 */
	String bean() default "";

	/**
	 * Specify if injection of dependency is required or not.
	 * 
	 * @return <code>true</code> if <code>null</code> value is allowed to
	 *         inject.
	 */
	boolean nullAllowed() default false;

	/**
	 * Type of bean to be injected (opiotnal).
	 * 
	 * @return type of bean.
	 */
	Class<?>type() default EMPTY.class;
}
