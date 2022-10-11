package nextstep.study.di.stage4.annotations;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
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
        this.beans.forEach(this::setFields);
    }

    private Set<Object> createBeans(final Set<Class<?>> classes) {
        return classes.stream()
                .map(FunctionWrapper.apply(Class::getDeclaredConstructor))
                .peek(constructor -> constructor.setAccessible(true))
                .map(FunctionWrapper.apply(Constructor::newInstance))
                .collect(Collectors.toSet());
    }

    public static DIContainer createContainerForPackage(final String rootPackageName) {
        Set<Class<?>> classes = ClassPathScanner.getAllClassesInPackage(rootPackageName);
        return classes.stream()
                .filter(clazz -> clazz.isAnnotationPresent(Service.class) || clazz.isAnnotationPresent(
                        Repository.class))
                .collect(Collectors.collectingAndThen(Collectors.toSet(), DIContainer::new));
    }

    // 빈 내부에 선언된 필드를 각각 셋팅한다.
    private void setFields(final Object bean) {
        Arrays.stream(bean.getClass().getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Inject.class))
                .forEach(field -> setFields(bean, field));
    }

    private void setFields(final Object bean, final Field field) {
        final var fieldType = field.getType();
        field.setAccessible(true);

        beans.stream()
                .filter(fieldType::isInstance)
                .forEach(ConsumerWrapper.accept(matchBean -> field.set(bean, matchBean)));
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(final Class<T> aClass) {
        return beans.stream()
                .filter(aClass::isInstance)
                .findFirst()
                .map(bean -> (T) bean)
                .orElseThrow(() -> new IllegalArgumentException("bean을 찾을 수 없습니다."));
    }
}
