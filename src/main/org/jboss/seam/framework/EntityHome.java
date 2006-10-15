package org.jboss.seam.framework;

import javax.persistence.EntityManager;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.core.FacesMessages;
import org.jboss.seam.persistence.PersistenceProvider;

public class EntityHome<E> extends Home<E>
{
   private EntityManager entityManager;
   
   @Override
   public void validate()
   {
      super.validate();
      if ( getEntityManager()==null )
      {
         throw new IllegalStateException("entityManager is null");
      }
   }

   @In(create=true) 
   private FacesMessages facesMessages; 
   
   @Transactional
   public boolean isManaged()
   {
      return getInstance()!=null && 
            getEntityManager().contains( getInstance() );
   }

   @Transactional
   public String update()
   {
      getEntityManager().joinTransaction();
      getEntityManager().flush();
      facesMessages.add( getUpdatedMessage() );
      return "updated";
   }
   
   @Transactional
   public String persist()
   {
      getEntityManager().joinTransaction();
      getEntityManager().persist( getInstance() );
      getEntityManager().flush();
      setId( PersistenceProvider.instance().getId( getInstance(), getEntityManager() ) );
      facesMessages.add( getCreatedMessage() );
      return "persisted";
   }

   @Transactional
   public String remove()
   {
      getEntityManager().joinTransaction();
      getEntityManager().remove( getInstance() );
      getEntityManager().flush();
      facesMessages.add( getDeletedMessage() );
      return "removed";
   }
   
   @Transactional
   @Override
   public E find()
   {
      getEntityManager().joinTransaction();
      E result = getEntityManager().find( getEntityClass(), getId() );
      if (result==null) result = handleNotFound();
      return result;
   }

   public EntityManager getEntityManager()
   {
      if (entityManager==null)
      {
         entityManager = (EntityManager) Component.getInstance("entityManager");
      }
      return entityManager;
   }

   public void setEntityManager(EntityManager entityManager)
   {
      this.entityManager = entityManager;
   }

}
