package com.pinyougou.sellergoods.service.impl;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.*;
import com.pinyougou.pojo.*;
import com.pinyougou.pojogroup.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.pojo.TbGoodsExample.Criteria;
import com.pinyougou.sellergoods.service.GoodsService;

import entity.PageResult;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
public class GoodsServiceImpl implements GoodsService {

	@Autowired
	private TbGoodsMapper goodsMapper;

	@Autowired
	private TbGoodsDescMapper goodsDescMapper;
	/**
	 * 查询全部
	 */
	@Override
	public List<TbGoods> findAll() {
		return goodsMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbGoods> page=   (Page<TbGoods>) goodsMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(Goods goods) {
		TbGoods tbGoods = goods.getGoods();
		tbGoods.setAuditStatus("0");//设置未审核
		tbGoods.setIsMarketable("0");//设置默认上架
		goodsMapper.insert(tbGoods);
		TbGoodsDesc goodsDesc = goods.getGoodsDesc();
		goodsDesc.setGoodsId(tbGoods.getId());
		goodsDescMapper.insert(goodsDesc);
		//添加规格列表
		addItemList(goods);
	}


	@Autowired
	private TbBrandMapper brandMapper;
	@Autowired
	private TbItemCatMapper itemCatMapper;
	@Autowired
	private TbSellerMapper sellerMapper;

	public void setTbItem(Goods goods,TbItem tbItem){
		tbItem.setCategoryid(goods.getGoods().getCategory3Id());
		tbItem.setCreateTime(new Date());
		tbItem.setUpdateTime(new Date());
		tbItem.setGoodsId(goods.getGoods().getId());
		tbItem.setSellerId(goods.getGoods().getSellerId());
		//品牌名称
		TbBrand tbBrand = brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId());
		tbItem.setBrand(tbBrand.getName());
		//分类名称
		TbItemCat tbItemCat = itemCatMapper.selectByPrimaryKey(goods.getGoods().getCategory3Id());
		tbItem.setCategory(tbItemCat.getName());
		//商家店铺名称
		TbSeller tbSeller = sellerMapper.selectByPrimaryKey(goods.getGoods().getSellerId());
		tbItem.setSeller(tbSeller.getNickName());
		//图片
		List<Map> maps = JSON.parseArray(goods.getGoodsDesc().getItemImages(), Map.class);
		if(maps.size() > 0){
			tbItem.setImage((String)maps.get(0).get("url"));
		}
	}

	public void addItemList(Goods goods){
		//添加规格列表

		if("1".equals(goods.getGoods().getIsEnableSpec())){
			//启用规格
			//添加商品sku列表
			List<TbItem> itemList = goods.getItemList();
			for (TbItem tbItem : itemList) {
				//构建title,goodsName+规格选项
				String title = goods.getGoods().getGoodsName();
				Map<String ,Object> map = JSON.parseObject(tbItem.getSpec());
				for (String key : map.keySet()) {
					title += " " + map.get(key);
				}
				tbItem.setTitle(title);
				setTbItem(goods,tbItem);
				itemMapper.insert(tbItem);

			}

		}else{
			//不启用规格，只插入单条sku
			TbItem tbItem = new TbItem();
			tbItem.setTitle(goods.getGoods().getGoodsName());
			tbItem.setPrice(goods.getGoods().getPrice());
			tbItem.setNum(999);
			tbItem.setStatus("1");
			tbItem.setIsDefault("1");
			tbItem.setSpec("{}");
			setTbItem(goods,tbItem);
			itemMapper.insert(tbItem);
		}
	}



	/**
	 * 修改
	 */
	@Override
	public void update(Goods goods){
        //更新的时候要重新设置状态
		TbGoods tbGoods = goods.getGoods();
		tbGoods.setAuditStatus("0");
		goodsMapper.updateByPrimaryKey(tbGoods);
		TbGoodsDesc goodsDesc = goods.getGoodsDesc();
		goodsDescMapper.updateByPrimaryKey(goodsDesc);
		//itemList先删除再添加
		TbItemExample example = new TbItemExample();
		TbItemExample.Criteria criteria = example.createCriteria();
		criteria.andGoodsIdEqualTo(goods.getGoods().getId());
		itemMapper.deleteByExample(example);
		//再添加
		addItemList(goods);

	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */

	@Autowired
	private TbItemMapper itemMapper;
	@Override
	public Goods findOne(Long id){

		Goods goods = new Goods();
		TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
		goods.setGoods(tbGoods);
		TbGoodsDesc tbGoodsDesc = goodsDescMapper.selectByPrimaryKey(id);
		goods.setGoodsDesc(tbGoodsDesc);

		TbItemExample example = new TbItemExample();
		TbItemExample.Criteria criteria = example.createCriteria();
		criteria.andGoodsIdEqualTo(id);
		List<TbItem> tbItems = itemMapper.selectByExample(example);
		goods.setItemList(tbItems);
		return goods;
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {

		for(Long id:ids){
			TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
			tbGoods.setIsDelete("1");
			goodsMapper.updateByPrimaryKey(tbGoods);
		}
	}
	
	
		@Override
	public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbGoodsExample example=new TbGoodsExample();
		Criteria criteria = example.createCriteria();
		
		if(goods!=null){			
						if(goods.getSellerId()!=null && goods.getSellerId().length()>0){
				criteria.andSellerIdLike("%"+goods.getSellerId()+"%");
			}
			if(goods.getGoodsName()!=null && goods.getGoodsName().length()>0){
				criteria.andGoodsNameLike("%"+goods.getGoodsName()+"%");
			}
			if(goods.getAuditStatus()!=null && goods.getAuditStatus().length()>0){
				criteria.andAuditStatusLike("%"+goods.getAuditStatus()+"%");
			}
			if(goods.getIsMarketable()!=null && goods.getIsMarketable().length()>0){
				criteria.andIsMarketableLike("%"+goods.getIsMarketable()+"%");
			}
			if(goods.getCaption()!=null && goods.getCaption().length()>0){
				criteria.andCaptionLike("%"+goods.getCaption()+"%");
			}
			if(goods.getSmallPic()!=null && goods.getSmallPic().length()>0){
				criteria.andSmallPicLike("%"+goods.getSmallPic()+"%");
			}
			if(goods.getIsEnableSpec()!=null && goods.getIsEnableSpec().length()>0){
				criteria.andIsEnableSpecLike("%"+goods.getIsEnableSpec()+"%");
			}
			if(goods.getIsDelete()!=null && goods.getIsDelete().length()>0){
				criteria.andIsDeleteLike("%"+goods.getIsDelete()+"%");
			}
	
		}
		
		Page<TbGoods> page= (Page<TbGoods>)goodsMapper.selectByExample(example);		
		return new PageResult(page.getTotal(), page.getResult());
	}


	@Override
	public void updateStatus(Long[] ids, String status) {
		for (Long id : ids) {
			TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
			tbGoods.setAuditStatus(status);
			goodsMapper.updateByPrimaryKey(tbGoods);

		}
	}

	@Override
	public void updateMarketable(Long[] ids, String status) {
		for (Long id : ids) {
			TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
			tbGoods.setIsMarketable(status);
			goodsMapper.updateByPrimaryKey(tbGoods);
		}
	}
	
}
