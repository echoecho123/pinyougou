 //控制层 
app.controller('goodsController' ,function($scope,$controller,$location ,goodsService,uploadService,itemCatService,typeTemplateService){
	
	$controller('baseController',{$scope:$scope});//继承
	
    //读取列表数据绑定到表单中  
	$scope.findAll=function(){
		goodsService.findAll().success(
			function(response){
				$scope.list=response;
			}			
		);
	}    
	
	//分页
	$scope.findPage=function(page,rows){			
		goodsService.findPage(page,rows).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}
	
	//查询实体 
	$scope.findOne=function(){
		var id = $location.search()['id'];
		if(id == null) {
            //说明是新增商品
            return;
        }
        //修改商品
		goodsService.findOne(id).success(
			function(response){
				$scope.entity= response;
				//读取富文本编辑器
				editor.html($scope.entity.goodsDesc.introduction);
				//读取图片列表
                $scope.entity.goodsDesc.itemImages = JSON.parse($scope.entity.goodsDesc.itemImages);
                //读取扩展属性
				$scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.entity.goodsDesc.customAttributeItems);
				//读取规格
				$scope.entity.goodsDesc.specificationItems = JSON.parse($scope.entity.goodsDesc.specificationItems);
				//转换SKU列表中的规格
                for(var i=0;i<$scope.entity.itemList.length;i++){
                    $scope.entity.itemList[i].spec = JSON.parse($scope.entity.itemList[i].spec);
				}
			}
		);				
	}
	
	//保存 
	$scope.save=function(){
        $scope.entity.goodsDesc.introduction = editor.html();
        var serviceObject;//服务层对象
        if($scope.entity.goods.id != null ){//如果有ID
            serviceObject=goodsService.update( $scope.entity ); //修改
        }else{
            serviceObject=goodsService.add( $scope.entity  );//增加
        }
        serviceObject.success(
            function(response){
                if(response.success){

                    alert("保存成功");
                    location.href="goods.html";
                    /*$scope.entity = {};
                    editor.html("");//重新加载*/
                }else{
                    alert(response.message);
                }
            }
        );


	}

	 
	//批量删除 
	$scope.dele=function(){			
		//获取选中的复选框			
		goodsService.dele( $scope.selectIds ).success(
			function(response){
				if(response.success){
					$scope.reloadList();//刷新列表
					$scope.selectIds=[];
				}						
			}		
		);				
	}
	
	$scope.searchEntity={};//定义搜索对象 
	
	//搜索
	$scope.search=function(page,rows){			
		goodsService.search(page,rows,$scope.searchEntity).success(
			function(response){
				$scope.list=response.rows;	
				$scope.paginationConf.totalItems=response.total;//更新总记录数
			}			
		);
	}

	//上传图片
	$scope.uploadFile=function () {
		uploadService.uploadFile().success(
			function (response) {
				if(response.success){
					$scope.imageEntity.url=response.message;
				}else{
					alert(response.message);
				}
            }
		);
    }

    $scope.entity={goodsDesc:{itemImages:[],specificationItems:[]}};
    $scope.addImageRow=function () {
        $scope.entity.goodsDesc.itemImages.push($scope.imageEntity);
    }

    $scope.removeImageRow = function (index) {
        $scope.entity.goodsDesc.itemImages.splice(index,1);
    }

    //查询一级分类目录
	$scope.selectItemCatList1=function () {
		itemCatService.findByParentId(0).success(
			function (response) {
				$scope.selectList1 = response;

            }
		)
    }

    $scope.$watch('entity.goods.category1Id',function (newValue,oldValue) {

        itemCatService.findByParentId(newValue).success(
            function (response) {
                $scope.selectList2 = response;

            }
        )
    })

    $scope.$watch('entity.goods.category2Id',function (newValue,oldValue) {

        itemCatService.findByParentId(newValue).success(
            function (response) {
                $scope.selectList3 = response;

            }
        )
    })

    $scope.$watch('entity.goods.category3Id',function (newValue,oldValue) {

        itemCatService.findOne(newValue).success(
            function (response) {
                $scope.entity.goods.typeTemplateId = response.typeId;

            }
        )
    });
    $scope.$watch('entity.goods.typeTemplateId',function (newValue,oldValue) {

        typeTemplateService.findOne(newValue).success(
            function (response) {
                $scope.typeTemplate = response;
                $scope.typeTemplate.brandIds = JSON.parse($scope.typeTemplate.brandIds);
                if($location.search()['id'] == null){
                    $scope.entity.goodsDesc.customAttributeItems = JSON.parse($scope.typeTemplate.customAttributeItems);
				}


            }
        );

        typeTemplateService.findSpecList(newValue).success(
        	function (response) {
				$scope.specList=response;
            }
		);
    });

    //勾选后，添加到规格列表
	$scope.updateSpecList=function ($event,name,value) {
		var object = $scope.findObjectBykey($scope.entity.goodsDesc.specificationItems,'attributeName',name);
		if(object != null){
			//选中，则添加
			if($event.target.checked){
				object.attributeValue.push(value);
			}else{
				//未选中，则移除
				object.attributeValue.splice(object.attributeValue.indexOf(value),1);
				//已空，则全部清除
				if(object.attributeValue.length == 0){
                    $scope.entity.goodsDesc.specificationItems.splice($scope.entity.goodsDesc.specificationItems.indexOf(object),1);
				}
			}
		}else{
			//直接添加
            $scope.entity.goodsDesc.specificationItems.push({"attributeName":name,"attributeValue":[value]});
		}

    }

    //根据规格列表，自动创建itemList表格
	$scope.createItemList=function () {
		var items = $scope.entity.goodsDesc.specificationItems;
		$scope.entity.itemList = [{spec:{},price:0,num:99999,status:'0',isDefault:'0' } ];
        for(var i = 0;i<items.length;i++){
            $scope.entity.itemList = addColumn($scope.entity.itemList,items[i].attributeName,items[i].attributeValue);
		}

    }



    addColumn = function (list,columnName,columnValues) {
		var newList = [];
        for(var i = 0;i<list.length;i++){
			var oldRow = list[i];
			for(var j = 0;j<columnValues.length;j++){
				var newRow = JSON.parse(JSON.stringify(oldRow));
				newRow.spec[columnName]=columnValues[j];
                newList.push(newRow);
			}
        }
		return newList;
    };


	$scope.statusList = ['未审核','审核通过','审核未通过','关闭'];
	//根据分类id,获取分类名称
    $scope.itemCatList = [];
	$scope.getItemCatList= function () {
		itemCatService.findAll().success(
			function (response) {
                for(var i = 0;i<response.length;i++){
                    $scope.itemCatList[response[i].id]=response[i].name;
				}
            }
		)
    };

	//回显时检查是否勾选
	$scope.checkAttributeValue=function (specName,optionName) {
		var obj = $scope.findObjectBykey($scope.entity.goodsDesc.specificationItems,'attributeName',specName);
		if(obj == null){
			return false;
		}else{
			if(obj.attributeValue.indexOf(optionName) >= 0){
				return true;
			}else{
				return false;
			}
		}

    }

    //商品上架
	$scope.updateMarketable=function (status) {
	if(confirm("确定要上架么？")){
        goodsService.updateMarketable($scope.selectIds,status).success(
            function (response) {
                if(response.success){
                    $scope.reloadList();
                    $scope.selectIds = [];
                }else{
                    alert(response.message);
                }
            }
        )
	}

    }
});	
