package tigase.kernel.beans;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Inject {

	public static final class EMPTY {
		private EMPTY() {
		}
	}

	String bean() default "";

	boolean nullAllowed() default true;

	Class<?> type() default EMPTY.class;
}
