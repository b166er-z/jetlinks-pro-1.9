package org.jetlinks.pro.reactorql;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ReactorQLBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private final MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

    @Override
    @SneakyThrows
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, @Nonnull BeanDefinitionRegistry registry) {
        Map<String, Object> attr = importingClassMetadata.getAnnotationAttributes(EnableReactorQL.class.getName());
        if (attr == null) {
            return;
        }
        String[] packages = (String[]) attr.get("value");
        if (packages.length == 0) {
            return;
        }
        String path = Arrays.stream(packages)
                            .map(str -> ResourcePatternResolver
                                .CLASSPATH_ALL_URL_PREFIX
                                .concat(str.replace(".", "/")).concat("/**/*.class"))
                            .collect(Collectors.joining());

        for (Resource resource : resourcePatternResolver.getResources(path)) {
            MetadataReader reader = metadataReaderFactory.getMetadataReader(resource);
            String className = reader.getClassMetadata().getClassName();
            Class<?> type = org.springframework.util.ClassUtils.forName(className, null);
            if (!type.isInterface() || type.getAnnotation(ReactorQLOperation.class) == null) {
                continue;
            }
            RootBeanDefinition definition = new RootBeanDefinition();
            definition.setTargetType(type);
            definition.setBeanClass(ReactorQLFactoryBean.class);
            definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            definition.getPropertyValues().add("target", type);
            log.debug("register ReactorQL Operator {}", type);
            registry.registerBeanDefinition(type.getSimpleName(), definition);
        }

    }

}
