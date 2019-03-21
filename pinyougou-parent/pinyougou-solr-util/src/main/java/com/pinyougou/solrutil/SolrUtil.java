package com.pinyougou.solrutil;

import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;


@Component
public class SolrUtil {

    @Autowired
    private TbItemMapper itemMapper;
    @Autowired
    private SolrTemplate solrTemplate;

    public void importItemData(){
        TbItemExample example = new TbItemExample();
        TbItemExample.Criteria criteria = example.createCriteria();
        criteria.andStatusEqualTo("1");
        List<TbItem> list = itemMapper.selectByExample(example);
        System.out.println("商品列表");
        for (TbItem tbItem : list) {
            System.out.println(tbItem.getId()+" "+tbItem.getTitle());
            String spec = tbItem.getSpec();//从数据库中提取的json字符串{}
            Map<String,String> specMap = JSON.parseObject(spec, Map.class);//装换为Map对象
            tbItem.setSpecMap(specMap);
        }

        solrTemplate.saveBeans(list);
        solrTemplate.commit();

    }

    public static void main(String[] args) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath*:spring/applicationContext*.xml");
        SolrUtil solrUtil = (SolrUtil)applicationContext.getBean("solrUtil");
        solrUtil.importItemData();


    }


}
