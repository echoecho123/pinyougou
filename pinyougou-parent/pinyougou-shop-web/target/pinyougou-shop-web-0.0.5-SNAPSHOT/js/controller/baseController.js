app.controller('baseController',function ($scope) {
    /*分页控件配置*/
    $scope.paginationConf = {
        currentPage: 1,
        totalItems: 10,
        itemsPerPage: 10,
        perPageOptions: [10, 20, 30, 40, 50],
        onChange: function () {
            $scope.reloadList();//重新加载
        }
    };

    //刷新列表
    $scope.reloadList=function () {
        $scope.search($scope.paginationConf.currentPage,$scope.paginationConf.itemsPerPage);
    }

    $scope.searchEntity={};//定义搜索对象
    //用户选中id数组
    $scope.selectIds = [ ];
    $scope.updateSelectionIds=function (id,$event) {
        if($event.target.checked){
            $scope.selectIds.push(id);
        }else{
            var index = $scope.selectIds.indexOf(id);
            $scope.selectIds.splice(index,1);
        }

    }
    //搜索框中条件初始化
    $scope.searchEntity = {};

    $scope.json2String=function (str,key) {
        var json= JSON.parse(str);
        var value= "";
        for(var i = 0;i<json.length;i++){
            if(i>0){
                value+=",";
            }
            value+=json[i][key];
        }
        return value;
    }
    //从集合中按照key查询对象
    $scope.searchObjectByKey=function (list,key,keyValue) {
        for(var i = 0;i<list.length;i++){
            if(list[i][key] == keyValue){
                return list[i];
            }
        }
        return null;
    }


});