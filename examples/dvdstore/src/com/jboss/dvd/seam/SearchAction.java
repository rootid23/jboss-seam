/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */ 
package com.jboss.dvd.seam;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Interceptor;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Conversational;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.datamodel.DataModel;

import org.jboss.seam.ejb.SeamInterceptor;

@Stateful
@Name("search")
@LoggedIn
@Conversational(ifNotBegunOutcome="browse")
@Interceptor(SeamInterceptor.class)
public class SearchAction
    implements Search,
               Serializable
{
    @In(create=true)
    ShoppingCart cart;

    @PersistenceContext(unitName="dvd")
    EntityManager em;

    int     pageSize    = 10;
    int     currentPage = 0; 
    boolean hasMore     = false;

    Integer category = new Integer(0);
    String  title    = null;
    String  actor    = null;

    public void setCategory(Integer category) {
        this.category = category ; 
    }
    public Integer getCategory() {
        return category;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }
    public String getActor() {
        return actor;
    }

    List<SelectableItem<Product>> searchResults;
    public List getSearchResults() {
        return searchResults;
    }


    List<Category>      categories;
    Map<String,Integer> categoryMap;


    public SearchAction() {
    }

    @Begin
    public String doSearch() {
        System.out.println("search.doSearch! -> browse");
        currentPage=0;
        updateResults();
        return "browse";
    }

    public boolean getHasResults() {
        return (searchResults != null) && (searchResults.size()>0);
    }

    public Map<String,Integer> getCategories() {
        if (categories == null) {
            categories = em.createQuery("from Category c").getResultList();

            Map<String,Integer> results = new HashMap<String, Integer>();
            
            results.put("Any", 0);
            for (Category category: categories) {
                results.put(category.getCategoryName(),category.getCategory());
            }
            categoryMap = results;
        }
        
        return categoryMap;
    }

    private Category categoryForNum(int value) {
        getCategories(); 
        if (categories != null) {
            for (Category category: categories) {
                if (category.getCategory() == value) {
                    return category;
                }
            }
        }
        return null;
    }
    
    public String nextPage() {
        if (!isLastPage()) {
            currentPage++;
            updateResults();
        }
        return null;
    }

    public String prevPage() {
        if (!isFirstPage()) {
            currentPage--;
            updateResults();
        }
        return null;
    }

    public boolean isLastPage() {
        return (searchResults != null) && !hasMore;
    }
    public boolean isFirstPage() {
        return (searchResults != null) && (currentPage == 0);
    }

    private void updateResults() {
        List<SelectableItem<Product>> items = new ArrayList<SelectableItem<Product>>();

        List<Product> products = searchQuery(getTitle(),
                                             getActor(),
                                             categoryForNum(getCategory()))
            .setMaxResults(pageSize+1)
            .setFirstResult(pageSize*currentPage)
            .getResultList();
        
        for (Product product: products) {
            items.add(new SelectableItem(product));
        }

        if (items.size() > pageSize) { 
            searchResults = new ArrayList(items.subList(0,pageSize));
            hasMore = true;
        } else {
            searchResults = items;
            hasMore = false;
        }
    }


    private Query searchQuery(String title, String actor, Category category) {
        title = (title == null) ? "%" : "%" + title.toLowerCase() + "%";
        actor = (actor == null) ? "%" : "%" + actor.toLowerCase() + "%";

        System.out.println("XCAT: " + category);

        if (category == null) {
            return em.createQuery("from Product p where lower(p.title) LIKE :title " + 
                                  "and lower(p.actor) LIKE :actor")
                .setParameter("title", title)
                .setParameter("actor", actor);
        } else { 
            return em.createQuery("from Product p where lower(p.title) LIKE :title " + 
                                  "and lower(p.actor) LIKE :actor " + 
                                  "and p.category = :category")
                .setParameter("title", title)
                .setParameter("actor", actor)
                .setParameter("category", category);
        }
    }



    public String addToCart() {
        System.out.println("ADD TO CART: " + cart);
        for (SelectableItem<Product> item: searchResults) {
            if (item.getSelected()) {
                item.setSelected(false);


                System.out.println("ADDING: " + item.getItem());
                cart.addProduct(item.getItem(), 1);
            }
        }

        System.out.println("!!");
        return null;
    }


    @End
    public String reset() {
        return null;
    }

    @Destroy 
    @Remove
    public void destroy() {
    }
}
