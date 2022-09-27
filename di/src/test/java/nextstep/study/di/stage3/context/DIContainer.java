package nextstep.study.di.stage3.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.stream.Collectors;
import nextstep.study.ConsumerWrapper;
import nextstep.study.FunctionWrapper;

/**
 * 스프링의 BeanFactory, ApplicationContext에 해당되는 클래스
 */
class DIContainer {

    private final Set<Object> beans;

    public DIContainer(final Set<Class<?>> classes) {
        this.beans = createBeans(classes);
        beans.forEach(this::setFields);
    }

    private static Set<Object> createBeans(final Set<Class<?>> classes) {
        return classes.stream()
                .map(FunctionWrapper.apply(Class::getDeclaredConstructor))
                .peek(constructor -> constructor.setAccessible(true))
                .map(FunctionWrapper.apply(Constructor::newInstance))
                .collect(Collectors.toSet());
    }

    private void setFields(final Object bean) {
        for (Field field : bean.getClass().getDeclaredFields()) {
            Class<?> type = field.getType();
            field.setAccessible(true);

            beans.stream()
                    .filter(type::isInstance)
                    .forEach(ConsumerWrapper.accept(matchBean -> field.set(bean, matchBean)));
        }
    }

    // 빈 컨테이너(DI)에서 관리하는 빈을 찾아서 반환한다.
    @SuppressWarnings("unchecked")
    public <T> T getBean(final Class<T> aClass) {
        return beans.stream()
                .filter(aClass::isInstance)
                .findFirst()
                .map(bean -> (T) bean)
                .orElseThrow(() -> new IllegalArgumentException("bean을 찾을 수 없습니다."));
    }
}
