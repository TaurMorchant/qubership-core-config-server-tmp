package org.qubership.cloud.configserver;

import org.qubership.cloud.configserver.config.SpringUtility;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import static org.junit.Assert.assertSame;

public class SpringUtilityTest {

    @Autowired
    private static ApplicationContext applicationContext;

    @Test
    public void applicationContextActions (){
        SpringUtility.setApplicationContext(applicationContext);
        assertSame(applicationContext,SpringUtility.getContext());
    }
}
