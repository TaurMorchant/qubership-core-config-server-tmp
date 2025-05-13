package org.qubership.cloud.configserver.config;

import org.springframework.context.ApplicationContext;

public class SpringUtility {

    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        SpringUtility.applicationContext = applicationContext;
    }

    private SpringUtility() {
    }

    public static <T> T getBean(final Class clazz) {
        return (T) applicationContext.getBean(clazz);
    }

    public static ApplicationContext getContext() {
        return applicationContext;
    }

}