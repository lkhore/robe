package io.robe.hibernate;

/**
 * Created by kamilbukum on 20/11/16.
 */
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.reflections.Reflections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class HibernateUtil {

    private static Map<String, SessionFactory> factoryMap = new LinkedHashMap<>();
    public static SessionFactory buildSessionFactory(Class<?> testClass){
        Configuration configuration = new Configuration();
        configuration.setProperty("connection.driver_class","org.h2.Driver");
        configuration.setProperty("hibernate.connection.url", "jdbc:h2:mem:" + testClass.getSimpleName());
        configuration.setProperty("hibernate.connection.username", "sa");
        configuration.setProperty("hibernate.connection.password", "");
        configuration.setProperty("dialect", "org.hibernate.dialect.H2Dialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "update");
        configuration.setProperty("show_sql", "true");
        configuration.setProperty(" hibernate.connection.pool_size", "10");

        Reflections reflections = new Reflections("io.robe.hibernate.test.entity");

        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(javax.persistence.Entity.class);

        for(Class<?> clazz : classes) {
            configuration.addAnnotatedClass(clazz);
        }

        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        return configuration.buildSessionFactory(serviceRegistry);
    }
    public static SessionFactory getSessionFactory(Class<?> testClass) {
        SessionFactory sessionFactory = factoryMap.computeIfAbsent(testClass.getSimpleName(), k -> buildSessionFactory(testClass));
        return sessionFactory;
    }
}