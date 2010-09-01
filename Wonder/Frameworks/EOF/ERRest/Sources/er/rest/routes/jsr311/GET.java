package er.rest.routes.jsr311;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import er.rest.routes.ERXRoute;

/**
 * <p>
 * JSR-311-esque "GET" request method declaration. 
 * </p>
 * 
 * @author mschrag
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod(value=ERXRoute.Method.Get)
public @interface GET {
}
