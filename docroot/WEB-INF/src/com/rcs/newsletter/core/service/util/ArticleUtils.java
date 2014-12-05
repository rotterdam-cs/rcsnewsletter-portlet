package com.rcs.newsletter.core.service.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.BooleanQueryFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.model.AssetTag;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetTagLocalServiceUtil;
import com.liferay.portlet.asset.service.persistence.AssetEntryQuery;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalArticleResource;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalArticleResourceLocalServiceUtil;
import com.liferay.portlet.journalcontent.util.JournalContentUtil;
import com.rcs.newsletter.core.service.NewsletterCategoryService;
/**
 * General Article Utils
 * @author pablo
 */
public class ArticleUtils {

    private static Log log = LogFactoryUtil.getLog(ArticleUtils.class);

    @Autowired
    NewsletterCategoryService newsLetterCategoryService;
    
    
    static final Comparator<JournalArticle> ARTICLE_ORDER = new Comparator<JournalArticle>() {
        public int compare(JournalArticle a1, JournalArticle a2) {
            return a2.getModifiedDate().compareTo(a1.getModifiedDate());
        }
    };

 

    /**
     * Get articles by Tag
     * @param themeDisplay
     * @param tagName
     * @return
     * @throws PortalException
     * @throws SystemException
     */
    public static List<JournalArticle> findArticlesByTag(ThemeDisplay themeDisplay, String tagName) throws PortalException, SystemException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        List<JournalArticle> journalArticleList = new ArrayList<JournalArticle>();
        if(AssetTagLocalServiceUtil.hasTag(themeDisplay.getScopeGroupId(), tagName)) {
            AssetTag assetTagObj = AssetTagLocalServiceUtil.getTag(themeDisplay.getScopeGroupId(), tagName);
            long tagid = assetTagObj.getTagId();
            AssetEntryQuery assetEntryQuery = new AssetEntryQuery();
            assetEntryQuery.setClassName(JournalArticle.class.getName());//to get only journal articles
            long[] anyTagIds = {tagid};
            assetEntryQuery.setAnyTagIds(anyTagIds);
            List <AssetEntry> assetEntryList = AssetEntryLocalServiceUtil.getEntries(assetEntryQuery);
            if (assetEntryList.size() > 0) {
                for (AssetEntry ae : assetEntryList) {
                    JournalArticleResource journalArticleResourceObj = JournalArticleResourceLocalServiceUtil.getJournalArticleResource(ae.getClassPK());
                    //JournalArticle journalArticleObj = JournalArticleLocalServiceUtil.getLatestArticle(themeDisplay.getScopeGroupId(), journalArticleResourceObj.getArticleId());
                    JournalArticle journalArticleObj = JournalArticleLocalServiceUtil.getArticle(journalArticleResourceObj.getGroupId(), journalArticleResourceObj.getArticleId());
                    journalArticleList.add(journalArticleObj);
                }
            }
        }
        return journalArticleList;
    }


    /**
     * Remove the tag and category and change the type to rcs-general
     * @param tagName
     * @param typeName
     * @param categoryName
     * @return
     */
	public static void untagUncategoryUntypeById(long id, String tagName, String typeName, String categoryName) throws PortalException, SystemException, ClassNotFoundException, InstantiationException, IllegalAccessException {

    	
    	JournalArticle articleToDeleteTagType = null;
    	
        for (JournalArticle article : JournalArticleLocalServiceUtil.getArticles()) {   
            if (article.getArticleId().equals((""+id))) {
            	articleToDeleteTagType = JournalArticleLocalServiceUtil.getArticle(article.getGroupId(), article.getArticleId());
                break;
            }
        }
    	
    	AssetEntry entry = AssetEntryLocalServiceUtil.getEntry(articleToDeleteTagType.getGroupId(),articleToDeleteTagType.getArticleResourceUuid());

    	//remove tag
    	List<AssetTag> tagsUnmodifiable;
    	tagsUnmodifiable = entry.getTags();
    	List<String> tags = Collections.emptyList();
   	
    	for(AssetTag tag : tagsUnmodifiable){
    		if(! tag.getName().equals(tagName)){
    			tags.add(tag.getName());
    			
    		}else{
    			log.info("Tag removed");
    		}
    	}

    	//Remove Category
    	long categoyId = findCategoryIdByName(categoryName);
    	long[] categoryIdsArray = entry.getCategoryIds();
    	List<Long> categoryIdsList = new ArrayList<Long>();

    	for(long catId : categoryIdsArray){
    		if(catId != categoyId){
    			categoryIdsList.add(new Long(catId));
    			log.info("Category added: "+ catId + " - ");
    		}else{
    			log.info("Category removed");
    		}
    	}

    	// Update Tag and Category
    	try{
	    	JournalArticleLocalServiceUtil.updateAsset(articleToDeleteTagType.getUserId(), articleToDeleteTagType, (long [])ArrayUtils.toPrimitive(categoryIdsList.toArray(new Long [categoryIdsList.size()])), tags.toArray(new String[tags.size()]),new long [0]);
	    	log.info("Tag and categories Updated");
    	}catch(Exception e){
    		log.error("Tag and Categories don't updated - error: "+e);
    	}
    	
    	//Remove Type
    	if(articleToDeleteTagType.getType().equals(typeName)){
    		articleToDeleteTagType.setType("rcs-general");
    		log.info("Type removed");
    	}
    	
    	// Update Type
    	JournalArticleLocalServiceUtil.updateJournalArticle(articleToDeleteTagType);
    	log.info("Type Updated");

    	return;       
    }
    

    
    
    
    /**
     * Search Articles by Keyword
     * @param themeDisplay
     * @param tagName
     * @return
     * @throws PortalException
     * @throws SystemException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @SuppressWarnings("deprecation")
	public static List<JournalArticle> findArticlesByKeyword(ThemeDisplay themeDisplay, String tagName, SearchContext searchContext) throws PortalException, SystemException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        List<JournalArticle> journalArticleList = new ArrayList<JournalArticle>();
        Long companyId = themeDisplay.getCompanyId();        
        BooleanQuery searchQuery = BooleanQueryFactoryUtil.create(searchContext);
        String keywords = tagName;
        if (Validator.isNotNull(keywords)) {
            keywords = keywords.trim();
            searchQuery.addTerm(Field.TITLE, keywords);
            searchQuery.addTerm(Field.CONTENT, keywords);
            searchQuery.addTerm(Field.NAME, keywords,true);
            searchQuery.addTerm(Field.DESCRIPTION, keywords);
            searchQuery.addTerm(Field.PROPERTIES, keywords);
            searchQuery.addTerm(Field.USER_NAME, keywords);
            searchQuery.addTerm(Field.ASSET_TAG_NAMES, keywords, true);
            searchQuery.addTerm("expando/custom_fields/Areasofexpertise",keywords,true);
            searchQuery.addTerm("expando/custom_fields/Areasofinterest",keywords,true);
            searchQuery.addTerm(Field.URL, keywords);
            searchQuery.addTerm(Field.USER_ID, keywords);
            searchQuery.addTerm("screenName", keywords);
            searchQuery.addTerm("middleName", keywords);
            searchQuery.addTerm("firstName", keywords);
            searchQuery.addTerm("lastName", keywords);
            searchQuery.addTerm("screenName", keywords,true);
            searchQuery.addTerm("middleName", keywords,true);
            searchQuery.addTerm("firstName", keywords,true);
            searchQuery.addTerm("lastName", keywords,true);
        }
        BooleanQuery fullQuery = BooleanQueryFactoryUtil.create(searchContext);
        if (searchQuery.clauses().size() > 0) {
            fullQuery.add(searchQuery, BooleanClauseOccur.MUST);
        }        
        Hits hits = SearchEngineUtil.search(companyId, searchQuery, -1, -1);

        if (hits != null) {
            List<Document> documents = hits.toList();
            for (Document document : documents) {
                //Check if is a Journal Article
                if (JournalArticleLocalServiceUtil.hasArticle(themeDisplay.getScopeGroupId(), document.get(Field.ENTRY_CLASS_PK))) {
                    JournalArticle journalArticleObj = JournalArticleLocalServiceUtil.getArticle(themeDisplay.getScopeGroupId(), document.get(Field.ENTRY_CLASS_PK));
                    journalArticleList.add(journalArticleObj);
                }
            }
        }
        return journalArticleList;
    }

    /**
     * Get the most Recent Article
     * @param journalArticleList
     * @return
     */
    public static JournalArticle getRecentArticle(List <JournalArticle>journalArticleList) {
        JournalArticle result = null;
        if (journalArticleList.size() > 1) {
            Collections.sort(journalArticleList, ARTICLE_ORDER);            
        }
        if (journalArticleList.size() > 0) {
            result = journalArticleList.get(0);
        }
        return result;
    }

    /**
     * Get recent articles
     * @param journalService
     * @param quantity
     * @return
     * @throws PortalException
     * @throws SystemException
     */
    public static List<JournalArticle> getLastArticles(int quantity) throws PortalException, SystemException {
        List<JournalArticle> journalArticleList = new ArrayList<JournalArticle>();
        try {
            List<String> journalArticlesIds =  new ArrayList<String>();
            for (JournalArticle article : JournalArticleLocalServiceUtil.getJournalArticles(-1, -1)) {
                 if (JournalArticleLocalServiceUtil.hasArticle(article.getGroupId(), article.getArticleId())) {
                    if (!journalArticlesIds.contains(article.getArticleId())) {
                    	JournalArticle a = JournalArticleLocalServiceUtil.getArticle(article.getGroupId(), article.getArticleId());
                        
                    	//JournalArticle a = JournalArticleLocalServiceUtil.getLatestArticle(article.getGroupId(), article.getArticleId());
                        journalArticleList.add(a);
                        journalArticlesIds.add(article.getArticleId());
                    }
                }
            }
        } catch (PortalException ex) {
            log.error(ex);
        }
        if (journalArticleList.size() > 1) {
            Collections.sort(journalArticleList, ARTICLE_ORDER);
        }
        if (journalArticleList.size() > quantity) {
            journalArticleList = journalArticleList.subList(0, quantity);
        }
        return journalArticleList;
    }

    /**
     * Get articles by authorId
     * @param journalService
     * @param userId
     * @return
     * @throws PortalException
     * @throws SystemException
     */
    public static List<JournalArticle> findArticlesByAuthorId(Long userId) throws PortalException, SystemException {
        List<JournalArticle> journalArticleList = new ArrayList<JournalArticle>();
        try {
            for (JournalArticle article : JournalArticleLocalServiceUtil.getArticles()) {                
                if (article.getUserId() == userId) {
                	JournalArticle a = JournalArticleLocalServiceUtil.getArticle(article.getGroupId(), article.getArticleId());
                    
                    //JournalArticle a = JournalArticleLocalServiceUtil.getLatestArticle(article.getGroupId(), article.getArticleId());
                    journalArticleList.add(a);
                }
            }
        } catch (SystemException ex) {
            log.error(ex);
        }
        return journalArticleList;
    }

    /**
     * Get Articles by Type
     * @param journalService
     * @param type
     * @return
     * @throws PortalException
     * @throws SystemException
     */
    public static List<JournalArticle> findArticlesByType(String type) throws PortalException, SystemException {
        List<JournalArticle> journalArticleList = new ArrayList<JournalArticle>();
        try {
            for (JournalArticle article : JournalArticleLocalServiceUtil.getArticles()) {
            	JournalArticle a = JournalArticleLocalServiceUtil.getArticle(article.getGroupId(), article.getArticleId());
                
            	//JournalArticle a = JournalArticleLocalServiceUtil.getLatestArticle(article.getGroupId(), article.getArticleId());
                if (a.getType().equalsIgnoreCase(type)) {
                    journalArticleList.add(a);
                }
            }
        } catch (SystemException ex) {
            log.error(ex);
        }
        return journalArticleList;
    }


    /**
     * Get Article Content
     * @param journalContentService
     * @param article
     * @param locale
     * @return
     * @throws Exception
     */
    public static String getArticleContent(JournalArticle article, String locale) throws Exception {
        try {
            String articleMultiLanguageXMLContent = article.getContent();
            String localizedContent = JournalContentUtil.getContent(article.getGroupId(), article.getArticleId(), null, locale, articleMultiLanguageXMLContent);
            return localizedContent;
        } catch (Exception ex) {
            log.error(ex);
            return "";
        }
    }
    
     
    /**
     * @@Not Used
     * Get Articles by Category
     * @param themeDisplay
     * @param category
     * @return
     * @throws PortalException
     * @throws SystemException
     */
    public static List<JournalArticle> findArticlesByCategory(String category) throws PortalException, SystemException {
        List<JournalArticle> journalArticleList = new ArrayList<JournalArticle>();
        AssetEntryQuery assetEntryQuery = new AssetEntryQuery();
        assetEntryQuery.setClassName(JournalArticle.class.getName());//to get only journal articles
        long[] anyCategoryIds = {findCategoryIdByName(category)};
        assetEntryQuery.setAnyCategoryIds(anyCategoryIds);
        List <AssetEntry> assetEntryList = AssetEntryLocalServiceUtil.getEntries(assetEntryQuery);        
        if (assetEntryList.size() > 0) {
            for (AssetEntry ae : assetEntryList) {
                JournalArticleResource journalArticleResourceObj = JournalArticleResourceLocalServiceUtil.getJournalArticleResource(ae.getClassPK());
                
            	//JournalArticleResource journalArticleResourceObj = JournalArticleResourceLocalServiceUtil.getJournalArticleResource(ae.getClassPK());
                JournalArticle journalArticleObj = JournalArticleLocalServiceUtil.getLatestArticle(journalArticleResourceObj.getGroupId(), journalArticleResourceObj.getArticleId());
                if (journalArticleObj != null) {
                    journalArticleList.add(journalArticleObj);
                }
            }
        }
        return journalArticleList;
    }

    /**
     * @@Not Used
     * Get Category ID By Name
     * @param articleName
     * @return
     * @throws SystemException
     */
    public static long findCategoryIdByName(String categoryName) throws SystemException {
        long result = 0;
        try {
            List <AssetCategory> assetCategories = AssetCategoryLocalServiceUtil.getCategories();
            for (AssetCategory assetCategory : assetCategories) {
                if (assetCategory.getName().equalsIgnoreCase(categoryName)) {
                    result = assetCategory.getCategoryId();
                }
            }
        } catch(SystemException ex) {
            log.error(ex);
        }
        return result;
    }
}