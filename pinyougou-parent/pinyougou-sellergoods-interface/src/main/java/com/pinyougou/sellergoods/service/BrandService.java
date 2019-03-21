package com.pinyougou.sellergoods.service;

import com.pinyougou.pojo.TbBrand;
import entity.PageResult;

import java.util.List;
import java.util.Map;

public interface BrandService {
    //查询所有，不分页
    public List<TbBrand> findAll();
    //查询所有，分页
    public PageResult findPage(int pageNum, int pageSize);
    //添加
    public void add(TbBrand brand);
    //修改，查询一条
    public TbBrand findOne(long id);
    //修改
    public void update(TbBrand brand);
    //根据id批量删除brand
    public void delete(Long[] ids);
    //根据条件查询所有，分页
    public PageResult findPage(TbBrand tbBrand, int pageNum, int pageSize);
    //获取品牌下拉列表，返回值是map集合
    public List<Map> brandOptionList();

}
