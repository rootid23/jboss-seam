package org.jboss.seam.ui.facelet;

import static org.jboss.seam.ScopeType.APPLICATION;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.contexts.Contexts;

import com.sun.facelets.compiler.Compiler;
import com.sun.facelets.compiler.SAXCompiler;

@Name("org.jboss.seam.ui.facelet.faceletCompiler")
@Scope(APPLICATION)
@BypassInterceptors
@AutoCreate
@Install(value = true, precedence = Install.BUILT_IN, classDependencies="com.sun.facelets.Facelet")
public class FaceletCompiler
{

   private Compiler compiler;
   
   @Create
   public void create()
   {
      compiler = new SAXCompiler();
   }
   
   @Unwrap
   public Compiler unwrap()
   {
      return compiler;
   }
   
   public static Compiler instance()
   {
      if ( !Contexts.isApplicationContextActive() )
      {
         throw new IllegalStateException("No active application scope");
      }
      return (Compiler) Component.getInstance(FaceletCompiler.class, ScopeType.APPLICATION);
   }
   
}
