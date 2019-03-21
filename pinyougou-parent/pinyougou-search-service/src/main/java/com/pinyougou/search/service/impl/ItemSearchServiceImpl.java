package com.pinyougou.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.omg.PortableInterceptor.INACTIVE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(timeout = 5000) //设置超时。默认1秒。
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private SolrTemplate solrTemplate;

    //后台查询入口
    @Override
    public Map search(Map searchMap) {
        String keywords = (String)searchMap.get("keywords");
        searchMap.put("keywords",keywords.replace(" ",""));
        Map<String, Object> map = new HashMap<>();
        //1.查询列表
        map.putAll(searchList(searchMap));
        //2.查询分类列表
        List<String> categoryList = searchCategoryList(searchMap);
        map.put("categoryList", categoryList);
        //3.查询品牌和规格列表
        String category = (String) searchMap.get("category");
        if(category.equals("")){
            //客户未选分类，默认显示分类1的品牌和规格
            if (categoryList.size() > 0) {
                map.putAll(searchBrandAndSpecList(categoryList.get(0)));
            }
        }else{
            //客户选择了分类，按客户选择的分类显示品牌和规格
            map.putAll(searchBrandAndSpecList(category));
        }


        return map;
    }

    private Map searchList(Map searchMap) {

        Map map = new HashMap();
        //高亮选项设置
        HighlightQuery query = new SimpleHighlightQuery();
        HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");//设置高亮的域
        highlightOptions.setSimplePrefix("<em style='color:red'>");//高亮前缀
        highlightOptions.setSimplePostfix("</em>");//高亮后
        query.setHighlightOptions(highlightOptions);//设置高亮选项//按照关键字查询
        //1.1按照关键字查询
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);

        //1.2按照商品分类过滤
        if(! "".equals(searchMap.get("category"))){

            FilterQuery filterQuery = new SimpleFilterQuery();
            Criteria criteriaCategory = new Criteria("item_category").is(searchMap.get("category"));
            filterQuery.addCriteria(criteriaCategory);
            query.addFilterQuery(filterQuery);
        }


        //1.3按照品牌过滤
        if(! "".equals(searchMap.get("brand"))){
            FilterQuery filterQuery = new SimpleFilterQuery();
            Criteria criteriaBrand = new Criteria("item_brand").is(searchMap.get("brand"));
            filterQuery.addCriteria(criteriaBrand);
            query.addFilterQuery(filterQuery);
        }

        //1.4按照规格过滤,规格有多项，需要循环
        if(searchMap.get("spec") != null){
            Map<String,String> specMap =  (Map)searchMap.get("spec");
            for (String key : specMap.keySet()) {
                FilterQuery filterQuery = new SimpleFilterQuery();
                Criteria criteriaSpec = new Criteria("item_spec_"+key).is(specMap.get(key));
                filterQuery.addCriteria(criteriaSpec);
                query.addFilterQuery(filterQuery);
            }

        }
        //1.5按照价格区间过滤
        if(! "".equals(searchMap.get("price"))){//参数字符串形式传递，“500-1000”
            String priceStr = (String)searchMap.get("price");
            String[] price = priceStr.split("-");
            if(!"0".equals(price[0])){  //价格下限
                FilterQuery filterQuery = new SimpleFilterQuery();
                Criteria criteriaBrand = new Criteria("item_price").greaterThanEqual(price[0]);
                filterQuery.addCriteria(criteriaBrand);
                query.addFilterQuery(filterQuery);
            }
            if(!"*".equals(price[1])){  //价格上限，*不执行，表示没有上限
                FilterQuery filterQuery = new SimpleFilterQuery();
                Criteria criteriaBrand = new Criteria("item_price").lessThanEqual(price[1]);
                filterQuery.addCriteria(criteriaBrand);
                query.addFilterQuery(filterQuery);
            }

        }

        //1.6
        Integer pageNo = (Integer)searchMap.get("pageNo");
        if(pageNo == null){
            pageNo = 1;    //设置当前页默认值
        }
        Integer pageSize = (Integer)searchMap.get("pageSize");
        if(pageSize == null){
            pageSize = 20;  //设置每页记录数默认值
        }
        query.setOffset((pageNo - 1)*pageSize);//获取起始索引
        query.setRows(pageSize);//设置每页显示记录条数


        //获取高亮结果集
        HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
        for (HighlightEntry<TbItem> h : page.getHighlighted()) {    //循环高亮入口集合
            TbItem item = h.getEntity();//获取原实体类
            if (h.getHighlights().size() > 0 && h.getHighlights().get(0).getSnipplets().size() > 0) {
                item.setTitle(h.getHighlights().get(0).getSnipplets().get(0));//设置高亮的结果
            }
        }
        map.put("rows", page.getContent());         //返回当前页的记录
        map.put("totalPages",page.getTotalPages()); //返回总页数
        map.put("total",page.getTotalElements());   //返回总记录数
        return map;
    }

    //按照分类，分组查询
    public List<String> searchCategoryList(Map searchMap) {
        List list = new ArrayList();
        Query query = new SimpleQuery();
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);

        GroupOptions groupOption = new GroupOptions().addGroupByField("item_category");
        query.setGroupOptions(groupOption);

        GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
        GroupResult<TbItem> groupResult = page.getGroupResult("item_category");
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
        List<GroupEntry<TbItem>> entryList = groupEntries.getContent();
        for (GroupEntry<TbItem> entry : entryList) {
            list.add(entry.getGroupValue());
        }
        return list;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    //从缓存中读取品牌和规格列表
    private Map searchBrandAndSpecList(String category) {
        Map map = new HashMap();
        Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
        if (typeId != null) {
            List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
            map.put("brandList", brandList);
            List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
            map.put("specList", specList);
        }
        return map;
    }


}


