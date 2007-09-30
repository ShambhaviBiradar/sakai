/**
 * BeanCollectorAutoRegistrar.java - entity-broker - 2007 Sep 29, 2007 10:57:42 AM - azeckoski
 */

package org.sakaiproject.entitybroker.impl.collector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.entitybroker.collector.AutoRegister;
import org.sakaiproject.entitybroker.collector.BeanCollector;
import org.sakaiproject.entitybroker.collector.BeanMapCollector;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * This will collect the autoregistered beans and place them into all the locations which requested them
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class BeanCollectorAutoRegistrar implements ApplicationListener, ApplicationContextAware, InitializingBean {

   private static Log log = LogFactory.getLog(BeanCollectorAutoRegistrar.class);

   private ApplicationContext applicationContext;
   public void setApplicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
   }

   public void onApplicationEvent(ApplicationEvent event) {
      // We do not actually respond to any events, purpose is to time initialisation only.
   }

   private Set<String> autoRegistered; 
   public void init() {
      String[] autobeans = applicationContext.getBeanNamesForType(AutoRegister.class, false, false);
      autoRegistered = new HashSet<String>();
      for (int i = 0; i < autobeans.length; i++) {
         autoRegistered.add(autobeans[i]);
      }
   }


   public void afterPropertiesSet() throws Exception {
      log.debug("setAC: " + applicationContext.getDisplayName());
      ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
      ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) cac.getBeanFactory();

      cbf.addBeanPostProcessor(new BeanPostProcessor() {
         public Object postProcessBeforeInitialization(Object bean, String beanName) {
            if (bean instanceof BeanCollector<?>) {
               BeanCollector<Object> bc = (BeanCollector<Object>) bean;
               Class<?> c = bc.getCollectedType();
               if (c == null) {
                  throw new IllegalArgumentException("collected type cannot be null");
               }

               List<Object> l = getAutoRegisteredBeansOfType(c);
               logCollectedBeanInsertion(beanName, c.getName(), l);
               bc.setCollectedBeans(l);
            } else if (bean instanceof BeanMapCollector) {
               BeanMapCollector bc = (BeanMapCollector) bean;
               Class<?>[] cArray = bc.getCollectedTypes();
               if (cArray == null) {
                  throw new IllegalArgumentException("collected types cannot be null");
               }

               Map<Class<?>, List<?>> collectedBeans = new HashMap<Class<?>, List<?>>();
               for (int i = 0; i < cArray.length; i++) {
                  List<Object> l = getAutoRegisteredBeansOfType(cArray[i]);
                  logCollectedBeanInsertion(beanName, cArray[i].getName(), l);
                  collectedBeans.put(cArray[i], l);
               }
               bc.setCollectedBeansMap(collectedBeans);
            }
            return bean;
         }

         public Object postProcessAfterInitialization(Object bean, String beanName) {
            return bean;
         }
      });

   }


   /**
    * Get all autoregistered beans of a specific type
    * @param c the class type
    * @return a list of the matching beans
    */
   private List<Object> getAutoRegisteredBeansOfType(Class<?> c) {
      String[] autobeans = applicationContext.getBeanNamesForType(c, false, false);
      List<Object> l = new ArrayList<Object>();
      for (String autobean : autobeans) {
         if (autoRegistered.contains(autobean)) {
            Object bean = applicationContext.getBean(autobean);
            l.add(bean);
         }
      }
      return l;
   }

   /**
    * Generate a log message about the collected insertion
    * @param beanName the name of the bean getting collected beans inserted into it
    * @param l the list of beans that were inserted
    */
   private void logCollectedBeanInsertion(String beanName, String beanType, List<Object> l) {
      StringBuilder registeredBeans = new StringBuilder();
      registeredBeans.append("[");
      for (int i = 0; i < l.size(); i++) {
         if (i > 0) {
            registeredBeans.append(",");
         }
         registeredBeans.append(l.get(i).getClass().getName());
      }
      registeredBeans.append("]");
      log.info("Set collected beans of type ("+beanType+") on bean ("+beanName+") to ("+l.size()+") " + registeredBeans.toString());
   }

}
