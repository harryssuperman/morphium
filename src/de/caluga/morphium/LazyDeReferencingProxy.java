package de.caluga.morphium;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.io.Serializable;
import java.lang.reflect.Method;

public class LazyDeReferencingProxy<T> implements MethodInterceptor, Serializable {
    private static final long serialVersionUID = 3777709000906217075L;
    private transient final Morphium morphium;
    private final String fieldname;
    private final Object container;
    private T deReferenced;
    private Class<? extends T> cls;
    private Object id;

    private final static Logger log = new Logger(LazyDeReferencingProxy.class);

    public LazyDeReferencingProxy(Morphium m, Class<? extends T> type, Object id, Object container, String fieldname) {
        cls = type;
        this.id = id;
        morphium = m;
        this.container = container;
        this.fieldname = fieldname;
    }

    public T __getDeref() {
        try {
            dereference();
        } catch (Throwable throwable) {
            throw(new RuntimeException(throwable));
        }
        return deReferenced;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
//            if (method.getName().equals("getClass")) {
//                return cls;
//            }
        if (method.getName().equals("__getType")) {
            return cls;
        }
        if (method.getName().equals("finalize")) {
            return methodProxy.invokeSuper(o, objects);
        }

        dereference();
        if (method.getName().equals("__getDeref")) {
            return deReferenced;
        }
        if (deReferenced != null) {
            return method.invoke(deReferenced, objects);
        }
        return methodProxy.invokeSuper(o, objects);

    }

    private void dereference() {
        if (deReferenced == null) {
            if (log.isDebugEnabled())
                log.debug("DeReferencing due to first access");
            morphium.fireWouldDereference(container, fieldname, id, cls, true);
            deReferenced = (T) morphium.findById(cls, id);
            morphium.fireDidDereference(container, fieldname, deReferenced, true);
        }
    }

}
