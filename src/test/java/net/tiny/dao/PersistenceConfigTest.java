package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.tiny.config.Configuration;
import net.tiny.config.ConfigurationHandler;
import net.tiny.config.ContextHandler;

public class PersistenceConfigTest {

    @Test
    public void testPersistenceYaml() throws Exception {
        ContextHandler.Listener listener = new Listener();
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setListener(listener);
        handler.setResource("src/test/resources/config/persistence-test.yml");
        handler.parse(true);
        Configuration configuration = handler.getConfiguration();
        assertNotNull(configuration);

        Properties jpaProp = configuration.getAs("jpa.properties", Properties.class);
        assertNotNull(jpaProp);
        assertEquals(17, jpaProp.size());
        assertEquals("org.eclipse.persistence.jpa.PersistenceProvider", jpaProp.getProperty("javax.persistence.jdbc.provider"));
        assertEquals("HSQL", jpaProp.getProperty("eclipselink.target-database"));

        EntityManagerProducer producer = configuration.getAs("jpa.producer", EntityManagerProducer.class);
        assertNotNull(producer);
        Properties prop = producer.getProperties();
        assertEquals(17, prop.size());
        System.out.println();
        for (String name : prop.stringPropertyNames()) {
           System.out.println(name + " : " + prop.getProperty(name));
        }
    }


    static class Listener implements ContextHandler.Listener {
        final Map<String, Object> collection = new HashMap<>();
        @Override
        public void created(Object bean, Class<?> beanClass) {
            System.out.println(String.format("[BOOT] '%s'#%d was created.", beanClass.getSimpleName(), bean.hashCode()));
        }

        @Override
        public void parsed(String type, String resource, int size) {
            System.out.println(String.format("[BOOT] %s '%s'(%d) was parsed.", type, String.valueOf(resource), size));
        }

        @Override
        public void cached(String name, Object value, boolean config) {
            if (config) {
                System.out.println(String.format("[BOOT] Cached Configuration#%d' by '%s'", value.hashCode(), name));
            } else {
                System.out.println(String.format("[BOOT] Cached '%s' = '%s'", name, value.toString()));
                collection.put(name, value);
            }
        }
        public Set<String> keys() {
            return collection.keySet();
        }
        public Object get(String name) {
            return collection.get(name);
        }
    }
}
