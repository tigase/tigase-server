package tigase.kernel.beans;

import java.lang.annotation.*;

/**
 * Defines name of bean.
 * <p>
 * This annotation is not required, but each bean must be named! Instead of
 * using annotation, name of bean may be defined during registration.
 * </p>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Bean {

	/**
	 * Name of bean.
	 * <br/>
	 * This name will be used to find bean and load configuration for the bean.
	 * 
	 * @return name of bean.
	 */
	String name();

	/**
	 * Is active by default.
	 * <br/>
	 * <ul>
	 *     <li><code>true</code> if annotated bean (if registered) should be active (automatically loaded if required);</li>
	 *     <li><code>false</code> if annotated bean will not be automatically loaded, even if it will be registered. It will require manual activation in the configuration.</li>
	 * </ul>
	 *
	 * @return <code>true</code> if bean should be automatically loaded
	 *
	 */
	boolean active();

	/**
	 * Is bean exportable?
	 * <br/>
	 * Exportable beans are available not only in the context of a kernel in which they are registered but are available also to all beans registered in subkernels.
	 *
	 * @return <code>true</code> if bean should be available also in subkernels
	 */
	boolean exportable() default false;

	/**
	 * Class of a parent bean.
	 * <br/>
	 * Returns parent class for which this bean should always be registered.
	 * <br/>
	 * Following cases are supported:
	 * <ul>
	 *     <li><code>Object</code> if bean should never be automatically registered</li>
	 *     <li>{@link tigase.kernel.core.Kernel}</code> if bean should be automatically registered in the main/root kernel</li>
	 *     <li>class implementing {@link tigase.kernel.beans.RegistrarBean} if bean should be loaded automatically for that class</li>
	 * </ul>
	 *
	 * @return parent class for which this class should be automatically registered
	 */
	Class parent() default Object.class;

	/**
	 * Classes of parent beans.
	 * <br/>
	 * In some cases same beans should be automatically registered for more than one class. This method allows to return more than one class.
	 * 
	 * @see #parent()
	 * @return array of classes for which this class should be automatically registered
	 *
	 */
	Class[] parents() default {};

	/**
	 * Automatic registration selectors.
	 * <br/>
	 * In some cases it is required/useful to decide if beans should be registered automatically depending on more global configuration option.
	 * This method returns array of {@link tigase.kernel.beans.BeanSelector} classes which provide a logic deciding if bean should be automatically loaded or not.
	 * <br/>
	 * Will only work if {@link #parent()} or {@link #parents()} returns correct values.
	 *
	 * @return array of classes deciding if bean should be automatically loaded.
	 */
	Class<? extends BeanSelector>[] selectors() default { };

}