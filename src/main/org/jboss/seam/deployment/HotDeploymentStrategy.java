package org.jboss.seam.deployment;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Set;

import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.util.Reflections;

/**
 * A deployment strategy for hot deployable Java Seam components
 * 
 * @author Pete Muir
 *
 */
public class HotDeploymentStrategy extends DeploymentStrategy
{
   /**
    * The default path at which hot deployable Seam components are placed
    */
   public static final String DEFAULT_HOT_DEPLOYMENT_DIRECTORY_PATH = "WEB-INF/dev";
   
   /**
    * The contextual variable name this deployment strategy is made available at
    * during Seam startup.
    */
   public static final String NAME = "hotDeploymentStrategy";
   
   /**
    * The key under which to list extra hot deployment directories
    * 
    * This can be specified as a System property or in 
    * /META-INF/seam-deployment.properties
    */
   //public static final String HOT_DEPLOY_DIRECTORIES_KEY = "org.jboss.seam.deployment.hotDeploymentDirectories";
   
   /**
    * The key under which to list extra deployment handlers.
    * 
    * This can be specified as a System property or in 
    * /META-INF/seam-deployment.properties
    */
   public static final String HANDLERS_KEY = "org.jboss.seam.deployment.hotDeploymentHandlers";
   
   private ClassLoader hotDeployClassLoader;
   
   private ComponentDeploymentHandler componentDeploymentHandler;
   private AnnotationDeploymentHandler annotationDeploymentHandler;
   
   private long timestamp = 0L;

   private ClassLoader classLoader;
   
   /**
    * @param classLoader The parent classloader of the hot deployment classloader
    * @param hotDeployDirectory The directory in which hot deployable Seam 
    * components are placed
    */
   public HotDeploymentStrategy(ClassLoader classLoader, File hotDeployDirectory, boolean enabled)
   {
      if (enabled)
      {
         this.classLoader = Thread.currentThread().getContextClassLoader();
         if (hotDeployDirectory != null && hotDeployDirectory.exists())
         {
            initHotDeployClassLoader(classLoader, hotDeployDirectory);
            componentDeploymentHandler = new ComponentDeploymentHandler();
            getDeploymentHandlers().put(ComponentDeploymentHandler.NAME, componentDeploymentHandler);
            annotationDeploymentHandler = new AnnotationDeploymentHandler(getPropertyValues(AnnotationDeploymentHandler.ANNOTATIONS_KEY), classLoader);
            getDeploymentHandlers().put(AnnotationDeploymentHandler.NAME, annotationDeploymentHandler);
         }
         getDeploymentHandlers().put(DotPageDotXmlDeploymentHandler.NAME, new DotPageDotXmlDeploymentHandler());
      }
   }
   
   private void initHotDeployClassLoader(ClassLoader classLoader, File hotDeployDirectory)
   {
      try
      {
         URL url = hotDeployDirectory.toURL();
         URL[] urls = { url };
         hotDeployClassLoader = new URLClassLoader(urls, classLoader);
         getFiles().add(hotDeployDirectory);
      }
      catch (MalformedURLException mue)
      {
         throw new RuntimeException(mue);
      }
   }
   
   public boolean isEnabled()
   {
      return classLoader != null;
   }
   
   public boolean isHotDeployClassLoaderEnabled()
   {
      return hotDeployClassLoader != null;
   }
   
   @Override
   protected String getDeploymentHandlersKey()
   {
      return HANDLERS_KEY;
   }

   /**
    * Get all hot deployable paths
    */
   public File[] getHotDeploymentPaths()
   {
      return getFiles().toArray(new File[0]);
   }

   /**
    * Return true if the component is from a hot deployment classloader
    */
   public boolean isFromHotDeployClassLoader(Class componentClass)
   {
      return componentClass.getClassLoader() == hotDeployClassLoader;
   }

   /**
    * Dynamically instantiate a {@link HotDeploymentStrategy}
    * 
    * Needed to prevent dependency on optional librarires
    * @param className The strategy to use 
    * @param classLoader The classloader to use with this strategy
    * @param hotDeployDirectory The directory which contains hot deployable
    * Seam components
    */
   public static HotDeploymentStrategy createInstance(String className, ClassLoader classLoader, File hotDeployDirectory, boolean enabled)
   {
      try
      {
         Class initializer = Reflections.classForName(className);
         Constructor ctr = initializer.getConstructor(ClassLoader.class, File.class, boolean.class);
         return (HotDeploymentStrategy) ctr.newInstance(classLoader, hotDeployDirectory, enabled);
      }
      catch (Exception e)
      {
         throw new IllegalArgumentException("No such deployment strategy " + className, e);
      }
   }

   @Override
   public ClassLoader getClassLoader()
   {
      return hotDeployClassLoader != null ? hotDeployClassLoader : classLoader;
   }
   
   /**
    * Get all Components which the strategy has scanned and handled
    */
   public Set<Class<Object>> getScannedComponentClasses()
   {
      return componentDeploymentHandler.getClasses();
   }

   public Map<String, Set<Class<Object>>> getAnnotatedClasses()
   {
      return annotationDeploymentHandler.getClasses();
   }
   
   @Override
   public void scan()
   {
      getScanner().scanDirectories(getFiles().toArray(new File[0]));
      
   }
   
   public static HotDeploymentStrategy instance()
   {
      if (Contexts.getEventContext().isSet(NAME))
      {
         return (HotDeploymentStrategy) Contexts.getEventContext().get(NAME);
      }
      return null;
   }
   
}
