
//定义前端controller
app.controller('brandController',function($scope,$http,brandService,$controller) {
    $controller('baseController',{$scope:$scope});
    $scope.findAll = function () {
        brandService.findAll().success(
            function (response) {
                $scope.list = response;
            }
        );
    }



    //分页
    $scope.findPage=function(page,size){
        brandService.findPage(page,size).success(
            function(response){
                $scope.list=response.rows;//显示当前页数据
                $scope.paginationConf.totalItems=response.total;//更新总记录数
            }
        );
    }

    //保存品牌，可新增，可修改
    $scope.save=function () {

        var object = null;
        //如果有id,说明是修改操作
        if($scope.entity.id != null){
            object = brandService.update($scope.entity);
        }else{
            object = brandService.add($scope.entity);
        }
        //根据methodName来决定执行add还是update操作
        object.success(
            function (response) {
                if(response.success){
                    $scope.reloadList();
                }else{
                    alert(response.message);
                }

            });
    }

    //查询修改项
    $scope.findOne=function (id) {
        brandService.findOne(id).success(
            function (response) {
                $scope.entity=response;

            });
    }



    //批量删除
    $scope.dele=function () {
         if(confirm("你确定删除么？")){
             brandService.dele($scope.selectIds).success(
                 function (response) {
                     if(response.success){
                         $scope.reloadList();
                     }else{
                         alert(response.message);
                     }
                 }
             );
         }
    }

    //条件查询
    $scope.searchEntity = {};
    $scope.search=function (page,size) {
        brandService.search(page,size,$scope.searchEntity).success(
            function(response){
                $scope.list=response.rows;//显示当前页数据
                $scope.paginationConf.totalItems=response.total;//更新总记录数
            }
        );
    }


});

